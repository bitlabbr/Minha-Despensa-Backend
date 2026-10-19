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

package com.bitlabbr.minhadespensa.modules.pantry.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryAggregatedResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryOccurrenceResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.ProductSummaryDto
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(DespensaController::class)
@Import(GlobalExceptionHandler::class)
class DespensaControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var pantryService: PantryService

    @Test
    fun `GET despensa should return aggregated pantry response`() {
        val aggregated = PantryAggregatedResponseDto(
            produto = ProductSummaryDto(
                id = "prod-1",
                nome = "Leite Integral Ninho 1L",
                codigoBarras = "7891000100103",
                marca = "Nestlé",
                categoria = "Laticínios",
                unidadeMedida = "UN"
            ),
            quantidadeTotal = 3.0,
            ocorrencias = listOf(
                PantryOccurrenceResponseDto(
                    id = "101",
                    quantidade = 2.0,
                    precoUnitario = 5.50,
                    dataCompra = "2026-08-10",
                    dataValidade = "2026-09-20",
                    localCompra = "Carrefour Hipermercado",
                    status = PantryItemStatus.EM_ESTOQUE
                ),
                PantryOccurrenceResponseDto(
                    id = "102",
                    quantidade = 1.0,
                    precoUnitario = 6.20,
                    dataCompra = "2026-09-15",
                    dataValidade = "2026-11-30",
                    localCompra = "Pão de Açúcar",
                    status = PantryItemStatus.EM_ESTOQUE
                )
            )
        )

        whenever(pantryService.getAggregatedPantry()).thenReturn(listOf(aggregated))

        mockMvc.perform(get("/api/v1/despensa"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].produto.nome").value("Leite Integral Ninho 1L"))
            .andExpect(jsonPath("$.data[0].quantidadeTotal").value(3.0))
            .andExpect(jsonPath("$.data[0].ocorrencias[0].id").value("101"))
            .andExpect(jsonPath("$.data[0].ocorrencias[0].precoUnitario").value(5.50))
            .andExpect(jsonPath("$.data[0].ocorrencias[0].status").value("EM_ESTOQUE"))
            .andExpect(jsonPath("$.data[0].ocorrencias[1].id").value("102"))
            .andExpect(jsonPath("$.data[0].ocorrencias[1].precoUnitario").value(6.20))
    }

    @Test
    fun `GET despensa ocorrencias should return occurrences for product`() {
        val occurrences = listOf(
            PantryOccurrenceResponseDto(
                id = "101",
                quantidade = 2.0,
                precoUnitario = 5.50,
                dataCompra = "2026-08-10",
                dataValidade = "2026-09-20",
                localCompra = "Carrefour",
                status = PantryItemStatus.EM_ESTOQUE
            )
        )

        whenever(pantryService.getOccurrencesByProductId("prod-1")).thenReturn(occurrences)

        mockMvc.perform(get("/api/v1/despensa/produtos/prod-1/ocorrencias"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("101"))
            .andExpect(jsonPath("$.data[0].quantidade").value(2.0))
    }

    @Test
    fun `POST despensa itens should create item`() {
        val payload = """
            {
                "produtoId": "prod-1",
                "quantidade": 2.0,
                "precoUnitario": 5.50,
                "dataCompra": "2026-08-10",
                "dataValidade": "2026-09-20",
                "localCompra": "Carrefour Hipermercado"
            }
        """.trimIndent()

        val response = PantryItemResponseDto(
            id = "item-101",
            productId = "prod-1",
            quantity = 2.0,
            unitPrice = 5.50,
            expirationDate = 1789862400000L,
            purchaseLocation = "Carrefour Hipermercado",
            status = PantryItemStatus.EM_ESTOQUE,
            batchNumber = null,
            updatedAt = 1000L,
            isDeleted = false
        )

        whenever(pantryService.saveItem(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/despensa/itens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("item-101"))
            .andExpect(jsonPath("$.data.quantity").value(2.0))
    }

    @Test
    fun `PATCH despensa itens consumir should deduct stock`() {
        val response = PantryItemResponseDto(
            id = "item-101",
            productId = "prod-1",
            quantity = 1.0,
            unitPrice = 5.50,
            expirationDate = 1789862400000L,
            purchaseLocation = "Carrefour",
            status = PantryItemStatus.EM_ESTOQUE,
            batchNumber = null,
            updatedAt = 2000L,
            isDeleted = false
        )

        whenever(pantryService.consumeItem(eq("item-101"), eq(1.0))).thenReturn(response)

        mockMvc.perform(patch("/api/v1/despensa/itens/item-101/consumir").param("quantidade", "1.0"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.quantity").value(1.0))
            .andExpect(jsonPath("$.data.status").value("EM_ESTOQUE"))
    }

    @Test
    fun `PATCH despensa itens descartar should discard stock`() {
        val response = PantryItemResponseDto(
            id = "item-101",
            productId = "prod-1",
            quantity = 0.0,
            unitPrice = 5.50,
            expirationDate = 1789862400000L,
            purchaseLocation = "Carrefour",
            status = PantryItemStatus.DESCARTADO,
            batchNumber = null,
            updatedAt = 2000L,
            isDeleted = false
        )

        whenever(pantryService.discardItem("item-101")).thenReturn(response)

        mockMvc.perform(patch("/api/v1/despensa/itens/item-101/descartar"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.quantity").value(0.0))
            .andExpect(jsonPath("$.data.status").value("DESCARTADO"))
    }
}
