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

package com.bitlabbr.minhadespensa.modules.shoppinglist.dto

import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListStatus
import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListType
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Dados para criação ou atualização de uma lista de compras")
data class ShoppingListRequestDto(
    @Schema(description = "ID único da lista (UUID)", example = "4fa85f64-5717-4562-b3fc-2c963f66afa7")
    val id: String? = null,

    @Schema(description = "Nome da lista de compras", example = "Compras do Mês")
    @NotBlank(message = "List name is required")
    @Size(max = 255, message = "List name must not exceed 255 characters")
    val name: String,

    @Schema(description = "Tipo da lista", example = "PLANNED")
    val type: ShoppingListType = ShoppingListType.PLANNED,

    @Schema(description = "Status da lista", example = "DRAFT")
    val status: ShoppingListStatus = ShoppingListStatus.DRAFT,

    @Schema(description = "Orçamento máximo estipulado em centavos", example = "50000")
    val budgetInCents: Long? = null,

    @Schema(description = "Timestamp da última modificação (epoch millis UTC)")
    val updatedAt: Long? = null,

    @Schema(description = "Indica se a lista foi marcada como excluída", example = "false")
    val isDeleted: Boolean = false
)

@Schema(description = "Dados de uma lista de compras retornada pela API")
data class ShoppingListResponseDto(
    @Schema(description = "ID único da lista (UUID)", example = "4fa85f64-5717-4562-b3fc-2c963f66afa7")
    val id: String,

    @Schema(description = "Nome da lista", example = "Compras do Mês")
    val name: String,

    @Schema(description = "Tipo da lista", example = "PLANNED")
    val type: ShoppingListType,

    @Schema(description = "Status da lista", example = "DRAFT")
    val status: ShoppingListStatus,

    @Schema(description = "Orçamento em centavos", example = "50000")
    val budgetInCents: Long?,

    @Schema(description = "Itens pertencentes a esta lista")
    val items: List<ShoppingItemResponseDto> = emptyList(),

    @Schema(description = "Timestamp da última modificação", example = "1727280000000")
    val updatedAt: Long,

    @Schema(description = "Versão monotônica de sincronização da casa", example = "42")
    val serverVersion: Long = 0L,

    @Schema(description = "Indica se a lista foi excluída", example = "false")
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(entity: ShoppingListEntity, items: List<ShoppingItemEntity> = emptyList()): ShoppingListResponseDto =
            ShoppingListResponseDto(
                id = entity.id,
                name = entity.name,
                type = entity.type,
                status = entity.status,
                budgetInCents = entity.budgetInCents,
                items = items.map { ShoppingItemResponseDto.fromEntity(it) },
                updatedAt = entity.updatedAt,
                serverVersion = entity.serverVersion,
                isDeleted = entity.isDeleted
            )
    }
}

@Schema(description = "Dados para criação ou atualização de um item da lista de compras")
data class ShoppingItemRequestDto(
    @Schema(description = "ID do item (UUID)", example = "5fa85f64-5717-4562-b3fc-2c963f66afa8")
    val id: String? = null,

    @Schema(description = "ID da lista de compras associada", example = "4fa85f64-5717-4562-b3fc-2c963f66afa7")
    @NotBlank(message = "List ID is required")
    val listId: String,

    @Schema(description = "ID do produto do catálogo, se houver vínculo", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val productId: String? = null,

    @Schema(description = "Texto livre caso o item não esteja no catálogo", example = "Comprar pão francês quentinho")
    @Size(max = 255, message = "Raw text must not exceed 255 characters")
    val rawText: String? = null,

    @Schema(description = "Quantidade", example = "2.0")
    val quantity: Double = 1.0,

    @Schema(description = "Preço unitário em centavos", example = "450")
    val priceAtTime: Long? = null,

    @Schema(description = "Marcado no carrinho", example = "false")
    val isChecked: Boolean = false,

    @Schema(description = "Timestamp da última modificação")
    val updatedAt: Long? = null,

    @Schema(description = "Indica se o item foi marcado como excluído", example = "false")
    val isDeleted: Boolean = false
)

@Schema(description = "Dados de um item da lista de compras retornado pela API")
data class ShoppingItemResponseDto(
    @Schema(description = "ID único do item (UUID)", example = "5fa85f64-5717-4562-b3fc-2c963f66afa8")
    val id: String,

    @Schema(description = "ID da lista associada", example = "4fa85f64-5717-4562-b3fc-2c963f66afa7")
    val listId: String,

    @Schema(description = "ID do produto vinculado", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val productId: String?,

    @Schema(description = "Texto livre do item", example = "Comprar pão francês quentinho")
    val rawText: String?,

    @Schema(description = "Quantidade", example = "2.0")
    val quantity: Double,

    @Schema(description = "Preço unitário em centavos", example = "450")
    val priceAtTime: Long?,

    @Schema(description = "Marcado no carrinho", example = "false")
    val isChecked: Boolean,

    @Schema(description = "Timestamp da última modificação", example = "1727280000000")
    val updatedAt: Long,

    @Schema(description = "Versão monotônica de sincronização da casa", example = "42")
    val serverVersion: Long = 0L,

    @Schema(description = "Indica se foi excluído", example = "false")
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(entity: ShoppingItemEntity): ShoppingItemResponseDto =
            ShoppingItemResponseDto(
                id = entity.id,
                listId = entity.listId,
                productId = entity.productId,
                rawText = entity.rawText,
                quantity = entity.quantity,
                priceAtTime = entity.priceAtTime,
                isChecked = entity.isChecked,
                updatedAt = entity.updatedAt,
                serverVersion = entity.serverVersion,
                isDeleted = entity.isDeleted
            )
    }
}
