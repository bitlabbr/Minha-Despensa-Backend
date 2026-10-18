package com.bitlabbr.minhadespensa.modules.sync.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import com.bitlabbr.minhadespensa.modules.sync.service.SyncService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SyncController::class)
@Import(GlobalExceptionHandler::class)
class SyncControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var syncService: SyncService

    @Test
    fun `POST sync push should return 200 with totalProcessed`() {
        whenever(syncService.push(any())).thenReturn(
            SyncPushResponseDto(
                serverTimestamp = 1727280000000L,
                totalProcessed = 3
            )
        )

        val payload = """
            {
                "products": [
                    { "name": "Arroz Integral", "category": "Grãos" }
                ],
                "pantryItems": [],
                "shoppingLists": [],
                "shoppingItems": [],
                "priceEntries": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalProcessed").value(3))
            .andExpect(jsonPath("$.data.serverTimestamp").value(1727280000000L))
    }

    @Test
    fun `GET sync pull should return 200 with deltas`() {
        whenever(syncService.pull(1000L)).thenReturn(
            SyncPullResponseDto(
                serverTimestamp = 2000L,
                products = emptyList(),
                pantryItems = emptyList(),
                shoppingLists = emptyList(),
                shoppingItems = emptyList(),
                priceEntries = emptyList()
            )
        )

        mockMvc.perform(
            get("/api/v1/sync/pull")
                .param("since", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.serverTimestamp").value(2000L))
            .andExpect(jsonPath("$.data.products").isArray)
    }
}
