package com.openex.core.orders

import com.fasterxml.jackson.databind.ObjectMapper
import com.openex.core.auth.JwtService
import com.openex.core.auth.User
import com.openex.core.auth.UserRepository
import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    private fun authToken(): String {
        val email = "ordertest-${UUID.randomUUID()}@openex.com"
        userRepository.save(
            User(email = email, passwordHash = passwordEncoder.encode("SecurePass123!"))
        )
        return jwtService.generateToken(email)
    }

    @Test
    fun `creates a limit order successfully`() {
        val account = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))
        val request = CreateOrderRequest(
            accountId = account.id,
            side = OrderSide.BUY,
            orderType = OrderType.LIMIT,
            price = BigDecimal("50000.00"),
            quantity = BigDecimal("0.5")
        )

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer ${authToken()}")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.quantity").value(0.5))
    }

    @Test
    fun `duplicate submission with same idempotency key returns cached response, not a new order`() {
        val account = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))
        val request = CreateOrderRequest(
            accountId = account.id,
            side = OrderSide.BUY,
            orderType = OrderType.MARKET,
            quantity = BigDecimal("1.0")
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val token = authToken()

        val firstResponse = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val secondResponse = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        assertEquals(firstResponse, secondResponse, "Cached response must match exactly")
        assertEquals(1, orderRepository.findByAccountId(account.id).size, "Only one order should exist")
    }

    @Test
    fun `missing idempotency key is rejected`() {
        val account = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))
        val request = CreateOrderRequest(
            accountId = account.id,
            side = OrderSide.SELL,
            orderType = OrderType.MARKET,
            quantity = BigDecimal("1.0")
        )

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer ${authToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }
}