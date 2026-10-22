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

package com.bitlabbr.minhadespensa.modules.sync.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import com.bitlabbr.minhadespensa.modules.sync.service.SyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sync")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sincronização Offline-First", description = "Motor de replicação bidirecional v2.1 entre os clientes móveis e o backend (CRDT / LWW / Casa Compartilhada)")
class SyncController(
    private val syncService: SyncService,
    private val householdService: HouseholdService
) {

    private val logger = LoggerFactory.getLogger(SyncController::class.java)

    @Operation(
        summary = "Sincronizar alterações locais (Push)",
        description = "Recebe um lote de alterações das 8 coleções de dados gravados offline no cliente, mescla com o banco via LWW e serializa com lock de versão da casa"
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lote processado e sincronizado com sucesso")
        ]
    )
    @PostMapping("/push")
    fun push(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador da casa", required = true) @RequestHeader("X-Household-Id") householdId: String,
        @Valid @RequestBody request: SyncPushRequestDto
    ): ApiResponse<SyncPushResponseDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.requireHouseholdAccess(userId, householdId)

        logger.info(
            "[Sync Push] Received batch from user={} for house={}: categories={}, products={}, lists={}, items={}, pantry={}, prices={}, movements={}, budgets={}",
            userId, householdId, request.categories.size, request.products.size, request.shoppingLists.size, request.shoppingItems.size,
            request.pantryItems.size, request.priceEntries.size, request.pantryMovements.size, request.monthlyBudgets.size
        )

        val result = syncService.push(householdId, request)

        logger.info(
            "[Sync Push OK] Processed {} items for house={}: serverVersion={}, remappedProducts={}, remappedCategories={}, rejected={}",
            result.totalProcessed, householdId, result.serverVersion, result.productIdMappings.size, result.categoryIdMappings.size, result.rejected.size
        )

        return ApiResponse.ok(result, "Sync completed successfully.")
    }

    @Operation(
        summary = "Baixar alterações do servidor (Pull)",
        description = "Retorna todos os registros das 8 coleções modificados ou excluídos com serverVersion superior a 'sinceVersion'"
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lote de alterações recuperado com sucesso")
        ]
    )
    @GetMapping("/pull")
    fun pull(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador da casa", required = true) @RequestHeader("X-Household-Id") householdId: String,
        @Parameter(description = "Versão monotônica de sincronização do último pull (cursor). Use 0 para carga inicial completa.", example = "0")
        @RequestParam(defaultValue = "0", name = "sinceVersion") sinceVersion: Long,
        @RequestParam(defaultValue = "0", name = "since", required = false) fallbackSince: Long
    ): ApiResponse<SyncPullResponseDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.requireHouseholdAccess(userId, householdId)

        val effectiveVersion = if (sinceVersion > 0L) sinceVersion else fallbackSince
        logger.info("[Sync Pull] User={} requested deltas for house={} sinceVersion={}", userId, householdId, effectiveVersion)

        val result = syncService.pull(householdId, effectiveVersion)

        logger.info(
            "[Sync Pull OK] Returned deltas for house={}: categories={}, products={}, lists={}, items={}, pantry={}, priceEntries={}, pantryMovements={}, budgets={}, serverVersion={}",
            householdId, result.categories.size, result.products.size, result.shoppingLists.size, result.shoppingItems.size,
            result.pantryItems.size, result.priceEntries.size, result.pantryMovements.size, result.monthlyBudgets.size, result.serverVersion
        )

        return ApiResponse.ok(result)
    }
}
