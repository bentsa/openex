package com.openex.core.orders

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class OrderSide { BUY, SELL }
enum class OrderType { LIMIT, MARKET }
enum class OrderStatus { OPEN, PARTIALLY_FILLED, FILLED, CANCELLED }

@Entity
@Table(name = "orders")
data class Order(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: OrderSide,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    val orderType: OrderType,

    @Column(precision = 18, scale = 8)
    val price: BigDecimal? = null,

    @Column(nullable = false, precision = 18, scale = 8)
    val quantity: BigDecimal,

    @Column(name = "filled_quantity", nullable = false, precision = 18, scale = 8)
    val filledQuantity: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrderStatus = OrderStatus.OPEN,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)