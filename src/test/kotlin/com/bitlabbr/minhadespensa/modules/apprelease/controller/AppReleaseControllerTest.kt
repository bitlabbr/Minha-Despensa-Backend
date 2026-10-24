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

package com.bitlabbr.minhadespensa.modules.apprelease.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.apprelease.dto.AppVersionResponseDto
import com.bitlabbr.minhadespensa.modules.apprelease.service.AppReleaseService
import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AppReleaseController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class)
class AppReleaseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var appReleaseService: AppReleaseService

    @Test
    fun `GET version latest should return 200 with AppVersionResponseDto envelope`() {
        val mockDto = AppVersionResponseDto(
            platform = "android",
            versionName = "2.1.0",
            versionCode = 2100L,
            minRequiredVersion = 2000L,
            releaseNotes = "Nova atualização OTA.",
            downloadUrl = "https://despensa.domus.arpa/api/v1/app/download/latest",
            sha256Checksum = "1234567890abcdef",
            fileSizeInBytes = 25000000L,
            releasedAt = 1792771200000L
        )

        whenever(appReleaseService.getLatestVersion("android")).thenReturn(mockDto)

        mockMvc.perform(get("/api/v1/app/version/latest?platform=android"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.platform").value("android"))
            .andExpect(jsonPath("$.data.versionName").value("2.1.0"))
            .andExpect(jsonPath("$.data.versionCode").value(2100))
            .andExpect(jsonPath("$.data.downloadUrl").value("https://despensa.domus.arpa/api/v1/app/download/latest"))
    }

    @Test
    fun `GET download latest should return 200 with APK attachment and octet-stream`() {
        val dummyBytes = "fake apk stream".toByteArray()
        val resource = ByteArrayResource(dummyBytes)
        val fileName = "minha-despensa-2.1.0.apk"

        whenever(appReleaseService.getDownloadResource(null)).thenReturn(Pair(resource, fileName))

        mockMvc.perform(get("/api/v1/app/download/latest"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\""))
            .andExpect(content().contentType(MediaType.parseMediaType("application/vnd.android.package-archive")))
            .andExpect(content().bytes(dummyBytes))
    }
}
