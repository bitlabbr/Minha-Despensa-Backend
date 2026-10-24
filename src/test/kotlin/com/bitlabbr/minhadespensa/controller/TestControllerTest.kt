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

package com.bitlabbr.minhadespensa.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.info.BuildProperties
import java.time.Instant
import java.util.Properties

class TestControllerTest {

    @Test
    fun `hello should return build properties when available`() {
        val props = Properties().apply {
            put("version", "2.1.0-SNAPSHOT")
            put("gitCommit", "abc1234")
            put("time", Instant.now().toEpochMilli().toString())
        }
        val buildProperties = BuildProperties(props)
        val controller = TestController(buildProperties)

        val result = controller.hello()

        assertEquals("UP", result["status"])
        assertEquals("Minha Despensa Backend", result["app"])
        assertEquals("2.1.0-SNAPSHOT", result["version"])
        assertEquals("abc1234", result["gitCommit"])
        assertNotNull(result["buildTime"])
    }

    @Test
    fun `hello should return fallback values when build properties are null`() {
        val controller = TestController(null)

        val result = controller.hello()

        assertEquals("UP", result["status"])
        assertEquals("2.1.0-SNAPSHOT", result["version"])
        assertEquals("unknown", result["gitCommit"])
    }
}
