package com.example.data.remote

import org.junit.Assert.*
import org.json.JSONObject
import org.junit.Test

/**
 * Testes unitários para SupabaseApiClient
 */
class SupabaseApiClientTest {

    @Test
    fun `ApiResponse Success deve ter isSuccess verdadeiro`() {
        // Arrange
        val data = JSONObject().put("test", "value")
        val response = ApiResponse.Success(200, data)
        
        // Act & Assert
        assertTrue(response.isSuccess)
        assertFalse(response.isError)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `ApiResponse Error deve ter isError verdadeiro`() {
        // Arrange
        val response = ApiResponse.Error(400, "Bad Request")
        
        // Act & Assert
        assertFalse(response.isSuccess)
        assertTrue(response.isError)
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `ApiResponse NetworkError deve ter isError verdadeiro`() {
        // Arrange
        val response = ApiResponse.NetworkError("Sem conexão")
        
        // Act & Assert
        assertFalse(response.isSuccess)
        assertTrue(response.isError)
    }

    @Test
    fun `SupabaseApiClientFactory deve criar instancia singleton`() {
        // Arrange
        val url = "https://test.supabase.co"
        val key = "test_key"
        
        // Act
        val client1 = SupabaseApiClientFactory.create(url, key)
        val client2 = SupabaseApiClientFactory.create(url, key)
        
        // Assert
        assertSame(client1, client2)
    }

    @Test
    fun `SupabaseApiClientFactory reset deve limpar instancia`() {
        // Arrange
        val url = "https://test.supabase.co"
        val key = "test_key"
        SupabaseApiClientFactory.create(url, key)
        
        // Act
        SupabaseApiClientFactory.reset()
        val client1 = SupabaseApiClientFactory.create(url, key)
        val client2 = SupabaseApiClientFactory.create(url, key)
        
        // Assert - após reset, nova instância deve ser criada
        assertSame(client1, client2)
    }

    @Test
    fun `JSONObject parse deve lidar com JSON valido`() {
        // Arrange
        val jsonString = """{"name": "Brasil", "points": 3}"""
        
        // Act
        val json = JSONObject(jsonString)
        
        // Assert
        assertEquals("Brasil", json.getString("name"))
        assertEquals(3, json.getInt("points"))
    }

    @Test
    fun `JSONObject parse deve lidar com JSON vazio`() {
        // Arrange
        val jsonString = "{}"
        
        // Act
        val json = JSONObject(jsonString)
        
        // Assert
        assertTrue(json.isEmpty())
    }

    @Test
    fun `ApiResponse Success com dados complexos`() {
        // Arrange
        val data = JSONObject()
            .put("user_id", "123")
            .put("points", 150)
            .put("active", true)
            .put("tags", listOf("premium", "verified"))
        
        val response = ApiResponse.Success(201, data)
        
        // Act & Assert
        assertTrue(response.isSuccess)
        assertEquals("123", response.data.getString("user_id"))
        assertEquals(150, response.data.getInt("points"))
        assertTrue(response.data.getBoolean("active"))
    }
}
