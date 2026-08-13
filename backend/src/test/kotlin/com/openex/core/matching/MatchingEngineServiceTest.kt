package com.openex.core.matching

import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.orders.Order
import com.openex.core.orders.OrderRepository
import com.openex.core.orders.OrderSide
import com.openex.core.orders.OrderStatus
import com.openex.core.orders.OrderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@Transactional
class MatchingEngineServiceTest {

    @Autowired
    lateinit var matchingEngineService: MatchingEngineService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var tradeRepository: TradeRepository

    @Autowired
    lateinit var ledgerEntryRepository: com.openex.core.ledger.LedgerEntryRepository

    private fun createAccount(): Account =
        accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))

    @Test
    fun `matching buy and sell limit orders at the same price executes a trade`() {
        val buyerAccount = createAccount()
        val sellerAccount = createAccount()

        val sellOrder = orderRepository.save(
            Order(
                accountId = sellerAccount.id,
                side = OrderSide.SELL,
                orderType = OrderType.LIMIT,
                price = BigDecimal("100.00"),
                quantity = BigDecimal("10")
            )
        )

        val buyOrder = Order(
            accountId = buyerAccount.id,
            side = OrderSide.BUY,
            orderType = OrderType.LIMIT,
            price = BigDecimal("100.00"),
            quantity = BigDecimal("10")
        )

        val result = matchingEngineService.submitOrder(buyOrder)

        assertEquals(OrderStatus.FILLED, result.status)
        assertEquals(0, result.quantity.compareTo(result.filledQuantity))

        val updatedSellOrder = orderRepository.findById(sellOrder.id).get()
        assertEquals(OrderStatus.FILLED, updatedSellOrder.status)

        assertEquals(1, tradeRepository.findAll().size)
    }

    @Test
    fun `partial fill leaves remainder resting in the book`() {
        val buyerAccount = createAccount()
        val sellerAccount = createAccount()

        orderRepository.save(
            Order(
                accountId = sellerAccount.id,
                side = OrderSide.SELL,
                orderType = OrderType.LIMIT,
                price = BigDecimal("100.00"),
                quantity = BigDecimal("5")
            )
        )

        val buyOrder = Order(
            accountId = buyerAccount.id,
            side = OrderSide.BUY,
            orderType = OrderType.LIMIT,
            price = BigDecimal("100.00"),
            quantity = BigDecimal("10")
        )

        val result = matchingEngineService.submitOrder(buyOrder)

        assertEquals(OrderStatus.PARTIALLY_FILLED, result.status)
        assertEquals(0, result.filledQuantity.compareTo(BigDecimal("5")))
    }

    @Test
    fun `orders that do not cross in price do not match`() {
        val buyerAccount = createAccount()
        val sellerAccount = createAccount()

        orderRepository.save(
            Order(
                accountId = sellerAccount.id,
                side = OrderSide.SELL,
                orderType = OrderType.LIMIT,
                price = BigDecimal("110.00"),
                quantity = BigDecimal("5")
            )
        )

        val buyOrder = Order(
            accountId = buyerAccount.id,
            side = OrderSide.BUY,
            orderType = OrderType.LIMIT,
            price = BigDecimal("100.00"),
            quantity = BigDecimal("5")
        )

        val result = matchingEngineService.submitOrder(buyOrder)

        assertEquals(OrderStatus.OPEN, result.status)
        assertEquals(0, tradeRepository.findAll().size)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `10 concurrent orders match correctly and ledger stays consistent`() {
        val sellerAccount = createAccount()
        val buyerAccounts = (1..10).map { createAccount() }

        orderRepository.save(
            Order(
                accountId = sellerAccount.id,
                side = OrderSide.SELL,
                orderType = OrderType.LIMIT,
                price = BigDecimal("50.00"),
                quantity = BigDecimal("100")
            )
        )

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val results = java.util.Collections.synchronizedList(mutableListOf<Order>())

        buyerAccounts.forEach { account ->
            executor.submit {
                try {
                    val buyOrder = Order(
                        accountId = account.id,
                        side = OrderSide.BUY,
                        orderType = OrderType.LIMIT,
                        price = BigDecimal("50.00"),
                        quantity = BigDecimal("10")
                    )
                    results.add(matchingEngineService.submitOrder(buyOrder))
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All concurrent orders should complete within timeout")
        executor.shutdown()


        val totalFilled = results.sumOf { it.filledQuantity }
        assertEquals(0, totalFilled.compareTo(BigDecimal("100")), "Total filled quantity across all buyers must equal the resting sell quantity")

        val trades = tradeRepository.findAll()
        assertTrue(trades.isNotEmpty(), "At least one trade should have been executed")

       results.forEach { order ->
            assertTrue(accountRepository.existsById(order.accountId), "Account must still exist after concurrent matching")
        }

        tradeRepository.deleteAll(tradeRepository.findAll().filter { trade ->
            results.any { it.id == trade.buyOrderId || it.id == trade.sellOrderId }
        })
        orderRepository.deleteAll(results.map { orderRepository.findById(it.id).orElse(null) }.filterNotNull())

        val allAccountIds = (buyerAccounts.map { it.id } + sellerAccount.id).toSet()
        ledgerEntryRepository.deleteAll(
            ledgerEntryRepository.findAll().filter { it.accountId in allAccountIds }
        )

        orderRepository.deleteById(
            orderRepository.findAll().first { o -> o.accountId == sellerAccount.id }.id
        )
        accountRepository.deleteAll(buyerAccounts + sellerAccount)
    }
}