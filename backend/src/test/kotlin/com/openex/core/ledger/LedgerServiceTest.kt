package com.openex.core.ledger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@Transactional // rolls back each test automatically after it runs
class LedgerServiceTest {
    @Autowired
    lateinit var ledgerService: LedgerService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var ledgerEntryRepository: LedgerEntryRepository

    private fun createAccount(): Account = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))

    @Test
    fun `balanced transaction posts successfully and entries sum to zero`() {
        val accountA = createAccount()
        val accountB = createAccount()

        val txId =
            ledgerService.postTransaction(
                listOf(
                    LedgerPosting(accountA.id, BigDecimal("100.00"), EntryDirection.DEBIT),
                    LedgerPosting(accountB.id, BigDecimal("100.00"), EntryDirection.CREDIT),
                ),
            )

        val entries = ledgerEntryRepository.findByTransactionId(txId)
        val sum =
            entries.sumOf {
                if (it.direction == EntryDirection.DEBIT) it.amount.negate() else it.amount
            }

        assertEquals(0, sum.compareTo(BigDecimal.ZERO), "Ledger entries must sum to zero")
        assertEquals(2, entries.size)
    }

    @Test
    fun `unbalanced transaction is rejected and nothing is persisted`() {
        val accountA = createAccount()
        val accountB = createAccount()

        assertThrows(UnbalancedTransactionException::class.java) {
            ledgerService.postTransaction(
                listOf(
                    LedgerPosting(accountA.id, BigDecimal("100.00"), EntryDirection.DEBIT),
                    LedgerPosting(accountB.id, BigDecimal("50.00"), EntryDirection.CREDIT),
                ),
            )
        }

        assertEquals(0, ledgerEntryRepository.findByAccountId(accountA.id).size)
        assertEquals(0, ledgerEntryRepository.findByAccountId(accountB.id).size)
    }

    @Test
    fun `balance is correctly derived from ledger entries`() {
        val account = createAccount()
        val counterparty = createAccount()

        ledgerService.postTransaction(
            listOf(
                LedgerPosting(account.id, BigDecimal("200.00"), EntryDirection.CREDIT),
                LedgerPosting(counterparty.id, BigDecimal("200.00"), EntryDirection.DEBIT),
            ),
        )

        assertEquals(0, ledgerService.getBalance(account.id).compareTo(BigDecimal("200.00")))
    }
}
