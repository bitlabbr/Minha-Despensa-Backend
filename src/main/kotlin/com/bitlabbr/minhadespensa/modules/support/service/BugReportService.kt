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
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportCategory
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportCreateCommand
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportResponseDto
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportStatus
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportSummaryDto
import com.bitlabbr.minhadespensa.modules.support.entity.BugReportEntity
import com.bitlabbr.minhadespensa.modules.support.repository.BugReportRepository
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BugReportService(
    private val bugReportRepository: BugReportRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${app.support.storage-path:/app/bug-reports}")
    private val storagePath: String,
    @Value("\${app.support.max-log-size-bytes:5242880}")
    private val maxLogSizeBytes: Long
) {

    private val logger = LoggerFactory.getLogger(BugReportService::class.java)
    private val secureRandom = SecureRandom()
    private val crockfordChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()

    @Transactional
    fun submitBugReport(command: BugReportCreateCommand, logFile: MultipartFile?): BugReportResponseDto {
        val category = BugReportCategory.fromRaw(command.category)
            ?: throw BusinessException("Categoria '${command.category}' inválida. Valores permitidos: BUG, SUGGESTION, QUESTION, CRASH")

        val cleanTitle = command.title.trim()
        if (cleanTitle.length < 3) {
            throw BusinessException("Título deve conter pelo menos 3 caracteres.")
        }

        val cleanDescription = command.description.trim()
        if (cleanDescription.length < 5) {
            throw BusinessException("Descrição deve conter pelo menos 5 caracteres.")
        }

        val cleanAppVersion = command.appVersion.trim()
        if (cleanAppVersion.isEmpty()) {
            throw BusinessException("Versão do aplicativo é obrigatória.")
        }

        val validLogFile = logFile?.takeIf { !it.isEmpty }
        val hasLogs = validLogFile != null
        if (validLogFile != null && validLogFile.size > maxLogSizeBytes) {
            val maxMb = maxLogSizeBytes / (1024 * 1024)
            logger.warn(
                "[Support:ValidationFailed] Log file rejected: size={} bytes exceeds limit of {} MB",
                validLogFile.size, maxMb
            )
            throw BusinessException("Arquivo de log excede o tamanho máximo permitido de $maxMb MB.")
        }

        val protocol = generateUniqueProtocol()
        var logFilePath: String? = null

        if (validLogFile != null) {
            logFilePath = saveLogFileToDisk(protocol, command, category, validLogFile)
        }

        val now = System.currentTimeMillis()
        val entity = BugReportEntity(
            protocol = protocol,
            category = category.name,
            title = cleanTitle,
            description = cleanDescription,
            appVersion = cleanAppVersion,
            deviceModel = command.deviceModel?.trim()?.takeIf { it.isNotBlank() },
            osVersion = command.osVersion?.trim()?.takeIf { it.isNotBlank() },
            userId = command.userId?.trim()?.takeIf { it.isNotBlank() },
            householdId = command.householdId?.trim()?.takeIf { it.isNotBlank() },
            userContact = command.userContact?.trim()?.takeIf { it.isNotBlank() },
            hasLogs = hasLogs,
            logFilePath = logFilePath,
            status = BugReportStatus.OPEN.name,
            createdAt = now,
            updatedAt = now
        )

        val saved = bugReportRepository.save(entity)

        logger.info(
            "[Support:Submit] New report registered: protocol={}, category={}, hasLogs={}, appVersion='{}', userId={}, householdId={}",
            saved.protocol, saved.category, saved.hasLogs, saved.appVersion, saved.userId ?: "anonymous", saved.householdId ?: "none"
        )

        return BugReportResponseDto.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun listReports(status: String? = null, category: String? = null): List<BugReportSummaryDto> {
        val cleanStatus = status?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        val cleanCategory = category?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

        val entities = when {
            cleanStatus != null -> bugReportRepository.findByStatusOrderByCreatedAtDesc(cleanStatus)
            cleanCategory != null -> bugReportRepository.findByCategoryOrderByCreatedAtDesc(cleanCategory)
            else -> bugReportRepository.findAllByOrderByCreatedAtDesc()
        }

        logger.debug("[Support:List] Listed {} bug reports (filterStatus={}, filterCategory={})", entities.size, cleanStatus, cleanCategory)
        return entities.map { BugReportSummaryDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getReportByProtocol(protocol: String): BugReportEntity {
        return bugReportRepository.findByProtocol(protocol.trim().uppercase())
            .orElseThrow {
                logger.warn("[Support:NotFound] Bug report protocol not found: {}", protocol)
                ResourceNotFoundException(
                    message = "Relato de suporte não encontrado para o protocolo '$protocol'",
                    resourceName = "BugReport",
                    resourceId = protocol
                )
            }
    }

    @Transactional(readOnly = true)
    fun getLogResource(protocol: String): Pair<Resource, String> {
        val entity = getReportByProtocol(protocol)

        if (!entity.hasLogs || entity.logFilePath.isNullOrBlank()) {
            logger.warn("[Support:Download] No log attached for protocol {}", protocol)
            throw ResourceNotFoundException(
                message = "Nenhum arquivo de log associado ao protocolo '$protocol'",
                resourceName = "BugReportLog",
                resourceId = protocol
            )
        }

        val targetFile = File(entity.logFilePath!!)
        if (!targetFile.exists() || !targetFile.isFile) {
            logger.error("[Support:Download] Log file missing on disk for protocol {}: path={}", protocol, targetFile.absolutePath)
            throw ResourceNotFoundException(
                message = "Arquivo de log não localizado no disco do servidor",
                resourceName = "BugReportLog",
                resourceId = protocol
            )
        }

        logger.info("[Support:Download] Serving log file for protocol {}: size={} bytes", protocol, targetFile.length())
        val downloadFilename = "device-${entity.protocol.lowercase()}.log"
        return Pair(FileSystemResource(targetFile), downloadFilename)
    }

    private fun saveLogFileToDisk(
        protocol: String,
        command: BugReportCreateCommand,
        category: BugReportCategory,
        logFile: MultipartFile
    ): String {
        val baseDir = File(storagePath)
        val reportDir = File(baseDir, protocol)
        if (!reportDir.exists()) {
            reportDir.mkdirs()
        }

        val logDest = File(reportDir, "device.log")
        logFile.inputStream.use { input ->
            logDest.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        try {
            val metadata = mapOf(
                "protocol" to protocol,
                "category" to category.name,
                "title" to command.title.trim(),
                "description" to command.description.trim(),
                "appVersion" to command.appVersion.trim(),
                "deviceModel" to command.deviceModel?.trim(),
                "osVersion" to command.osVersion?.trim(),
                "userContact" to command.userContact?.trim(),
                "userId" to command.userId?.trim(),
                "householdId" to command.householdId?.trim(),
                "logFileName" to logDest.name,
                "logSizeBytes" to logDest.length(),
                "createdAt" to System.currentTimeMillis()
            )
            val metadataFile = File(reportDir, "metadata.json")
            metadataFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata))
        } catch (ex: Exception) {
            logger.warn("[Support:Storage] Failed to write metadata.json for protocol {}: {}", protocol, ex.message)
        }

        logger.info("[Support:Storage] Saved diagnostic log for protocol {}: path={}, size={} bytes", protocol, logDest.absolutePath, logDest.length())
        return logDest.absolutePath
    }

    private fun generateUniqueProtocol(): String {
        val datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
        var protocol: String
        var attempts = 0

        do {
            val randomSuffix = (1..4)
                .map { crockfordChars[secureRandom.nextInt(crockfordChars.size)] }
                .joinToString("")
            protocol = "BUG-$datePart-$randomSuffix"
            attempts++
        } while (bugReportRepository.findByProtocol(protocol).isPresent && attempts < 10)

        return protocol
    }
}
