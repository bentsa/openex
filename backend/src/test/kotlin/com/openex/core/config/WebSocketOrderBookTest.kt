package com.openex.core.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openex.core.ledger.Account
import com.openex.core.ledger.AccountRepository
import com.openex.core.matching.MatchingEngineService
import com.openex.core.matching.OrderBookSnapshot
import com.openex.core.matching.TradeRepository
import com.openex.core.orders.Order
import com.openex.core.orders.OrderRepository
import com.openex.core.orders.OrderSide
import com.openex.core.orders.OrderType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketOrderBookTest {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var matchingEngineService: MatchingEngineService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var tradeRepository: TradeRepository

    @Test
    fun `matching an order broadcasts an order book snapshot over the orderbook topic`() {
        val sellerAccount = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))
        val buyerAccount = accountRepository.save(Account(userId = UUID.randomUUID(), currency = "USD"))

        val stompClient = WebSocketStompClient(StandardWebSocketClient())
        stompClient.messageConverter =
            MappingJackson2MessageConverter().apply {
                objectMapper = jacksonObjectMapper()
            }

        val received = LinkedBlockingQueue<OrderBookSnapshot>()

        val session =
            stompClient
                .connectAsync(
                    "ws://localhost:$port/ws/websocket",
                    object : StompSessionHandlerAdapter() {
                        override fun handleException(
                            session: StompSession,
                            command: StompCommand?,
                            headers: StompHeaders,
                            payload: ByteArray,
                            exception: Throwable,
                        ) {
                            println("DEBUG: STOMP EXCEPTION command=$command payload=${String(payload)}")
                            exception.printStackTrace()
                        }

                        override fun handleTransportError(
                            session: StompSession,
                            exception: Throwable,
                        ) {
                            println("DEBUG: TRANSPORT ERROR")
                            exception.printStackTrace()
                        }
                    },
                )
                .get(10, TimeUnit.SECONDS)
        println("DEBUG: session connected = ${session.isConnected}")

        val subscription =
            session.subscribe(
                "/topic/orderbook",
                object : StompFrameHandler {
                    override fun getPayloadType(headers: StompHeaders): Type = OrderBookSnapshot::class.java

                    override fun handleFrame(
                        headers: StompHeaders,
                        payload: Any?,
                    ) {
                        println("DEBUG: frame received, payload=$payload")
                        received.add(payload as OrderBookSnapshot)
                    }
                },
            )
        println("DEBUG: subscription id = ${subscription.subscriptionHeaders.id}")

        Thread.sleep(500) // give the STOMP subscription time to register server-side before we trigger a broadcast

        matchingEngineService.submitOrder(
            Order(
                accountId = sellerAccount.id,
                side = OrderSide.SELL,
                orderType = OrderType.LIMIT,
                price = BigDecimal("75.00"),
                quantity = BigDecimal("5"),
            ),
        )

        matchingEngineService.submitOrder(
            Order(
                accountId = buyerAccount.id,
                side = OrderSide.BUY,
                orderType = OrderType.LIMIT,
                price = BigDecimal("75.00"),
                quantity = BigDecimal("5"),
            ),
        )

        println("DEBUG: session still connected before poll = ${session.isConnected}")
        val snapshot = received.poll(10, TimeUnit.SECONDS)
        println("DEBUG: snapshot after poll = $snapshot")
        assertTrue(snapshot != null, "Expected an order book snapshot to be broadcast over /topic/orderbook")

        session.disconnect()

        // This test's orders/trades commit for real via the embedded server (separate
        // thread from the test's own transaction), so clean them up explicitly or
        // they'll pollute subsequent tests' order books. Accounts are left in place
        // since ledger_entries reference them (FK constraint) and leftover accounts
        // don't affect other tests — only orders/trades matter for order-book state.
        tradeRepository.deleteAll()
        orderRepository.findAll()
            .filter { it.accountId == sellerAccount.id || it.accountId == buyerAccount.id }
            .forEach { orderRepository.deleteById(it.id) }
    }
}
