package com.openex.core.wallet

import com.fasterxml.jackson.databind.ObjectMapper
import com.openex.core.auth.JwtService
import com.openex.core.auth.User
import com.openex.core.auth.UserRepository
import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.ledger.LedgerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WalletControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var ledgerService: LedgerService

    /** Registers a fresh user and returns (JWT, that user's USD account). */
    private fun authTokenWithAccount(): Pair<String, Account> {
        val email = "wallettest-${UUID.randomUUID()}@openex.com"
        val user =
            userRepository.save(
                User(email = email, passwordHash = passwordEncoder.encode("SecurePass123!")),
            )
        val account = accountRepository.save(Account(userId = user.id, currency = "USD"))
        return jwtService.generateToken(email) to account
    }

    @Test
    fun `deposit credits the account and the balance is reflected in the ledger`() {
        val (token, account) = authTokenWithAccount()
        val request = DepositRequest(accountId = account.id, amount = BigDecimal("500.00"))

        mockMvc.perform(
            post("/api/wallets/deposit")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.newBalance").value(500.00))

        // Not just trusting the response body - confirm the ledger itself agrees.
        assertEquals(BigDecimal("500.00"), ledgerService.getBalance(account.id))
    }

    @Test
    fun `two deposits accumulate correctly in the ledger`() {
        val (token, account) = authTokenWithAccount()

        listOf("100.00", "250.50").forEach { amount ->
            mockMvc.perform(
                post("/api/wallets/deposit")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            DepositRequest(accountId = account.id, amount = BigDecimal(amount)),
                        ),
                    ),
            ).andExpect(status().isOk)
        }

        assertEquals(BigDecimal("350.50"), ledgerService.getBalance(account.id))
    }

    @Test
    fun `deposit to a nonexistent account is rejected`() {
        val (token, _) = authTokenWithAccount()
        val request = DepositRequest(accountId = UUID.randomUUID(), amount = BigDecimal("100.00"))

        mockMvc.perform(
            post("/api/wallets/deposit")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `deposit without a JWT is rejected`() {
        val (_, account) = authTokenWithAccount()
        val request = DepositRequest(accountId = account.id, amount = BigDecimal("100.00"))

        mockMvc.perform(
            post("/api/wallets/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `getWallets returns balances for all supported currencies after a deposit`() {
        val (token, account) = authTokenWithAccount()

        mockMvc.perform(
            post("/api/wallets/deposit")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        DepositRequest(accountId = account.id, amount = BigDecimal("42.00")),
                    ),
                ),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/wallets")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.currency == 'USD')].balance").value(42.00))
            .andExpect(jsonPath("$[?(@.currency == 'BTC')].balance").value(0))
    }
}