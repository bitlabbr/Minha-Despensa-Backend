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

package com.bitlabbr.minhadespensa.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class DeterministicIdUtilsTest {

    @Test
    fun `should normalize text by removing accents, converting to lowercase and collapsing whitespace`() {
        assertEquals("pao frances", DeterministicIdUtils.normalizeText("  Pão   Francês  "))
        assertEquals("maca gala", DeterministicIdUtils.normalizeText("Maçã    Gala"))
        assertEquals("acucar refinado", DeterministicIdUtils.normalizeText("AÇÚCAR REFINADO"))
        assertEquals("leite integral", DeterministicIdUtils.normalizeText("Leite Integral"))
    }

    @Test
    fun `productIdForEan should generate deterministic UUIDv5 matching across identical EANs`() {
        val ean1 = "7891000100103"
        val ean2 = "  7891000100103  "

        val id1 = DeterministicIdUtils.productIdForEan(ean1)
        val id2 = DeterministicIdUtils.productIdForEan(ean2)

        assertEquals(id1, id2)
        assertNotNull(UUID.fromString(id1))
        assertEquals(5, UUID.fromString(id1).version())
    }

    @Test
    fun `productIdForEan should fail on blank EAN`() {
        assertThrows<IllegalArgumentException> {
            DeterministicIdUtils.productIdForEan("   ")
        }
    }

    @Test
    fun `productIdForNameAndUnit should generate identical UUIDv5 regardless of casing, accents and whitespace`() {
        val id1 = DeterministicIdUtils.productIdForNameAndUnit("Banana Prata", "KG")
        val id2 = DeterministicIdUtils.productIdForNameAndUnit("  banana prata  ", "kg")
        val id3 = DeterministicIdUtils.productIdForNameAndUnit("BANANA   PRATA", "Kg")

        assertEquals(id1, id2)
        assertEquals(id2, id3)
        assertEquals(5, UUID.fromString(id1).version())
    }

    @Test
    fun `productIdForNameAndUnit should differentiate between different measurement units`() {
        val idKg = DeterministicIdUtils.productIdForNameAndUnit("Banana Prata", "KG")
        val idUnit = DeterministicIdUtils.productIdForNameAndUnit("Banana Prata", "UNIT")

        assertNotEquals(idKg, idUnit)
    }

    @Test
    fun `categoryIdForName should generate identical UUIDv5 regardless of casing and accents`() {
        val id1 = DeterministicIdUtils.categoryIdForName("Laticínios")
        val id2 = DeterministicIdUtils.categoryIdForName("  laticinios  ")
        val id3 = DeterministicIdUtils.categoryIdForName("LATICÍNIOS")

        assertEquals(id1, id2)
        assertEquals(id2, id3)
        assertEquals(5, UUID.fromString(id1).version())
    }
}
