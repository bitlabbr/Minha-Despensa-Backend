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

package com.bitlabbr.minhadespensa.modules.household.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.household.dto.HouseholdDetailResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.HouseholdMemberResponseDto
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
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

@WebMvcTest(HouseholdController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class HouseholdControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var householdService: HouseholdService

    @Test
    fun `GET households should return 200 with list of households`() {
        val mockHousehold = HouseholdDetailResponseDto(
            id = "house-1",
            name = "Despensa de Willian",
            inviteCode = "CASA-TESTCODE",
            ownerId = "user-1",
            syncVersion = 5L,
            members = listOf(
                HouseholdMemberResponseDto(
                    userId = "user-1",
                    name = "Willian",
                    email = "willian@test.com",
                    role = "OWNER",
                    joinedAt = System.currentTimeMillis()
                )
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        whenever(householdService.getUserHouseholds("user-1")).thenReturn(listOf(mockHousehold))

        mockMvc.perform(
            get("/api/v1/households")
                .with(jwt().jwt { it.subject("user-1") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("house-1"))
            .andExpect(jsonPath("$.data[0].name").value("Despensa de Willian"))
            .andExpect(jsonPath("$.data[0].inviteCode").value("CASA-TESTCODE"))
            .andExpect(jsonPath("$.data[0].members[0].role").value("OWNER"))
    }

    @Test
    fun `POST households should return 201 Created`() {
        val payload = """
            {
                "name": "Casa da Serra"
            }
        """.trimIndent()

        val mockHousehold = HouseholdDetailResponseDto(
            id = "house-2",
            name = "Casa da Serra",
            inviteCode = "CASA-SERRA001",
            ownerId = "user-1",
            syncVersion = 0L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        whenever(householdService.createHousehold(eq("user-1"), any())).thenReturn(mockHousehold)

        mockMvc.perform(
            post("/api/v1/households")
                .with(jwt().jwt { it.subject("user-1") })
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("house-2"))
            .andExpect(jsonPath("$.data.name").value("Casa da Serra"))
    }

    @Test
    fun `POST households join should return 200 OK`() {
        val payload = """
            {
                "inviteCode": "CASA-SERRA001"
            }
        """.trimIndent()

        val mockHousehold = HouseholdDetailResponseDto(
            id = "house-2",
            name = "Casa da Serra",
            inviteCode = "CASA-SERRA001",
            ownerId = "owner-99",
            syncVersion = 10L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        whenever(householdService.joinHousehold(eq("user-1"), any())).thenReturn(mockHousehold)

        mockMvc.perform(
            post("/api/v1/households/join")
                .with(jwt().jwt { it.subject("user-1") })
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.inviteCode").value("CASA-SERRA001"))
    }
}
