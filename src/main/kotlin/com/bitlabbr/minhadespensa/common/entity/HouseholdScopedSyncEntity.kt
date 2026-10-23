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

package com.bitlabbr.minhadespensa.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@MappedSuperclass
@IdClass(HouseholdEntityId::class)
abstract class HouseholdScopedSyncEntity(
    @Id
    @Column(name = "household_id", nullable = false, length = 36)
    open var householdId: String = "",

    @Id
    @Column(name = "id", nullable = false, length = 36)
    open var id: String = UUID.randomUUID().toString(),

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Long = System.currentTimeMillis(),

    @Column(name = "server_version", nullable = false)
    open var serverVersion: Long = 0L,

    @Column(name = "is_deleted", nullable = false)
    open var isDeleted: Boolean = false
)
