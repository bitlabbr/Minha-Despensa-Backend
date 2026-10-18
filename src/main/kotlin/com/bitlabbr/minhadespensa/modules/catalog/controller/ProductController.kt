package com.bitlabbr.minhadespensa.modules.catalog.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.common.dto.ErrorResponse
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v1/products", "/api/v1/produtos"])
@Tag(name = "Catálogo de Produtos", description = "Endpoints para gerenciamento do catálogo de produtos e consultas por código de barras")
class ProductController(
    private val productService: ProductService
) {

    @Operation(summary = "Listar ou buscar produtos", description = "Retorna todos os produtos ativos ou filtra por nome, marca ou EAN")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso")
        ]
    )
    @GetMapping
    fun listProducts(
        @Parameter(description = "Termo de busca opcional (nome, marca ou código EAN)")
        @RequestParam(required = false) q: String?,
        @Parameter(description = "Termo de busca alternativo (nome, marca ou código EAN)")
        @RequestParam(required = false) busca: String?
    ): ApiResponse<List<ProductResponseDto>> {
        val searchTerm = q ?: busca
        val result = if (searchTerm.isNullOrBlank()) {
            productService.getAllProducts()
        } else {
            productService.searchProducts(searchTerm)
        }
        return ApiResponse.ok(result)
    }

    @Operation(summary = "Buscar produto por ID", description = "Recupera os detalhes de um produto através do seu UUID")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Produto encontrado com sucesso"),
            SwaggerResponse(
                responseCode = "404",
                description = "Produto não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getProductById(
        @Parameter(description = "Identificador único (UUID) do produto")
        @PathVariable id: String
    ): ApiResponse<ProductResponseDto> {
        val product = productService.getProductById(id)
        return ApiResponse.ok(product)
    }

    @Operation(summary = "Buscar produto por código de barras (EAN)", description = "Recupera os dados de um produto ativo a partir do seu código EAN")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Produto correspondente ao EAN encontrado"),
            SwaggerResponse(
                responseCode = "404",
                description = "Nenhum produto cadastrado com este EAN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/ean/{ean}")
    fun getProductByEan(
        @Parameter(description = "Código de barras do produto")
        @PathVariable ean: String
    ): ApiResponse<ProductResponseDto> {
        val product = productService.getProductByEan(ean)
        return ApiResponse.ok(product)
    }

    @Operation(summary = "Cadastrar novo produto", description = "Adiciona um novo produto ao catálogo do backend")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Produto cadastrado com sucesso"),
            SwaggerResponse(
                responseCode = "400",
                description = "Dados inválidos fornecidos no corpo da requisição",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            SwaggerResponse(
                responseCode = "409",
                description = "Código EAN já cadastrado em outro produto",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    fun createProduct(
        @Valid @RequestBody dto: ProductRequestDto
    ): ResponseEntity<ApiResponse<ProductResponseDto>> {
        val created = productService.saveProduct(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Produto cadastrado com sucesso."))
    }

    @Operation(summary = "Atualizar produto existente", description = "Atualiza os dados de um produto respeitando a semântica CRDT/LWW")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            SwaggerResponse(
                responseCode = "400",
                description = "Dados inválidos fornecidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            SwaggerResponse(
                responseCode = "409",
                description = "Conflito de unicidade de EAN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: String,
        @Valid @RequestBody dto: ProductRequestDto
    ): ApiResponse<ProductResponseDto> {
        val toSave = dto.copy(id = id)
        val updated = productService.saveProduct(toSave)
        return ApiResponse.ok(updated, "Produto atualizado com sucesso.")
    }

    @Operation(summary = "Excluir produto", description = "Realiza exclusão lógica (soft-delete) do produto para sincronização offline")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "204", description = "Produto marcado como excluído com sucesso"),
            SwaggerResponse(
                responseCode = "404",
                description = "Produto não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable id: String) {
        productService.deleteProduct(id)
    }
}

typealias ProdutoController = ProductController
