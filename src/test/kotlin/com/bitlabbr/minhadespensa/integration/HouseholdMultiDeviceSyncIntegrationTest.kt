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

package com.bitlabbr.minhadespensa.integration

import com.bitlabbr.minhadespensa.modules.auth.dto.RegisterRequestDto
import com.bitlabbr.minhadespensa.modules.auth.service.AuthService
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.household.dto.CreateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.JoinHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdRepository
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryItemRepository
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingItemRepository
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingListRepository
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import com.bitlabbr.minhadespensa.modules.sync.service.SyncService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HouseholdMultiDeviceSyncIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var householdService: HouseholdService

    @Autowired
    private lateinit var householdRepository: HouseholdRepository

    @Autowired
    private lateinit var syncService: SyncService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var pantryItemRepository: PantryItemRepository

    @Autowired
    private lateinit var shoppingListRepository: ShoppingListRepository

    @Autowired
    private lateinit var shoppingItemRepository: ShoppingItemRepository

    @Autowired
    private lateinit var aliasRepository: EntityIdAliasRepository

    @Test
    fun `full multi-device sync simulation with household sharing, EAN dedupe and cascade tombstone`() {
        // 1. Alice e Bob se registram no sistema
        val aliceAuth = authService.register(
            RegisterRequestDto(email = "alice.multi@test.com", password = "PasswordAlice123!", name = "Alice")
        )
        val bobAuth = authService.register(
            RegisterRequestDto(email = "bob.multi@test.com", password = "PasswordBob123!", name = "Bob")
        )

        // 2. Alice cria uma nova casa compartilhada
        val sharedHousehold = householdService.createHousehold(
            userId = aliceAuth.user.id,
            dto = CreateHouseholdRequestDto(name = "Casa Alice e Bob")
        )
        val householdId = sharedHousehold.id
        val inviteCode = sharedHousehold.inviteCode
        assertNotNull(inviteCode)

        // 3. Bob ingressa na casa compartilhada através do código de convite
        val joinedHousehold = householdService.joinHousehold(
            userId = bobAuth.user.id,
            dto = JoinHouseholdRequestDto(inviteCode = inviteCode)
        )
        assertEquals(householdId, joinedHousehold.id)

        // Validação de acesso de ambos à casa
        val aliceContext = householdService.requireHouseholdAccess(aliceAuth.user.id, householdId)
        val bobContext = householdService.requireHouseholdAccess(bobAuth.user.id, householdId)
        assertEquals(householdId, aliceContext.householdId)
        assertEquals(householdId, bobContext.householdId)

        // 4. Dispositivo de Alice: Cadastra produto industrializado com EAN e uma lista de compras
        val aliceProdId = "prod-alice-uuid-1111"
        val aliceListId = "list-alice-uuid-2222"
        val aliceItemId = "item-alice-uuid-3333"

        val pushAlice = SyncPushRequestDto(
            products = listOf(
                ProductRequestDto(
                    id = aliceProdId,
                    ean = "7891111111111",
                    name = "Arroz Tio João Tipo 1 5kg",
                    category = "Alimentos",
                    brand = "Tio João",
                    measureUnit = MeasureUnit.PACKAGE,
                    netWeight = 5.0,
                    updatedAt = 1000L
                )
            ),
            shoppingLists = listOf(
                ShoppingListRequestDto(
                    id = aliceListId,
                    name = "Compras Semanais",
                    budgetInCents = 10000L,
                    updatedAt = 1000L
                )
            ),
            shoppingItems = listOf(
                ShoppingItemRequestDto(
                    id = aliceItemId,
                    listId = aliceListId,
                    productId = aliceProdId,
                    rawText = "Comprar arroz",
                    quantity = 2.0,
                    priceAtTime = 2500L,
                    updatedAt = 1000L
                )
            )
        )

        val alicePushResponse = syncService.push(householdId, pushAlice)
        assertTrue(alicePushResponse.serverVersion > 0L)
        assertEquals(3, alicePushResponse.totalProcessed)
        assertTrue(alicePushResponse.rejected.isEmpty())

        // 5. Dispositivo de Bob: Cadastrou o mesmo produto offline com mesmo EAN mas UUID diferente
        val bobProdId = "prod-bob-uuid-9999"
        val bobPantryId = "pantry-bob-uuid-8888"

        val pushBob = SyncPushRequestDto(
            products = listOf(
                ProductRequestDto(
                    id = bobProdId,
                    ean = "7891111111111", // Mesmo EAN do produto cadastrado por Alice
                    name = "Arroz Tio João 5kg",
                    category = "Alimentos",
                    measureUnit = MeasureUnit.PACKAGE,
                    updatedAt = 2000L
                )
            ),
            pantryItems = listOf(
                PantryItemRequestDto(
                    id = bobPantryId,
                    productId = bobProdId, // Bob aponta para seu UUID local
                    quantity = 3.0,
                    unitPriceInCents = 2490L,
                    updatedAt = 2000L
                )
            )
        )

        val bobPushResponse = syncService.push(householdId, pushBob)

        // Validações da Deduplicação Híbrida e Reescrita de Chaves Estrangeiras
        assertTrue(bobPushResponse.serverVersion > alicePushResponse.serverVersion)
        assertEquals(2, bobPushResponse.totalProcessed)
        assertTrue(bobPushResponse.rejected.isEmpty())

        // O backend deve ter remapeado o ID de Bob para o ID canônico de Alice
        assertEquals(aliceProdId, bobPushResponse.productIdMappings[bobProdId])

        // No banco de dados: apenas 1 produto existe no catálogo da casa com aquele EAN
        val savedProduct = productRepository.findByHouseholdIdAndEan(householdId, "7891111111111")
        assertTrue(savedProduct.isPresent)
        assertEquals(aliceProdId, savedProduct.get().id)

        // O item de despensa gravado por Bob teve o productId reescrito para o canônico aliceProdId
        val savedPantryItem = pantryItemRepository.findByHouseholdIdAndId(householdId, bobPantryId)
        assertTrue(savedPantryItem.isPresent)
        assertEquals(aliceProdId, savedPantryItem.get().productId)

        // A tabela entity_id_aliases contém o registro durável do alias
        val aliasCanonical = aliasRepository.findCanonicalId(householdId, "PRODUCT", bobProdId)
        assertEquals(aliceProdId, aliasCanonical)

        // 6. Bob realiza Pull (sinceVersion = 0) e recebe o estado consolidado da casa
        val bobPull = syncService.pull(householdId, sinceVersion = 0L)
        assertEquals(1, bobPull.products.size)
        assertEquals(aliceProdId, bobPull.products[0].id)
        assertEquals(1, bobPull.shoppingLists.size)
        assertEquals(aliceListId, bobPull.shoppingLists[0].id)
        assertEquals(1, bobPull.shoppingItems.size)
        assertEquals(aliceItemId, bobPull.shoppingItems[0].id)
        assertEquals(1, bobPull.pantryItems.size)
        assertEquals(bobPantryId, bobPull.pantryItems[0].id)

        // 7. Alice exclui a lista de compras (Cascade Tombstone)
        val aliceDeletePush = SyncPushRequestDto(
            shoppingLists = listOf(
                ShoppingListRequestDto(
                    id = aliceListId,
                    name = "Compras Semanais",
                    updatedAt = 3000L,
                    isDeleted = true
                )
            )
        )
        val deleteResponse = syncService.push(householdId, aliceDeletePush)
        assertTrue(deleteResponse.serverVersion > bobPushResponse.serverVersion)

        // 8. Bob sincroniza (Pull sinceVersion = bobPushResponse.serverVersion)
        val bobPullDeltas = syncService.pull(householdId, sinceVersion = bobPushResponse.serverVersion)
        assertEquals(1, bobPullDeltas.shoppingLists.size)
        assertTrue(bobPullDeltas.shoppingLists[0].isDeleted)

        // Cascade Tombstone: o shopping item filho foi automaticamente tombado no servidor
        assertEquals(1, bobPullDeltas.shoppingItems.size)
        assertEquals(aliceItemId, bobPullDeltas.shoppingItems[0].id)
        assertTrue(bobPullDeltas.shoppingItems[0].isDeleted)
    }
}
