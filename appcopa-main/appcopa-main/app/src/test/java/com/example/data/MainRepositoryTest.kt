package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para MainRepository e Entities
 */
class MainRepositoryTest {

    @Test
    fun `test TeamStats data class initialization`() {
        val stats = MainRepository.TeamStats(
            name = "BRA",
            flag = "🇧🇷"
        )

        assertEquals("BRA", stats.name)
        assertEquals("🇧🇷", stats.flag)
        assertEquals(0, stats.points)
        assertEquals(0, stats.goalDiff)
        assertEquals(0, stats.goalsScored)
    }

    @Test
    fun `test TeamStats with initial values`() {
        val stats = MainRepository.TeamStats(
            name = "SUI",
            flag = "🇨🇭",
            points = 3,
            goalDiff = 2,
            goalsScored = 5
        )

        assertEquals("SUI", stats.name)
        assertEquals(3, stats.points)
        assertEquals(2, stats.goalDiff)
        assertEquals(5, stats.goalsScored)
    }

    @Test
    fun `test UserEntity default values`() {
        val user = UserEntity(
            name = "Test User",
            email = "test@example.com"
        )

        assertEquals("Test User", user.name)
        assertEquals("test@example.com", user.email)
        assertEquals("", user.username)
        assertEquals(1250, user.points)
        assertEquals("Veteran Player", user.levelName)
        assertEquals(8450, user.xp)
        assertEquals(10000, user.maxXp)
        assertEquals(45, user.accurateScoresCount)
        assertEquals(68, user.winRate)
        assertEquals(1450.0, user.balance, 0.001)
    }

    @Test
    fun `test ClubEntity default values`() {
        val club = ClubEntity(
            name = "Test Club",
            description = "A test club",
            isPrivate = false,
            entryFee = 50.0
        )

        assertEquals("Test Club", club.name)
        assertFalse(club.isPrivate)
        assertEquals(50.0, club.entryFee, 0.001)
        assertEquals(70, club.prizeFirstPlace)
        assertEquals(20, club.prizeSecondPlace)
        assertEquals(10, club.prizeThirdPlace)
        assertEquals(1, club.memberCount)
        assertEquals(150000.0, club.estimatedPrizePool, 0.001)
    }

    @Test
    fun `test MatchEntity nullable predictions`() {
        val matchWithPrediction = MatchEntity(
            groupName = "A",
            teamA = "BRA",
            teamB = "ARG",
            flagA = "🇧🇷",
            flagB = "🇦🇷",
            predictedScoreA = 2,
            predictedScoreB = 1,
            date = "24 JUN 2026",
            time = "16:00"
        )

        assertTrue(matchWithPrediction.predictedScoreA != null)
        assertTrue(matchWithPrediction.predictedScoreB != null)
        assertEquals(2, matchWithPrediction.predictedScoreA)
        assertEquals(1, matchWithPrediction.predictedScoreB)
    }

    @Test
    fun `test MatchEntity without predictions`() {
        val matchWithoutPrediction = MatchEntity(
            groupName = "A",
            teamA = "BRA",
            teamB = "ARG",
            flagA = "🇧🇷",
            flagB = "🇦🇷",
            predictedScoreA = null,
            predictedScoreB = null,
            actualScoreA = 3,
            actualScoreB = 1,
            date = "24 JUN 2026",
            time = "16:00"
        )

        assertTrue(matchWithoutPrediction.predictedScoreA == null)
        assertTrue(matchWithoutPrediction.predictedScoreB == null)
        assertEquals(3, matchWithoutPrediction.actualScoreA)
        assertEquals(1, matchWithoutPrediction.actualScoreB)
    }

    @Test
    fun `test TransactionEntity types`() {
        val deposit = TransactionEntity(
            type = "deposit",
            description = "Depósito via Stripe",
            method = "Stripe",
            amount = 100.0,
            date = "Hoje",
            status = "CONCLUÍDO"
        )

        val fee = TransactionEntity(
            type = "fee",
            description = "Taxa de clube",
            method = "Stripe",
            amount = -50.0,
            date = "Hoje",
            status = "CONCLUÍDO"
        )

        val prize = TransactionEntity(
            type = "prize",
            description = "Prêmio recebido",
            method = "System",
            amount = 500.0,
            date = "Hoje",
            status = "PAGO"
        )

        assertEquals("deposit", deposit.type)
        assertEquals("fee", fee.type)
        assertEquals("prize", prize.type)
        assertTrue(deposit.amount > 0)
        assertTrue(fee.amount < 0)
        assertTrue(prize.amount > 0)
    }

    @Test
    fun `test ClubEntity private club with password`() {
        val privateClub = ClubEntity(
            name = "Private Club",
            description = "Members only",
            isPrivate = true,
            accessPassword = "SECRET123",
            entryFee = 100.0
        )

        assertTrue(privateClub.isPrivate)
        assertEquals("SECRET123", privateClub.accessPassword)
    }

    @Test
    fun `test UserEntity copy updates balance`() {
        val user = UserEntity(
            name = "Test User",
            email = "test@example.com",
            balance = 1000.0
        )

        val updatedUser = user.copy(balance = 950.0)

        assertEquals(1000.0, user.balance, 0.001)
        assertEquals(950.0, updatedUser.balance, 0.001)
        assertEquals(user.name, updatedUser.name)
    }

    @Test
    fun `test ClubEntity copy updates member count`() {
        val club = ClubEntity(
            name = "Test Club",
            description = "Test",
            isPrivate = false,
            entryFee = 50.0,
            memberCount = 10
        )

        val updatedClub = club.copy(memberCount = 11)

        assertEquals(10, club.memberCount)
        assertEquals(11, updatedClub.memberCount)
        assertEquals(club.name, updatedClub.name)
    }
}
