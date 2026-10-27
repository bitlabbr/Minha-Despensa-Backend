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
import com.bitlabbr.minhadespensa.modules.support.dto.SupportAdminLoginResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class SupportAuthService(
    private val jwtTokenService: JwtTokenService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.support.admin.username:admin}")
    private val adminUsername: String,
    @Value("\${app.support.admin.password:admin}")
    private val adminPassword: String
) {

    private val logger = LoggerFactory.getLogger(SupportAuthService::class.java)

    fun login(request: SupportAdminLoginRequest): SupportAdminLoginResponse {
        val cleanUsername = request.username.trim()

        val usernameMatches = cleanUsername.equals(adminUsername, ignoreCase = false)
        val passwordMatches = if (adminPassword.startsWith("\$2a\$") || adminPassword.startsWith("\$2b\$")) {
            passwordEncoder.matches(request.password, adminPassword)
        } else {
            request.password == adminPassword
        }

        if (!usernameMatches || !passwordMatches) {
            logger.warn("[SupportAuth:Failed] Unauthorized admin login attempt for username='{}'", cleanUsername)
            throw UnauthorizedException("Credenciais de administrador inválidas.")
        }

        val (accessToken, expiresInSeconds) = jwtTokenService.generateAdminAccessToken(adminUsername)

        logger.info("[SupportAuth:Success] System admin '{}' authenticated successfully", adminUsername)

        return SupportAdminLoginResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            username = adminUsername,
            expiresInSeconds = expiresInSeconds,
            role = "SYSTEM_ADMIN"
        )
    }
}
