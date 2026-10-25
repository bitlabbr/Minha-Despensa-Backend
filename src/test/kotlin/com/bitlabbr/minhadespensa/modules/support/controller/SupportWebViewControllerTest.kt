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

package com.bitlabbr.minhadespensa.modules.support.controller

import com.bitlabbr.minhadespensa.modules.auth.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SupportWebViewController::class)
@Import(SecurityConfig::class)
class SupportWebViewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET support without slash should forward to support index html anonymously`() {
        mockMvc.perform(get("/support"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/support/index.html"))
    }

    @Test
    fun `GET support with slash should forward to support index html anonymously`() {
        mockMvc.perform(get("/support/"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/support/index.html"))
    }
}
