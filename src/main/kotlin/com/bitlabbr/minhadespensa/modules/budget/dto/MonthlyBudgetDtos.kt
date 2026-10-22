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

package com.bitlabbr.minhadespensa.modules.budget.dto

import com.bitlabbr.minhadespensa.modules.budget.entity.MonthlyBudgetEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Schema(description = "DTO de orçamento mensal para sincronização bidirecional")
data class MonthlyBudgetSyncDto(
    @Schema(description = "Identificador natural do orçamento no formato 'AAAA-M'", example = "2026-10")
    val id: String? = null,

    @Schema(description = "Ano de referência", example = "2026")
    @Min(value = 2000, message = "O ano deve ser maior ou igual a 2000")
    val year: Int,

    @Schema(description = "Mês de referência (1 a 12)", example = "10")
    @Min(value = 1, message = "O mês deve ser entre 1 e 12")
    @Max(value = 12, message = "O mês deve ser entre 1 e 12")
    val month: Int,

    @Schema(description = "Valor orçado em centavos de real (R$ 1.500,00 = 150000)", example = "150000")
    val budgetInCents: Long,

    @Schema(description = "Timestamp da última modificação em milissegundos UTC")
    val updatedAt: Long = System.currentTimeMillis(),

    @Schema(description = "Flag de exclusão lógica (soft delete)")
    val isDeleted: Boolean = false
) {
    companion object {
        fun fromEntity(entity: MonthlyBudgetEntity): MonthlyBudgetSyncDto =
            MonthlyBudgetSyncDto(
                id = entity.id,
                year = entity.year,
                month = entity.month,
                budgetInCents = entity.budgetInCents,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted
            )
    }
}
