package com.bitlabbr.minhadespensa.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Health Check & Test", description = "Endpoints de diagnóstico e integridade do servidor")
class TestController {

    @Operation(summary = "Verificar status da aplicação", description = "Retorna o status operacional do backend e informações do host")
    @GetMapping("/hello")
    fun hello(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "app" to "Minha Despensa Backend",
            "message" to "Servidor online e rodando no Domus!",
            "timestamp" to LocalDateTime.now().toString()
        )
    }
}