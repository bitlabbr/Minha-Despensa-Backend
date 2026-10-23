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

package com.bitlabbr.minhadespensa.modules.household.repository

import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface HouseholdRepository : JpaRepository<HouseholdEntity, String> {
    fun findByInviteCodeIgnoreCase(inviteCode: String): Optional<HouseholdEntity>
    fun existsByInviteCodeIgnoreCase(inviteCode: String): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HouseholdEntity h SET h.syncVersion = h.syncVersion + 1, h.updatedAt = :now WHERE h.id = :id")
    fun incrementSyncVersion(@Param("id") id: String, @Param("now") now: Long): Int
}
