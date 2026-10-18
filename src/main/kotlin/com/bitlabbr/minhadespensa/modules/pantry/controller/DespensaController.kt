package com.bitlabbr.minhadespensa.modules.pantry.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.common.dto.ErrorResponse
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryAggregatedResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryOccurrenceResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/despensa")
@Tag(name = "Despensa (Português)", description = "Contratos em português para o módulo da Despensa e controle de ocorrências/lotes")
class DespensaController(
    private val pantryService: PantryService
) {

    @Operation(summary = "Resumo do estoque agregado", description = "Retorna o resumo do estoque agregado (o que tem hoje na despensa)")
    @GetMapping
    fun getResumoEstoque(): ApiResponse<List<PantryAggregatedResponseDto>> {
        val aggregated = pantryService.getAggregatedPantry()
        return ApiResponse.ok(aggregated)
    }

    @Operation(summary = "Ocorrências ativas de um produto", description = "Retorna os lotes/compras ativas daquele produto ao clicar nele")
    @GetMapping("/produtos/{produtoId}/ocorrencias")
    fun getOcorrenciasProduto(
        @Parameter(description = "ID único do produto catalogado")
        @PathVariable produtoId: String
    ): ApiResponse<List<PantryOccurrenceResponseDto>> {
        val occurrences = pantryService.getOccurrencesByProductId(produtoId)
        return ApiResponse.ok(occurrences)
    }

    @Operation(summary = "Cadastrar entrada de lote", description = "Registra a entrada manual de uma nova compra/lote")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Item/lote adicionado com sucesso"),
            SwaggerResponse(
                responseCode = "400",
                description = "Dados inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/itens")
    fun criarItem(
        @Valid @RequestBody dto: PantryItemRequestDto
    ): ResponseEntity<ApiResponse<PantryItemResponseDto>> {
        val created = pantryService.saveItem(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Item adicionado à despensa."))
    }

    @Operation(summary = "Consumir item/lote", description = "Dá baixa na quantidade consumida")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Item consumido com sucesso"),
            SwaggerResponse(
                responseCode = "400",
                description = "Quantidade inválida",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            SwaggerResponse(
                responseCode = "404",
                description = "Item não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            SwaggerResponse(
                responseCode = "422",
                description = "Estoque insuficiente para consumo",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PatchMapping("/itens/{itemId}/consumir")
    fun consumirItem(
        @PathVariable itemId: String,
        @Parameter(description = "Quantidade a ser consumida (padrão: 1.0)")
        @RequestParam(required = false, defaultValue = "1.0") quantidade: Double?,
        @RequestParam(required = false) quantity: Double?
    ): ApiResponse<PantryItemResponseDto> {
        val toConsume = quantidade ?: quantity ?: 1.0
        val updated = pantryService.consumeItem(itemId, toConsume)
        return ApiResponse.ok(updated, "Quantidade consumida com sucesso.")
    }

    @Operation(summary = "Descartar item/lote", description = "Marca o lote como descartado/vencido com saldo zerado")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Item descartado com sucesso"),
            SwaggerResponse(
                responseCode = "404",
                description = "Item não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PatchMapping("/itens/{itemId}/descartar")
    fun descartarItem(@PathVariable itemId: String): ApiResponse<PantryItemResponseDto> {
        val updated = pantryService.discardItem(itemId)
        return ApiResponse.ok(updated, "Item descartado com sucesso.")
    }

    @Operation(summary = "Buscar item por ID", description = "Retorna os detalhes de um item na despensa pelo seu ID")
    @GetMapping("/itens/{itemId}")
    fun getItemPorId(@PathVariable itemId: String): ApiResponse<PantryItemResponseDto> {
        val item = pantryService.getItemById(itemId)
        return ApiResponse.ok(item)
    }

    @Operation(summary = "Remover item da despensa", description = "Realiza exclusão lógica (soft-delete)")
    @DeleteMapping("/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removerItem(@PathVariable itemId: String) {
        pantryService.deleteItem(itemId)
    }
}
