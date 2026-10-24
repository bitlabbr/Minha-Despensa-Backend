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

package com.bitlabbr.minhadespensa.common.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val accessLogger = LoggerFactory.getLogger("com.bitlabbr.minhadespensa.http.AccessLog")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        // Ignora recursos estáticos e endpoints do Swagger para não poluir os logs de negócio
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".ico") ||
                path.endsWith(".png")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val method = request.method
        val uri = request.requestURI
        val query = request.queryString?.let { "?$it" } ?: ""
        val clientIp = extractClientIp(request)

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val status = response.status

            val authHeader = request.getHeader("Authorization")
            val authTag = when {
                authHeader == null -> "no-token"
                authHeader.startsWith("Bearer ") -> "bearer"
                else -> "non-bearer"
            }
            val houseTag = request.getHeader("X-Household-Id")?.let { "house=$it" } ?: "no-house"

            val logMessage = "[HTTP] $method $uri$query [$authTag, $houseTag] - Status: $status (${duration}ms) - Client: $clientIp"

            when {
                status >= 500 -> accessLogger.error(logMessage)
                status >= 400 -> accessLogger.warn(logMessage)
                else -> accessLogger.info(logMessage)
            }
        }
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",").first().trim()
        }
        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }
        return request.remoteAddr ?: "unknown"
    }
}
