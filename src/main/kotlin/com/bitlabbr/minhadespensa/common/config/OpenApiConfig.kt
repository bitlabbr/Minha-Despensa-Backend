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

package com.bitlabbr.minhadespensa.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Autowired(required = false) private val buildProperties: BuildProperties? = null
) {

    @Bean
    fun customOpenAPI(): OpenAPI {
        val appVersion = buildProperties?.version ?: "2.1.0-SNAPSHOT"
        val gitCommit = buildProperties?.get("gitCommit")
        val versionDisplay = if (gitCommit != null && gitCommit != "unknown") "$appVersion ($gitCommit)" else appVersion

        return OpenAPI()
            .info(
                Info()
                    .title("Minha Despensa - Backend API")
                    .version(versionDisplay)
                    .description(
                        "API RESTful do backend de Minha Despensa: hub multi-tenant de sincronização bidirecional (v2.1), " +
                        "gestão de casas compartilhadas (Household), catálogo de produtos, estoque e leitura de notas fiscais SEFAZ."
                    )
                    .contact(
                        Contact()
                            .name("BitLab BR / Willian Santos")
                            .email("pro.wj.santos@gmail.com")
                    )
                    .license(
                        License()
                            .name("CC BY-NC 4.0")
                            .url("https://creativecommons.org/licenses/by-nc/4.0/")
                    )
            )
            .servers(
                listOf(
                    Server().url("https://despensa.domus.arpa").description("Produção Domus (Tailscale HTTPS)"),
                    Server().url("http://localhost:8080").description("Servidor Local de Desenvolvimento")
                )
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Insira o token JWT de acesso emitido no login ou registro")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }
}
