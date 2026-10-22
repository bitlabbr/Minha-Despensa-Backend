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

package com.bitlabbr.minhadespensa.modules.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Payload para registro de novo usuário individual")
data class RegisterRequestDto(
    @Schema(description = "Nome completo do usuário", example = "Willian Santos")
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome não pode exceder 150 caracteres")
    val name: String,

    @Schema(description = "E-mail de acesso único", example = "willian@exemplo.com")
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    val email: String,

    @Schema(description = "Senha de acesso (mínimo 8 caracteres)", example = "SegredoForte@123")
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres")
    val password: String
)

@Schema(description = "Payload para autenticação (Login)")
data class LoginRequestDto(
    @Schema(description = "E-mail cadastrado", example = "willian@exemplo.com")
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    val email: String,

    @Schema(description = "Senha cadastrada", example = "SegredoForte@123")
    @NotBlank(message = "A senha é obrigatória")
    val password: String
)

@Schema(description = "Payload para renovação de sessão por Refresh Token")
data class RefreshTokenRequestDto(
    @Schema(description = "Refresh token opaco emitido no login/registro")
    @NotBlank(message = "O refresh token é obrigatório")
    val refreshToken: String
)

@Schema(description = "Resumo dos dados do usuário autenticado")
data class UserSummaryDto(
    val id: String,
    val name: String,
    val email: String,
    val defaultHouseholdId: String? = null
)

@Schema(description = "Resumo dos dados da casa associada ao usuário")
data class HouseholdSummaryDto(
    val id: String,
    val name: String,
    val inviteCode: String,
    val role: String
)

@Schema(description = "Resposta de autenticação contendo par de tokens e dados do usuário")
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserSummaryDto,
    val activeHousehold: HouseholdSummaryDto? = null
)
