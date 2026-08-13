package com.openex.core.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long
) {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    fun extractEmail(token: String): String = extractClaims(token).subject

    fun isTokenValid(token: String, email: String): Boolean {
        val extractedEmail = extractEmail(token)
        return extractedEmail == email && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean =
        extractClaims(token).expiration.before(Date())

    private fun extractClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}