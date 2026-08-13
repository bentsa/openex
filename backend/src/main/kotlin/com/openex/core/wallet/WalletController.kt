package com.openex.core.wallet

import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.ledger.EntryDirection
import com.openex.core.ledger.LedgerPosting
import com.openex.core.ledger.LedgerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/wallets")
class WalletController(
    private val ledgerService: LedgerService,
    private val accountRepository: AccountRepository
) {
    companion object {
        // Fixed system liquidity account that "mints" simulated funds on deposit
        val SYSTEM_ACCOUNT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }

    @PostMapping("/deposit")
    fun deposit(@RequestBody request: DepositRequest): ResponseEntity<DepositResponse> {
        ensureSystemAccountExists()

        // Confirm the target account exists too, or fail fast with a clear error
        accountRepository.findById(request.accountId)
            .orElseThrow { IllegalArgumentException("Account ${request.accountId} does not exist") }

        val transactionId = ledgerService.postTransaction(
            listOf(
                LedgerPosting(SYSTEM_ACCOUNT_ID, request.amount, EntryDirection.DEBIT),
                LedgerPosting(request.accountId, request.amount, EntryDirection.CREDIT)
            )
        )

        val newBalance = ledgerService.getBalance(request.accountId)

        return ResponseEntity.ok(
            DepositResponse(transactionId, request.accountId, newBalance)
        )
    }

    private fun ensureSystemAccountExists() {
        if (!accountRepository.existsById(SYSTEM_ACCOUNT_ID)) {
            accountRepository.save(
                Account(id = SYSTEM_ACCOUNT_ID, userId = SYSTEM_ACCOUNT_ID, currency = "USD")
            )
        }
    }
}