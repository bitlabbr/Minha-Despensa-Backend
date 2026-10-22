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
import com.bitlabbr.minhadespensa.modules.auth.repository.UserRepository
import com.bitlabbr.minhadespensa.modules.household.domain.HouseholdRole
import com.bitlabbr.minhadespensa.modules.household.dto.CreateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.HouseholdDetailResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.HouseholdMemberResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.InviteCodeResponseDto
import com.bitlabbr.minhadespensa.modules.household.dto.JoinHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.dto.UpdateHouseholdRequestDto
import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdEntity
import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdMemberEntity
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdMemberRepository
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdRepository
import com.bitlabbr.minhadespensa.modules.household.security.HouseholdContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class HouseholdService(
    private val householdRepository: HouseholdRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(HouseholdService::class.java)
    private val secureRandom = SecureRandom()
    private val crockfordAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional(readOnly = true)
    fun requireHouseholdAccess(userId: String, headerHouseholdId: String?): HouseholdContext {
        if (headerHouseholdId.isNullOrBlank()) {
            throw InvalidRequestException("Header 'X-Household-Id' é obrigatório para acessar este recurso.")
        }

        val membership = householdMemberRepository.findByHouseholdIdAndUserId(headerHouseholdId.trim(), userId)
            .orElseThrow {
                ForbiddenException("Acesso negado: o usuário não é membro da casa '$headerHouseholdId'")
            }

        return HouseholdContext(
            householdId = headerHouseholdId.trim(),
            userId = userId,
            role = membership.role
        )
    }

    @Transactional
    fun nextSyncVersion(householdId: String): Long {
        val now = System.currentTimeMillis()
        val updatedRows = householdRepository.incrementSyncVersion(householdId, now)
        if (updatedRows == 0) {
            throw ResourceNotFoundException("Casa não encontrada para incremento de versão", "Household", householdId)
        }

        val household = householdRepository.findById(householdId)
            .orElseThrow { ResourceNotFoundException("Casa não encontrada", "Household", householdId) }

        return household.syncVersion
    }

    @Transactional(readOnly = true)
    fun getUserHouseholds(userId: String): List<HouseholdDetailResponseDto> {
        val memberships = householdMemberRepository.findByUserId(userId)
        if (memberships.isEmpty()) return emptyList()

        val householdIds = memberships.map { it.householdId }
        val households = householdRepository.findAllById(householdIds)

        return households.map { household ->
            val members = loadMembersForHousehold(household.id)
            mapToDetailDto(household, members)
        }
    }

    @Transactional
    fun createHousehold(userId: String, dto: CreateHouseholdRequestDto): HouseholdDetailResponseDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado", "User", userId) }

        val household = HouseholdEntity(
            name = dto.name.trim(),
            inviteCode = generateUniqueInviteCode(),
            ownerId = user.id
        )
        val savedHousehold = householdRepository.save(household)

        val member = HouseholdMemberEntity(
            householdId = savedHousehold.id,
            userId = user.id,
            role = HouseholdRole.OWNER
        )
        householdMemberRepository.save(member)

        if (user.defaultHouseholdId.isNullOrBlank()) {
            user.defaultHouseholdId = savedHousehold.id
            userRepository.save(user)
        }

        logger.info("Nova casa criada: id=${savedHousehold.id}, name='${savedHousehold.name}', ownerId=$userId")
        val members = loadMembersForHousehold(savedHousehold.id)
        return mapToDetailDto(savedHousehold, members)
    }

    @Transactional
    fun updateHousehold(householdId: String, userId: String, dto: UpdateHouseholdRequestDto): HouseholdDetailResponseDto {
        val context = requireHouseholdAccess(userId, householdId)
        val household = householdRepository.findById(context.householdId)
            .orElseThrow { ResourceNotFoundException("Casa não encontrada", "Household", context.householdId) }

        household.name = dto.name.trim()
        household.updatedAt = System.currentTimeMillis()
        val updatedHousehold = householdRepository.save(household)

        logger.info("Casa atualizada: id=${updatedHousehold.id}, name='${updatedHousehold.name}'")
        val members = loadMembersForHousehold(updatedHousehold.id)
        return mapToDetailDto(updatedHousehold, members)
    }

    @Transactional
    fun joinHousehold(userId: String, dto: JoinHouseholdRequestDto): HouseholdDetailResponseDto {
        val cleanCode = dto.inviteCode.trim().uppercase()
        val household = householdRepository.findByInviteCodeIgnoreCase(cleanCode)
            .orElseThrow {
                ResourceNotFoundException("Nenhuma casa encontrada com o código de convite '$cleanCode'")
            }

        val existingMember = householdMemberRepository.findByHouseholdIdAndUserId(household.id, userId)
        if (existingMember.isEmpty) {
            val newMember = HouseholdMemberEntity(
                householdId = household.id,
                userId = userId,
                role = HouseholdRole.MEMBER
            )
            householdMemberRepository.save(newMember)
            logger.info("Usuário $userId ingressou na casa ${household.id} via código de convite $cleanCode")
        }

        // Define a casa recém-ingressada como a default do usuário
        val user = userRepository.findById(userId).orElse(null)
        if (user != null) {
            user.defaultHouseholdId = household.id
            userRepository.save(user)
        }

        val members = loadMembersForHousehold(household.id)
        return mapToDetailDto(household, members)
    }

    @Transactional
    fun leaveHousehold(householdId: String, userId: String) {
        val membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId)
            .orElseThrow { ResourceNotFoundException("Membro não encontrado nesta casa") }

        if (membership.role == HouseholdRole.OWNER) {
            val otherMembers = householdMemberRepository.findByHouseholdId(householdId).filter { it.userId != userId }
            if (otherMembers.isNotEmpty()) {
                throw BusinessException("O proprietário da casa não pode sair enquanto houver outros membros. Transfira a posse antes de sair.")
            }
        }

        householdMemberRepository.deleteByHouseholdIdAndUserId(householdId, userId)
        logger.info("Usuário $userId saiu da casa $householdId")

        // Se era a casa default do usuário, redefine para outra casa disponível
        val user = userRepository.findById(userId).orElse(null)
        if (user != null && user.defaultHouseholdId == householdId) {
            val remainingMemberships = householdMemberRepository.findByUserId(userId)
            user.defaultHouseholdId = remainingMemberships.firstOrNull()?.householdId
            userRepository.save(user)
        }
    }

    @Transactional
    fun regenerateInviteCode(householdId: String, userId: String): InviteCodeResponseDto {
        val context = requireHouseholdAccess(userId, householdId)
        if (context.role != HouseholdRole.OWNER) {
            throw ForbiddenException("Apenas o proprietário da casa pode regenerar o código de convite")
        }

        val household = householdRepository.findById(context.householdId)
            .orElseThrow { ResourceNotFoundException("Casa não encontrada", "Household", context.householdId) }

        val newCode = generateUniqueInviteCode()
        household.inviteCode = newCode
        household.updatedAt = System.currentTimeMillis()
        householdRepository.save(household)

        logger.info("Código de convite regenerado para a casa ${household.id}: novo código=$newCode")
        return InviteCodeResponseDto(inviteCode = newCode)
    }

    @Transactional
    fun removeMember(householdId: String, ownerUserId: String, targetUserId: String) {
        val context = requireHouseholdAccess(ownerUserId, householdId)
        if (context.role != HouseholdRole.OWNER) {
            throw ForbiddenException("Apenas o proprietário da casa pode remover outros membros")
        }

        if (ownerUserId == targetUserId) {
            throw BusinessException("O proprietário não pode se auto-remover da casa. Utilize a opção de sair.")
        }

        householdMemberRepository.deleteByHouseholdIdAndUserId(householdId, targetUserId)
        logger.info("Proprietário $ownerUserId removeu o membro $targetUserId da casa $householdId")

        val targetUser = userRepository.findById(targetUserId).orElse(null)
        if (targetUser != null && targetUser.defaultHouseholdId == householdId) {
            val remainingMemberships = householdMemberRepository.findByUserId(targetUserId)
            targetUser.defaultHouseholdId = remainingMemberships.firstOrNull()?.householdId
            userRepository.save(targetUser)
        }
    }

    private fun loadMembersForHousehold(householdId: String): List<HouseholdMemberResponseDto> {
        val memberEntities = householdMemberRepository.findByHouseholdId(householdId)
        if (memberEntities.isEmpty()) return emptyList()

        val userIds = memberEntities.map { it.userId }
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        return memberEntities.map { member ->
            val user = usersMap[member.userId]
            HouseholdMemberResponseDto(
                userId = member.userId,
                name = user?.name ?: "Membro",
                email = user?.email ?: "",
                role = member.role.name,
                joinedAt = member.joinedAt
            )
        }
    }

    private fun mapToDetailDto(household: HouseholdEntity, members: List<HouseholdMemberResponseDto>): HouseholdDetailResponseDto {
        return HouseholdDetailResponseDto(
            id = household.id,
            name = household.name,
            inviteCode = household.inviteCode,
            ownerId = household.ownerId,
            syncVersion = household.syncVersion,
            members = members,
            createdAt = household.createdAt,
            updatedAt = household.updatedAt
        )
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
