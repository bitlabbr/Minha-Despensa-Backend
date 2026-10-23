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

package com.bitlabbr.minhadespensa.modules.catalog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class ProductMediaId(
    var householdId: String = "",
    var productId: String = ""
) : Serializable

@Entity
@Table(name = "product_media")
@IdClass(ProductMediaId::class)
class ProductMediaEntity(
    @Id
    @Column(name = "household_id", nullable = false, length = 36)
    var householdId: String = "",

    @Id
    @Column(name = "product_id", nullable = false, length = 36)
    var productId: String = "",

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String = "image/webp",

    @Column(name = "sha256_hash", nullable = false, length = 64)
    var sha256Hash: String = "",

    @Column(name = "image_bytes", nullable = false)
    var imageBytes: ByteArray = byteArrayOf(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @Column(name = "server_version", nullable = false)
    var serverVersion: Long = 0L
)
