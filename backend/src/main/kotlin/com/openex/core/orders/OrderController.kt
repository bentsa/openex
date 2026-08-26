package com.openex.core.orders

import com.openex.core.auth.UserRepository
import com.openex.core.idempotency.IdempotencyService
import com.openex.core.ledger.AccountRepository
import com.openex.core.matching.MatchingEngineService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

class ForbiddenAccountAccessException(message: String) : RuntimeException(message)

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val matchingEngineService: MatchingEngineService,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val idempotencyService: IdempotencyService,
    private val orderRepository: OrderRepository
) {

    @PostMapping
    fun createOrder(
        authentication: Authentication,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        return idempotencyService.executeIdempotently(
            key = idempotencyKey,
            responseType = OrderResponse::class.java
        ) {
            val account = accountRepository.findById(request.accountId)
                .orElseThrow { IllegalArgumentException("Account ${request.accountId} does not exist") }

            // Ownership check: a JWT only authorizes orders against accounts the
            // caller actually owns, otherwise anyone could drain another user's wallet.
            val callingUser = userRepository.findByEmail(authentication.name)
                ?: throw IllegalStateException("Authenticated user ${authentication.name} not found")
            if (account.userId != callingUser.id) {
                throw ForbiddenAccountAccessException(
                    "Account ${request.accountId} does not belong to the authenticated user"
                )
            }

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

    @GetMapping
    fun getOrders(@RequestParam accountId: UUID): ResponseEntity<List<OrderResponse>> {
        val orders = orderRepository.findByAccountId(accountId)
            .sortedByDescending { it.createdAt }
            .map { it.toResponse() }
        return ResponseEntity.ok(orders)
    }
}