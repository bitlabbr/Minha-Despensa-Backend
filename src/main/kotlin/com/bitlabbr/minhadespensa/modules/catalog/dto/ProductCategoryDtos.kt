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

package com.bitlabbr.minhadespensa.modules.catalog.dto

import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductCategoryEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "DTO de categoria de produto para sincronização bidirecional")
data class ProductCategorySyncDto(
    @Schema(description = "UUID da categoria", example = "6ba7b811-9dad-11d1-80b4-00c04fd430c8")
    val id: String,

    @Schema(description = "Nome de exibição da categoria", example = "Laticínios")
    @NotBlank(message = "O nome da categoria é obrigatório")
    val name: String,

    @Schema(description = "Indica se é uma categoria padrão do sistema", example = "true")
    val isDefault: Boolean = false,

    @Schema(description = "Timestamp da última modificação em milissegundos UTC")
    val updatedAt: Long = System.currentTimeMillis(),

    @Schema(description = "Flag de exclusão lógica (soft delete)")
    val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(entity: ProductCategoryEntity): ProductCategorySyncDto =
            ProductCategorySyncDto(
                id = entity.id,
                name = entity.name,
                isDefault = entity.isDefault,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted
            )
    }
}

data class ProductCategoryUpsertResult(
    val entity: ProductCategoryEntity,
    val aliasId: String? = null,
    val canonicalId: String
)
