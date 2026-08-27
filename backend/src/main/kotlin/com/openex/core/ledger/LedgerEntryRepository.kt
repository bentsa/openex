package com.openex.core.ledger

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LedgerEntryRepository : JpaRepository<LedgerEntry, UUID> {
    fun findByTransactionId(transactionId: UUID): List<LedgerEntry>

    fun findByAccountId(accountId: UUID): List<LedgerEntry>
}
