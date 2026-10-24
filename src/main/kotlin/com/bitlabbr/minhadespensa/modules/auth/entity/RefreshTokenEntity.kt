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

package com.bitlabbr.minhadespensa.modules.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    var id: String = UUID.randomUUID().toString(),

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    var tokenHash: String = "",

    @Column(name = "family_id", nullable = false, length = 36)
    var familyId: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Long = 0L,

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis()
)
