package com.bitlabbr.minhadespensa.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Estrutura padrão para retorno de erros da API")
data class ErrorResponse(
    @Schema(description = "Timestamp UTC do momento do erro")
    val timestamp: Instant = Instant.now(),

    @Schema(description = "Código de status HTTP retornado", example = "404")
    val status: Int,

    @Schema(description = "Descrição curta do status HTTP", example = "Not Found")
    val error: String,

    @Schema(description = "Mensagem detalhada do erro", example = "Produto com ID '123' não encontrado.")
    val message: String,

    @Schema(description = "Caminho do endpoint da requisição que gerou o erro", example = "/api/v1/products/123")
    val path: String,

    @Schema(description = "Lista detalhada de inconsistências de validação por campo, se houver")
    val fieldErrors: List<FieldErrorDetail>? = null
)

@Schema(description = "Detalhe do erro de validação de um campo específico")
data class FieldErrorDetail(
    @Schema(description = "Nome do campo com erro", example = "name")
    val field: String,

    @Schema(description = "Mensagem explicativa da validação que falhou", example = "O nome do produto não pode ficar em branco")
    val message: String
)
