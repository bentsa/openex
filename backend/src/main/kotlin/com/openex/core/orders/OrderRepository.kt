package com.openex.core.orders

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByAccountId(accountId: UUID): List<Order>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT o FROM Order o WHERE o.side = :side AND o.status IN " +
            "(com.openex.core.orders.OrderStatus.OPEN, com.openex.core.orders.OrderStatus.PARTIALLY_FILLED) " +
            "ORDER BY o.createdAt ASC",
    )
    fun findMatchableForUpdate(side: OrderSide): List<Order>
}
