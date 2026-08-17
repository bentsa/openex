package com.openex.core.orders

import com.openex.core.idempotency.IdempotencyService
import com.openex.core.ledger.AccountRepository
import com.openex.core.matching.MatchingEngineService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val matchingEngineService: MatchingEngineService,
    private val accountRepository: AccountRepository,
    private val idempotencyService: IdempotencyService
) {

    @PostMapping
    fun createOrder(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        return idempotencyService.executeIdempotently(
            key = idempotencyKey,
            responseType = OrderResponse::class.java
        ) {
            accountRepository.findById(request.accountId)
                .orElseThrow { IllegalArgumentException("Account ${request.accountId} does not exist") }

            require(request.quantity > java.math.BigDecimal.ZERO) { "Quantity must be positive" }

            if (request.orderType == OrderType.LIMIT) {
                requireNotNull(request.price) { "Limit orders require a price" }
                require(request.price > java.math.BigDecimal.ZERO) { "Price must be positive" }
            }

            val order = matchingEngineService.submitOrder(
                Order(
                    accountId = request.accountId,
                    side = request.side,
                    orderType = request.orderType,
                    price = request.price,
                    quantity = request.quantity
                )
            )

            ResponseEntity.status(HttpStatus.CREATED).body(order.toResponse())
        }
    }
}