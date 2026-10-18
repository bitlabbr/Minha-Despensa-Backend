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

package com.bitlabbr.minhadespensa.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Envelope padrão de respostas bem-sucedidas da API")
data class ApiResponse<T>(
    @Schema(description = "Indica se a operação foi bem-sucedida", example = "true")
    val success: Boolean = true,

    @Schema(description = "Mensagem descritiva opcional", example = "Operação realizada com sucesso")
    val message: String? = null,

    @Schema(description = "Dados retornados pela operação")
    val data: T? = null,

    @Schema(description = "Timestamp UTC do processamento da requisição")
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> ok(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun ok(message: String): ApiResponse<Unit> =
            ApiResponse(success = true, message = message, data = null)
    }
}
