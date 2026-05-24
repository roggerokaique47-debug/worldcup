package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente API REST para comunicação com Supabase Edge Functions
 * e outros serviços externos (Stripe Webhooks, etc.)
 * 
 * Usado quando é necessário chamadas HTTP personalizadas
 * além do cliente Postgrest padrão do Supabase.
 */
class SupabaseApiClient(
    private val supabaseUrl: String,
    private val supabaseKey: String
) {
    companion object {
        private const val TAG = "SupabaseApiClient"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Faz uma requisição GET para uma Edge Function
     */
    suspend fun get(functionName: String, path: String = ""): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$supabaseUrl/functions/v1/$functionName/$path"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val code = response.code
                    
                    if (response.isSuccessful) {
                        ApiResponse.Success(code, parseJson(body))
                    } else {
                        ApiResponse.Error(code, "Erro na requisição: $body")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Erro de rede no GET", e)
                ApiResponse.NetworkError(e.message ?: "Sem conexão")
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado no GET", e)
                ApiResponse.Error(500, e.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Faz uma requisição POST para uma Edge Function
     */
    suspend fun post(functionName: String, body: Map<String, Any>, path: String = ""): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$supabaseUrl/functions/v1/$functionName/$path"
                val jsonBody = JSONObject(body).toString()
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    val code = response.code
                    
                    if (response.isSuccessful) {
                        ApiResponse.Success(code, parseJson(responseBody))
                    } else {
                        ApiResponse.Error(code, "Erro na requisição: $responseBody")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Erro de rede no POST", e)
                ApiResponse.NetworkError(e.message ?: "Sem conexão")
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado no POST", e)
                ApiResponse.Error(500, e.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Invoca Edge Function específica para processamento de pagamentos Stripe
     */
    suspend fun processStripePayment(paymentData: Map<String, Any>): ApiResponse {
        return post("process-payment", paymentData)
    }

    /**
     * Envia evento de analytics para processamento pela IA
     */
    suspend fun sendAnalyticsEvent(eventData: Map<String, Any>): ApiResponse {
        return post("analytics-processor", eventData)
    }

    /**
     * Busca sugestões da IA para um usuário
     */
    suspend fun getAiSuggestions(userId: String): ApiResponse {
        return get("ai-suggestions", "user/$userId")
    }

    /**
     * Verifica saúde da API
     */
    suspend fun healthCheck(): Boolean {
        return try {
            val response = get("health")
            response is ApiResponse.Success
        } catch (e: Exception) {
            false
        }
    }

    private fun parseJson(json: String): JSONObject {
        return try {
            JSONObject(json)
        } catch (e: Exception) {
            JSONObject().put("raw", json)
        }
    }
}

/**
 * Selado de resposta da API
 */
sealed class ApiResponse {
    data class Success(val statusCode: Int, val data: JSONObject) : ApiResponse()
    data class Error(val statusCode: Int, val message: String) : ApiResponse()
    data class NetworkError(val message: String) : ApiResponse()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error || this is NetworkError
}

/**
 * Factory para criar instâncias do cliente API
 */
object SupabaseApiClientFactory {
    
    private var INSTANCE: SupabaseApiClient? = null
    
    fun create(supabaseUrl: String, supabaseKey: String): SupabaseApiClient {
        return INSTANCE ?: synchronized(this) {
            val client = SupabaseApiClient(supabaseUrl, supabaseKey)
            INSTANCE = client
            client
        }
    }
    
    fun reset() {
        INSTANCE = null
    }
}
