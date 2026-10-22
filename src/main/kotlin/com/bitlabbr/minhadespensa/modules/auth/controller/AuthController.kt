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

package com.bitlabbr.minhadespensa.modules.auth.controller

import com.bitlabbr.minhadespensa.common.dto.ApiResponse
import com.bitlabbr.minhadespensa.modules.auth.dto.AuthResponseDto
import com.bitlabbr.minhadespensa.modules.auth.dto.LoginRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RefreshTokenRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RegisterRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.UserSummaryDto
import com.bitlabbr.minhadespensa.modules.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação & Identidade", description = "Endpoints para registro, login, rotação de tokens JWT e gestão de sessão de usuários")
class AuthController(
    private val authService: AuthService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @Operation(summary = "Criar nova conta de usuário", description = "Registra um usuário individual, gera automaticamente sua casa pessoal inicial e devolve o par de tokens JWT.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            SwaggerResponse(responseCode = "400", description = "Dados inválidos no formulário"),
            SwaggerResponse(responseCode = "409", description = "E-mail já existente")
        ]
    )
    @PostMapping("/register")
    fun register(@Valid @RequestBody dto: RegisterRequestDto): ResponseEntity<ApiResponse<AuthResponseDto>> {
        logger.info("[Auth] Register requested for email='{}', name='{}'", dto.email, dto.name)
        val result = authService.register(dto)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(result, "User registered successfully."))
    }

    @Operation(summary = "Autenticação (Login)", description = "Valida e-mail e senha cadastrados e devolve o par de tokens JWT + refresh token.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Login bem-sucedido"),
            SwaggerResponse(responseCode = "401", description = "Credenciais inválidas")
        ]
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody dto: LoginRequestDto): ApiResponse<AuthResponseDto> {
        logger.info("[Auth] Login requested for email='{}'", dto.email)
        val result = authService.login(dto)
        return ApiResponse.ok(result, "Login successful.")
    }

    @Operation(summary = "Renovar Token de Acesso", description = "Rotaciona o refresh token apresentado e emite um novo access token JWT.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "Sessão renovada com sucesso"),
            SwaggerResponse(responseCode = "401", description = "Refresh token inválido, expirado ou reutilizado")
        ]
    )
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody dto: RefreshTokenRequestDto): ApiResponse<AuthResponseDto> {
        val result = authService.refreshToken(dto)
        return ApiResponse.ok(result, "Session refreshed successfully.")
    }

    @Operation(summary = "Encerrar Sessão (Logout)", description = "Invalida todos os refresh tokens ativos do usuário.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal jwt: Jwt): ApiResponse<Unit> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        authService.logout(userId)
        return ApiResponse.ok("Logout successful.")
    }

    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna os detalhes cadastrais do usuário associado ao token JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal jwt: Jwt): ApiResponse<UserSummaryDto> {
        val userId = requireNotNull(jwt.subject) { "User identifier missing from token" }
        val result = authService.getCurrentUser(userId)
        return ApiResponse.ok(result)
    }
}
