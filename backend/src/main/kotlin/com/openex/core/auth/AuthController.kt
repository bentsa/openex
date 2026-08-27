package com.openex.core.auth

import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) {
    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
    ): ResponseEntity<AuthResponse> {
        if (userRepository.findByEmail(request.email) != null) {
            return ResponseEntity.badRequest().build()
        }

        val user =
            User(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
            )
        userRepository.save(user)

        val token = jwtService.generateToken(user.email)
        return ResponseEntity.ok(AuthResponse(token, user.email))
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<AuthResponse> {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password),
        )

        val token = jwtService.generateToken(request.email)
        return ResponseEntity.ok(AuthResponse(token, request.email))
    }
}
