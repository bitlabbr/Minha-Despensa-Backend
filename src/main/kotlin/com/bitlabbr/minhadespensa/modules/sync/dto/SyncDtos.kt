package com.bitlabbr.minhadespensa.modules.sync.dto

import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "Payload de envio (Push) contendo alterações locais para sincronização com o backend")
data class SyncPushRequestDto(
    @field:Schema(description = "Lista de alterações em produtos do catálogo")
    @field:Valid
    val products: List<ProductRequestDto> = emptyList(),

    @field:Schema(description = "Lista de alterações em itens da despensa")
    @field:Valid
    val pantryItems: List<PantryItemRequestDto> = emptyList(),

    @field:Schema(description = "Lista de alterações em listas de compras")
    @field:Valid
    val shoppingLists: List<ShoppingListRequestDto> = emptyList(),

    @field:Schema(description = "Lista de alterações em itens das listas de compras")
    @field:Valid
    val shoppingItems: List<ShoppingItemRequestDto> = emptyList(),

    @field:Schema(description = "Lista de alterações em histórico de preços")
    @field:Valid
    val priceEntries: List<PriceEntryRequestDto> = emptyList()
)

@Schema(description = "Resultado do processamento de sincronização (Push)")
data class SyncPushResponseDto(
    @Schema(description = "Timestamp do servidor após a conclusão do processamento em milissegundos UTC")
    val serverTimestamp: Long = System.currentTimeMillis(),

    @Schema(description = "Total de registros processados com sucesso")
    val totalProcessed: Int
)

@Schema(description = "Payload de recepção (Pull) contendo alterações registradas no servidor desde o último timestamp")
data class SyncPullResponseDto(
    @Schema(description = "Timestamp do servidor no momento da extração do lote (usar como novo ponteiro 'since')")
    val serverTimestamp: Long = System.currentTimeMillis(),

    @Schema(description = "Produtos alterados ou excluídos desde 'since'")
    val products: List<ProductResponseDto> = emptyList(),

    @Schema(description = "Itens da despensa alterados ou excluídos desde 'since'")
    val pantryItems: List<PantryItemResponseDto> = emptyList(),

    @Schema(description = "Listas de compras alteradas ou excluídas desde 'since'")
    val shoppingLists: List<ShoppingListResponseDto> = emptyList(),

    @Schema(description = "Itens de listas de compras alterados ou excluídos desde 'since'")
    val shoppingItems: List<ShoppingItemResponseDto> = emptyList(),

    @Schema(description = "Registros de preços alterados ou excluídos desde 'since'")
    val priceEntries: List<PriceEntryResponseDto> = emptyList()
)
