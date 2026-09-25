package com.bitlabbr.minhadespensa.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/test")
class TestController {

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