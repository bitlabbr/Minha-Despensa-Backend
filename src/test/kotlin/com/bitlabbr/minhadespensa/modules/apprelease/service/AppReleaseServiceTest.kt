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

package com.bitlabbr.minhadespensa.modules.apprelease.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Path

class AppReleaseServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private val objectMapper = ObjectMapper()
    private lateinit var appReleaseService: AppReleaseService

    @BeforeEach
    fun setUp() {
        appReleaseService = AppReleaseService(
            storagePath = tempDir.toString(),
            baseDownloadUrl = "https://despensa.domus.arpa/api/v1/app/download",
            objectMapper = objectMapper
        )
    }

    @Test
    fun `getLatestVersion should parse manifest and return valid DTO`() {
        val manifestJson = """
            {
                "platform": "android",
                "versionName": "2.1.0",
                "versionCode": 2100,
                "minRequiredVersion": 2000,
                "releaseNotes": "Melhorias de sync e nova tela.",
                "sha256Checksum": "abc123sha",
                "fileSizeInBytes": 10240,
                "releasedAt": 1792771200000
            }
        """.trimIndent()

        val manifestFile = File(tempDir.toFile(), "android-latest.json")
        manifestFile.writeText(manifestJson)

        val result = appReleaseService.getLatestVersion("android")

        assertNotNull(result)
        assertEquals("android", result.platform)
        assertEquals("2.1.0", result.versionName)
        assertEquals(2100L, result.versionCode)
        assertEquals(2000L, result.minRequiredVersion)
        assertEquals("Melhorias de sync e nova tela.", result.releaseNotes)
        assertEquals("abc123sha", result.sha256Checksum)
        assertEquals("https://despensa.domus.arpa/api/v1/app/download/latest", result.downloadUrl)
    }

    @Test
    fun `getLatestVersion should throw ResourceNotFoundException when manifest does not exist`() {
        assertThrows<ResourceNotFoundException> {
            appReleaseService.getLatestVersion("android")
        }
    }

    @Test
    fun `getDownloadResource should return valid resource when file exists`() {
        val manifestJson = """
            {
                "versionName": "2.1.0",
                "versionCode": 2100
            }
        """.trimIndent()
        File(tempDir.toFile(), "android-latest.json").writeText(manifestJson)

        val apkFile = File(tempDir.toFile(), "minha-despensa-2.1.0.apk")
        apkFile.writeBytes("dummy apk content".toByteArray())

        val (resource, fileName) = appReleaseService.getDownloadResource("latest")

        assertEquals("minha-despensa-2.1.0.apk", fileName)
        assertTrue(resource.exists())
        assertEquals(apkFile.length(), resource.contentLength())
    }

    @Test
    fun `getDownloadResource should throw ResourceNotFoundException when apk is missing`() {
        val manifestJson = """
            {
                "versionName": "2.1.0",
                "versionCode": 2100
            }
        """.trimIndent()
        File(tempDir.toFile(), "android-latest.json").writeText(manifestJson)

        assertThrows<ResourceNotFoundException> {
            appReleaseService.getDownloadResource("latest")
        }
    }
}
