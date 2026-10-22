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

package com.bitlabbr.minhadespensa.modules.auth.security

import com.bitlabbr.minhadespensa.modules.auth.entity.UserEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

@Service
class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.jwt.access-expiration-ms:3600000}")
    private val accessExpirationMs: Long
) {

    fun generateAccessToken(user: UserEntity): Pair<String, Long> {
        val now = Instant.now()
        val expiresAt = now.plusMillis(accessExpirationMs)

        val claims = JwtClaimsSet.builder()
            .issuer("minha-despensa")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.id)
            .claim("email", user.email)
            .claim("name", user.name)
            .build()

        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue

        return Pair(token, accessExpirationMs / 1000)
    }

    fun hashToken(rawToken: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
