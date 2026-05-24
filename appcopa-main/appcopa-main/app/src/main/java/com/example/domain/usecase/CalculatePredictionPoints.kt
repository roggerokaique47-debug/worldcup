package com.example.domain.usecase

import com.example.domain.model.Match
import com.example.domain.model.MatchResult
import com.example.domain.model.ScoreCalculation

/**
 * Caso de uso para calcular pontos de um palpite
 * Regras:
 * - Acertar vencedor: 3 pontos (ou 1 ponto se empate)
 * - Acertar placar exato: multiplicador 2x
 * - Ponto extra por acertar classificados da fase de grupos
 */
class CalculatePredictionPoints {

    /**
     * Calcula os pontos para um palpite baseado no resultado real
     */
    fun calculate(
        predictedScoreA: Int?,
        predictedScoreB: Int?,
        actualScoreA: Int,
        actualScoreB: Int
    ): ScoreCalculation {
        // Se não houve palpite, retorna 0 pontos
        if (predictedScoreA == null || predictedScoreB == null) {
            return ScoreCalculation(
                prediction = createMockPrediction(predictedScoreA, predictedScoreB),
                actualScoreA = actualScoreA,
                actualScoreB = actualScoreB,
                basePoints = 0,
                exactScoreBonus = false,
                multiplier = 1.0,
                totalPoints = 0.0
            )
        }

        // Verifica se acertou o placar exato
        val exactScore = predictedScoreA == actualScoreA && predictedScoreB == actualScoreB

        // Determina o resultado previsto e real
        val predictedResult = getMatchResult(predictedScoreA, predictedScoreB)
        val actualResult = getMatchResult(actualScoreA, actualScoreB)

        // Calcula pontos base
        val basePoints = when {
            predictedResult == actualResult -> {
                if (predictedResult == MatchResult.DRAW) {
                    ScoreCalculation.DRAW_POINTS
                } else {
                    ScoreCalculation.WIN_POINTS
                }
            }
            else -> 0
        }

        // Aplica multiplicador se acertou o placar exato
        val multiplier = if (exactScore) {
            ScoreCalculation.EXACT_SCORE_MULTIPLIER
        } else {
            1.0
        }

        // Bonus por placar exato
        val exactScoreBonusPoints = if (exactScore) {
            ScoreCalculation.BASE_EXACT_SCORE_BONUS
        } else {
            0
        }

        val totalPoints = (basePoints * multiplier) + exactScoreBonusPoints

        return ScoreCalculation(
            prediction = createMockPrediction(predictedScoreA, predictedScoreB),
            actualScoreA = actualScoreA,
            actualScoreB = actualScoreB,
            basePoints = basePoints,
            exactScoreBonus = exactScore,
            multiplier = multiplier,
            totalPoints = totalPoints
        )
    }

    /**
     * Determina o resultado de uma partida baseado nos placares
     */
    private fun getMatchResult(scoreA: Int, scoreB: Int): MatchResult {
        return when {
            scoreA > scoreB -> MatchResult.TEAM_A_WINS
            scoreA < scoreB -> MatchResult.TEAM_B_WINS
            else -> MatchResult.DRAW
        }
    }

    private fun createMockPrediction(scoreA: Int?, scoreB: Int?) = 
        com.example.domain.model.Prediction(
            matchId = 0,
            userId = 0,
            scoreA = scoreA,
            scoreB = scoreB
        )
}
