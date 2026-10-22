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

package com.bitlabbr.minhadespensa.modules.sync.dto

import com.bitlabbr.minhadespensa.modules.budget.dto.MonthlyBudgetSyncDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategorySyncDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryMovementSyncDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "Detalhes de um item rejeitado durante a sincronização")
data class SyncRejectedItemDto(
    @Schema(description = "Tipo da entidade (ex: PANTRY_ITEM, PRICE_ENTRY)", example = "PANTRY_ITEM")
    val entityType: String,

    @Schema(description = "ID do item rejeitado", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val id: String,

    @Schema(description = "Motivo da rejeição", example = "Produto referenciado não encontrado na casa")
    val reason: String
)

@Schema(description = "Payload de envio (Push) contendo alterações locais das 8 coleções para sincronização com o backend")
data class SyncPushRequestDto(
    @Schema(description = "Lista de categorias de produtos")
    @Valid
    val categories: List<ProductCategorySyncDto> = emptyList(),

    @Schema(description = "Lista de alterações em produtos do catálogo")
    @Valid
    val products: List<ProductRequestDto> = emptyList(),

    @Schema(description = "Lista de alterações em listas de compras")
    @Valid
    val shoppingLists: List<ShoppingListRequestDto> = emptyList(),

    @Schema(description = "Lista de alterações em itens das listas de compras")
    @Valid
    val shoppingItems: List<ShoppingItemRequestDto> = emptyList(),

    @Schema(description = "Lista de alterações em itens da despensa (estoque)")
    @Valid
    val pantryItems: List<PantryItemRequestDto> = emptyList(),

    @Schema(description = "Lista de alterações em histórico de preços")
    @Valid
    val priceEntries: List<PriceEntryRequestDto> = emptyList(),

    @Schema(description = "Lista de movimentações de estoque (livro-razão)")
    @Valid
    val pantryMovements: List<PantryMovementSyncDto> = emptyList(),

    @Schema(description = "Lista de orçamentos mensais")
    @Valid
    val monthlyBudgets: List<MonthlyBudgetSyncDto> = emptyList()
)

@Schema(description = "Resultado do processamento de sincronização (Push)")
data class SyncPushResponseDto(
    @Schema(description = "Timestamp do servidor após a conclusão do processamento em milissegundos UTC")
    val serverTimestamp: Long = System.currentTimeMillis(),

    @Schema(description = "Versão monotônica de sincronização atribuída a este lote pela casa", example = "42")
    val serverVersion: Long = 0L,

    @Schema(description = "Total de registros processados com sucesso", example = "15")
    val totalProcessed: Int = 0,

    @Schema(description = "Mapeamento de IDs de produtos remapeados para canônicos")
    val productIdMappings: Map<String, String> = emptyMap(),

    @Schema(description = "Mapeamento de IDs de categorias remapeadas para canônicas")
    val categoryIdMappings: Map<String, String> = emptyMap(),

    @Schema(description = "Lista de itens rejeitados que não puderam ser persistidos")
    val rejected: List<SyncRejectedItemDto> = emptyList()
)

@Schema(description = "Payload de recepção (Pull) contendo alterações registradas no servidor das 8 coleções")
data class SyncPullResponseDto(
    @Schema(description = "Timestamp do servidor no momento da extração do lote em milissegundos UTC")
    val serverTimestamp: Long = System.currentTimeMillis(),

    @Schema(description = "Versão monotônica de sincronização atual da casa", example = "42")
    val serverVersion: Long = 0L,

    @Schema(description = "Indica se há mais páginas disponíveis para sincronização", example = "false")
    val hasMore: Boolean = false,

    @Schema(description = "Categorias alteradas ou excluídas")
    val categories: List<ProductCategorySyncDto> = emptyList(),

    @Schema(description = "Produtos alterados ou excluídos")
    val products: List<ProductResponseDto> = emptyList(),

    @Schema(description = "Listas de compras alteradas ou excluídas")
    val shoppingLists: List<ShoppingListResponseDto> = emptyList(),

    @Schema(description = "Itens de listas de compras alterados ou excluídos")
    val shoppingItems: List<ShoppingItemResponseDto> = emptyList(),

    @Schema(description = "Itens da despensa alterados ou excluídos")
    val pantryItems: List<PantryItemResponseDto> = emptyList(),

    @Schema(description = "Registros de preços alterados ou excluídos")
    val priceEntries: List<PriceEntryResponseDto> = emptyList(),

    @Schema(description = "Movimentações de despensa alteradas ou excluídas")
    val pantryMovements: List<PantryMovementSyncDto> = emptyList(),

    @Schema(description = "Orçamentos mensais alterados ou excluídos")
    val monthlyBudgets: List<MonthlyBudgetSyncDto> = emptyList()
)
