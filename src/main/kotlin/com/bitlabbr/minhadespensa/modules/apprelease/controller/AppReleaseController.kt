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

package com.bitlabbr.minhadespensa.modules.apprelease.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.apprelease.dto.AppVersionResponseDto
import com.bitlabbr.minhadespensa.modules.apprelease.service.AppReleaseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/app")
@Tag(name = "Distribuição do Aplicativo (App Releases)", description = "Verificação de atualizações in-app e download direto de binários APK para Android")
class AppReleaseController(
    private val appReleaseService: AppReleaseService
) {

    @Operation(
        summary = "Consultar versão mais recente do aplicativo",
        description = "Retorna os metadados da versão mais recente disponível no servidor Domus para verificação in-app (OTA)."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Metadados da versão retornados com sucesso"),
            SwaggerResponse(responseCode = "404", description = "Nenhuma versão encontrada para a plataforma")
        ]
    )
    @GetMapping("/version/latest")
    fun getLatestVersion(
        @Parameter(description = "Plataforma alvo ('android')", example = "android")
        @RequestParam(defaultValue = "android") platform: String
    ): ApiResponse<AppVersionResponseDto> {
        val result = appReleaseService.getLatestVersion(platform)
        return ApiResponse.ok(result)
    }

    @Operation(
        summary = "Download do APK da versão mais recente",
        description = "Realiza o streaming binário do arquivo APK do aplicativo para instalação direta no Android."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Download do APK iniciado"),
            SwaggerResponse(responseCode = "404", description = "Arquivo APK não localizado no servidor")
        ]
    )
    @GetMapping("/download/latest")
    fun downloadLatestApk(): ResponseEntity<Resource> {
        return buildFileResponse(null)
    }

    @Operation(
        summary = "Download do APK de uma versão específica",
        description = "Permite baixar o arquivo APK correspondente a uma versão histórica informada."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Download do APK iniciado"),
            SwaggerResponse(responseCode = "404", description = "Arquivo APK não localizado")
        ]
    )
    @GetMapping("/download/{versionName}")
    fun downloadSpecificApk(
        @Parameter(description = "Nome semântico da versão (ex: '2.1.0')", required = true)
        @PathVariable versionName: String
    ): ResponseEntity<Resource> {
        return buildFileResponse(versionName)
    }

    private fun buildFileResponse(versionName: String?): ResponseEntity<Resource> {
        val (resource, fileName) = appReleaseService.getDownloadResource(versionName)
        val fileLength = resource.contentLength()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentLength(fileLength)
            .body(resource)
    }
}
