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

package com.bitlabbr.minhadespensa.modules.support.repository

import com.bitlabbr.minhadespensa.modules.support.entity.BugReportEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BugReportRepository : JpaRepository<BugReportEntity, String> {
    fun findByProtocol(protocol: String): Optional<BugReportEntity>
    fun findAllByOrderByCreatedAtDesc(): List<BugReportEntity>
    fun findByStatusOrderByCreatedAtDesc(status: String): List<BugReportEntity>
    fun findByHouseholdIdOrderByCreatedAtDesc(householdId: String): List<BugReportEntity>
    fun findByCategoryOrderByCreatedAtDesc(category: String): List<BugReportEntity>
}
