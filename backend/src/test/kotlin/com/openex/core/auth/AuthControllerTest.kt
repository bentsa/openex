package com.openex.core.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `register creates a user and returns a valid JWT`() {
        val request = RegisterRequest(email = "newuser@openex.com", password = "SecurePass123!")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("newuser@openex.com"))
    }

    @Test
    fun `register then login succeeds with correct credentials`() {
        val registerRequest = RegisterRequest(email = "logintest@openex.com", password = "SecurePass123!")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isOk)

        val loginRequest = LoginRequest(email = "logintest@openex.com", password = "SecurePass123!")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("logintest@openex.com"))
    }

    @Test
    fun `duplicate registration is rejected`() {
        val request = RegisterRequest(email = "dupe@openex.com", password = "SecurePass123!")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }
}