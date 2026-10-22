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

package com.bitlabbr.minhadespensa.common.exception

/**
 * Exceção base para regras de negócio violadas no domínio.
 */
open class BusinessException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exceção lançada quando um recurso solicitado não é encontrado no sistema.
 */
class ResourceNotFoundException(
    message: String,
    val resourceName: String? = null,
    val resourceId: Any? = null
) : BusinessException(message)

/**
 * Exceção lançada em caso de conflitos de estado ou duplicidade de chaves únicas (ex: EAN duplicado).
 */
class ConflictException(
    message: String
) : BusinessException(message)

/**
 * Exceção lançada quando a validação de parâmetros ou dados de sincronização falha.
 */
class InvalidRequestException(
    message: String
) : BusinessException(message)

/**
 * Exceção lançada quando a requisição não possui credenciais válidas ou o token expirou (401).
 */
class UnauthorizedException(
    message: String = "Não autenticado"
) : BusinessException(message)

/**
 * Exceção lançada quando o usuário autenticado não tem permissão para acessar o recurso ou household solicitado (403).
 */
class ForbiddenException(
    message: String = "Acesso negado"
) : BusinessException(message)
