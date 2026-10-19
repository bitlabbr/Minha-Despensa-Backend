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

package com.bitlabbr.minhadespensa.modules.invoice.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Item extraído da nota fiscal")
data class InvoiceItemResponseDto(
    @field:Schema(description = "Código de barras global (EAN / GTIN), se identificado", example = "7891234567890")
    val ean: String? = null,

    @field:Schema(description = "Descrição comercial do produto", example = "ARROZ BRANCO TIPO 1 1KG")
    val name: String,

    @field:Schema(description = "Quantidade adquirida", example = "2.0")
    val quantity: Double,

    @field:Schema(description = "Unidade de medida na nota fiscal (UN, KG, G, L, ML, etc.)", example = "KG")
    val measureUnit: String = "UN",

    @field:Schema(description = "Preço unitário em centavos", example = "650")
    val unitPriceInCents: Long? = null,

    @field:Schema(description = "Preço total do item em centavos", example = "1300")
    val totalPriceInCents: Long? = null
)
