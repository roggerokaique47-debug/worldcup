package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val username: String = "",
    val points: Int = 1250,
    val levelName: String = "Veteran Player",
    val xp: Int = 8450,
    val maxXp: Int = 10000,
    val accurateScoresCount: Int = 45,
    val winRate: Int = 68,
    val balance: Double = 1450.0
)

@Entity(tableName = "clubs")
data class ClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val isPrivate: Boolean,
    val accessPassword: String = "",
    val entryFee: Double,
    val prizeFirstPlace: Int = 70,
    val prizeSecondPlace: Int = 20,
    val prizeThirdPlace: Int = 10,
    val memberCount: Int = 1,
    val estimatedPrizePool: Double = 150000.0,
    val matchCount: Int = 12
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "deposit", "withdrawal", "prize", "fee"
    val description: String,
    val method: String, // "Stripe", "System", etc.
    val amount: Double,
    val date: String,
    val status: String // "CONCLUÍDO", "PAGO"
)
