package com.bitlabbr.minhadespensa.modules.sync.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import com.bitlabbr.minhadespensa.modules.sync.service.SyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sincronização Offline-First", description = "Endpoints de replicação de dados entre os clientes móveis e o backend (CRDT / Last-Write-Wins)")
class SyncController(
    private val syncService: SyncService
) {

    @Operation(
        summary = "Sincronizar alterações locais (Push)",
        description = "Recebe um lote de alterações de produtos, despensa, listas e preços gravados offline no cliente e mescla com o banco via LWW"
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lote processado e sincronizado com sucesso")
        ]
    )
    @PostMapping("/push")
    fun push(
        @Valid @RequestBody request: SyncPushRequestDto
    ): ApiResponse<SyncPushResponseDto> {
        val result = syncService.push(request)
        return ApiResponse.ok(result, "Sincronização concluída com sucesso.")
    }

    @Operation(
        summary = "Baixar alterações do servidor (Pull)",
        description = "Retorna todos os registros de todas as entidades que foram modificados ou excluídos após o timestamp informado em 'since'"
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Lote de alterações recuperado com sucesso")
        ]
    )
    @GetMapping("/pull")
    fun pull(
        @Parameter(description = "Timestamp em milissegundos UTC do último checkpoint do cliente. Use 0 para carga inicial completa.", example = "1727280000000")
        @RequestParam(defaultValue = "0") since: Long
    ): ApiResponse<SyncPullResponseDto> {
        val result = syncService.pull(since)
        return ApiResponse.ok(result)
    }
}
