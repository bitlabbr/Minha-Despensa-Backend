package com.bitlabbr.minhadespensa.modules.sync.service

import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.price.service.PriceEntryService
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.service.ShoppingListService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncService(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val pantryService: PantryService,
    private val shoppingListService: ShoppingListService,
    private val priceEntryService: PriceEntryService
) {
    private val logger = LoggerFactory.getLogger(SyncService::class.java)

    @Transactional
    fun push(request: SyncPushRequestDto): SyncPushResponseDto {
        var count = 0

        // 1. Processar produtos do catálogo
        request.products.forEach {
            productService.saveProduct(it)
            count++
        }

        // 2. Processar itens da despensa
        request.pantryItems.forEach {
            pantryService.saveItem(it)
            count++
        }

        // 3. Processar listas de compras
        request.shoppingLists.forEach {
            shoppingListService.saveList(it)
            count++
        }

        // 4. Processar itens de compras
        request.shoppingItems.forEach {
            shoppingListService.saveItem(it)
            count++
        }

        // 5. Processar registros de preços
        request.priceEntries.forEach {
            priceEntryService.savePrice(it)
            count++
        }

        val serverTimestamp = System.currentTimeMillis()
        logger.info("Sync PUSH concluído com sucesso: $count registros processados no timestamp $serverTimestamp")

        return SyncPushResponseDto(
            serverTimestamp = serverTimestamp,
            totalProcessed = count
        )
    }

    @Transactional(readOnly = true)
    fun pull(since: Long): SyncPullResponseDto {
        val serverTimestamp = System.currentTimeMillis()

        val products = productRepository.findByUpdatedAtGreaterThan(since)
            .map { ProductResponseDto.fromEntity(it) }

        val pantryItems = pantryService.getModifiedSince(since)
            .map { PantryItemResponseDto.fromEntity(it) }

        val shoppingLists = shoppingListService.getListsModifiedSince(since)
            .map { ShoppingListResponseDto.fromEntity(it) }

        val shoppingItems = shoppingListService.getItemsModifiedSince(since)
            .map { ShoppingItemResponseDto.fromEntity(it) }

        val priceEntries = priceEntryService.getPricesModifiedSince(since)
            .map { PriceEntryResponseDto.fromEntity(it) }

        val total = products.size + pantryItems.size + shoppingLists.size + shoppingItems.size + priceEntries.size
        logger.info("Sync PULL concluído: $total registros alterados desde $since (servidor=$serverTimestamp)")

        return SyncPullResponseDto(
            serverTimestamp = serverTimestamp,
            products = products,
            pantryItems = pantryItems,
            shoppingLists = shoppingLists,
            shoppingItems = shoppingItems,
            priceEntries = priceEntries
        )
    }
}
