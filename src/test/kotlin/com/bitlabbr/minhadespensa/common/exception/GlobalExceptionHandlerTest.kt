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

package com.bitlabbr.minhadespensa.common.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()
    private val request = MockHttpServletRequest("GET", "/api/v1/test")

    @Test
    fun `should handle UnauthorizedException returning 401 Unauthorized`() {
        val ex = UnauthorizedException("Token inválido ou expirado")
        val response = handler.handleUnauthorized(ex, request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(401, body?.status)
        assertEquals("Token inválido ou expirado", body?.message)
        assertEquals("/api/v1/test", body?.path)
    }

    @Test
    fun `should handle ForbiddenException returning 403 Forbidden`() {
        val ex = ForbiddenException("Usuário não pertence à casa")
        val response = handler.handleForbidden(ex, request)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(403, body?.status)
        assertEquals("Usuário não pertence à casa", body?.message)
        assertEquals("/api/v1/test", body?.path)
    }

    @Test
    fun `should handle ResourceNotFoundException returning 404 Not Found`() {
        val ex = ResourceNotFoundException("Item não encontrado")
        val response = handler.handleResourceNotFound(ex, request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(404, body?.status)
        assertEquals("Item não encontrado", body?.message)
    }

    @Test
    fun `should handle ConflictException returning 409 Conflict`() {
        val ex = ConflictException("EAN já cadastrado")
        val response = handler.handleConflict(ex, request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(409, body?.status)
        assertEquals("EAN já cadastrado", body?.message)
    }
}
