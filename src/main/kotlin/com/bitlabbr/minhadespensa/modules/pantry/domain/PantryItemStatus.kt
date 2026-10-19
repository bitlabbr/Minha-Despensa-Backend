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
