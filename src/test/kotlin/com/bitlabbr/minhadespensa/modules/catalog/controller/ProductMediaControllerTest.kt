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
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductMediaEntity
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductMediaService
import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
import com.bitlabbr.minhadespensa.modules.household.security.HouseholdContext
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductMediaController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class ProductMediaControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var productMediaService: ProductMediaService

    @MockitoBean
    private lateinit var householdService: HouseholdService

    @Test
    fun `PUT media should save image and return sha256`() {
        val bytes = "fake-webp-image".toByteArray()
        val mockMedia = ProductMediaEntity(
            householdId = "house-1",
            productId = "prod-1",
            contentType = "image/webp",
            sha256Hash = "abc123hash",
            imageBytes = bytes
        )

        whenever(householdService.requireHouseholdAccess("user-1", "house-1"))
            .thenReturn(HouseholdContext("house-1", "user-1", HouseholdRole.OWNER))
        whenever(householdService.nextSyncVersion("house-1")).thenReturn(3L)
        whenever(productMediaService.saveMedia(eq("house-1"), eq("prod-1"), any(), any(), eq(3L)))
            .thenReturn(mockMedia)

        mockMvc.perform(
            put("/api/v1/media/products/prod-1")
                .with(jwt().jwt { it.subject("user-1") })
                .header("X-Household-Id", "house-1")
                .contentType("image/webp")
                .content(bytes)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sha256Hash").value("abc123hash"))
    }

    @Test
    fun `GET media should return image bytes and ETag`() {
        val bytes = "fake-webp-image".toByteArray()
        val mockMedia = ProductMediaEntity(
            householdId = "house-1",
            productId = "prod-1",
            contentType = "image/webp",
            sha256Hash = "abc123hash",
            imageBytes = bytes
        )

        whenever(householdService.requireHouseholdAccess("user-1", "house-1"))
            .thenReturn(HouseholdContext("house-1", "user-1", HouseholdRole.MEMBER))
        whenever(productMediaService.getMedia("house-1", "prod-1")).thenReturn(mockMedia)

        mockMvc.perform(
            get("/api/v1/media/products/prod-1")
                .with(jwt().jwt { it.subject("user-1") })
                .header("X-Household-Id", "house-1")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("ETag", "\"abc123hash\""))
            .andExpect(content().contentType(MediaType.parseMediaType("image/webp")))
            .andExpect(content().bytes(bytes))
    }

    @Test
    fun `GET media with matching If-None-Match should return 304 Not Modified`() {
        val mockMedia = ProductMediaEntity(
            householdId = "house-1",
            productId = "prod-1",
            sha256Hash = "abc123hash",
            imageBytes = "fake".toByteArray()
        )

        whenever(householdService.requireHouseholdAccess("user-1", "house-1"))
            .thenReturn(HouseholdContext("house-1", "user-1", HouseholdRole.MEMBER))
        whenever(productMediaService.getMedia("house-1", "prod-1")).thenReturn(mockMedia)

        mockMvc.perform(
            get("/api/v1/media/products/prod-1")
                .with(jwt().jwt { it.subject("user-1") })
                .header("X-Household-Id", "house-1")
                .header("If-None-Match", "abc123hash")
        )
            .andExpect(status().isNotModified)
    }

    @Test
    fun `DELETE media should delete image and return success`() {
        whenever(householdService.requireHouseholdAccess("user-1", "house-1"))
            .thenReturn(HouseholdContext("house-1", "user-1", HouseholdRole.MEMBER))
        whenever(householdService.nextSyncVersion("house-1")).thenReturn(4L)

        mockMvc.perform(
            delete("/api/v1/media/products/prod-1")
                .with(jwt().jwt { it.subject("user-1") })
                .header("X-Household-Id", "house-1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(productMediaService).deleteMedia("house-1", "prod-1", 4L)
    }
}
