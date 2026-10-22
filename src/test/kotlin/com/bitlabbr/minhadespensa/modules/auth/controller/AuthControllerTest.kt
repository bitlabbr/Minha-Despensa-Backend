/*
 *   Copyright (c) 2026 Willian Santos
 *
 *   This work is licensed under the Creative Commons
 *   Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0).
 *
 *   You are free to:
 *     - Share  — copy and redistribute the material in any medium or format
 *     - Adapt  — remix, transform, and build upon the material
 *
 *   Under the following terms:
 *     - Attribution    — You must give appropriate credit, provide a link to
 *                        the license, and indicate if changes were made.
 *     - NonCommercial  — You may not use the material for commercial purposes.
 *
 *   Owner rights:
 *     - Willian Santos retains all commercial rights.
 *    - The copyright holder may use, sell, sublicense, or relicense this
 *       work under different terms at any time.
 *
 *   Full license: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package com.bitlabbr.minhadespensa.modules.auth.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.auth.dto.AuthResponseDto
import com.bitlabbr.minhadespensa.modules.auth.dto.HouseholdSummaryDto
import com.bitlabbr.minhadespensa.modules.auth.dto.UserSummaryDto
import com.bitlabbr.minhadespensa.modules.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @Test
    fun `POST register should return 201 Created with auth tokens`() {
        val payload = """
            {
                "name": "Willian Santos",
                "email": "willian@test.com",
                "password": "strongPassword123"
            }
        """.trimIndent()

        val authResponse = AuthResponseDto(
            accessToken = "test-jwt-token",
            refreshToken = "test-refresh-token",
            expiresInSeconds = 3600,
            user = UserSummaryDto(id = "user-1", name = "Willian Santos", email = "willian@test.com", defaultHouseholdId = "house-1"),
            activeHousehold = HouseholdSummaryDto(id = "house-1", name = "Despensa de Willian", inviteCode = "CASA-ABC12345", role = "OWNER")
        )

        whenever(authService.register(any())).thenReturn(authResponse)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("test-jwt-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
            .andExpect(jsonPath("$.data.user.email").value("willian@test.com"))
            .andExpect(jsonPath("$.data.activeHousehold.inviteCode").value("CASA-ABC12345"))
    }

    @Test
    fun `POST register with invalid email should return 400 Bad Request`() {
        val payload = """
            {
                "name": "Willian Santos",
                "email": "not-an-email",
                "password": "strongPassword123"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST login should return 200 OK with auth tokens`() {
        val payload = """
            {
                "email": "willian@test.com",
                "password": "strongPassword123"
            }
        """.trimIndent()

        val authResponse = AuthResponseDto(
            accessToken = "login-jwt-token",
            refreshToken = "login-refresh-token",
            expiresInSeconds = 3600,
            user = UserSummaryDto(id = "user-1", name = "Willian Santos", email = "willian@test.com", defaultHouseholdId = "house-1"),
            activeHousehold = HouseholdSummaryDto(id = "house-1", name = "Despensa de Willian", inviteCode = "CASA-ABC12345", role = "OWNER")
        )

        whenever(authService.login(any())).thenReturn(authResponse)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("login-jwt-token"))
            .andExpect(jsonPath("$.data.user.email").value("willian@test.com"))
    }

    @Test
    fun `POST refresh should return 200 OK with new tokens`() {
        val payload = """
            {
                "refreshToken": "valid-refresh-token"
            }
        """.trimIndent()

        val authResponse = AuthResponseDto(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresInSeconds = 3600,
            user = UserSummaryDto(id = "user-1", name = "Willian Santos", email = "willian@test.com", defaultHouseholdId = "house-1")
        )

        whenever(authService.refreshToken(any())).thenReturn(authResponse)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
    }
}
