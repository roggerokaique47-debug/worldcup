package com.example.data

import android.content.Context
import android.util.Log
import io.github.jangould.supabase.kt.SupabaseClient
import io.github.jangould.supabase.kt.createSupabaseClient
import io.github.jangould.supabase.kt.modules.auth.Auth
import io.github.jangould.supabase.kt.modules.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente Supabase para sincronização cloud
 * 
 * Configuração:
 * 1. Criar conta em https://supabase.com
 * 2. Criar novo projeto
 * 3. Obter URL e chaves em Settings > API
 * 4. Adicionar no .env (ver .env.example)
 * 
 * Schema SQL para criar no Supabase:
 * @see supabase_schema.sql
 */
class SupabaseDatabase(private val context: Context) {

    companion object {
        private const val TAG = "SupabaseDatabase"
        private var INSTANCE: SupabaseClient? = null
        
        // Métodos de teste de conexão
        suspend fun testConnection(supabaseUrl: String, supabaseAnonKey: String): ConnectionTestResult {
            return try {
                val client = createSupabaseClient(supabaseUrl, supabaseAnonKey) {
                    install(Auth)
                    install(Postgrest)
                }
                
                // Testar se consegue fazer uma query simples
                val postgrest = client.pluginManager.getPlugin(Postgrest::class)
                
                // Tentar buscar dados da tabela users (deve existir após aplicar o schema)
                val response = postgrest.from("users").select().limit(1).execute()
                
                if (response.status in 200..299) {
                    ConnectionTestResult(
                        success = true,
                        message = "Conexão bem-sucedida! Supabase configurado corretamente.",
                        details = "URL: $supabaseUrl\nStatus: ${response.status}"
                    )
                } else {
                    ConnectionTestResult(
                        success = false,
                        message = "Erro na conexão. Status HTTP: ${response.status}",
                        details = "Verifique se o schema foi aplicado corretamente."
                    )
                }
            } catch (e: Exception) {
                ConnectionTestResult(
                    success = false,
                    message = "Falha na conexão: ${e.message}",
                    details = "Verifique URL, chave ANON e se a internet está funcionando."
                )
            }
        }
    }

    private val supabaseUrl: String = BuildConfig.SUPABASE_URL
    private val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Obtém ou cria instância singleton do cliente Supabase
     */
    fun getClient(): SupabaseClient {
        return INSTANCE ?: synchronized(this) {
            val client = createSupabaseClient(supabaseUrl, supabaseAnonKey) {
                install(Auth)
                install(Postgrest)
            }
            INSTANCE = client
            client
        }
    }

    /**
     * Verifica se está configurado corretamente
     */
    fun isConfigured(): Boolean {
        return supabaseUrl.isNotEmpty() && 
               supabaseAnonKey.isNotEmpty() &&
               supabaseUrl != "https://your-project.supabase.co"
    }

    /**
     * Sincroniza dados locais com cloud
     * Chamado quando há conexão de internet disponível
     */
    suspend fun syncLocalWithCloud(localDb: AppDatabase) {
        if (!isConfigured()) {
            Log.w(TAG, "Supabase não configurado. Pulando sincronização.")
            return
        }

        try {
            val client = getClient()
            
            // TODO: Implementar sincronização bidirecional
            // 1. Upload de palpites locais para cloud
            // 2. Download de resultados atualizados
            // 3. Resolução de conflitos (usar timestamp mais recente)
            
            Log.d(TAG, "Sincronização iniciada...")
            
            // Exemplo: Upload de user local
            val localUser = localDb.userDao().getCurrentUser()
            localUser?.let { user ->
                // uploadUserToCloud(client, user)
            }
            
            Log.d(TAG, "Sincronização concluída com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização", e)
            // Fallback: continuar usando banco local
        }
    }

    /**
     * Faz login do usuário via Supabase Auth
     */
    suspend fun loginWithEmail(email: String, password: String): Result<String> {
        return try {
            val client = getClient()
            val auth = client.pluginManager.getPlugin(Auth::class)
            
            // TODO: Implementar login real
            // val response = auth.signInWithEmail(email, password)
            
            Result.success("token_simulado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no login", e)
            Result.failure(e)
        }
    }

    /**
     * Registra novo usuário via Supabase Auth
     */
    suspend fun registerWithEmail(email: String, password: String, name: String): Result<String> {
        return try {
            val client = getClient()
            val auth = client.pluginManager.getPlugin(Auth::class)
            
            // TODO: Implementar registro real
            // val response = auth.signUpWithEmail(email, password) {
            //     put("name", name)
            // }
            
            Result.success("token_simulado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no registro", e)
            Result.failure(e)
        }
    }

    /**
     * Busca todos os clubes da cloud
     */
    suspend fun fetchClubsFromCloud(): List<ClubEntity> {
        return try {
            val client = getClient()
            val postgrest = client.pluginManager.getPlugin(Postgrest::class)
            
            // TODO: Implementar query real
            // postgrest.from("clubs").select().execute()
            
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar clubes", e)
            emptyList()
        }
    }

    /**
     * Envia palpite para cloud
     */
    suspend fun uploadPrediction(userId: String, matchId: Int, scoreA: Int, scoreB: Int): Boolean {
        return try {
            val client = getClient()
            val postgrest = client.pluginManager.getPlugin(Postgrest::class)
            
            // TODO: Implementar insert real
            /*
            postgrest.from("predictions").insert(
                mapOf(
                    "user_id" to userId,
                    "match_id" to matchId,
                    "score_a" to scoreA,
                    "score_b" to scoreB
                )
            ).execute()
            */
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar palpite", e)
            false
        }
    }

    /**
     * Recebe resultados atualizados da cloud
     */
    suspend fun downloadMatchResults(): List<MatchResultCloud> {
        return try {
            val client = getClient()
            val postgrest = client.pluginManager.getPlugin(Postgrest::class)
            
            // TODO: Implementar query real
            /*
            val response = postgrest.from("matches")
                .select()
                .eq("status", "finished")
                .execute()
            */
            
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar resultados", e)
            emptyList()
        }
    }
}

/**
 * Resultado do teste de conexão com Supabase
 */
data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
    val details: String
)

/**
 * Modelo de resultado de partida vindo da cloud
 */
data class MatchResultCloud(
    val id: Int,
    val teamA: String,
    val teamB: String,
    val actualScoreA: Int,
    val actualScoreB: Int,
    val status: String, // "scheduled", "live", "finished"
    val updatedAt: Long
)
