package com.openex.core.idempotency

import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyKeyRepository : JpaRepository<IdempotencyKey, String>