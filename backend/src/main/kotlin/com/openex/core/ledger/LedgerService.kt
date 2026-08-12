package com.openex.core.ledger

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

class UnbalancedTransactionException(message: String) : RuntimeException(message)

data class LedgerPosting(
    val accountId: UUID,
    val amount: BigDecimal,
    val direction: EntryDirection
)

@Service
class LedgerService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * Posts a set of balanced debit/credit entries as a single atomic transaction.
     * All postings share one transactionId. Debits must equal credits, or the
     * whole operation is rolled back.
     */
    @Transactional
    fun postTransaction(postings: List<LedgerPosting>): UUID {
        require(postings.isNotEmpty()) { "A transaction must contain at least one posting" }

        val transactionId = UUID.randomUUID()

        val totalDebits = postings
            .filter { it.direction == EntryDirection.DEBIT }
            .sumOf { it.amount }
        val totalCredits = postings
            .filter { it.direction == EntryDirection.CREDIT }
            .sumOf { it.amount }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw UnbalancedTransactionException(
                "Debits ($totalDebits) do not equal credits ($totalCredits) for transaction $transactionId"
            )
        }

        postings.forEach { posting ->
            // Confirms the account exists — throws if not, rolling back the whole transaction
            accountRepository.findById(posting.accountId)
                .orElseThrow { IllegalArgumentException("Account ${posting.accountId} does not exist") }

            ledgerEntryRepository.save(
                LedgerEntry(
                    transactionId = transactionId,
                    accountId = posting.accountId,
                    amount = posting.amount,
                    direction = posting.direction
                )
            )
        }

        return transactionId
    }

    /**
     * Computes an account's balance as sum(credits) - sum(debits).
     */
    fun getBalance(accountId: UUID): BigDecimal {
        val entries = ledgerEntryRepository.findByAccountId(accountId)
        val credits = entries.filter { it.direction == EntryDirection.CREDIT }.sumOf { it.amount }
        val debits = entries.filter { it.direction == EntryDirection.DEBIT }.sumOf { it.amount }
        return credits.subtract(debits)
    }
}