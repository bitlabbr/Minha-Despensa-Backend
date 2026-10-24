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
 *                        the license, and indicate if changes来的.
 *     - NonCommercial  — You may not use the material for commercial purposes.
 *
 *   Owner rights:
 *     - Willian Santos retains all commercial rights.
 *    - The copyright holder may use, sell, sublicense, or relicense this
 *       work under different terms at any time.
 *
 *   Full license: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package com.bitlabbr.minhadespensa.modules.apprelease.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadados da versão do aplicativo móvel para atualização de software (OTA)")
data class AppVersionResponseDto(
    @param:Schema(description = "Plataforma alvo do aplicativo", example = "android")
    val platform: String,

    @param:Schema(description = "Nome semântico da versão", example = "2.1.0")
    val versionName: String,

    @param:Schema(description = "Código numérico inteiro da versão (Android versionCode)", example = "2100")
    val versionCode: Long,

    @param:Schema(description = "Versão mínima obrigatória para forçar atualização", example = "2000")
    val minRequiredVersion: Long,

    @param:Schema(description = "Notas da versão e changelog em Markdown", example = "Novos recursos de sincronização e correções de estabilidade.")
    val releaseNotes: String,

    @param:Schema(description = "URL absoluta ou relativa para download direto do binário (APK)", example = "/api/v1/app/download/latest")
    val downloadUrl: String,

    @param:Schema(description = "Hash SHA-256 do arquivo binário para validação de integridade", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    val sha256Checksum: String,

    @param:Schema(description = "Tamanho do arquivo em bytes", example = "35489201")
    val fileSizeInBytes: Long,

    @param:Schema(description = "Timestamp epoch millis UTC do lançamento da versão", example = "1792771200000")
    val releasedAt: Long
)
