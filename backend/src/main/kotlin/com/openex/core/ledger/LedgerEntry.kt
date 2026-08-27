package com.openex.core.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class EntryDirection { CREDIT, DEBIT }

@Entity
@Table(name = "ledger_entries")
data class LedgerEntry(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "transaction_id", nullable = false)
    val transactionId: UUID,
    @Column(name = "account_id", nullable = false)
    val accountId: UUID,
    @Column(nullable = false, precision = 18, scale = 8)
    val amount: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: EntryDirection,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
