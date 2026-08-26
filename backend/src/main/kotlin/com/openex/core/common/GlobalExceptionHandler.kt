package com.openex.core.common

import com.openex.core.ledger.UnbalancedTransactionException
import com.openex.core.orders.ForbiddenAccountAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val status: Int, val error: String, val message: String?)

/**
 * Maps domain exceptions to correct HTTP status codes. Without this, every
 * IllegalArgumentException / IllegalStateException thrown by a service bubbles
 * up as an opaque 500, which is both a poor client experience and technically
 * incorrect REST behavior (client errors should be 4xx, not 5xx).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenAccountAccessException::class)
    fun handleForbidden(ex: ForbiddenAccountAccessException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.message))

    @ExceptionHandler(UnbalancedTransactionException::class)
    fun handleUnbalanced(ex: UnbalancedTransactionException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request", message))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError(HttpStatus.CONFLICT.value(), "Conflict", ex.message))
}
