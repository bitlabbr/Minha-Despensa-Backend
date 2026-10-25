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

package com.bitlabbr.minhadespensa.modules.support.service

import com.bitlabbr.minhadespensa.common.exception.UnauthorizedException
import com.bitlabbr.minhadespensa.modules.auth.security.JwtTokenService
import com.bitlabbr.minhadespensa.modules.support.dto.SupportAdminLoginRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class SupportAuthServiceTest {

    private val jwtTokenService: JwtTokenService = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var supportAuthService: SupportAuthService

    @BeforeEach
    fun setUp() {
        supportAuthService = SupportAuthService(
            jwtTokenService = jwtTokenService,
            passwordEncoder = passwordEncoder,
            adminUsername = "admin",
            adminPassword = "securePassword123"
        )
    }

    @Test
    fun `should login successfully with valid admin credentials`() {
        whenever(jwtTokenService.generateAdminAccessToken("admin")).thenReturn(Pair("mocked-admin-token", 86400L))

        val response = supportAuthService.login(
            SupportAdminLoginRequest(username = "admin", password = "securePassword123")
        )

        assertNotNull(response)
        assertEquals("mocked-admin-token", response.accessToken)
        assertEquals("admin", response.username)
        assertEquals("SYSTEM_ADMIN", response.role)
        assertEquals(86400L, response.expiresInSeconds)
    }

    @Test
    fun `should throw UnauthorizedException when password is incorrect`() {
        assertThrows<UnauthorizedException> {
            supportAuthService.login(
                SupportAdminLoginRequest(username = "admin", password = "wrongPassword")
            )
        }
    }

    @Test
    fun `should throw UnauthorizedException when username is incorrect`() {
        assertThrows<UnauthorizedException> {
            supportAuthService.login(
                SupportAdminLoginRequest(username = "wrongAdmin", password = "securePassword123")
            )
        }
    }
}
