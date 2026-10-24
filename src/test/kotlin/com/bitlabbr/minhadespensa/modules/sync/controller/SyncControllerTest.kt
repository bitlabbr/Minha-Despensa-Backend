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

package com.bitlabbr.minhadespensa.modules.sync.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
import com.bitlabbr.minhadespensa.modules.household.security.HouseholdContext
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import com.bitlabbr.minhadespensa.modules.sync.service.SyncService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SyncController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class SyncControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var syncService: SyncService

    @MockitoBean
    private lateinit var householdService: HouseholdService

    private val testUserId = "user-test-uuid"
    private val testHouseholdId = "house-test-uuid"

    @Test
    fun `POST sync push should reject unauthorized requests`() {
        mockMvc.perform(
            post("/api/v1/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST sync push should reject missing X-Household-Id header`() {
        mockMvc.perform(
            post("/api/v1/sync/push")
                .with(jwt().jwt { it.subject(testUserId) })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST sync push should return 200 with totalProcessed and serverVersion`() {
        val mockContext = HouseholdContext(
            householdId = testHouseholdId,
            userId = testUserId,
            role = HouseholdRole.OWNER
        )
        whenever(householdService.requireHouseholdAccess(testUserId, testHouseholdId)).thenReturn(mockContext)

        whenever(syncService.push(eq(testHouseholdId), any())).thenReturn(
            SyncPushResponseDto(
                serverTimestamp = 1727280000000L,
                serverVersion = 42L,
                totalProcessed = 3,
                productIdMappings = mapOf("old-id" to "canonical-id")
            )
        )

        val payload = """
            {
                "products": [
                    { "name": "Arroz Integral", "category": "Grãos" }
                ],
                "pantryItems": [],
                "shoppingLists": [],
                "shoppingItems": [],
                "priceEntries": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/sync/push")
                .with(jwt().jwt { it.subject(testUserId) })
                .header("X-Household-Id", testHouseholdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalProcessed").value(3))
            .andExpect(jsonPath("$.data.serverVersion").value(42))
            .andExpect(jsonPath("$.data.productIdMappings.old-id").value("canonical-id"))
    }

    @Test
    fun `GET sync pull should return 200 with deltas and current serverVersion`() {
        val mockContext = HouseholdContext(
            householdId = testHouseholdId,
            userId = testUserId,
            role = HouseholdRole.OWNER
        )
        whenever(householdService.requireHouseholdAccess(testUserId, testHouseholdId)).thenReturn(mockContext)

        whenever(syncService.pull(testHouseholdId, 10L)).thenReturn(
            SyncPullResponseDto(
                serverTimestamp = 2000L,
                serverVersion = 50L,
                hasMore = false,
                products = emptyList(),
                pantryItems = emptyList(),
                shoppingLists = emptyList(),
                shoppingItems = emptyList(),
                priceEntries = emptyList()
            )
        )

        mockMvc.perform(
            get("/api/v1/sync/pull")
                .with(jwt().jwt { it.subject(testUserId) })
                .header("X-Household-Id", testHouseholdId)
                .param("sinceVersion", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.serverVersion").value(50))
            .andExpect(jsonPath("$.data.products").isArray)
    }
}
