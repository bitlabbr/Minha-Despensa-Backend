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
import com.bitlabbr.minhadespensa.modules.apprelease.dto.AppVersionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

@Service
class AppReleaseService(
    @Value("\${app.releases.storage-path:/app/releases}")
    private val storagePath: String,
    @Value("\${app.releases.base-download-url:https://despensa.domus.arpa/api/v1/app/download}")
    private val baseDownloadUrl: String,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(AppReleaseService::class.java)

    fun getLatestVersion(platform: String = "android"): AppVersionResponseDto {
        val cleanPlatform = platform.trim().lowercase()
        logger.info("[AppRelease:Check] Checking latest version for platform '{}'", cleanPlatform)

        val baseDir = File(storagePath)
        val manifestFile = File(baseDir, "$cleanPlatform-latest.json")

        if (!manifestFile.exists()) {
            logger.warn("[AppRelease:Check] Manifest file not found at: {}", manifestFile.absolutePath)
            throw ResourceNotFoundException(
                message = "Nenhuma versão encontrada para a plataforma '$cleanPlatform'",
                resourceName = "AppRelease",
                resourceId = cleanPlatform
            )
        }

        return try {
            val rootNode = objectMapper.readTree(manifestFile)
            val versionName = rootNode.get("versionName")?.asString() ?: "1.0.0"
            val versionCode = rootNode.get("versionCode")?.asLong() ?: 1000L
            val minRequiredVersion = rootNode.get("minRequiredVersion")?.asLong() ?: versionCode
            val releaseNotes = rootNode.get("releaseNotes")?.asString() ?: "Atualizações e correções gerais."
            val rawSha256 = rootNode.get("sha256Checksum")?.asString() ?: ""
            val rawFileSize = rootNode.get("fileSizeInBytes")?.asLong() ?: 0L

            val apkFile = resolveApkFile(versionName)
            val finalSha256 = if (rawSha256.isNotBlank()) rawSha256 else calculateSha256(apkFile)
            val finalFileSize = if (rawFileSize > 0) rawFileSize else if (apkFile.exists()) apkFile.length() else 0L

            val downloadUrl = "$baseDownloadUrl/latest"

            logger.info(
                "[AppRelease:Check] Found version: platform='{}', version='{}', code={}, size={} bytes",
                cleanPlatform, versionName, versionCode, finalFileSize
            )

            AppVersionResponseDto(
                platform = cleanPlatform,
                versionName = versionName,
                versionCode = versionCode,
                minRequiredVersion = minRequiredVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                sha256Checksum = finalSha256,
                fileSizeInBytes = finalFileSize,
                releasedAt = rootNode.get("releasedAt")?.asLong() ?: manifestFile.lastModified()
            )
        } catch (ex: Exception) {
            logger.error("[AppRelease:Check] Failed to parse manifest for platform '{}': {}", cleanPlatform, ex.message)
            throw ex
        }
    }

    fun getDownloadResource(versionName: String?): Pair<Resource, String> {
        val targetFile = if (versionName.isNullOrBlank() || versionName.equals("latest", ignoreCase = true)) {
            val latest = getLatestVersion("android")
            resolveApkFile(latest.versionName)
        } else {
            resolveApkFile(versionName)
        }

        if (!targetFile.exists() || !targetFile.isFile) {
            logger.warn("[AppRelease:Download] APK file not found at path: {}", targetFile.absolutePath)
            throw ResourceNotFoundException(
                message = "Arquivo do aplicativo não localizado no servidor.",
                resourceName = "AppReleaseFile",
                resourceId = targetFile.name
            )
        }

        logger.info("[AppRelease:Download] Serving APK stream: file='{}', size={} bytes", targetFile.name, targetFile.length())
        return Pair(FileSystemResource(targetFile), targetFile.name)
    }

    private fun resolveApkFile(versionName: String): File {
        val baseDir = File(storagePath)
        val specificFile = File(baseDir, "minha-despensa-$versionName.apk")
        if (specificFile.exists()) return specificFile

        val genericFile = File(baseDir, "app-release.apk")
        if (genericFile.exists()) return genericFile

        return specificFile
    }

    private fun calculateSha256(file: File): String {
        if (!file.exists()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (ex: Exception) {
            logger.warn("[AppRelease:Checksum] Failed to calculate sha256 for file '{}': {}", file.name, ex.message)
            ""
        }
    }
}
