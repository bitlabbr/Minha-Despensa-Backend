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
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.common.exception.UnauthorizedException
import com.bitlabbr.minhadespensa.modules.auth.dto.AuthResponseDto
import com.bitlabbr.minhadespensa.modules.auth.dto.HouseholdSummaryDto
import com.bitlabbr.minhadespensa.modules.auth.dto.LoginRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RefreshTokenRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.RegisterRequestDto
import com.bitlabbr.minhadespensa.modules.auth.dto.UserSummaryDto
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val householdRepository: HouseholdRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    @Value("\${app.jwt.refresh-expiration-ms:2592000000}")
    private val refreshExpirationMs: Long
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()
    private val crockfordAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun register(dto: RegisterRequestDto): AuthResponseDto {
        val normalizedEmail = dto.email.trim().lowercase()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException("Email '$normalizedEmail' is already registered")
        }

        val user = UserEntity(
            name = dto.name.trim(),
            email = normalizedEmail,
            passwordHash = requireNotNull(passwordEncoder.encode(dto.password)) { "Error generating password hash" }
        )
        val savedUser = userRepository.save(user)

        // Cria a Casa Pessoal inicial do novo usuário
        val firstName = dto.name.trim().split(" ").firstOrNull() ?: "Minha"
        val household = HouseholdEntity(
            name = "Despensa de $firstName",
            inviteCode = generateUniqueInviteCode(),
            ownerId = savedUser.id
        )
        val savedHousehold = householdRepository.save(household)

        // Adiciona como OWNER da casa pessoal
        val member = HouseholdMemberEntity(
            householdId = savedHousehold.id,
            userId = savedUser.id,
            role = HouseholdRole.OWNER
        )
        householdMemberRepository.save(member)

        savedUser.defaultHouseholdId = savedHousehold.id
        userRepository.save(savedUser)

        val (accessToken, expiresInSeconds) = jwtTokenService.generateAccessToken(savedUser)
        val rawRefreshToken = generateOpaqueToken()
        val familyId = UUID.randomUUID().toString()

        saveRefreshToken(savedUser.id, rawRefreshToken, familyId)

        logger.info("User registered successfully: id=${savedUser.id}, email=${savedUser.email}, householdId=${savedHousehold.id}")

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresInSeconds = expiresInSeconds,
            user = UserSummaryDto(
                id = savedUser.id,
                name = savedUser.name,
                email = savedUser.email,
                defaultHouseholdId = savedHousehold.id
            ),
            activeHousehold = HouseholdSummaryDto(
                id = savedHousehold.id,
                name = savedHousehold.name,
                inviteCode = savedHousehold.inviteCode,
                role = HouseholdRole.OWNER.name
            )
        )
    }

    @Transactional
    fun login(dto: LoginRequestDto): AuthResponseDto {
        val normalizedEmail = dto.email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { UnauthorizedException("Invalid email or password") }

        if (!passwordEncoder.matches(dto.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }

        val activeHousehold = resolveActiveHouseholdSummary(user)
        val (accessToken, expiresInSeconds) = jwtTokenService.generateAccessToken(user)
        val rawRefreshToken = generateOpaqueToken()
        val familyId = UUID.randomUUID().toString()

        saveRefreshToken(user.id, rawRefreshToken, familyId)

        logger.info("Login successful: id=${user.id}, email=${user.email}")

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresInSeconds = expiresInSeconds,
            user = UserSummaryDto(
                id = user.id,
                name = user.name,
                email = user.email,
                defaultHouseholdId = user.defaultHouseholdId
            ),
            activeHousehold = activeHousehold
        )
    }

    @Transactional
    fun refreshToken(dto: RefreshTokenRequestDto): AuthResponseDto {
        val tokenHash = jwtTokenService.hashToken(dto.refreshToken)
        val tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { UnauthorizedException("Invalid or missing refresh token") }

        if (tokenEntity.expiresAt < System.currentTimeMillis()) {
            throw UnauthorizedException("Refresh token expired. Please log in again.")
        }

        if (tokenEntity.revoked) {
            // Detecção de reutilização de token revogado (possível ataque de replay/roubo de token)
            refreshTokenRepository.revokeAllInFamily(tokenEntity.familyId)
            logger.warn("Revoked refresh token reuse detected! Revoked family ${tokenEntity.familyId}.")
            throw UnauthorizedException("Refresh token reuse detected. All sessions for this device have been terminated.")
        }

        // Revoga o token atual
        tokenEntity.revoked = true
        refreshTokenRepository.save(tokenEntity)

        val user = userRepository.findById(tokenEntity.userId)
            .orElseThrow { UnauthorizedException("User not found") }

        // Rotaciona para um novo refresh token dentro da mesma família
        val newRawRefreshToken = generateOpaqueToken()
        saveRefreshToken(user.id, newRawRefreshToken, tokenEntity.familyId)

        val (accessToken, expiresInSeconds) = jwtTokenService.generateAccessToken(user)
        val activeHousehold = resolveActiveHouseholdSummary(user)

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = newRawRefreshToken,
            expiresInSeconds = expiresInSeconds,
            user = UserSummaryDto(
                id = user.id,
                name = user.name,
                email = user.email,
                defaultHouseholdId = user.defaultHouseholdId
            ),
            activeHousehold = activeHousehold
        )
    }

    @Transactional
    fun logout(userId: String) {
        refreshTokenRepository.deleteByUserId(userId)
        logger.info("Logout completed for user $userId")
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(userId: String): UserSummaryDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found", "User", userId) }

        return UserSummaryDto(
            id = user.id,
            name = user.name,
            email = user.email,
            defaultHouseholdId = user.defaultHouseholdId
        )
    }

    private fun resolveActiveHouseholdSummary(user: UserEntity): HouseholdSummaryDto? {
        val targetHouseholdId = user.defaultHouseholdId
        if (targetHouseholdId != null) {
            val householdOpt = householdRepository.findById(targetHouseholdId)
            if (householdOpt.isPresent) {
                val household = householdOpt.get()
                val memberOpt = householdMemberRepository.findByHouseholdIdAndUserId(household.id, user.id)
                val role = memberOpt.map { it.role.name }.orElse(HouseholdRole.MEMBER.name)
                return HouseholdSummaryDto(
                    id = household.id,
                    name = household.name,
                    inviteCode = household.inviteCode,
                    role = role
                )
            }
        }

        // Fallback: pega a primeira casa da qual o usuário é membro
        val memberships = householdMemberRepository.findByUserId(user.id)
        val firstMembership = memberships.firstOrNull() ?: return null
        val household = householdRepository.findById(firstMembership.householdId).orElse(null) ?: return null

        return HouseholdSummaryDto(
            id = household.id,
            name = household.name,
            inviteCode = household.inviteCode,
            role = firstMembership.role.name
        )
    }

    private fun saveRefreshToken(userId: String, rawToken: String, familyId: String) {
        val tokenHash = jwtTokenService.hashToken(rawToken)
        val entity = RefreshTokenEntity(
            tokenHash = tokenHash,
            familyId = familyId,
            userId = userId,
            expiresAt = System.currentTimeMillis() + refreshExpirationMs
        )
        refreshTokenRepository.save(entity)
    }

    private fun generateOpaqueToken(): String {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "")
    }

    private fun generateUniqueInviteCode(): String {
        var code: String
        do {
            val sb = StringBuilder("CASA-")
            for (i in 0 until 8) {
                sb.append(crockfordAlphabet[secureRandom.nextInt(crockfordAlphabet.length)])
            }
            code = sb.toString()
        } while (householdRepository.existsByInviteCodeIgnoreCase(code))
        return code
    }
}
