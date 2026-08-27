package com.openex.core.matching

import java.math.BigDecimal

data class OrderBookLevel(
    val price: BigDecimal,
    val quantity: BigDecimal,
)

data class OrderBookSnapshot(
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
)
