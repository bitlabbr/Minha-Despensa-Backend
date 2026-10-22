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

package com.bitlabbr.minhadespensa.modules.household.entity

import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "household_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_household_member", columnNames = ["household_id", "user_id"])
    ]
)
class HouseholdMemberEntity(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    var id: String = UUID.randomUUID().toString(),

    @Column(name = "household_id", nullable = false, length = 36)
    var householdId: String = "",

    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: HouseholdRole = HouseholdRole.OWNER,

    @Column(name = "joined_at", nullable = false)
    var joinedAt: Long = System.currentTimeMillis()
)
