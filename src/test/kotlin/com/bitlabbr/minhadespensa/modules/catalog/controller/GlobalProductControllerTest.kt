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

package com.bitlabbr.minhadespensa.modules.catalog.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.common.exception.ValidationException
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.GlobalProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.service.GlobalProductService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(GlobalProductController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class GlobalProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var globalProductService: GlobalProductService

    @Test
    fun `GET product by EAN with valid JWT should return 200 and envelope`() {
        val dto = GlobalProductResponseDto(
            id = "global-1",
            ean = "7891000100103",
            name = "Leite Condensado Moça",
            brand = "Nestlé",
            category = "Laticínios",
            measureUnit = MeasureUnit.GRAM,
            netWeight = 395.0,
            source = "USER_SYNC",
            occurrenceCount = 12L,
            updatedAt = 1730761200000L
        )
        whenever(globalProductService.getProductByEan("7891000100103")).thenReturn(dto)

        mockMvc.perform(
            get("/api/v1/catalog/global/7891000100103")
                .with(jwt().jwt { it.subject("user-1") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ean").value("7891000100103"))
            .andExpect(jsonPath("$.data.name").value("Leite Condensado Moça"))
            .andExpect(jsonPath("$.data.brand").value("Nestlé"))
            .andExpect(jsonPath("$.data.measureUnit").value("GRAM"))
            .andExpect(jsonPath("$.data.occurrenceCount").value(12))
    }

    @Test
    fun `GET product by non-existent EAN should return 404 Not Found`() {
        whenever(globalProductService.getProductByEan("7899999999999"))
            .thenThrow(ResourceNotFoundException("Produto global não encontrado."))

        mockMvc.perform(
            get("/api/v1/catalog/global/7899999999999")
                .with(jwt().jwt { it.subject("user-1") })
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `GET product by invalid EAN should return 400 Bad Request`() {
        whenever(globalProductService.getProductByEan("INVALID"))
            .thenThrow(ValidationException("Código EAN inválido"))

        mockMvc.perform(
            get("/api/v1/catalog/global/INVALID")
                .with(jwt().jwt { it.subject("user-1") })
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `GET product by EAN without Bearer Token should return 401 Unauthorized`() {
        mockMvc.perform(
            get("/api/v1/catalog/global/7891000100103")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET search products with valid JWT should return 200 and list of results`() {
        val dto = GlobalProductResponseDto(
            id = "global-1",
            ean = "7891000100103",
            name = "Leite Condensado Moça",
            brand = "Nestlé",
            category = "Laticínios",
            measureUnit = MeasureUnit.GRAM,
            netWeight = 395.0,
            source = "USER_SYNC",
            occurrenceCount = 12L,
            updatedAt = 1730761200000L
        )
        whenever(globalProductService.searchProducts("Leite", 10)).thenReturn(listOf(dto))

        mockMvc.perform(
            get("/api/v1/catalog/global/search")
                .param("query", "Leite")
                .param("limit", "10")
                .with(jwt().jwt { it.subject("user-1") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].ean").value("7891000100103"))
            .andExpect(jsonPath("$.data[0].name").value("Leite Condensado Moça"))
    }
}
