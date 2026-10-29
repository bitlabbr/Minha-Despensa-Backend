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

package com.bitlabbr.minhadespensa.modules.support.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportResponseDto
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportSummaryDto
import com.bitlabbr.minhadespensa.modules.support.dto.SupportAdminLoginResponse
import com.bitlabbr.minhadespensa.modules.support.service.BugReportService
import com.bitlabbr.minhadespensa.modules.support.service.SupportAuthService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BugReportController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class BugReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var bugReportService: BugReportService

    @MockitoBean
    private lateinit var supportAuthService: SupportAuthService

    @Test
    fun `POST submitBugReport with file and jwt should return 201 Created`() {
        val mockFile = MockMultipartFile(
            "logFile",
            "device.log",
            "text/plain",
            "sample log content".toByteArray()
        )

        val responseDto = BugReportResponseDto(
            id = "550e8400-e29b-41d4-a716-446655440000",
            protocol = "BUG-20261024-TEST",
            category = "BUG",
            title = "Tela preta ao abrir despensa",
            status = "OPEN",
            hasLogs = true,
            createdAt = 1729795200000L
        )

        whenever(bugReportService.submitBugReport(any(), any())).thenReturn(responseDto)

        mockMvc.perform(
            multipart("/api/v1/support/bug-reports")
                .file(mockFile)
                .param("category", "BUG")
                .param("title", "Tela preta ao abrir despensa")
                .param("description", "Ocorre no Motorola Edge")
                .param("appVersion", "2.1.0")
                .param("deviceModel", "Motorola Edge 30")
                .param("osVersion", "Android 14")
                .header("X-Household-Id", "house-123")
                .with(jwt().jwt { it.subject("user-456") })
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.protocol").value("BUG-20261024-TEST"))
            .andExpect(jsonPath("$.data.category").value("BUG"))
            .andExpect(jsonPath("$.data.hasLogs").value(true))
            .andExpect(jsonPath("$.data.title").value("Tela preta ao abrir despensa"))
    }

    @Test
    fun `POST submitBugReport anonymous without file should return 201 Created`() {
        val responseDto = BugReportResponseDto(
            id = "550e8400-e29b-41d4-a716-446655440001",
            protocol = "BUG-20261024-ANON",
            category = "SUGGESTION",
            title = "Sugestão de filtro de validade",
            status = "OPEN",
            hasLogs = false,
            createdAt = 1729795200000L
        )

        whenever(bugReportService.submitBugReport(any(), eq(null))).thenReturn(responseDto)

        mockMvc.perform(
            multipart("/api/v1/support/bug-reports")
                .param("category", "SUGGESTION")
                .param("title", "Sugestão de filtro de validade")
                .param("description", "Poderia ordenar por dias restantes")
                .param("appVersion", "2.1.0")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.protocol").value("BUG-20261024-ANON"))
            .andExpect(jsonPath("$.data.hasLogs").value(false))
    }

    @Test
    fun `GET listReports authenticated should return 200 OK with summary list`() {
        val summary = BugReportSummaryDto(
            id = "550e8400-e29b-41d4-a716-446655440002",
            protocol = "BUG-20261024-0001",
            category = "BUG",
            title = "Falha de sync",
            description = "Erro ao sincronizar produtos",
            appVersion = "2.1.0",
            deviceModel = "Pixel 7",
            osVersion = "Android 14",
            userId = "user-1",
            householdId = "house-1",
            userContact = "user@test.com",
            hasLogs = true,
            status = "OPEN",
            createdAt = 1729795200000L
        )

        whenever(bugReportService.listReports(null, null)).thenReturn(listOf(summary))

        mockMvc.perform(
            get("/api/v1/support/bug-reports")
                .with(jwt().jwt { it.subject("system-admin").claim("role", "SYSTEM_ADMIN") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].protocol").value("BUG-20261024-0001"))
            .andExpect(jsonPath("$.data[0].category").value("BUG"))
            .andExpect(jsonPath("$.data[0].hasLogs").value(true))
    }

    @Test
    fun `GET listReports unauthenticated should return 401 Unauthorized`() {
        mockMvc.perform(get("/api/v1/support/bug-reports"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET getReportByProtocol authenticated should return 200 OK with report details`() {
        val summary = BugReportSummaryDto(
            id = "550e8400-e29b-41d4-a716-446655440003",
            protocol = "BUG-20261024-0002",
            category = "CRASH",
            title = "Crash ao iniciar",
            description = "Falha no boot",
            appVersion = "2.1.0",
            deviceModel = "Galaxy S24",
            osVersion = "Android 14",
            userId = "user-2",
            householdId = "house-2",
            userContact = "user2@test.com",
            hasLogs = true,
            status = "OPEN",
            createdAt = 1729795200000L
        )

        whenever(bugReportService.getReportSummary("BUG-20261024-0002")).thenReturn(summary)

        mockMvc.perform(
            get("/api/v1/support/bug-reports/BUG-20261024-0002")
                .with(jwt().jwt { it.subject("system-admin").claim("role", "SYSTEM_ADMIN") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.protocol").value("BUG-20261024-0002"))
            .andExpect(jsonPath("$.data.category").value("CRASH"))
            .andExpect(jsonPath("$.data.title").value("Crash ao iniciar"))
    }

    @Test
    fun `PATCH updateReportStatus authenticated should return 200 OK with updated report`() {
        val updatedSummary = BugReportSummaryDto(
            id = "550e8400-e29b-41d4-a716-446655440003",
            protocol = "BUG-20261024-0002",
            category = "CRASH",
            title = "Crash ao iniciar",
            description = "Falha no boot",
            appVersion = "2.1.0",
            deviceModel = "Galaxy S24",
            osVersion = "Android 14",
            userId = "user-2",
            householdId = "house-2",
            userContact = "user2@test.com",
            hasLogs = true,
            status = "RESOLVED",
            createdAt = 1729795200000L
        )

        whenever(bugReportService.updateReportStatus("BUG-20261024-0002", "RESOLVED")).thenReturn(updatedSummary)

        mockMvc.perform(
            patch("/api/v1/support/bug-reports/BUG-20261024-0002/status")
                .param("status", "RESOLVED")
                .with(jwt().jwt { it.subject("system-admin").claim("role", "SYSTEM_ADMIN") })
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.protocol").value("BUG-20261024-0002"))
            .andExpect(jsonPath("$.data.status").value("RESOLVED"))
    }

    @Test
    fun `GET downloadLog authenticated should return 200 OK with text stream`() {
        val logContent = "2026-10-24 12:00:00 [INFO] [App] Started\n2026-10-24 12:00:05 [ERROR] Crash occurred"
        val resource = ByteArrayResource(logContent.toByteArray(Charsets.UTF_8))
        val filename = "BUG-20261024-0001-device.log"

        whenever(bugReportService.getLogResource("BUG-20261024-0001"))
            .thenReturn(Pair(resource, filename))

        mockMvc.perform(
            get("/api/v1/support/bug-reports/BUG-20261024-0001/log")
                .with(jwt().jwt { it.subject("system-admin").claim("role", "SYSTEM_ADMIN") })
        )
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BUG-20261024-0001-device.log\""))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().string(logContent))
    }

    @Test
    fun `POST adminLogin with valid credentials should return 200 OK`() {
        val authResponse = SupportAdminLoginResponse(
            accessToken = "admin-jwt-token",
            username = "admin",
            expiresInSeconds = 86400L,
            role = "SYSTEM_ADMIN"
        )

        whenever(supportAuthService.login(any())).thenReturn(authResponse)

        mockMvc.perform(
            post("/api/v1/support/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"admin","password":"password123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("admin-jwt-token"))
            .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"))
    }

    @Test
    fun `GET listReports with regular mobile user should return 403 Forbidden`() {
        mockMvc.perform(
            get("/api/v1/support/bug-reports")
                .with(jwt().jwt { it.subject("regular-mobile-user") })
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET downloadLog unauthenticated should return 401 Unauthorized`() {
        mockMvc.perform(get("/api/v1/support/bug-reports/BUG-20261024-0001/log"))
            .andExpect(status().isUnauthorized)
    }
}
