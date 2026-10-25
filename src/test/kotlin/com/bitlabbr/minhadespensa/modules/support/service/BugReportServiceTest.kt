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

package com.bitlabbr.minhadespensa.modules.support.service

import com.bitlabbr.minhadespensa.common.exception.BusinessException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportCreateCommand
import com.bitlabbr.minhadespensa.modules.support.entity.BugReportEntity
import com.bitlabbr.minhadespensa.modules.support.repository.BugReportRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Path
import java.util.Optional

class BugReportServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private val bugReportRepository: BugReportRepository = mock()
    private val objectMapper = ObjectMapper()
    private lateinit var bugReportService: BugReportService

    @BeforeEach
    fun setUp() {
        bugReportService = BugReportService(
            bugReportRepository = bugReportRepository,
            objectMapper = objectMapper,
            storagePath = tempDir.toString(),
            maxLogSizeBytes = 1024 * 1024 // 1 MB para teste
        )
    }

    @Test
    fun `should submit report without log file successfully`() {
        whenever(bugReportRepository.findByProtocol(any())).thenReturn(Optional.empty())
        whenever(bugReportRepository.save(any<BugReportEntity>())).thenAnswer { invocation ->
            invocation.arguments[0] as BugReportEntity
        }

        val command = BugReportCreateCommand(
            category = "SUGGESTION",
            title = "Adicionar modo escuro automático",
            description = "Seria excelente se o app alternasse o tema com base no sistema.",
            appVersion = "1.2.0-alpha",
            deviceModel = "Samsung Galaxy S23",
            osVersion = "Android 14",
            userContact = "tester@despensa.app",
            householdId = "house-123",
            userId = "user-abc"
        )

        val response = bugReportService.submitBugReport(command, null)

        assertNotNull(response)
        assertTrue(response.protocol.startsWith("BUG-"))
        assertEquals("SUGGESTION", response.category)
        assertEquals("Adicionar modo escuro automático", response.title)
        assertEquals("OPEN", response.status)
        assertFalse(response.hasLogs)
    }

    @Test
    fun `should submit report with log file and write to disk`() {
        whenever(bugReportRepository.findByProtocol(any())).thenReturn(Optional.empty())
        whenever(bugReportRepository.save(any<BugReportEntity>())).thenAnswer { invocation ->
            invocation.arguments[0] as BugReportEntity
        }

        val command = BugReportCreateCommand(
            category = "BUG",
            title = "Falha ao escanear código",
            description = "O leitor de código de barras congela após a leitura.",
            appVersion = "1.2.0-alpha"
        )

        val logContent = "2026-10-24 12:00:00 [ERROR] Camera preview timeout\nException in thread main"
        val mockFile = MockMultipartFile(
            "logFile",
            "device.log",
            "text/plain",
            logContent.toByteArray(Charsets.UTF_8)
        )

        val response = bugReportService.submitBugReport(command, mockFile)

        assertNotNull(response)
        assertTrue(response.hasLogs)

        val expectedReportDir = File(tempDir.toFile(), response.protocol)
        assertTrue(expectedReportDir.exists(), "Diretório do protocolo deve existir no disco")

        val expectedLogFile = File(expectedReportDir, "device.log")
        assertTrue(expectedLogFile.exists(), "Arquivo device.log deve existir no disco")
        assertEquals(logContent, expectedLogFile.readText(Charsets.UTF_8))

        val metadataFile = File(expectedReportDir, "metadata.json")
        assertTrue(metadataFile.exists(), "Arquivo metadata.json deve existir no disco")
    }

    @Test
    fun `should throw BusinessException when category is invalid`() {
        val command = BugReportCreateCommand(
            category = "INVALID_CATEGORY",
            title = "Título válido",
            description = "Descrição com tamanho válido para o teste.",
            appVersion = "1.2.0-alpha"
        )

        val exception = assertThrows<BusinessException> {
            bugReportService.submitBugReport(command, null)
        }
        assertTrue(exception.message!!.contains("Categoria 'INVALID_CATEGORY' inválida"))
    }

    @Test
    fun `should throw BusinessException when title is shorter than 3 characters`() {
        val command = BugReportCreateCommand(
            category = "BUG",
            title = "Oi",
            description = "Descrição com tamanho válido para o teste.",
            appVersion = "1.2.0-alpha"
        )

        val exception = assertThrows<BusinessException> {
            bugReportService.submitBugReport(command, null)
        }
        assertTrue(exception.message!!.contains("Título deve conter pelo menos 3 caracteres"))
    }

    @Test
    fun `should throw BusinessException when description is shorter than 5 characters`() {
        val command = BugReportCreateCommand(
            category = "BUG",
            title = "Título válido",
            description = "Erro",
            appVersion = "1.2.0-alpha"
        )

        val exception = assertThrows<BusinessException> {
            bugReportService.submitBugReport(command, null)
        }
        assertTrue(exception.message!!.contains("Descrição deve conter pelo menos 5 caracteres"))
    }

    @Test
    fun `should throw BusinessException when log file exceeds size limit`() {
        val command = BugReportCreateCommand(
            category = "BUG",
            title = "Título válido",
            description = "Descrição com tamanho válido para o teste.",
            appVersion = "1.2.0-alpha"
        )

        val oversizedBytes = ByteArray(2 * 1024 * 1024) // 2 MB (limite configurado no setUp é 1 MB)
        val mockFile = MockMultipartFile("logFile", "device.log", "text/plain", oversizedBytes)

        val exception = assertThrows<BusinessException> {
            bugReportService.submitBugReport(command, mockFile)
        }
        assertTrue(exception.message!!.contains("Arquivo de log excede o tamanho máximo permitido"))
    }

    @Test
    fun `should list reports with optional status and category filters`() {
        val entity1 = BugReportEntity(
            protocol = "BUG-20261024-AAA1",
            category = "BUG",
            title = "Bug 1",
            description = "Desc 1",
            appVersion = "1.2.0-alpha",
            status = "OPEN"
        )
        val entity2 = BugReportEntity(
            protocol = "BUG-20261024-AAA2",
            category = "SUGGESTION",
            title = "Sugestão 1",
            description = "Desc 2",
            appVersion = "1.2.0-alpha",
            status = "RESOLVED"
        )

        whenever(bugReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(listOf(entity1, entity2))
        whenever(bugReportRepository.findByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(listOf(entity1))
        whenever(bugReportRepository.findByCategoryOrderByCreatedAtDesc("BUG")).thenReturn(listOf(entity1))

        val all = bugReportService.listReports(null, null)
        assertEquals(2, all.size)

        val filteredStatus = bugReportService.listReports(status = "OPEN", category = null)
        assertEquals(1, filteredStatus.size)
        assertEquals("BUG-20261024-AAA1", filteredStatus[0].protocol)

        val filteredCategory = bugReportService.listReports(status = null, category = "BUG")
        assertEquals(1, filteredCategory.size)
        assertEquals("BUG-20261024-AAA1", filteredCategory[0].protocol)
    }

    @Test
    fun `should return log resource when protocol exists and has logs`() {
        val testFile = File(tempDir.toFile(), "device.log")
        testFile.writeText("log data", Charsets.UTF_8)

        val entity = BugReportEntity(
            protocol = "BUG-20261024-TEST",
            category = "BUG",
            title = "Test",
            description = "Test description",
            appVersion = "1.2.0-alpha",
            hasLogs = true,
            logFilePath = testFile.absolutePath
        )

        whenever(bugReportRepository.findByProtocol("BUG-20261024-TEST")).thenReturn(Optional.of(entity))

        val (resource, filename) = bugReportService.getLogResource("BUG-20261024-TEST")
        assertNotNull(resource)
        assertTrue(resource.exists())
        assertEquals("device-bug-20261024-test.log", filename)
    }

    @Test
    fun `should throw ResourceNotFoundException when report has no logs`() {
        val entity = BugReportEntity(
            protocol = "BUG-20261024-NOLOG",
            category = "SUGGESTION",
            title = "Test",
            description = "Test description",
            appVersion = "1.2.0-alpha",
            hasLogs = false,
            logFilePath = null
        )

        whenever(bugReportRepository.findByProtocol("BUG-20261024-NOLOG")).thenReturn(Optional.of(entity))

        assertThrows<ResourceNotFoundException> {
            bugReportService.getLogResource("BUG-20261024-NOLOG")
        }
    }

    @Test
    fun `should get report summary by protocol`() {
        val entity = BugReportEntity(
            protocol = "BUG-20261024-SUMM",
            category = "BUG",
            title = "Crash ao abrir câmera",
            description = "Falha no init",
            appVersion = "1.2.0",
            deviceModel = "Pixel 6",
            status = "OPEN"
        )
        whenever(bugReportRepository.findByProtocol("BUG-20261024-SUMM")).thenReturn(Optional.of(entity))

        val result = bugReportService.getReportSummary("BUG-20261024-SUMM")

        assertNotNull(result)
        assertEquals("BUG-20261024-SUMM", result.protocol)
        assertEquals("Crash ao abrir câmera", result.title)
        assertEquals("Pixel 6", result.deviceModel)
    }

    @Test
    fun `should update report status successfully`() {
        val entity = BugReportEntity(
            protocol = "BUG-20261024-STAT",
            category = "BUG",
            title = "Crash ao salvar produto",
            description = "Null pointer ao salvar",
            appVersion = "1.2.0",
            status = "OPEN"
        )
        whenever(bugReportRepository.findByProtocol("BUG-20261024-STAT")).thenReturn(Optional.of(entity))
        whenever(bugReportRepository.save(any<BugReportEntity>())).thenAnswer { it.arguments[0] as BugReportEntity }

        val result = bugReportService.updateReportStatus("BUG-20261024-STAT", "INVESTIGATING")

        assertEquals("INVESTIGATING", result.status)
        assertEquals("INVESTIGATING", entity.status)
    }

    @Test
    fun `should throw BusinessException when updating with invalid status`() {
        val exception = assertThrows<BusinessException> {
            bugReportService.updateReportStatus("BUG-20261024-STAT", "INVALID_STATUS")
        }
        assertTrue(exception.message!!.contains("Status 'INVALID_STATUS' inválido"))
    }

    @Test
    fun `should throw ResourceNotFoundException when updating non-existent report`() {
        whenever(bugReportRepository.findByProtocol("BUG-UNKNOWN")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            bugReportService.updateReportStatus("BUG-UNKNOWN", "RESOLVED")
        }
    }

    @Test
    fun `should throw ResourceNotFoundException when protocol does not exist`() {
        whenever(bugReportRepository.findByProtocol("BUG-UNKNOWN")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            bugReportService.getLogResource("BUG-UNKNOWN")
        }
    }
}
