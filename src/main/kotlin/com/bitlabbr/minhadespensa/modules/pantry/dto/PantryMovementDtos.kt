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

package com.bitlabbr.minhadespensa.modules.pantry.dto

import com.bitlabbr.minhadespensa.modules.pantry.domain.MovementType
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryMovementEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "DTO de movimentação de estoque/despensa para sincronização")
data class PantryMovementSyncDto(
    @Schema(description = "UUID da movimentação", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @Schema(description = "UUID do item de despensa associado (opcional)", example = "550e8400-e29b-41d4-a716-446655440001")
    val pantryItemId: String? = null,

    @Schema(description = "UUID do produto no catálogo", example = "550e8400-e29b-41d4-a716-446655440002")
    @NotBlank(message = "Product ID is required")
    val productId: String,

    @Schema(description = "Tipo de movimentação de estoque", example = "ENTRY_PURCHASE")
    val type: MovementType = MovementType.ENTRY_MANUAL,

    @Schema(description = "Quantidade movimentada", example = "2.0")
    val quantity: Double = 1.0,

    @Schema(description = "Preço unitário em centavos", example = "650")
    val unitPriceInCents: Long? = null,

    @Schema(description = "Preço total em centavos", example = "1300")
    val totalPriceInCents: Long? = null,

    @Schema(description = "Estabelecimento ou motivo do descarte/consumo", example = "Supermercado Pão de Açúcar")
    val storeOrReason: String? = null,

    @Schema(description = "Timestamp da última modificação em milissegundos UTC")
    val updatedAt: Long = System.currentTimeMillis(),

    @Schema(description = "Flag de exclusão lógica (soft delete)")
    val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(entity: PantryMovementEntity): PantryMovementSyncDto =
            PantryMovementSyncDto(
                id = entity.id,
                pantryItemId = entity.pantryItemId,
                productId = entity.productId,
                type = entity.type,
                quantity = entity.quantity,
                unitPriceInCents = entity.unitPriceInCents,
                totalPriceInCents = entity.totalPriceInCents,
                storeOrReason = entity.storeOrReason,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted
            )
    }
}
