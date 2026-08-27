package com.openex.core.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class IdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Executes [action] only if [key] hasn't been seen before. If it has, the
     * previously cached response is returned instead and [action] is never called.
     */
    fun <T> executeIdempotently(
        key: String,
        responseType: Class<T>,
        action: () -> ResponseEntity<T>,
    ): ResponseEntity<T> {
        val existing = idempotencyKeyRepository.findById(key)

        if (existing.isPresent) {
            val cached = existing.get()
            val body = objectMapper.readValue(cached.responseBody, responseType)
            return ResponseEntity.status(cached.statusCode).body(body)
        }

        val response = action()

        idempotencyKeyRepository.save(
            IdempotencyKey(
                key = key,
                responseBody = objectMapper.writeValueAsString(response.body),
                statusCode = response.statusCode.value(),
            ),
        )

        return response
    }
}
