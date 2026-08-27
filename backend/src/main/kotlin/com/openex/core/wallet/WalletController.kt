package com.openex.core.wallet

import com.openex.core.auth.UserRepository
import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.ledger.EntryDirection
import com.openex.core.ledger.LedgerPosting
import com.openex.core.ledger.LedgerService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/wallets")
class WalletController(
    private val ledgerService: LedgerService,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
) {
    companion object {
        val SYSTEM_ACCOUNT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SUPPORTED_CURRENCIES = listOf("USD", "BTC")
    }

    @GetMapping
    fun getWallets(authentication: Authentication): ResponseEntity<List<WalletBalanceResponse>> {
        val user =
            userRepository.findByEmail(authentication.name)
                ?: return ResponseEntity.notFound().build()

        val balances =
            SUPPORTED_CURRENCIES.map { currency ->
                val account = findOrCreateAccount(user.id, currency)
                val balance = ledgerService.getBalance(account.id)
                WalletBalanceResponse(account.id, currency, balance)
            }

        return ResponseEntity.ok(balances)
    }

    @PostMapping("/deposit")
    fun deposit(
        @RequestBody request: DepositRequest,
    ): ResponseEntity<DepositResponse> {
        ensureSystemAccountExists()

        accountRepository.findById(request.accountId)
            .orElseThrow { IllegalArgumentException("Account ${request.accountId} does not exist") }

        val transactionId =
            ledgerService.postTransaction(
                listOf(
                    LedgerPosting(SYSTEM_ACCOUNT_ID, request.amount, EntryDirection.DEBIT),
                    LedgerPosting(request.accountId, request.amount, EntryDirection.CREDIT),
                ),
            )

        val newBalance = ledgerService.getBalance(request.accountId)

        return ResponseEntity.ok(
            DepositResponse(transactionId, request.accountId, newBalance),
        )
    }

    private fun findOrCreateAccount(
        userId: UUID,
        currency: String,
    ): Account {
        return accountRepository.findByUserIdAndCurrency(userId, currency)
            ?: accountRepository.save(Account(userId = userId, currency = currency))
    }

    private fun ensureSystemAccountExists() {
        if (!accountRepository.existsById(SYSTEM_ACCOUNT_ID)) {
            accountRepository.save(
                Account(id = SYSTEM_ACCOUNT_ID, userId = SYSTEM_ACCOUNT_ID, currency = "USD"),
            )
        }
    }
}

data class WalletBalanceResponse(
    val accountId: UUID,
    val currency: String,
    val balance: BigDecimal,
)
