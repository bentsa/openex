package com.openex.core.matching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "trades")
data class Trade(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "buy_order_id", nullable = false)
    val buyOrderId: UUID,

    @Column(name = "sell_order_id", nullable = false)
    val sellOrderId: UUID,

    @Column(nullable = false, precision = 18, scale = 8)
    val price: BigDecimal,

    @Column(nullable = false, precision = 18, scale = 8)
    val quantity: BigDecimal,

    @Column(name = "executed_at")
    val executedAt: LocalDateTime = LocalDateTime.now()
)