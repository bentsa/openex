package com.openex.core.orders

import java.math.BigDecimal
import java.util.UUID

data class CreateOrderRequest(
    val accountId: UUID,
    val side: OrderSide,
    val orderType: OrderType,
    val price: BigDecimal? = null,
    val quantity: BigDecimal,
)

data class OrderResponse(
    val id: UUID,
    val accountId: UUID,
    val side: OrderSide,
    val orderType: OrderType,
    val price: BigDecimal?,
    val quantity: BigDecimal,
    val filledQuantity: BigDecimal,
    val status: OrderStatus,
)

fun Order.toResponse() =
    OrderResponse(
        id = id,
        accountId = accountId,
        side = side,
        orderType = orderType,
        price = price,
        quantity = quantity,
        filledQuantity = filledQuantity,
        status = status,
    )
