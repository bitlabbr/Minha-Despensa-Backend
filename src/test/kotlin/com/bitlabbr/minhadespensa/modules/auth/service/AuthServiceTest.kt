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

package com.bitlabbr.minhadespensa.modules.auth.service

import com.bitlabbr.minhadespensa.common.exception.ConflictException
import com.bitlabbr.minhadespensa.common.exception.UnauthorizedException
import com.bitlabbr.minhadespensa.modules.auth.dto.LoginRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RefreshTokenRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RegisterRequestDto
import com.bitlabbr.minhadespensa.modules.auth.entity.RefreshTokenEntity
import com.bitlabbr.minhadespensa.modules.auth.entity.UserEntity
import com.bitlabbr.minhadespensa.modules.auth.repository.RefreshTokenRepository
import com.bitlabbr.minhadespensa.modules.auth.repository.UserRepository
import com.bitlabbr.minhadespensa.modules.auth.security.JwtTokenService
import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository: UserRepository = mock()
    private val refreshTokenRepository: RefreshTokenRepository = mock()
    private val householdRepository: HouseholdRepository = mock()
    private val householdMemberRepository: HouseholdMemberRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val jwtTokenService: JwtTokenService = mock()

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            householdRepository = householdRepository,
            householdMemberRepository = householdMemberRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenService = jwtTokenService,
            refreshExpirationMs = 2592000000L
        )
    }

    @Test
    fun `register should create user, default household, member as OWNER and return tokens`() {
        val dto = RegisterRequestDto(name = "Willian Santos", email = "willian@test.com", password = "password123")

        whenever(userRepository.existsByEmail("willian@test.com")).thenReturn(false)
        whenever(passwordEncoder.encode("password123")).thenReturn("encodedPassword")
        whenever(householdRepository.existsByInviteCodeIgnoreCase(any())).thenReturn(false)

        val userEntity = UserEntity(
            id = "user-1",
            name = "Willian Santos",
            email = "willian@test.com",
            passwordHash = "encodedPassword"
        )
        whenever(userRepository.save(any<UserEntity>())).thenReturn(userEntity)

        val householdEntity = HouseholdEntity(
            id = "house-1",
            name = "Despensa de Willian",
            inviteCode = "CASA-TESTCODE",
            ownerId = "user-1"
        )
        whenever(householdRepository.save(any<HouseholdEntity>())).thenReturn(householdEntity)

        val memberEntity = HouseholdMemberEntity(
            id = "member-1",
            householdId = "house-1",
            userId = "user-1",
            role = HouseholdRole.OWNER
        )
        whenever(householdMemberRepository.save(any<HouseholdMemberEntity>())).thenReturn(memberEntity)

        whenever(jwtTokenService.generateAccessToken(any())).thenReturn(Pair("access-token-jwt", 3600L))
        whenever(jwtTokenService.hashToken(any())).thenReturn("hashed-token")

        val response = authService.register(dto)

        assertNotNull(response)
        assertEquals("access-token-jwt", response.accessToken)
        assertEquals(3600L, response.expiresInSeconds)
        assertEquals("willian@test.com", response.user.email)
        assertEquals("house-1", response.activeHousehold?.id)
        assertEquals("OWNER", response.activeHousehold?.role)

        verify(userRepository, org.mockito.kotlin.times(2)).save(any<UserEntity>())
        verify(householdRepository).save(any<HouseholdEntity>())
        verify(householdMemberRepository).save(any<HouseholdMemberEntity>())
        verify(refreshTokenRepository).save(any<RefreshTokenEntity>())
    }

    @Test
    fun `register should throw ConflictException when email is already taken`() {
        val dto = RegisterRequestDto(name = "Willian", email = "duplicate@test.com", password = "password123")
        whenever(userRepository.existsByEmail("duplicate@test.com")).thenReturn(true)

        assertThrows<ConflictException> {
            authService.register(dto)
        }
    }

    @Test
    fun `login should succeed with valid credentials`() {
        val dto = LoginRequestDto(email = "willian@test.com", password = "password123")
        val user = UserEntity(
            id = "user-1",
            name = "Willian",
            email = "willian@test.com",
            passwordHash = "encodedPassword",
            defaultHouseholdId = "house-1"
        )
        val household = HouseholdEntity(
            id = "house-1",
            name = "Casa",
            inviteCode = "CASA-ABC",
            ownerId = "user-1"
        )
        val member = HouseholdMemberEntity(householdId = "house-1", userId = "user-1", role = HouseholdRole.OWNER)

        whenever(userRepository.findByEmail("willian@test.com")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true)
        whenever(householdRepository.findById("house-1")).thenReturn(Optional.of(household))
        whenever(householdMemberRepository.findByHouseholdIdAndUserId("house-1", "user-1")).thenReturn(Optional.of(member))
        whenever(jwtTokenService.generateAccessToken(user)).thenReturn(Pair("access-jwt", 3600L))
        whenever(jwtTokenService.hashToken(any())).thenReturn("token-hash")

        val response = authService.login(dto)

        assertNotNull(response)
        assertEquals("access-jwt", response.accessToken)
        assertEquals("willian@test.com", response.user.email)
        assertEquals("house-1", response.activeHousehold?.id)
    }

    @Test
    fun `login should throw UnauthorizedException on wrong password`() {
        val dto = LoginRequestDto(email = "willian@test.com", password = "wrongPassword")
        val user = UserEntity(id = "user-1", email = "willian@test.com", passwordHash = "encodedPassword")

        whenever(userRepository.findByEmail("willian@test.com")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false)

        assertThrows<UnauthorizedException> {
            authService.login(dto)
        }
    }

    @Test
    fun `refreshToken should detect reuse of revoked token and revoke entire family`() {
        val dto = RefreshTokenRequestDto(refreshToken = "reused-token")
        val familyId = UUID.randomUUID().toString()
        val revokedToken = RefreshTokenEntity(
            tokenHash = "hash-123",
            familyId = familyId,
            userId = "user-1",
            expiresAt = System.currentTimeMillis() + 100000,
            revoked = true
        )

        whenever(jwtTokenService.hashToken("reused-token")).thenReturn("hash-123")
        whenever(refreshTokenRepository.findByTokenHash("hash-123")).thenReturn(Optional.of(revokedToken))

        val ex = assertThrows<UnauthorizedException> {
            authService.refreshToken(dto)
        }

        assertTrue(ex.message!!.contains("reutilização"))
        verify(refreshTokenRepository).revokeAllInFamily(familyId)
    }
}
