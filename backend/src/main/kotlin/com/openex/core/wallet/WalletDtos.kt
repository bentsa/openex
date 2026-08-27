package com.openex.core.wallet

import java.math.BigDecimal
import java.util.UUID

data class DepositRequest(
    val accountId: UUID,
    val amount: BigDecimal,
)

data class DepositResponse(
    val transactionId: UUID,
    val accountId: UUID,
    val newBalance: BigDecimal,
)
