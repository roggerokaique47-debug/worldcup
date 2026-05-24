package com.example.domain.usecase

import com.example.domain.model.TeamStats

/**
 * Caso de uso para calcular classificação na fase de grupos
 */
class CalculateGroupStandings {

    /**
     * Calcula a classificação de um grupo baseado nas partidas
     * Critérios de desempate:
     * 1. Pontos
     * 2. Saldo de gols
     * 3. Gols pró
     */
    fun calculate(matches: List<com.example.domain.model.Match>): List<TeamStats> {
        val statsMap = mutableMapOf<String, TeamStats>()

        matches.forEach { match ->
            // Inicializa times se não existirem
            if (!statsMap.containsKey(match.teamA)) {
                statsMap[match.teamA] = TeamStats(
                    name = match.teamA,
                    flag = match.flagA
                )
            }
            if (!statsMap.containsKey(match.teamB)) {
                statsMap[match.teamB] = TeamStats(
                    name = match.teamB,
                    flag = match.flagB
                )
            }

            val statA = statsMap[match.teamA]!!
            val statB = statsMap[match.teamB]!!

            // Usa placar previsto se disponível, caso contrário usa o real
            val scoreA = match.predictedScoreA ?: match.actualScoreA
            val scoreB = match.predictedScoreB ?: match.actualScoreB

            // Atualiza estatísticas
            statA.played++
            statB.played++
            statA.goalsScored += scoreA
            statB.goalsScored += scoreB
            statA.goalDiff += (scoreA - scoreB)
            statB.goalDiff += (scoreB - scoreA)

            // Distribui pontos
            when {
                scoreA > scoreB -> {
                    statA.points += 3
                    statA.won++
                    statB.lost++
                }
                scoreA < scoreB -> {
                    statB.points += 3
                    statB.won++
                    statA.lost++
                }
                else -> {
                    statA.points += 1
                    statB.points += 1
                    statA.drawn++
                    statB.drawn++
                }
            }

            statsMap[match.teamA] = statA
            statsMap[match.teamB] = statB
        }

        // Ordena pelos critérios de desempate
        return statsMap.values.sortedWith(
            compareByDescending<TeamStats> { it.points }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsScored }
        )
    }

    /**
     * Retorna os times classificados (top N)
     */
    fun getQualifiedTeams(standings: List<TeamStats>, count: Int = 2): List<TeamStats> {
        return standings.take(count)
    }

    /**
     * Verifica se um time específico está entre os classificados
     */
    fun isTeamQualified(teamName: String, standings: List<TeamStats>, count: Int = 2): Boolean {
        return standings.take(count).any { it.name == teamName }
    }
}
