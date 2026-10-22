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

package com.bitlabbr.minhadespensa.modules.household.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.household.dto.CreateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.HouseholdDetailResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.InviteCodeResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.JoinHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.UpdateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/households")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Casas Compartilhadas (Households)", description = "Gestão de casas, membros da família e códigos de convite compartilhados")
class HouseholdController(
    private val householdService: HouseholdService
) {

    private val logger = LoggerFactory.getLogger(HouseholdController::class.java)

    @Operation(summary = "Listar casas do usuário", description = "Retorna todas as casas das quais o usuário autenticado é membro, acompanhadas dos integrantes e código de convite.")
    @GetMapping
    fun getUserHouseholds(@AuthenticationPrincipal jwt: Jwt): ApiResponse<List<HouseholdDetailResponseDto>> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        logger.info("[Households] User {} requested their households list", userId)
        val result = householdService.getUserHouseholds(userId)
        logger.info("[Households] User {} retrieved {} household(s)", userId, result.size)
        return ApiResponse.ok(result)
    }

    @Operation(summary = "Criar nova casa compartilhada", description = "Cria uma nova casa e associa o usuário criador como OWNER.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Casa criada com sucesso"),
            SwaggerResponse(responseCode = "400", description = "Dados inválidos")
        ]
    )
    @PostMapping
    fun createHousehold(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody dto: CreateHouseholdRequestDto
    ): ResponseEntity<ApiResponse<HouseholdDetailResponseDto>> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        val result = householdService.createHousehold(userId, dto)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(result, "Household created successfully."))
    }

    @Operation(summary = "Atualizar dados da casa", description = "Permite alterar o nome de identificação da casa.")
    @PutMapping("/{id}")
    fun updateHousehold(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador UUID da casa", required = true) @PathVariable id: String,
        @Valid @RequestBody dto: UpdateHouseholdRequestDto
    ): ApiResponse<HouseholdDetailResponseDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        val result = householdService.updateHousehold(id, userId, dto)
        return ApiResponse.ok(result, "Household updated successfully.")
    }

    @Operation(summary = "Ingressar em uma casa existente", description = "Associa o usuário autenticado a uma casa existente a partir de seu código de convite (CASA-XXXXXXXX).")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Ingresso realizado com sucesso"),
            SwaggerResponse(responseCode = "404", description = "Código de convite não encontrado")
        ]
    )
    @PostMapping("/join")
    fun joinHousehold(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody dto: JoinHouseholdRequestDto
    ): ApiResponse<HouseholdDetailResponseDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        val result = householdService.joinHousehold(userId, dto)
        return ApiResponse.ok(result, "Joined household successfully.")
    }

    @Operation(summary = "Sair de uma casa compartilhada", description = "Remove o vínculo do usuário com a casa indicada.")
    @PostMapping("/{id}/leave")
    fun leaveHousehold(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador UUID da casa", required = true) @PathVariable id: String
    ): ApiResponse<Unit> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.leaveHousehold(id, userId)
        return ApiResponse.ok("Left household successfully.")
    }

    @Operation(summary = "Regenerar código de convite da casa", description = "Apenas o proprietário (OWNER) pode regenerar o código, invalidando o anterior.")
    @PostMapping("/{id}/invite-code/regenerate")
    fun regenerateInviteCode(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador UUID da casa", required = true) @PathVariable id: String
    ): ApiResponse<InviteCodeResponseDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        val result = householdService.regenerateInviteCode(id, userId)
        return ApiResponse.ok(result, "Invite code regenerated successfully.")
    }

    @Operation(summary = "Remover membro da casa", description = "Apenas o proprietário (OWNER) pode remover outros membros da casa.")
    @DeleteMapping("/{id}/members/{memberUserId}")
    fun removeMember(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador UUID da casa", required = true) @PathVariable id: String,
        @Parameter(description = "UUID do membro a ser removido", required = true) @PathVariable memberUserId: String
    ): ApiResponse<Unit> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.removeMember(id, userId, memberUserId)
        return ApiResponse.ok("Member removed from household successfully.")
    }
}
