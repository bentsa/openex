package com.openex.core.ledger

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID> {
    fun findByUserId(userId: UUID): List<Account>

    fun findByUserIdAndCurrency(
        userId: UUID,
        currency: String,
    ): Account?
}
