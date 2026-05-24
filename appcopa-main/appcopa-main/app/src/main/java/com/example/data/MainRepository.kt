package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MainRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val clubDao = database.clubDao()
    private val matchDao = database.matchDao()
    private val transactionDao = database.transactionDao()

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allClubsFlow: Flow<List<ClubEntity>> = clubDao.getAllClubsFlow()
    val allMatchesFlow: Flow<List<MatchEntity>> = matchDao.getAllMatchesFlow()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()

    // Standings calculation for Group A
    data class TeamStats(
        val name: String,
        val flag: String,
        var points: Int = 0,
        var goalDiff: Int = 0,
        var goalsScored: Int = 0
    )

    val groupAStandingsFlow: Flow<List<TeamStats>> = allMatchesFlow.map { matches ->
        val statsMap = mutableMapOf(
            "BRA" to TeamStats("BRA", "🇧🇷"),
            "SUI" to TeamStats("SUI", "🇨🇭"),
            "SRB" to TeamStats("SRB", "🇷🇸"),
            "CMR" to TeamStats("CMR", "🇨🇲")
        )

        matches.filter { it.groupName == "A" }.forEach { match ->
            val scoreA = match.predictedScoreA ?: match.actualScoreA
            val scoreB = match.predictedScoreB ?: match.actualScoreB

            val statA = statsMap[match.teamA] ?: TeamStats(match.teamA, match.flagA)
            val statB = statsMap[match.teamB] ?: TeamStats(match.teamB, match.flagB)

            statA.goalsScored += scoreA
            statB.goalsScored += scoreB
            statA.goalDiff += (scoreA - scoreB)
            statB.goalDiff += (scoreB - scoreA)

            if (scoreA > scoreB) {
                statA.points += 3
            } else if (scoreA < scoreB) {
                statB.points += 3
            } else {
                statA.points += 1
                statB.points += 1
            }

            statsMap[match.teamA] = statA
            statsMap[match.teamB] = statB
        }

        statsMap.values.sortedWith(
            compareByDescending<TeamStats> { it.points }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsScored }
        )
    }

    suspend fun savePrediction(matchId: Int, scoreA: Int?, scoreB: Int?) {
        matchDao.updatePrediction(matchId, scoreA, scoreB)
    }

    suspend fun createClubAndPay(
        name: String,
        description: String,
        isPrivate: Boolean,
        passwordCustom: String,
        entryFee: Double,
        prizesFirst: Int,
        prizesSecond: Int,
        prizesThird: Int
    ): Boolean {
        val user = userDao.getCurrentUser() ?: return false
        
        // Simulating Stripe payment if entry fee > 0 (for owner or registration test)
        // Insert Club
        val clubId = clubDao.insertClub(
            ClubEntity(
                name = name,
                description = description,
                isPrivate = isPrivate,
                accessPassword = passwordCustom,
                entryFee = entryFee,
                prizeFirstPlace = prizesFirst,
                prizeSecondPlace = prizesSecond,
                prizeThirdPlace = prizesThird,
                memberCount = 1,
                estimatedPrizePool = entryFee * 100 // simulation of projected 100 members
            )
        )

        if (entryFee > 0.0) {
            val updatedUser = user.copy(balance = user.balance - entryFee)
            userDao.updateUser(updatedUser)

            // Add Transaction
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = "fee",
                    description = "Criação de Clube: $name (Fee)",
                    method = "Stripe",
                    amount = -entryFee,
                    date = "Hoje, agora",
                    status = "CONCLUÍDO"
                )
            )
        }
        return true
    }

    suspend fun joinClubAndPay(clubId: Int): Boolean {
        val user = userDao.getCurrentUser() ?: return false
        val club = clubDao.getClubById(clubId) ?: return false

        if (user.balance < club.entryFee) {
            return false // insufficient balance
        }

        // Simulating Stripe charge
        val updatedUser = user.copy(balance = user.balance - club.entryFee)
        userDao.updateUser(updatedUser)

        val updatedClub = club.copy(
            memberCount = club.memberCount + 1,
            estimatedPrizePool = club.estimatedPrizePool + club.entryFee
        )
        clubDao.updateClub(updatedClub)

        // Record the Stripe transaction
        transactionDao.insertTransaction(
            TransactionEntity(
                type = "fee",
                description = "Participação: ${club.name}",
                method = "Stripe",
                amount = -club.entryFee,
                date = "Hoje, agora",
                status = "CONCLUÍDO"
            )
        )
        return true
    }

    suspend fun addFundsSimulated(amount: Double): Boolean {
        val user = userDao.getCurrentUser() ?: return false
        val updatedUser = user.copy(balance = user.balance + amount)
        userDao.updateUser(updatedUser)

        // Insert Stripe Deposit transaction
        transactionDao.insertTransaction(
            TransactionEntity(
                type = "deposit",
                description = "Depósito - Stripe",
                method = "Stripe",
                amount = amount,
                date = "Hoje, agora",
                status = "CONCLUÍDO"
            )
        )
        return true
    }

    suspend fun registerUser(name: String, email: String, username: String): Boolean {
        val existing = userDao.getCurrentUser()
        if (existing == null) {
            userDao.insertUser(
                UserEntity(
                    name = name,
                    email = email,
                    username = username,
                    balance = 1000.0 // Start user with R$ 1000 balance
                )
            )
        } else {
            userDao.updateUser(
                existing.copy(
                    name = name,
                    email = email,
                    username = username
                )
            )
        }
        return true
    }
}
