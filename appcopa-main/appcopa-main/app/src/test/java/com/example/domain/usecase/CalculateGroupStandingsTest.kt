package com.example.domain.usecase

import com.example.domain.model.TeamStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o caso de uso CalculateGroupStandings
 */
class CalculateGroupStandingsTest {

    private val useCase = CalculateGroupStandings()

    @Test
    fun `test calculate standings with clear winner`() {
        val matches = createMockMatchesGroupA()
        val standings = useCase.calculate(matches)

        // Verifica se há 4 times na classificação
        assertEquals(4, standings.size)

        // Verifica ordenação por pontos
        for (i in 0 until standings.size - 1) {
            assertTrue(standings[i].points >= standings[i + 1].points)
        }
    }

    @Test
    fun `test team wins all matches gets 9 points`() {
        val matches = listOf(
            createMatch("BRA", "SUI", 3, 0),
            createMatch("BRA", "SRB", 2, 0),
            createMatch("BRA", "CMR", 1, 0)
        )

        val standings = useCase.calculate(matches)
        val brazil = standings.find { it.name == "BRA" }

        assertEquals(9, brazil?.points)
        assertEquals(3, brazil?.won)
        assertEquals(3, brazil?.played)
    }

    @Test
    fun `test team loses all matches gets 0 points`() {
        val matches = listOf(
            createMatch("BRA", "CMR", 3, 0),
            createMatch("SRB", "CMR", 2, 0),
            createMatch("SUI", "CMR", 1, 0)
        )

        val standings = useCase.calculate(matches)
        val cameroon = standings.find { it.name == "CMR" }

        assertEquals(0, cameroon?.points)
        assertEquals(3, cameroon?.lost)
        assertEquals(3, cameroon?.played)
    }

    @Test
    fun `test draw gives 1 point to each team`() {
        val matches = listOf(
            createMatch("BRA", "SUI", 1, 1)
        )

        val standings = useCase.calculate(matches)

        val brazil = standings.find { it.name == "BRA" }
        val switzerland = standings.find { it.name == "SUI" }

        assertEquals(1, brazil?.points)
        assertEquals(1, switzerland?.points)
        assertEquals(1, brazil?.drawn)
        assertEquals(1, switzerland?.drawn)
    }

    @Test
    fun `test goal difference tiebreaker`() {
        val matches = listOf(
            createMatch("BRA", "SUI", 3, 0),  // BRA: +3 GD
            createMatch("SRB", "CMR", 1, 0),  // SRB: +1 GD
            createMatch("BRA", "SRB", 0, 1),  // SRB: +1 GD (total +2)
            createMatch("SUI", "CMR", 2, 0)   // SUI: +2 GD
        )

        val standings = useCase.calculate(matches)

        // BRA deve estar em primeiro (mais pontos)
        assertEquals("BRA", standings[0].name)
    }

    @Test
    fun `test goals scored tiebreaker`() {
        val matches = listOf(
            createMatch("BRA", "SUI", 3, 2),  // BRA: 3 pts, +1 GD, 3 GF
            createMatch("SRB", "CMR", 2, 1)   // SRB: 3 pts, +1 GD, 2 GF
        )

        val standings = useCase.calculate(matches)

        // Mesmos pontos e GD, BRA vence no gols pró
        assertEquals("BRA", standings[0].name)
        assertEquals("SRB", standings[1].name)
    }

    @Test
    fun `test get qualified teams returns top 2`() {
        val matches = createMockMatchesGroupA()
        val standings = useCase.calculate(matches)
        val qualified = useCase.getQualifiedTeams(standings, 2)

        assertEquals(2, qualified.size)
        assertTrue(standings[0] in qualified)
        assertTrue(standings[1] in qualified)
    }

    @Test
    fun `test is team qualified`() {
        val matches = createMockMatchesGroupA()
        val standings = useCase.calculate(matches)

        // Primeiro time está classificado
        assertTrue(useCase.isTeamQualified(standings[0].name, standings, 2))
        assertTrue(useCase.isTeamQualified(standings[1].name, standings, 2))

        // Terceiro time não está classificado
        assertFalse(useCase.isTeamQualified(standings[2].name, standings, 2))
        assertFalse(useCase.isTeamQualified(standings[3].name, standings, 2))
    }

    @Test
    fun `test empty matches list returns empty standings`() {
        val standings = useCase.calculate(emptyList())
        assertTrue(standings.isEmpty())
    }

    @Test
    fun `test single match creates two teams in standings`() {
        val matches = listOf(createMatch("BRA", "SUI", 2, 1))
        val standings = useCase.calculate(matches)

        assertEquals(2, standings.size)
        assertTrue(standings.any { it.name == "BRA" })
        assertTrue(standings.any { it.name == "SUI" })
    }

    private fun createMockMatchesGroupA(): List<com.example.domain.model.Match> {
        return listOf(
            createMatch("BRA", "SUI", 2, 1),
            createMatch("SRB", "CMR", 1, 0),
            createMatch("BRA", "SRB", 3, 1),
            createMatch("CMR", "SUI", 1, 2),
            createMatch("BRA", "CMR", 2, 0),
            createMatch("SUI", "SRB", 1, 1)
        )
    }

    private fun createMatch(teamA: String, teamB: String, scoreA: Int, scoreB: Int): com.example.domain.model.Match {
        return com.example.domain.model.Match(
            id = 0,
            groupName = "A",
            teamA = teamA,
            teamB = teamB,
            flagA = "🇧🇷",
            flagB = "🇨🇭",
            actualScoreA = scoreA,
            actualScoreB = scoreB,
            date = "24 JUN 2026",
            time = "16:00"
        )
    }
}
