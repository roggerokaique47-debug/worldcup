package com.example.domain.model

/**
 * Modelo de domínio para partidas do bolão
 */
data class Match(
    val id: Int,
    val groupName: String,
    val teamA: String,
    val teamB: String,
    val flagA: String,
    val flagB: String,
    val predictedScoreA: Int? = null,
    val predictedScoreB: Int? = null,
    val actualScoreA: Int = 0,
    val actualScoreB: Int = 0,
    val date: String,
    val time: String,
    val stadium: String = "Maracanã"
)

/**
 * Resultado possível de uma partida
 */
enum class MatchResult {
    TEAM_A_WINS,
    DRAW,
    TEAM_B_WINS
}

/**
 * Estatísticas de um time na fase de grupos
 */
data class TeamStats(
    val name: String,
    val flag: String,
    var points: Int = 0,
    var goalDiff: Int = 0,
    var goalsScored: Int = 0,
    var played: Int = 0,
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0
)

/**
 * Palpite de um usuário para uma partida
 */
data class Prediction(
    val matchId: Int,
    val userId: Int,
    val scoreA: Int?,
    val scoreB: Int?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Cálculo de pontos para um palpite
 */
data class ScoreCalculation(
    val prediction: Prediction,
    val actualScoreA: Int,
    val actualScoreB: Int,
    val basePoints: Int,
    val exactScoreBonus: Boolean,
    val multiplier: Double,
    val totalPoints: Double
) {
    companion object {
        const val WIN_POINTS = 3
        const val DRAW_POINTS = 1
        const val EXACT_SCORE_MULTIPLIER = 2.0
        const val BASE_EXACT_SCORE_BONUS = 5
    }
}
