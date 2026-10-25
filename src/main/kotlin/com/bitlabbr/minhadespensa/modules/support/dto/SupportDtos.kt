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

package com.bitlabbr.minhadespensa.modules.support.dto

import com.bitlabbr.minhadespensa.modules.support.entity.BugReportEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

enum class BugReportCategory {
    BUG,
    SUGGESTION,
    QUESTION,
    CRASH;

    companion object {
        fun fromRaw(raw: String?): BugReportCategory? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

enum class BugReportStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    CLOSED;

    companion object {
        fun fromRaw(raw: String?): BugReportStatus? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

@Schema(description = "Comando de criação de relato de suporte ou bug")
data class BugReportCreateCommand(
    @param:Schema(description = "Categoria do relato (BUG, SUGGESTION, QUESTION, CRASH)", example = "BUG")
    @field:NotBlank(message = "Categoria é obrigatória")
    val category: String,

    @param:Schema(description = "Título resumido do relato", example = "Falha ao escanear código de barras")
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 255, message = "Título deve conter entre 3 e 255 caracteres")
    val title: String,

    @param:Schema(description = "Descrição detalhada do problema ou sugestão", example = "Ao apontar a câmera para o produto X, a leitura congela.")
    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 5, max = 10000, message = "Descrição deve conter entre 5 e 10000 caracteres")
    val description: String,

    @param:Schema(description = "Versão do aplicativo cliente", example = "1.2.0-alpha")
    @field:NotBlank(message = "Versão do app é obrigatória")
    val appVersion: String,

    @param:Schema(description = "Modelo do dispositivo", example = "Samsung SM-S911B")
    val deviceModel: String? = null,

    @param:Schema(description = "Versão do sistema operacional", example = "Android 14 (API 34)")
    val osVersion: String? = null,

    @param:Schema(description = "Contato opcional para retorno (e-mail ou telefone)", example = "usuario@dominio.com")
    val userContact: String? = null,

    @param:Schema(description = "Identificador da casa/despensa ativa no momento do envio", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val householdId: String? = null,

    @param:Schema(description = "Identificador do usuário autenticado (se logado)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val userId: String? = null
)

@Schema(description = "Recibo retornado após registro do relato de suporte")
data class BugReportResponseDto(
    @param:Schema(description = "Identificador único do relato (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @param:Schema(description = "Número amigável de protocolo para acompanhamento", example = "BUG-20261024-8F2A")
    val protocol: String,

    @param:Schema(description = "Categoria registrada", example = "BUG")
    val category: String,

    @param:Schema(description = "Título do relato", example = "Falha ao escanear código de barras")
    val title: String,

    @param:Schema(description = "Status atual do chamado", example = "OPEN")
    val status: String,

    @param:Schema(description = "Indica se o arquivo de log foi anexado com sucesso", example = "true")
    val hasLogs: Boolean,

    @param:Schema(description = "Timestamp epoch da submissão", example = "1729795200000")
    val createdAt: Long
) {
    companion object {
        fun fromEntity(entity: BugReportEntity): BugReportResponseDto =
            BugReportResponseDto(
                id = entity.id,
                protocol = entity.protocol,
                category = entity.category,
                title = entity.title,
                status = entity.status,
                hasLogs = entity.hasLogs,
                createdAt = entity.createdAt
            )
    }
}

@Schema(description = "Resumo do relato de suporte para listagem e triagem")
data class BugReportSummaryDto(
    @param:Schema(description = "Identificador único do relato", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @param:Schema(description = "Número de protocolo do chamado", example = "BUG-20261024-8F2A")
    val protocol: String,

    @param:Schema(description = "Categoria do relato", example = "BUG")
    val category: String,

    @param:Schema(description = "Título do relato", example = "Falha ao escanear código de barras")
    val title: String,

    @param:Schema(description = "Descrição detalhada", example = "Ao apontar a câmera...")
    val description: String,

    @param:Schema(description = "Versão do aplicativo", example = "1.2.0-alpha")
    val appVersion: String,

    @param:Schema(description = "Modelo do aparelho", example = "Samsung SM-S911B")
    val deviceModel: String?,

    @param:Schema(description = "Versão do sistema operacional", example = "Android 14")
    val osVersion: String?,

    @param:Schema(description = "ID do usuário (se autenticado)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val userId: String?,

    @param:Schema(description = "ID da casa (se aplicável)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val householdId: String?,

    @param:Schema(description = "Contato informado pelo usuário", example = "usuario@dominio.com")
    val userContact: String?,

    @param:Schema(description = "Indica se há logs associados para download", example = "true")
    val hasLogs: Boolean,

    @param:Schema(description = "Status do chamado", example = "OPEN")
    val status: String,

    @param:Schema(description = "Data de criação em epoch milissegundos", example = "1729795200000")
    val createdAt: Long
) {
    companion object {
        fun fromEntity(entity: BugReportEntity): BugReportSummaryDto =
            BugReportSummaryDto(
                id = entity.id,
                protocol = entity.protocol,
                category = entity.category,
                title = entity.title,
                description = entity.description,
                appVersion = entity.appVersion,
                deviceModel = entity.deviceModel,
                osVersion = entity.osVersion,
                userId = entity.userId,
                householdId = entity.householdId,
                userContact = entity.userContact,
                hasLogs = entity.hasLogs,
                status = entity.status,
                createdAt = entity.createdAt
            )
    }
}

@Schema(description = "Requisição de login administrativo do sistema")
data class SupportAdminLoginRequest(
    @param:Schema(description = "Nome de usuário do administrador", example = "admin")
    @field:NotBlank(message = "Usuário é obrigatório")
    val username: String,

    @param:Schema(description = "Senha do administrador", example = "admin")
    @field:NotBlank(message = "Senha é obrigatória")
    val password: String
)

@Schema(description = "Resposta de autenticação do administrador do sistema")
data class SupportAdminLoginResponse(
    @param:Schema(description = "Token JWT de acesso", example = "eyJhbGciOi...")
    val accessToken: String,

    @param:Schema(description = "Tipo do token", example = "Bearer")
    val tokenType: String = "Bearer",

    @param:Schema(description = "Nome de usuário autenticado", example = "admin")
    val username: String,

    @param:Schema(description = "Tempo de expiração do token em segundos", example = "86400")
    val expiresInSeconds: Long,

    @param:Schema(description = "Perfil de autorização", example = "SYSTEM_ADMIN")
    val role: String = "SYSTEM_ADMIN"
)
