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
