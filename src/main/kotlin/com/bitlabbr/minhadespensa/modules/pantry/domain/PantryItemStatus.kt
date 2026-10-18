package com.bitlabbr.minhadespensa.modules.pantry.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Status do item/lote na despensa")
enum class PantryItemStatus {
    EM_ESTOQUE,
    CONSUMIDO,
    DESCARTADO;

    companion object {
        val IN_STOCK get() = EM_ESTOQUE
        val CONSUMED get() = CONSUMIDO
        val DISCARDED get() = DESCARTADO

        @JsonCreator
        @JvmStatic
        fun fromValue(value: String?): PantryItemStatus {
            if (value.isNullOrBlank()) return EM_ESTOQUE
            return when (value.trim().uppercase()) {
                "EM_ESTOQUE", "IN_STOCK", "ACTIVE", "ESTOQUE" -> EM_ESTOQUE
                "CONSUMIDO", "CONSUMED" -> CONSUMIDO
                "DESCARTADO", "DISCARDED", "EXPIRED", "VENCIDO" -> DESCARTADO
                else -> runCatching { valueOf(value.trim().uppercase()) }.getOrDefault(EM_ESTOQUE)
            }
        }
    }
}

typealias StatusItem = PantryItemStatus
