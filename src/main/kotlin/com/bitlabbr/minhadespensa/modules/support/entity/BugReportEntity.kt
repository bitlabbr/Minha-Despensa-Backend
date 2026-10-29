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

package com.bitlabbr.minhadespensa.modules.support.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "bug_reports")
class BugReportEntity(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    var id: String = UUID.randomUUID().toString(),

    @Column(name = "protocol", nullable = false, length = 32, unique = true)
    var protocol: String = "",

    @Column(name = "category", nullable = false, length = 32)
    var category: String = "BUG",

    @Column(name = "title", nullable = false, length = 255)
    var title: String = "",

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = "",

    @Column(name = "app_version", nullable = false, length = 64)
    var appVersion: String = "",

    @Column(name = "device_model", length = 128)
    var deviceModel: String? = null,

    @Column(name = "os_version", length = 64)
    var osVersion: String? = null,

    @Column(name = "user_id", length = 36)
    var userId: String? = null,

    @Column(name = "household_id", length = 36)
    var householdId: String? = null,

    @Column(name = "user_contact", length = 255)
    var userContact: String? = null,

    @Column(name = "has_logs", nullable = false)
    var hasLogs: Boolean = false,

    @Column(name = "log_file_path", length = 512)
    var logFilePath: String? = null,

    @Column(name = "status", nullable = false, length = 32)
    var status: String = "OPEN",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
)
