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
 *                        the license, and indicate if changes stranded were made.
 *     - NonCommercial  — You may not use the material for commercial purposes.
 *
 *   Owner rights:
 *     - Willian Santos retains all commercial rights.
 *    - The copyright holder may use, sell, sublicense, or relicense this
 *       work under different terms at any time.
 *
 *   Full license: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package com.bitlabbr.minhadespensa.modules.household.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Payload para criação de nova casa compartilhada")
data class CreateHouseholdRequestDto(
    @Schema(description = "Nome de identificação da casa", example = "Casa de Praia")
    @NotBlank(message = "Household name is required")
    @Size(min = 2, max = 80, message = "Household name must be between 2 and 80 characters")
    val name: String
)

@Schema(description = "Payload para atualização de nome da casa")
data class UpdateHouseholdRequestDto(
    @Schema(description = "Novo nome da casa", example = "Nossa Casa")
    @NotBlank(message = "Household name is required")
    @Size(min = 2, max = 80, message = "Household name must be between 2 and 80 characters")
    val name: String
)

@Schema(description = "Payload para ingressar em uma casa existente por código de convite")
data class JoinHouseholdRequestDto(
    @Schema(description = "Código de convite da casa (ex: CASA-XXXXXXXX)", example = "CASA-7X9K2M3P")
    @NotBlank(message = "Invite code is required")
    val inviteCode: String
)

@Schema(description = "Dados de um membro integrante da casa")
data class HouseholdMemberResponseDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val joinedAt: Long
)

@Schema(description = "Detalhes completos de uma casa compartilhada")
data class HouseholdDetailResponseDto(
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val syncVersion: Long,
    val members: List<HouseholdMemberResponseDto> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

@Schema(description = "Novo código de convite gerado")
data class InviteCodeResponseDto(
    val inviteCode: String
)
