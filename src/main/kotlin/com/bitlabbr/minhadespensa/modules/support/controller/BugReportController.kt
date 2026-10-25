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

package com.bitlabbr.minhadespensa.modules.support.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportCreateCommand
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportResponseDto
import com.bitlabbr.minhadespensa.modules.support.dto.BugReportSummaryDto
import com.bitlabbr.minhadespensa.modules.support.service.BugReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/support")
@Tag(name = "Suporte e Diagnóstico", description = "Endpoints para registro de bugs, feedback e download de diagnósticos de log")
class BugReportController(
    private val bugReportService: BugReportService
) {

    @Operation(
        summary = "Submeter relato de suporte, bug ou feedback",
        description = "Registra um novo chamado de suporte (proativo ou pós-crash), anexando opcionalmente o arquivo de log sanitizado do dispositivo."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Relato registrado com sucesso com emissão de protocolo"),
            SwaggerResponse(responseCode = "400", description = "Parâmetros obrigatórios ausentes ou inválidos"),
            SwaggerResponse(responseCode = "422", description = "Arquivo de log excede o tamanho máximo permitido")
        ]
    )
    @PostMapping(
        value = ["/bug-reports"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun submitBugReport(
        @AuthenticationPrincipal jwt: Jwt?,
        @Parameter(description = "Identificador da casa ativa (opcional)")
        @RequestHeader(value = "X-Household-Id", required = false) householdIdHeader: String?,
        @RequestParam("category") category: String,
        @RequestParam("title") title: String,
        @RequestParam("description") description: String,
        @RequestParam("appVersion") appVersion: String,
        @RequestParam(value = "deviceModel", required = false) deviceModel: String?,
        @RequestParam(value = "osVersion", required = false) osVersion: String?,
        @RequestParam(value = "userContact", required = false) userContact: String?,
        @RequestParam(value = "householdId", required = false) householdIdParam: String?,
        @RequestPart(value = "logFile", required = false) logFile: MultipartFile?
    ): ApiResponse<BugReportResponseDto> {
        val effectiveHouseholdId = householdIdHeader ?: householdIdParam
        val command = BugReportCreateCommand(
            category = category,
            title = title,
            description = description,
            appVersion = appVersion,
            deviceModel = deviceModel,
            osVersion = osVersion,
            userContact = userContact,
            householdId = effectiveHouseholdId,
            userId = jwt?.subject
        )

        val response = bugReportService.submitBugReport(command, logFile)
        return ApiResponse.created(response, "Relatório de suporte registrado com sucesso.")
    }

    @Operation(
        summary = "Listar relatos de suporte",
        description = "Retorna a lista de relatos registrados no servidor, com filtros opcionais por status ou categoria."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            SwaggerResponse(responseCode = "401", description = "Requer autenticação JWT")
        ]
    )
    @GetMapping("/bug-reports")
    fun listReports(
        @Parameter(description = "Filtro por status (OPEN, INVESTIGATING, RESOLVED, CLOSED)")
        @RequestParam(value = "status", required = false) status: String?,
        @Parameter(description = "Filtro por categoria (BUG, SUGGESTION, QUESTION, CRASH)")
        @RequestParam(value = "category", required = false) category: String?
    ): ApiResponse<List<BugReportSummaryDto>> {
        val reports = bugReportService.listReports(status, category)
        return ApiResponse.ok(reports)
    }

    @Operation(
        summary = "Consultar detalhes de um chamado de suporte",
        description = "Retorna os detalhes completos do chamado pelo seu protocolo único."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Detalhes do chamado retornados com sucesso"),
            SwaggerResponse(responseCode = "401", description = "Requer autenticação JWT"),
            SwaggerResponse(responseCode = "404", description = "Protocolo não encontrado")
        ]
    )
    @GetMapping("/bug-reports/{protocol}")
    fun getReportByProtocol(
        @Parameter(description = "Protocolo único do chamado (ex: BUG-20261024-8F2A)")
        @PathVariable protocol: String
    ): ApiResponse<BugReportSummaryDto> {
        val summary = bugReportService.getReportSummary(protocol)
        return ApiResponse.ok(summary)
    }

    @Operation(
        summary = "Atualizar status de um chamado de suporte",
        description = "Permite alterar o status de triagem do chamado (OPEN, INVESTIGATING, RESOLVED, CLOSED)."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            SwaggerResponse(responseCode = "400", description = "Status informado inválido"),
            SwaggerResponse(responseCode = "401", description = "Requer autenticação JWT"),
            SwaggerResponse(responseCode = "404", description = "Protocolo não encontrado")
        ]
    )
    @PatchMapping("/bug-reports/{protocol}/status")
    fun updateReportStatus(
        @Parameter(description = "Protocolo único do chamado (ex: BUG-20261024-8F2A)")
        @PathVariable protocol: String,
        @Parameter(description = "Novo status (OPEN, INVESTIGATING, RESOLVED, CLOSED)")
        @RequestParam("status") status: String
    ): ApiResponse<BugReportSummaryDto> {
        val updated = bugReportService.updateReportStatus(protocol, status)
        return ApiResponse.ok(updated, "Status do chamado atualizado com sucesso.")
    }

    @Operation(
        summary = "Download do arquivo de log de diagnóstico",
        description = "Retorna o stream de texto bruto do arquivo device.log associado ao protocolo do chamado."
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Arquivo de log retornado com sucesso"),
            SwaggerResponse(responseCode = "404", description = "Protocolo não encontrado ou sem logs anexados")
        ]
    )
    @GetMapping("/bug-reports/{protocol}/log")
    fun downloadLog(
        @Parameter(description = "Protocolo único do chamado (ex: BUG-20261024-8F2A)")
        @PathVariable protocol: String
    ): ResponseEntity<Resource> {
        val (resource, filename) = bugReportService.getLogResource(protocol)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .body(resource)
    }
}
