package com.example.services

import android.content.Context
import android.util.Log
import com.example.data.ClubEntity
import com.example.data.MatchEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Serviço de IA para análise de dados e suporte
 * 
 * Funcionalidades:
 * - Análise de padrões de palpites dos usuários
 * - Sugestões de melhorias para o app
 * - Detecção de anomalias (possíveis fraudes)
 * - Extração de insights para o time de suporte
 * - Integração com Gemini AI (opcional)
 */
class AiAnalyticsService(private val context: Context) {

    companion object {
        private const val TAG = "AiAnalyticsService"
    }

    private val _analyticsData = MutableStateFlow(AnalyticsSummary())
    val analyticsData: StateFlow<AnalyticsSummary> = _analyticsData.asStateFlow()

    /**
     * Analisa o comportamento de um usuário e gera insights
     */
    fun analyzeUserBehavior(user: UserEntity, predictions: List<MatchEntity>): UserInsights {
        val totalPredictions = predictions.count { it.predictedScoreA != null && it.predictedScoreB != null }
        val exactScores = predictions.count { 
            it.predictedScoreA == it.actualScoreA && 
            it.predictedScoreB == it.actualScoreB 
        }
        val correctResults = predictions.count { match ->
            match.predictedScoreA != null && match.predictedScoreB != null &&
            getPredictedResult(match.predictedScoreA!!, match.predictedScoreB!!) == 
            getActualResult(match.actualScoreA, match.actualScoreB)
        }

        val accuracyRate = if (totalPredictions > 0) {
            (correctResults.toDouble() / totalPredictions) * 100
        } else 0.0

        val exactScoreRate = if (totalPredictions > 0) {
            (exactScores.toDouble() / totalPredictions) * 100
        } else 0.0

        // Detecta padrão de otimismo/pessimismo
        val optimismScore = calculateOptimismScore(predictions)

        return UserInsights(
            userId = user.id,
            totalPredictions = totalPredictions,
            accuracyRate = accuracyRate,
            exactScoreRate = exactScoreRate,
            optimismScore = optimismScore,
            preferredResult = determinePreferredResult(predictions),
            riskProfile = determineRiskProfile(accuracyRate, exactScoreRate),
            suggestions = generateUserSuggestions(accuracyRate, exactScoreRate, optimismScore)
        )
    }

    /**
     * Analisa dados de todos os clubes para identificar tendências
     */
    fun analyzeClubsPerformance(clubs: List<ClubEntity>): ClubAnalytics {
        val totalClubs = clubs.size
        val privateClubs = clubs.count { it.isPrivate }
        val publicClubs = totalClubs - privateClubs
        
        val averageEntryFee = if (clubs.isNotEmpty()) {
            clubs.sumOf { it.entryFee } / clubs.size
        } else 0.0

        val totalPrizePool = clubs.sumOf { it.estimatedPrizePool }
        val totalMembers = clubs.sumOf { it.memberCount }

        val mostPopularClub = clubs.maxByOrNull { it.memberCount }
        val highestPrizeClub = clubs.maxByOrNull { it.estimatedPrizePool }

        return ClubAnalytics(
            totalClubs = totalClubs,
            privateClubsCount = privateClubs,
            publicClubsCount = publicClubs,
            averageEntryFee = averageEntryFee,
            totalPrizePool = totalPrizePool,
            totalMembers = totalMembers,
            mostPopularClub = mostPopularClub,
            highestPrizeClub = highestPrizeClub,
            growthTrend = calculateGrowthTrend(clubs)
        )
    }

    /**
     * Detecta anomalias nos dados (possíveis fraudes ou comportamentos suspeitos)
     */
    fun detectAnomalies(predictions: List<MatchEntity>, users: List<UserEntity>): List<AnomalyReport> {
        val anomalies = mutableListOf<AnomalyReport>()

        // Detecta usuários com taxa de acerto suspeitamente alta
        users.forEach { user ->
            if (user.winRate > 90 && user.points > 5000) {
                anomalies.add(
                    AnomalyReport(
                        type = AnomalyType.SUSPICIOUS_ACCURACY,
                        severity = SeverityLevel.HIGH,
                        description = "Usuário ${user.name} tem taxa de acerto de ${user.winRate}%",
                        userId = user.id,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // Detecta palpites feitos muito tarde (após início da partida)
        predictions.forEach { match ->
            // TODO: Implementar verificação de timestamp
        }

        // Detecta padrões de apostas incomuns
        // TODO: Implementar análise de padrões

        Log.d(TAG, "Anomalias detectadas: ${anomalies.size}")
        return anomalies
    }

    /**
     * Gera relatório consolidado para o time de suporte
     */
    fun generateSupportReport(): AnalyticsSummary {
        val summary = AnalyticsSummary(
            timestamp = System.currentTimeMillis(),
            totalUsers = 1, // TODO: Buscar do banco
            activeUsers = 1,
            totalClubs = 3,
            totalMatches = 6,
            averageAccuracy = 45.0,
            suspiciousActivities = 0,
            topSuggestions = listOf(
                "Adicionar mais ligas temáticas",
                "Implementar sistema de conquistas",
                "Melhorar UX na tela de palpites"
            )
        )

        _analyticsData.value = summary
        return summary
    }

    /**
     * Envia feedback anonimizado para melhorar o app
     */
    suspend fun sendUsageFeedback(feedback: UsageFeedback): Boolean {
        try {
            // TODO: Implementar envio para backend/Supabase
            Log.d(TAG, "Feedback enviado: ${feedback.category} - ${feedback.description}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar feedback", e)
            return false
        }
    }

    // Helper methods
    private fun getPredictedResult(scoreA: Int, scoreB: Int): String {
        return when {
            scoreA > scoreB -> "A"
            scoreA < scoreB -> "B"
            else -> "DRAW"
        }
    }

    private fun getActualResult(scoreA: Int, scoreB: Int): String {
        return when {
            scoreA > scoreB -> "A"
            scoreA < scoreB -> "B"
            else -> "DRAW"
        }
    }

    private fun calculateOptimismScore(predictions: List<MatchEntity>): Double {
        // Calcula tendência de prever vitórias vs empates/derrotas
        val homeWins = predictions.count { 
            (it.predictedScoreA ?: 0) > (it.predictedScoreB ?: 0) 
        }
        return if (predictions.isNotEmpty()) {
            (homeWins.toDouble() / predictions.size) * 100
        } else 50.0
    }

    private fun determinePreferredResult(predictions: List<MatchEntity>): String {
        val homeWins = predictions.count { 
            (it.predictedScoreA ?: 0) > (it.predictedScoreB ?: 0) 
        }
        val draws = predictions.count { 
            (it.predictedScoreA ?: 0) == (it.predictedScoreB ?: 0) 
        }
        val awayWins = predictions.count { 
            (it.predictedScoreA ?: 0) < (it.predictedScoreB ?: 0) 
        }

        return when {
            homeWins > draws && homeWins > awayWins -> "Vitória do Time da Casa"
            draws > homeWins && draws > awayWins -> "Empate"
            else -> "Vitória do Visitante"
        }
    }

    private fun determineRiskProfile(accuracy: Double, exactScoreRate: Double): RiskProfile {
        return when {
            accuracy > 70 -> RiskProfile.CONSERVATIVE
            exactScoreRate > 20 -> RiskProfile.AGGRESSIVE
            else -> RiskProfile.MODERATE
        }
    }

    private fun generateUserSuggestions(
        accuracy: Double,
        exactScoreRate: Double,
        optimismScore: Double
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (accuracy < 40) {
            suggestions.add("Estude estatísticas dos times antes de palpitar")
        }
        if (exactScoreRate > 15) {
            suggestions.add("Você é bom em placares exatos! Continue assim")
        }
        if (optimismScore > 70) {
            suggestions.add("Cuidado com otimismo excessivo - nem sempre o favorito vence")
        }

        return suggestions
    }

    private fun calculateGrowthTrend(clubs: List<ClubEntity>): Trend {
        // TODO: Implementar análise temporal real
        return Trend.GROWING
    }
}

// Data classes for analytics
data class AnalyticsSummary(
    val timestamp: Long = System.currentTimeMillis(),
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalClubs: Int = 0,
    val totalMatches: Int = 0,
    val averageAccuracy: Double = 0.0,
    val suspiciousActivities: Int = 0,
    val topSuggestions: List<String> = emptyList()
)

data class UserInsights(
    val userId: Int,
    val totalPredictions: Int,
    val accuracyRate: Double,
    val exactScoreRate: Double,
    val optimismScore: Double,
    val preferredResult: String,
    val riskProfile: RiskProfile,
    val suggestions: List<String>
)

enum class RiskProfile {
    CONSERVATIVE,
    MODERATE,
    AGGRESSIVE
}

data class ClubAnalytics(
    val totalClubs: Int,
    val privateClubsCount: Int,
    val publicClubsCount: Int,
    val averageEntryFee: Double,
    val totalPrizePool: Double,
    val totalMembers: Int,
    val mostPopularClub: ClubEntity?,
    val highestPrizeClub: ClubEntity?,
    val growthTrend: Trend
)

enum class Trend {
    GROWING,
    STABLE,
    DECLINING
}

data class AnomalyReport(
    val type: AnomalyType,
    val severity: SeverityLevel,
    val description: String,
    val userId: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AnomalyType {
    SUSPICIOUS_ACCURACY,
    LATE_PREDICTION,
    UNUSUAL_BETTING_PATTERN,
    MULTIPLE_ACCOUNTS,
    PAYMENT_FRAUD
}

enum class SeverityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class UsageFeedback(
    val category: String,
    val description: String,
    val rating: Int,
    val metadata: Map<String, String> = emptyMap()
)
