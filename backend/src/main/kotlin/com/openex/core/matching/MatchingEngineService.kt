package com.openex.core.matching

import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.ledger.EntryDirection
import com.openex.core.ledger.LedgerPosting
import com.openex.core.ledger.LedgerService
import com.openex.core.orders.Order
import com.openex.core.orders.OrderRepository
import com.openex.core.orders.OrderSide
import com.openex.core.orders.OrderStatus
import com.openex.core.orders.OrderType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class MatchingEngineService(
    private val orderMatcher: OrderMatcher,
) {
    private val matchingLock = Any()

    fun submitOrder(incomingOrder: Order): Order {
        synchronized(matchingLock) {
            return orderMatcher.match(incomingOrder)
        }
    }
}

@Service
class OrderMatcher(
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository,
    private val accountRepository: AccountRepository,
    private val ledgerService: LedgerService,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    companion object {
        // Single-pair simulated exchange: every order's account trades against
        // this asset (BASE) priced in this currency (QUOTE). Matches the pair
        // WalletController provisions accounts for.
        const val BASE_CURRENCY = "BTC"
        const val QUOTE_CURRENCY = "USD"
    }

    @Transactional
    fun match(incomingOrder: Order): Order {
        var current = orderRepository.save(incomingOrder)
        var remaining = current.quantity

        val restingOrders = findMatchableRestingOrders(current)

        for (resting in restingOrders) {
            if (remaining <= BigDecimal.ZERO) break

            val restingRemaining = resting.quantity.subtract(resting.filledQuantity)
            if (restingRemaining <= BigDecimal.ZERO) continue

            if (!pricesCross(current, resting)) continue

            val tradeQuantity = remaining.min(restingRemaining)
            val tradePrice =
                resting.price
                    ?: current.price
                    ?: throw IllegalStateException("No price available to execute trade")

            executeTrade(current, resting, tradePrice, tradeQuantity)

            remaining = remaining.subtract(tradeQuantity)

            val updatedRestingFilled = resting.filledQuantity.add(tradeQuantity)
            val updatedRestingStatus =
                if (updatedRestingFilled.compareTo(resting.quantity) == 0) {
                    OrderStatus.FILLED
                } else {
                    OrderStatus.PARTIALLY_FILLED
                }
            orderRepository.save(
                resting.copy(filledQuantity = updatedRestingFilled, status = updatedRestingStatus),
            )

            val updatedCurrentFilled = current.filledQuantity.add(tradeQuantity)
            current = current.copy(filledQuantity = updatedCurrentFilled)
        }

        val finalStatus =
            when {
                current.filledQuantity.compareTo(current.quantity) == 0 -> OrderStatus.FILLED
                current.filledQuantity > BigDecimal.ZERO -> OrderStatus.PARTIALLY_FILLED
                else -> OrderStatus.OPEN
            }

        val savedOrder = orderRepository.save(current.copy(status = finalStatus))
        broadcastOrderBookSnapshot()
        return savedOrder
    }

    private fun broadcastOrderBookSnapshot() {
        val bids =
            orderRepository.findMatchableForUpdate(OrderSide.BUY)
                .filter { it.price != null }
                .groupBy { it.price!! }
                .map { (price, orders) -> OrderBookLevel(price, orders.sumOf { it.quantity - it.filledQuantity }) }
                .sortedByDescending { it.price }

        val asks =
            orderRepository.findMatchableForUpdate(OrderSide.SELL)
                .filter { it.price != null }
                .groupBy { it.price!! }
                .map { (price, orders) -> OrderBookLevel(price, orders.sumOf { it.quantity - it.filledQuantity }) }
                .sortedBy { it.price }

        messagingTemplate.convertAndSend("/topic/orderbook", OrderBookSnapshot(bids, asks))
    }

    private fun findMatchableRestingOrders(incoming: Order): List<Order> {
        val oppositeSide = if (incoming.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY

        val candidates =
            orderRepository.findMatchableForUpdate(oppositeSide)
                .filter { it.id != incoming.id }

        return if (oppositeSide == OrderSide.SELL) {
            candidates.sortedWith(compareBy({ it.price ?: BigDecimal.ZERO }, { it.createdAt }))
        } else {
            candidates.sortedWith(compareByDescending<Order> { it.price ?: BigDecimal.ZERO }.thenBy { it.createdAt })
        }
    }

    private fun pricesCross(
        incoming: Order,
        resting: Order,
    ): Boolean {
        if (incoming.orderType == OrderType.MARKET) return true

        val incomingPrice = incoming.price ?: return false
        val restingPrice = resting.price ?: return false

        return if (incoming.side == OrderSide.BUY) {
            incomingPrice >= restingPrice
        } else {
            incomingPrice <= restingPrice
        }
    }

    private fun executeTrade(
        current: Order,
        resting: Order,
        price: BigDecimal,
        quantity: BigDecimal,
    ) {
        val buyOrder = if (current.side == OrderSide.BUY) current else resting
        val sellOrder = if (current.side == OrderSide.SELL) current else resting

        tradeRepository.save(
            Trade(
                buyOrderId = buyOrder.id,
                sellOrderId = sellOrder.id,
                price = price,
                quantity = quantity,
            ),
        )

        val notional = price.multiply(quantity)

        // A trade moves TWO assets, not one: the quote currency (USD) flows
        // buyer -> seller, and the base currency (BTC) flows seller -> buyer.
        // Settling only the notional leg (the previous behavior) left the
        // traded asset itself never actually delivered to the buyer.
        val buyerUserId =
            accountRepository.findById(buyOrder.accountId)
                .orElseThrow { IllegalStateException("Account ${buyOrder.accountId} does not exist") }.userId
        val sellerUserId =
            accountRepository.findById(sellOrder.accountId)
                .orElseThrow { IllegalStateException("Account ${sellOrder.accountId} does not exist") }.userId

        val buyerQuoteAccount = findOrCreateAccount(buyerUserId, QUOTE_CURRENCY)
        val buyerBaseAccount = findOrCreateAccount(buyerUserId, BASE_CURRENCY)
        val sellerQuoteAccount = findOrCreateAccount(sellerUserId, QUOTE_CURRENCY)
        val sellerBaseAccount = findOrCreateAccount(sellerUserId, BASE_CURRENCY)

        ledgerService.postTransaction(
            listOf(
                LedgerPosting(buyerQuoteAccount.id, notional, EntryDirection.DEBIT),
                LedgerPosting(sellerQuoteAccount.id, notional, EntryDirection.CREDIT),
                LedgerPosting(sellerBaseAccount.id, quantity, EntryDirection.DEBIT),
                LedgerPosting(buyerBaseAccount.id, quantity, EntryDirection.CREDIT),
            ),
        )
    }

    private fun findOrCreateAccount(
        userId: UUID,
        currency: String,
    ): Account =
        accountRepository.findByUserIdAndCurrency(userId, currency)
            ?: accountRepository.save(Account(userId = userId, currency = currency))
}
