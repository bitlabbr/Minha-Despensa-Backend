package com.bitlabbr.minhadespensa.modules.catalog.controller

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
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

@WebMvcTest(ProductController::class)
@Import(GlobalExceptionHandler::class)
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var productService: ProductService

    @Test
    fun `GET products by id should return 200 when found`() {
        val responseDto = ProductResponseDto(
            id = "prod-1",
            ean = "7891000100103",
            name = "Café",
            category = "Bebidas",
            brand = "Pilão",
            measureUnit = MeasureUnit.PACKAGE,
            netWeight = 500.0,
            thumbnailUrl = null,
            manuallyAdded = true,
            notes = null,
            updatedAt = 1000L,
            isDeleted = false
        )

        whenever(productService.getProductById("prod-1")).thenReturn(responseDto)

        mockMvc.perform(get("/api/v1/products/prod-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("prod-1"))
            .andExpect(jsonPath("$.data.name").value("Café"))
    }

    @Test
    fun `GET products by id should return 404 when not found`() {
        whenever(productService.getProductById("unknown"))
            .thenThrow(ResourceNotFoundException("Produto com ID 'unknown' não encontrado."))

        mockMvc.perform(get("/api/v1/products/unknown"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Produto com ID 'unknown' não encontrado."))
    }

    @Test
    fun `POST products with blank name should return 400 with fieldErrors`() {
        val invalidPayload = """
            {
                "name": "",
                "category": "Geral"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldErrors").isArray)
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
    }

    @Test
    fun `GET produtos with busca param should search products`() {
        val responseDto = ProductResponseDto(
            id = "prod-1",
            ean = "7891000100103",
            name = "Leite Ninho",
            category = "Laticínios",
            brand = "Nestlé",
            measureUnit = MeasureUnit.UNIT,
            netWeight = 1.0,
            thumbnailUrl = null,
            manuallyAdded = true,
            notes = null,
            updatedAt = 1000L,
            isDeleted = false
        )

        whenever(productService.searchProducts("Leite")).thenReturn(listOf(responseDto))

        mockMvc.perform(get("/api/v1/produtos").param("busca", "Leite"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("prod-1"))
            .andExpect(jsonPath("$.data[0].name").value("Leite Ninho"))
    }

    @Test
    fun `POST produtos with Portuguese payload should create product`() {
        val payload = """
            {
                "nome": "Leite Ninho",
                "codigoBarras": "7891000100103",
                "marca": "Nestlé",
                "categoria": "Laticínios",
                "unidadeMedida": "UNIT",
                "pesoLiquido": 1.0
            }
        """.trimIndent()

        val responseDto = ProductResponseDto(
            id = "prod-new",
            ean = "7891000100103",
            name = "Leite Ninho",
            category = "Laticínios",
            brand = "Nestlé",
            measureUnit = MeasureUnit.UNIT,
            netWeight = 1.0,
            thumbnailUrl = null,
            manuallyAdded = true,
            notes = null,
            updatedAt = 1000L,
            isDeleted = false
        )

        whenever(productService.saveProduct(any())).thenReturn(responseDto)

        mockMvc.perform(
            post("/api/v1/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Leite Ninho"))
    }
}
