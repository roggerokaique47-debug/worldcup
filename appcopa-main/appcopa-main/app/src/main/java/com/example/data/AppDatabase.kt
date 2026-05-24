package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ClubEntity::class,
        MatchEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun clubDao(): ClubDao
    abstract fun matchDao(): MatchDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bolao_database"
                )
                    .addCallback(DatabaseSeederCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeederCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed initial data using background IO coroutine thread
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                
                // Seed Current User
                database.userDao().insertUser(
                    UserEntity(
                        name = "Rogerio Kaique",
                        email = "roggerokaique48@gmail.com",
                        username = "roggero_wc26",
                        points = 1450,
                        levelName = "Veteran Player",
                        xp = 8450,
                        maxXp = 10000,
                        accurateScoresCount = 45,
                        winRate = 68,
                        balance = 1450.0
                    )
                )

                // Seed Default Matches (Group A matches where the user inputs predictions)
                val groupAMatches = listOf(
                    MatchEntity(
                        groupName = "A",
                        teamA = "BRA",
                        teamB = "SUI",
                        flagA = "🇧🇷",
                        flagB = "🇨🇭",
                        predictedScoreA = 2,
                        predictedScoreB = 1,
                        actualScoreA = 2,
                        actualScoreB = 1,
                        date = "24 JUN 2026",
                        time = "16:00",
                        stadium = "Maracanã"
                    ),
                    MatchEntity(
                        groupName = "A",
                        teamA = "SRB",
                        teamB = "CMR",
                        flagA = "🇷🇸",
                        flagB = "🇨🇲",
                        predictedScoreA = 0,
                        predictedScoreB = 0,
                        actualScoreA = 0,
                        actualScoreB = 0,
                        date = "24 JUN 2026",
                        time = "16:00",
                        stadium = "Maracanã"
                    ),
                    MatchEntity(
                        groupName = "A",
                        teamA = "BRA",
                        teamB = "SRB",
                        flagA = "🇧🇷",
                        flagB = "🇷🇸",
                        predictedScoreA = null,
                        predictedScoreB = null,
                        actualScoreA = 3,
                        actualScoreB = 1,
                        date = "28 JUN 2026",
                        time = "13:00",
                        stadium = "Arena Corinthians"
                    ),
                    MatchEntity(
                        groupName = "A",
                        teamA = "CMR",
                        teamB = "SUI",
                        flagA = "🇨🇲",
                        flagB = "🇨🇭",
                        predictedScoreA = null,
                        predictedScoreB = null,
                        actualScoreA = 1,
                        actualScoreB = 2,
                        date = "28 JUN 2026",
                        time = "21:00",
                        stadium = "Mineirão"
                    ),
                    MatchEntity(
                        groupName = "A",
                        teamA = "BRA",
                        teamB = "CMR",
                        flagA = "🇧🇷",
                        flagB = "🇨🇲",
                        predictedScoreA = null,
                        predictedScoreB = null,
                        actualScoreA = 2,
                        actualScoreB = 0,
                        date = "02 JUL 2026",
                        time = "17:00",
                        stadium = "Beira-Rio"
                    ),
                    MatchEntity(
                        groupName = "A",
                        teamA = "SUI",
                        teamB = "SRB",
                        flagA = "🇨🇭",
                        flagB = "🇷🇸",
                        predictedScoreA = null,
                        predictedScoreB = null,
                        actualScoreA = 1,
                        actualScoreB = 1,
                        date = "02 JUL 2026",
                        time = "17:00",
                        stadium = "Mané Garrincha"
                    )
                )
                database.matchDao().insertMatches(groupAMatches)

                // Seed open clubs advertisements
                val initialClubs = listOf(
                    ClubEntity(
                        name = "Liga dos Campeões BR",
                        description = "O grupo oficial para debater futebol com a galera da firma e concorrer ao grande prêmio. Quem ficar em último lugar paga o churrasco da final!",
                        isPrivate = false,
                        entryFee = 100.0,
                        estimatedPrizePool = 150000.0,
                        memberCount = 128
                    ),
                    ClubEntity(
                        name = "Copa Ouro 26",
                        description = "Liga privada de alta performance focada em apostas precisas com distribuição proporcional aos top 3. Peça participação com a senha.",
                        isPrivate = true,
                        accessPassword = "GOLD",
                        entryFee = 50.0,
                        estimatedPrizePool = 50000.0,
                        memberCount = 64
                    ),
                    ClubEntity(
                        name = "Iniciantes da Copa",
                        description = "Quer testar suas habilidades sem riscos? Entre nessa liga pública gratuita e comece a subir no ranking internacional de boloões.",
                        isPrivate = false,
                        entryFee = 0.0,
                        estimatedPrizePool = 5000.0,
                        memberCount = 256
                    )
                )
                for (club in initialClubs) {
                    database.clubDao().insertClub(club)
                }

                // Seed Transactions
                val seedTransactions = listOf(
                    TransactionEntity(
                        type = "deposit",
                        description = "Depósito - Stripe",
                        method = "Stripe",
                        amount = 500.0,
                        date = "Hoje, 14:30",
                        status = "CONCLUÍDO"
                    ),
                    TransactionEntity(
                        type = "prize",
                        description = "Prêmio: BRA x ARG",
                        method = "System",
                        amount = 320.50,
                        date = "Hoje, 10:15",
                        status = "PAGO"
                    ),
                    TransactionEntity(
                        type = "fee",
                        description = "Aposta: FRA x ENG",
                        method = "System",
                        amount = -150.0,
                        date = "Ontem, 21:00",
                        status = "CONCLUÍDO"
                    ),
                    TransactionEntity(
                        type = "deposit",
                        description = "Depósito - Stripe",
                        method = "Stripe",
                        amount = 1000.0,
                        date = "Ontem, 18:45",
                        status = "CONCLUÍDO"
                    )
                )
                for (trans in seedTransactions) {
                    database.transactionDao().insertTransaction(trans)
                }
            }
        }
    }
}
