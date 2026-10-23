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

package com.bitlabbr.minhadespensa.modules.sync.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class EntityIdAliasId(
    var householdId: String = "",
    var entityType: String = "",
    var aliasId: String = ""
) : Serializable

@Entity
@Table(name = "entity_id_aliases")
@IdClass(EntityIdAliasId::class)
class EntityIdAliasEntity(
    @Id
    @Column(name = "household_id", nullable = false, length = 36)
    var householdId: String = "",

    @Id
    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = "",

    @Id
    @Column(name = "alias_id", nullable = false, length = 36)
    var aliasId: String = "",

    @Column(name = "canonical_id", nullable = false, length = 36)
    var canonicalId: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis()
)
