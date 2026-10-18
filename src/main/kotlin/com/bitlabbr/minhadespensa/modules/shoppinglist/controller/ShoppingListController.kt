package com.bitlabbr.minhadespensa.modules.shoppinglist.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.common.dto.ErrorResponse
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.service.ShoppingListService
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/shopping-lists")
@Tag(name = "Listas de Compras", description = "Endpoints para gerenciamento de listas de compras (planejadas, scratchpad e assistente) e itens associados")
class ShoppingListController(
    private val shoppingListService: ShoppingListService
) {

    @Operation(summary = "Listar todas as listas ativas", description = "Retorna todas as listas de compras ativas juntamente com seus itens")
    @GetMapping
    fun getAllLists(): ApiResponse<List<ShoppingListResponseDto>> {
        val lists = shoppingListService.getAllLists()
        return ApiResponse.ok(lists)
    }

    @Operation(summary = "Buscar lista por ID", description = "Recupera uma lista de compras específica e seus itens ativos")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lista encontrada"),
            SwaggerResponse(
                responseCode = "404",
                description = "Lista não encontrada",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getListById(@PathVariable id: String): ApiResponse<ShoppingListResponseDto> {
        val list = shoppingListService.getListById(id)
        return ApiResponse.ok(list)
    }

    @Operation(summary = "Criar nova lista de compras", description = "Cadastra uma lista de compras")
    @PostMapping
    fun createList(
        @Valid @RequestBody dto: ShoppingListRequestDto
    ): ResponseEntity<ApiResponse<ShoppingListResponseDto>> {
        val created = shoppingListService.saveList(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Lista de compras criada com sucesso."))
    }

    @Operation(summary = "Atualizar lista de compras", description = "Atualiza nome, tipo, status ou orçamento da lista")
    @PutMapping("/{id}")
    fun updateList(
        @PathVariable id: String,
        @Valid @RequestBody dto: ShoppingListRequestDto
    ): ApiResponse<ShoppingListResponseDto> {
        val toSave = dto.copy(id = id)
        val updated = shoppingListService.saveList(toSave)
        return ApiResponse.ok(updated, "Lista atualizada com sucesso.")
    }

    @Operation(summary = "Excluir lista de compras", description = "Realiza soft-delete da lista e de todos os itens vinculados")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteList(@PathVariable id: String) {
        shoppingListService.deleteList(id)
    }

    @Operation(summary = "Adicionar ou atualizar item na lista", description = "Salva um item avulso ou vinculado a um produto do catálogo")
    @PostMapping("/{listId}/items")
    fun saveItem(
        @PathVariable listId: String,
        @Valid @RequestBody dto: ShoppingItemRequestDto
    ): ResponseEntity<ApiResponse<ShoppingItemResponseDto>> {
        val toSave = dto.copy(listId = listId)
        val saved = shoppingListService.saveItem(toSave)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved, "Item salvo na lista."))
    }

    @Operation(summary = "Remover item da lista", description = "Realiza soft-delete do item da lista")
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable itemId: String) {
        shoppingListService.deleteItem(itemId)
    }
}
