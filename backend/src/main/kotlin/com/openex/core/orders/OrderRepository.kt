package com.openex.core.orders

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByAccountId(accountId: UUID): List<Order>
}