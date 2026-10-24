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
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductMediaService
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/media/products")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mídia de Produtos", description = "Upload e download de fotos e thumbnails de produtos escopados por casa")
class ProductMediaController(
    private val productMediaService: ProductMediaService,
    private val householdService: HouseholdService
) {

    @Operation(summary = "Upload de imagem do produto", description = "Recebe os bytes brutos da imagem (WebP/JPEG, até 200KB) e armazena calculando o hash SHA-256.")
    @PutMapping(value = ["/{productId}"], consumes = [MediaType.ALL_VALUE])
    fun uploadMedia(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador da casa", required = true) @RequestHeader("X-Household-Id") householdId: String,
        @PathVariable productId: String,
        @RequestBody bytes: ByteArray,
        @RequestHeader(value = "Content-Type", defaultValue = "image/webp") contentType: String
    ): ApiResponse<Map<String, String>> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.requireHouseholdAccess(userId, householdId)
        val syncVersion = householdService.nextSyncVersion(householdId)

        val media = productMediaService.saveMedia(householdId, productId, bytes, contentType, syncVersion)
        return ApiResponse.ok(mapOf("sha256Hash" to media.sha256Hash), "Image saved successfully.")
    }

    @Operation(summary = "Download de imagem do produto", description = "Retorna os bytes da imagem com suporte a ETag (304 Not Modified).")
    @GetMapping(value = ["/{productId}"])
    fun getMedia(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador da casa", required = true) @RequestHeader("X-Household-Id") householdId: String,
        @PathVariable productId: String,
        @RequestHeader(value = "If-None-Match", required = false) ifNoneMatch: String?
    ): ResponseEntity<ByteArray> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.requireHouseholdAccess(userId, householdId)

        val media = productMediaService.getMedia(householdId, productId)
            ?: throw ResourceNotFoundException("Image not found for product $productId", "ProductMedia", productId)

        if (!ifNoneMatch.isNullOrBlank() && (ifNoneMatch == media.sha256Hash || ifNoneMatch == "\"${media.sha256Hash}\"")) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()
        }

        return ResponseEntity.ok()
            .eTag(media.sha256Hash)
            .contentType(MediaType.parseMediaType(media.contentType))
            .body(media.imageBytes)
    }

    @Operation(summary = "Remover imagem do produto", description = "Exclui a foto do produto na casa.")
    @DeleteMapping("/{productId}")
    fun deleteMedia(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Identificador da casa", required = true) @RequestHeader("X-Household-Id") householdId: String,
        @PathVariable productId: String
    ): ApiResponse<Unit> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        householdService.requireHouseholdAccess(userId, householdId)
        val syncVersion = householdService.nextSyncVersion(householdId)

        productMediaService.deleteMedia(householdId, productId, syncVersion)
        return ApiResponse.ok("Image removed successfully.")
    }
}
