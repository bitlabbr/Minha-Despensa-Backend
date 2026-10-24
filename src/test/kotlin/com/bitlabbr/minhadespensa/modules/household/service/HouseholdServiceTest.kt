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

package com.bitlabbr.minhadespensa.modules.household.service

import com.bitlabbr.minhadespensa.common.exception.BusinessException
import com.bitlabbr.minhadespensa.common.exception.ForbiddenException
import com.bitlabbr.minhadespensa.common.exception.InvalidRequestException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.auth.entity.UserEntity
import com.bitlabbr.minhadespensa.modules.auth.repository.UserRepository
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductCategoryService
import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
import com.bitlabbr.minhadespensa.modules.household.dto.CreateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.JoinHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdEntity
import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdMemberEntity
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdMemberRepository
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class HouseholdServiceTest {

    private val householdRepository: HouseholdRepository = mock()
    private val householdMemberRepository: HouseholdMemberRepository = mock()
    private val userRepository: UserRepository = mock()
    private val productCategoryService: ProductCategoryService = mock()

    private lateinit var householdService: HouseholdService

    @BeforeEach
    fun setUp() {
        householdService = HouseholdService(
            householdRepository = householdRepository,
            householdMemberRepository = householdMemberRepository,
            userRepository = userRepository,
            productCategoryService = productCategoryService
        )
    }

    @Test
    fun `requireHouseholdAccess should throw InvalidRequestException when header is blank`() {
        assertThrows<InvalidRequestException> {
            householdService.requireHouseholdAccess("user-1", null)
        }
        assertThrows<InvalidRequestException> {
            householdService.requireHouseholdAccess("user-1", "   ")
        }
    }

    @Test
    fun `requireHouseholdAccess should throw ForbiddenException when user is not a member`() {
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1"))
            .thenReturn(Optional.empty())

        assertThrows<ForbiddenException> {
            householdService.requireHouseholdAccess("user-1", "house-1")
        }
    }

    @Test
    fun `requireHouseholdAccess should succeed for valid member`() {
        val member = HouseholdMemberEntity(householdId = "house-1", userId = "user-1", role = HouseholdRole.MEMBER)
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1"))
            .thenReturn(Optional.of(member))

        val context = householdService.requireHouseholdAccess("user-1", "house-1")

        assertEquals("house-1", context.householdId)
        assertEquals("user-1", context.userId)
        assertEquals(HouseholdRole.MEMBER, context.role)
    }

    @Test
    fun `nextSyncVersion should increment syncVersion atomically`() {
        whenever(householdRepository.incrementSyncVersion(any(), any())).thenReturn(1)
        val household = HouseholdEntity(id = "house-1", syncVersion = 42L)
        whenever(householdRepository.findById("house-1")).thenReturn(Optional.of(household))

        val version = householdService.nextSyncVersion("house-1")

        assertEquals(42L, version)
        verify(householdRepository).incrementSyncVersion(any(), any())
    }

    @Test
    fun `createHousehold should save household and associate creator as OWNER`() {
        val user = UserEntity(id = "user-1", name = "Willian", email = "willian@test.com")
        whenever(userRepository.findById("user-1")).thenReturn(Optional.of(user))
        whenever(householdRepository.existsByInviteCodeIgnoreCase(any())).thenReturn(false)

        val household = HouseholdEntity(id = "house-1", name = "Nossa Casa", inviteCode = "CASA-12345678", ownerId = "user-1")
        whenever(householdRepository.save(any<HouseholdEntity>())).thenReturn(household)
        whenever(householdMemberRepository.save(any<HouseholdMemberEntity>())).thenAnswer { it.arguments[0] }
        whenever(householdMemberRepository.findByHouseholdId("house-1")).thenReturn(emptyList())

        val result = householdService.createHousehold("user-1", CreateHouseholdRequestDto(name = "Nossa Casa"))

        assertNotNull(result)
        assertEquals("house-1", result.id)
        assertEquals("Nossa Casa", result.name)
        assertTrue(result.inviteCode.startsWith("CASA-"))

        verify(householdRepository).save(any<HouseholdEntity>())
        verify(householdMemberRepository).save(any<HouseholdMemberEntity>())
        verify(productCategoryService).seedDefaultCategories("house-1")
    }

    @Test
    fun `joinHousehold should link user as MEMBER to found household`() {
        val household = HouseholdEntity(id = "house-1", name = "Casa da Família", inviteCode = "CASA-JOINCODE", ownerId = "owner-1")
        whenever(householdRepository.findByInviteCodeIgnoreCase("CASA-JOINCODE")).thenReturn(Optional.of(household))
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-2")).thenReturn(Optional.empty())
        whenever(userRepository.findById("user-2")).thenReturn(Optional.of(UserEntity(id = "user-2")))
        whenever(householdMemberRepository.findByHouseholdId("house-1")).thenReturn(emptyList())

        val result = householdService.joinHousehold("user-2", JoinHouseholdRequestDto(inviteCode = "casa-joincode"))

        assertEquals("house-1", result.id)
        verify(householdMemberRepository).save(any<HouseholdMemberEntity>())
    }

    @Test
    fun `joinHousehold should throw ResourceNotFoundException on invalid invite code`() {
        whenever(householdRepository.findByInviteCodeIgnoreCase("CASA-INVALID")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            householdService.joinHousehold("user-1", JoinHouseholdRequestDto(inviteCode = "CASA-INVALID"))
        }
    }

    @Test
    fun `leaveHousehold should disallow OWNER from leaving when there are other members`() {
        val ownerMember = HouseholdMemberEntity(householdId = "house-1", userId = "user-1", role = HouseholdRole.OWNER)
        val otherMember = HouseholdMemberEntity(householdId = "house-1", userId = "user-2", role = HouseholdRole.MEMBER)

        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1"))
            .thenReturn(Optional.of(ownerMember))
        whenever(householdMemberRepository.findByHouseholdId("house-1"))
            .thenReturn(listOf(ownerMember, otherMember))

        assertThrows<BusinessException> {
            householdService.leaveHousehold("house-1", "user-1")
        }
    }

    @Test
    fun `regenerateInviteCode should fail if user is not OWNER`() {
        val member = HouseholdMemberEntity(householdId = "house-1", userId = "user-2", role = HouseholdRole.MEMBER)
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-2"))
            .thenReturn(Optional.of(member))

        assertThrows<ForbiddenException> {
            householdService.regenerateInviteCode("house-1", "user-2")
        }
    }

    @Test
    fun `getHouseholdById should return detail DTO when user is member`() {
        val household = HouseholdEntity(id = "house-1", name = "Minha Casa", inviteCode = "CASA-12345678", ownerId = "user-1")
        val member = HouseholdMemberEntity(householdId = "house-1", userId = "user-1", role = HouseholdRole.OWNER)
        whenever(householdRepository.findById("house-1")).thenReturn(Optional.of(household))
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1")).thenReturn(Optional.of(member))
        whenever(householdMemberRepository.findByHouseholdId("house-1")).thenReturn(listOf(member))
        whenever(userRepository.findAllById(listOf("user-1"))).thenReturn(listOf(UserEntity(id = "user-1", name = "Willian", email = "willian@test.com")))

        val result = householdService.getHouseholdById("house-1", "user-1")

        assertNotNull(result)
        assertEquals("house-1", result.id)
        assertEquals("Minha Casa", result.name)
        assertEquals(1, result.members.size)
        assertEquals("Willian", result.members[0].name)
    }

    @Test
    fun `getHouseholdById should throw ResourceNotFoundException when household does not exist`() {
        whenever(householdRepository.findById("house-nonexistent")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            householdService.getHouseholdById("house-nonexistent", "user-1")
        }
    }

    @Test
    fun `getHouseholdById should throw ForbiddenException when user is not member`() {
        val household = HouseholdEntity(id = "house-1", name = "Minha Casa", inviteCode = "CASA-12345678", ownerId = "user-2")
        whenever(householdRepository.findById("house-1")).thenReturn(Optional.of(household))
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1")).thenReturn(Optional.empty())

        assertThrows<ForbiddenException> {
            householdService.getHouseholdById("house-1", "user-1")
        }
    }
}
