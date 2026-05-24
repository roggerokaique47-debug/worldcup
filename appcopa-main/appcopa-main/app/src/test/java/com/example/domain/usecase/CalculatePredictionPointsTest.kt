package com.example.domain.usecase

import com.example.domain.model.MatchResult
import com.example.domain.model.TeamStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o caso de uso CalculatePredictionPoints
 */
class CalculatePredictionPointsTest {

    private val useCase = CalculatePredictionPoints()

    @Test
    fun `test calculate points when predicted winner correctly`() {
        // Time A vence na previsão e na realidade
        val result = useCase.calculate(
            predictedScoreA = 2,
            predictedScoreB = 1,
            actualScoreA = 3,
            actualScoreB = 1
        )

        assertEquals(3, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(3.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points when predicted loser incorrectly`() {
        // Previu vitória do A, mas B venceu
        val result = useCase.calculate(
            predictedScoreA = 2,
            predictedScoreB = 1,
            actualScoreA = 1,
            actualScoreB = 2
        )

        assertEquals(0, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(0.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points with exact score prediction`() {
        // Acertou placar exato
        val result = useCase.calculate(
            predictedScoreA = 2,
            predictedScoreB = 1,
            actualScoreA = 2,
            actualScoreB = 1
        )

        assertEquals(3, result.basePoints)
        assertTrue(result.exactScoreBonus)
        assertEquals(2.0, result.multiplier, 0.001)
        // (3 * 2.0) + 5 = 11
        assertEquals(11.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points for correct draw prediction`() {
        // Previu empate e houve empate
        val result = useCase.calculate(
            predictedScoreA = 1,
            predictedScoreB = 1,
            actualScoreA = 2,
            actualScoreB = 2
        )

        assertEquals(1, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(1.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points for exact draw prediction`() {
        // Acertou empate exato
        val result = useCase.calculate(
            predictedScoreA = 1,
            predictedScoreB = 1,
            actualScoreA = 1,
            actualScoreB = 1
        )

        assertEquals(1, result.basePoints)
        assertTrue(result.exactScoreBonus)
        assertEquals(2.0, result.multiplier, 0.001)
        // (1 * 2.0) + 5 = 7
        assertEquals(7.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points when no prediction made`() {
        // Usuário não fez palpite
        val result = useCase.calculate(
            predictedScoreA = null,
            predictedScoreB = null,
            actualScoreA = 2,
            actualScoreB = 1
        )

        assertEquals(0, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(0.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points when only one team score predicted`() {
        // Palpite parcial (apenas um time)
        val result = useCase.calculate(
            predictedScoreA = 2,
            predictedScoreB = null,
            actualScoreA = 2,
            actualScoreB = 1
        )

        assertEquals(0, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(0.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points for wrong draw prediction`() {
        // Previu empate mas houve vencedor
        val result = useCase.calculate(
            predictedScoreA = 1,
            predictedScoreB = 1,
            actualScoreA = 2,
            actualScoreB = 0
        )

        assertEquals(0, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(0.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test calculate points for predicting draw when there was a winner`() {
        // Previu vitória do A mas foi empate
        val result = useCase.calculate(
            predictedScoreA = 2,
            predictedScoreB = 0,
            actualScoreA = 1,
            actualScoreB = 1
        )

        assertEquals(0, result.basePoints)
        assertFalse(result.exactScoreBonus)
        assertEquals(1.0, result.multiplier, 0.001)
        assertEquals(0.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test high score exact prediction`() {
        // Placar exato com muitos gols
        val result = useCase.calculate(
            predictedScoreA = 4,
            predictedScoreB = 3,
            actualScoreA = 4,
            actualScoreB = 3
        )

        assertEquals(3, result.basePoints)
        assertTrue(result.exactScoreBonus)
        assertEquals(2.0, result.multiplier, 0.001)
        assertEquals(11.0, result.totalPoints, 0.001)
    }

    @Test
    fun `test zero zero exact prediction`() {
        // 0x0 exato
        val result = useCase.calculate(
            predictedScoreA = 0,
            predictedScoreB = 0,
            actualScoreA = 0,
            actualScoreB = 0
        )

        assertEquals(1, result.basePoints) // empate vale 1 ponto
        assertTrue(result.exactScoreBonus)
        assertEquals(2.0, result.multiplier, 0.001)
        assertEquals(7.0, result.totalPoints, 0.001)
    }
}
