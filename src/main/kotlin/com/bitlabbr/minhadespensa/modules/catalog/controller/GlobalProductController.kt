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

package com.bitlabbr.minhadespensa.modules.catalog.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.catalog.dto.GlobalProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.service.GlobalProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/catalog/global")
@Tag(name = "Catálogo Global", description = "Endpoints de consulta ao catálogo centralizado e consolidado de produtos por EAN")
@SecurityRequirement(name = "BearerAuth")
class GlobalProductController(
    private val globalProductService: GlobalProductService
) {

    @GetMapping("/{ean}")
    @Operation(summary = "Consultar produto global por EAN", description = "Retorna os metadados consolidados de um produto cadastrado no catálogo central por código de barras.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Produto localizado com sucesso."),
            SwaggerApiResponse(responseCode = "400", description = "Formato de código EAN inválido."),
            SwaggerApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado."),
            SwaggerApiResponse(responseCode = "404", description = "Produto não localizado no catálogo central.")
        ]
    )
    fun getProductByEan(
        @Parameter(description = "Código de barras universal EAN (8, 12, 13 ou 14 dígitos)", example = "7891000100103")
        @PathVariable ean: String
    ): ResponseEntity<ApiResponse<GlobalProductResponseDto>> {
        val product = globalProductService.getProductByEan(ean)
        return ResponseEntity.ok(ApiResponse.ok(product))
    }

    @GetMapping("/search")
    @Operation(summary = "Pesquisar produtos no catálogo global", description = "Realiza busca aproximada no catálogo centralizado por nome, marca ou início de código EAN.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Lista de produtos localizados."),
            SwaggerApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado.")
        ]
    )
    fun searchProducts(
        @Parameter(description = "Termo de busca (mínimo de 2 caracteres)", example = "Leite")
        @RequestParam(name = "query") query: String,
        @Parameter(description = "Limite máximo de registros a retornar (entre 1 e 50)", example = "20")
        @RequestParam(name = "limit", defaultValue = "20") limit: Int
    ): ResponseEntity<ApiResponse<List<GlobalProductResponseDto>>> {
        val results = globalProductService.searchProducts(query, limit)
        return ResponseEntity.ok(ApiResponse.ok(results))
    }
}
