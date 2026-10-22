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

package com.bitlabbr.minhadespensa.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Health Check & Test", description = "Endpoints de diagnóstico e integridade do servidor")
class TestController(
    @Autowired(required = false) private val buildProperties: BuildProperties? = null
) {

    @Operation(summary = "Verificar status da aplicação", description = "Retorna o status operacional do backend, versão semântica, git commit e build timestamp")
    @GetMapping("/hello")
    fun hello(): Map<String, Any> {
        val version = buildProperties?.version ?: "2.0.0-SNAPSHOT"
        val gitCommit = buildProperties?.get("gitCommit") ?: "unknown"
        val buildTime = buildProperties?.time?.toString() ?: LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

        return mapOf(
            "status" to "UP",
            "app" to "Minha Despensa Backend",
            "version" to version,
            "gitCommit" to gitCommit,
            "buildTime" to buildTime,
            "message" to "Server online and running on Domus!",
            "timestamp" to LocalDateTime.now().toString()
        )
    }
}