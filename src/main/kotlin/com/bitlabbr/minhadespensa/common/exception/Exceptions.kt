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
