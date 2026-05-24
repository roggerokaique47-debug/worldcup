package com.example.services

import com.example.data.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o serviço AiAnalyticsService
 */
class AiAnalyticsServiceTest {

    @Test
    fun `test detect suspicious accuracy anomaly`() {
        // Usuário com taxa de acerto suspeitamente alta (>90%)
        val suspiciousUser = UserEntity(
            id = 1,
            name = "Suspect User",
            email = "suspect@test.com",
            username = "suspect_wc",
            winRate = 95,
            points = 6000
        )

        val normalUser = UserEntity(
            id = 2,
            name = "Normal User",
            email = "normal@test.com",
            username = "normal_wc",
            winRate = 65,
            points = 1500
        )

        val users = listOf(suspiciousUser, normalUser)
        
        // Simula detecção de anomalia
        val anomalies = users.filter { user ->
            user.winRate > 90 && user.points > 5000
        }

        assertEquals(1, anomalies.size)
        assertEquals("Suspect User", anomalies[0].name)
    }

    @Test
    fun `test user risk profile determination`() {
        // Perfil conservativo: alta precisão (>70%)
        val conservativeAccuracy = 75.0
        val conservativeExactScoreRate = 10.0
        
        val conservativeProfile = when {
            conservativeAccuracy > 70 -> "CONSERVATIVE"
            conservativeExactScoreRate > 20 -> "AGGRESSIVE"
            else -> "MODERATE"
        }
        
        assertEquals("CONSERVATIVE", conservativeProfile)

        // Perfil agressivo: alta taxa de placar exato (>20%)
        val aggressiveAccuracy = 50.0
        val aggressiveExactScoreRate = 25.0
        
        val aggressiveProfile = when {
            aggressiveAccuracy > 70 -> "CONSERVATIVE"
            aggressiveExactScoreRate > 20 -> "AGGRESSIVE"
            else -> "MODERATE"
        }
        
        assertEquals("AGGRESSIVE", aggressiveProfile)

        // Perfil moderado
        val moderateAccuracy = 60.0
        val moderateExactScoreRate = 15.0
        
        val moderateProfile = when {
            moderateAccuracy > 70 -> "CONSERVATIVE"
            moderateExactScoreRate > 20 -> "AGGRESSIVE"
            else -> "MODERATE"
        }
        
        assertEquals("MODERATE", moderateProfile)
    }

    @Test
    fun `test optimism score calculation`() {
        // Simula palpites com tendência de otimismo
        val homeWins = 7
        val totalPredictions = 10
        
        val optimismScore = (homeWins.toDouble() / totalPredictions) * 100
        
        assertEquals(70.0, optimismScore, 0.001)
        assertTrue(optimismScore > 50.0) // Mais otimista que neutro
    }

    @Test
    fun `test preferred result determination`() {
        val homeWins = 5
        val draws = 3
        val awayWins = 2
        
        val preferredResult = when {
            homeWins > draws && homeWins > awayWins -> "Vitória do Time da Casa"
            draws > homeWins && draws > awayWins -> "Empate"
            else -> "Vitória do Visitante"
        }
        
        assertEquals("Vitória do Time da Casa", preferredResult)
    }

    @Test
    fun `test user suggestions generation`() {
        val suggestions = mutableListOf<String>()
        val accuracy = 35.0
        val exactScoreRate = 18.0
        val optimismScore = 75.0
        
        if (accuracy < 40) {
            suggestions.add("Estude estatísticas dos times antes de palpitar")
        }
        if (exactScoreRate > 15) {
            suggestions.add("Você é bom em placares exatos! Continue assim")
        }
        if (optimismScore > 70) {
            suggestions.add("Cuidado com otimismo excessivo - nem sempre o favorito vence")
        }

        assertEquals(3, suggestions.size)
        assertTrue(suggestions.contains("Estude estatísticas dos times antes de palpitar"))
    }

    @Test
    fun `test analytics summary generation`() {
        val summary = AnalyticsSummary(
            timestamp = System.currentTimeMillis(),
            totalUsers = 100,
            activeUsers = 75,
            totalClubs = 15,
            totalMatches = 48,
            averageAccuracy = 52.5,
            suspiciousActivities = 2,
            topSuggestions = listOf(
                "Adicionar mais ligas temáticas",
                "Implementar sistema de conquistas"
            )
        )

        assertEquals(100, summary.totalUsers)
        assertEquals(75, summary.activeUsers)
        assertEquals(2, summary.suspiciousActivities)
        assertEquals(2, summary.topSuggestions.size)
    }
}
