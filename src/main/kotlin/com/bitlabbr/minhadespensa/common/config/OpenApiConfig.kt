package com.bitlabbr.minhadespensa.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Minha Despensa - Backend API")
                    .version("0.0.1")
                    .description(
                        "API RESTful do backend de Minha Despensa: gestão de catálogo de produtos, despensa doméstica, " +
                        "listas de compras e sincronização com clientes Kotlin Multiplatform (CRDT / LWW)."
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
                    Server().url("http://localhost:8080").description("Servidor Local"),
                    Server().url("http://100.98.199.97:8080").description("Domus (Tailscale)")
                )
            )
    }
}
