package com.bitlabbr.minhadespensa.modules.shoppinglist.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Tipo de lista de compras")
enum class ShoppingListType {
    SCRATCHPAD,
    PLANNED,
    ASSISTANT
}

@Schema(description = "Status do ciclo de vida da lista de compras")
enum class ShoppingListStatus {
    DRAFT,
    SHOPPING,
    COMPLETED,
    CANCELLED
}
