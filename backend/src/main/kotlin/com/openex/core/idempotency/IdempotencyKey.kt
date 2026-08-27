package com.openex.core.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "idempotency_keys")
data class IdempotencyKey(
    @Id
    @Column(name = "idempotency_key")
    val key: String,
    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    val responseBody: String,
    @Column(name = "status_code", nullable = false)
    val statusCode: Int,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
