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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "households")
class HouseholdEntity(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    var id: String = UUID.randomUUID().toString(),

    @Column(name = "name", nullable = false, length = 150)
    var name: String = "",

    @Column(name = "invite_code", nullable = false, length = 32, unique = true)
    var inviteCode: String = "",

    @Column(name = "owner_id", nullable = false, length = 36)
    var ownerId: String = "",

    @Column(name = "sync_version", nullable = false)
    var syncVersion: Long = 0L,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
)
