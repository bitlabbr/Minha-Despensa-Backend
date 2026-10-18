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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pantry")
@Tag(name = "Despensa", description = "Endpoints para gerenciamento do estoque doméstico, lotes e controle de consumo")
class PantryController(
    private val pantryService: PantryService
) {

    @Operation(summary = "Listar itens da despensa", description = "Retorna todos os itens ativos na despensa ou filtra por ID de produto")
    @GetMapping
    fun listItems(
        @Parameter(description = "ID opcional do produto para filtrar estoque específico")
        @RequestParam(required = false) productId: String?
    ): ApiResponse<List<PantryItemResponseDto>> {
        val items = if (productId.isNullOrBlank()) {
            pantryService.getAllItems()
        } else {
            pantryService.getItemsByProductId(productId)
        }
        return ApiResponse.ok(items)
    }

    @Operation(summary = "Estoque agregado da despensa", description = "Retorna o resumo do estoque agrupado por produto com suas ocorrências ativas")
    @GetMapping("/aggregated")
    fun getAggregatedPantry(): ApiResponse<List<PantryAggregatedResponseDto>> {
        val aggregated = pantryService.getAggregatedPantry()
        return ApiResponse.ok(aggregated)
    }

    @Operation(summary = "Ocorrências ativas por produto", description = "Lista todos os lotes e compras ativas de um determinado produto")
    @GetMapping("/products/{productId}/occurrences")
    fun getOccurrencesByProduct(
        @PathVariable productId: String
    ): ApiResponse<List<PantryOccurrenceResponseDto>> {
        val occurrences = pantryService.getOccurrencesByProductId(productId)
        return ApiResponse.ok(occurrences)
    }

    @Operation(summary = "Buscar item por ID", description = "Recupera os detalhes de um item na despensa pelo seu UUID")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Item encontrado com sucesso"),
            SwaggerResponse(
                responseCode = "404",
                description = "Item não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getItemById(@PathVariable id: String): ApiResponse<PantryItemResponseDto> {
        val item = pantryService.getItemById(id)
        return ApiResponse.ok(item)
    }

    @Operation(summary = "Adicionar item à despensa", description = "Cadastra uma nova quantidade/lote de produto na despensa")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Item adicionado com sucesso"),
            SwaggerResponse(
                responseCode = "400",
                description = "Dados inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping(value = ["", "/items"])
    fun createItem(
        @Valid @RequestBody dto: PantryItemRequestDto
    ): ResponseEntity<ApiResponse<PantryItemResponseDto>> {
        val created = pantryService.saveItem(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Item adicionado à despensa."))
    }

    @Operation(summary = "Atualizar item da despensa", description = "Atualiza a quantidade ou informações de validade de um item")
    @PutMapping("/{id}")
    fun updateItem(
        @PathVariable id: String,
        @Valid @RequestBody dto: PantryItemRequestDto
    ): ApiResponse<PantryItemResponseDto> {
        val toSave = dto.copy(id = id)
        val updated = pantryService.saveItem(toSave)
        return ApiResponse.ok(updated, "Item da despensa atualizado.")
    }

    @Operation(summary = "Consumir item da despensa", description = "Dá baixa na quantidade consumida de um determinado lote")
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
    @PatchMapping("/items/{id}/consume")
    fun consumeItem(
        @PathVariable id: String,
        @Parameter(description = "Quantidade a ser consumida (padrão: 1.0)")
        @RequestParam(required = false, defaultValue = "1.0") quantity: Double?,
        @Parameter(description = "Quantidade alternativa em português")
        @RequestParam(required = false) quantidade: Double?
    ): ApiResponse<PantryItemResponseDto> {
        val toConsume = quantidade ?: quantity ?: 1.0
        val updated = pantryService.consumeItem(id, toConsume)
        return ApiResponse.ok(updated, "Quantidade consumida com sucesso.")
    }

    @Operation(summary = "Descartar item da despensa", description = "Marca um lote como descartado/vencido com saldo zerado")
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
    @PatchMapping("/items/{id}/discard")
    fun discardItem(@PathVariable id: String): ApiResponse<PantryItemResponseDto> {
        val updated = pantryService.discardItem(id)
        return ApiResponse.ok(updated, "Item descartado com sucesso.")
    }

    @Operation(summary = "Remover item da despensa", description = "Realiza exclusão lógica (soft-delete) do item")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable id: String) {
        pantryService.deleteItem(id)
    }
}
