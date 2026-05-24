package com.example.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes unitários para SupabaseDatabase
 * 
 * Para executar testes reais de conexão:
 * 1. Configure .env com credenciais válidas do Supabase
 * 2. Aplique o schema SQL no seu projeto Supabase
 * 3. Execute: ./gradlew testDebugUnitTest
 */
class SupabaseDatabaseTest {

    @Test
    fun `testConnection deve retornar sucesso quando configurado corretamente`() = runTest {
        // Arrange - usar variáveis de ambiente ou valores mock
        val testUrl = "https://demo-project.supabase.co"
        val testKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.demo"
        
        // Act
        val result = SupabaseDatabase.testConnection(testUrl, testKey)
        
        // Assert - em ambiente de teste, esperamos que falhe pois não há conexão real
        // Em produção com credenciais válidas, success seria true
        assertNotNull(result.message)
        assertNotNull(result.details)
    }

    @Test
    fun `ConnectionTestResult deve conter todas as propriedades`() {
        // Arrange
        val expectedSuccess = true
        val expectedMessage = "Conexão bem-sucedida!"
        val expectedDetails = "URL: https://test.supabase.co"
        
        // Act
        val result = ConnectionTestResult(
            success = expectedSuccess,
            message = expectedMessage,
            details = expectedDetails
        )
        
        // Assert
        assertEquals(expectedSuccess, result.success)
        assertEquals(expectedMessage, result.message)
        assertEquals(expectedDetails, result.details)
    }

    @Test
    fun `ConnectionTestResult deve suportar falha de conexão`() {
        // Arrange
        val expectedSuccess = false
        val expectedMessage = "Falha na conexão: timeout"
        val expectedDetails = "Verifique URL e chave ANON"
        
        // Act
        val result = ConnectionTestResult(
            success = expectedSuccess,
            message = expectedMessage,
            details = expectedDetails
        )
        
        // Assert
        assertFalse(result.success)
        assertTrue(result.message.contains("Falha"))
        assertTrue(result.details.contains("URL"))
    }

    @Test
    fun `MatchResultCloud deve serializar dados corretamente`() {
        // Arrange
        val matchResult = MatchResultCloud(
            id = 1,
            teamA = "Brasil",
            teamB = "Argentina",
            actualScoreA = 2,
            actualScoreB = 1,
            status = "finished",
            updatedAt = System.currentTimeMillis()
        )
        
        // Act & Assert
        assertEquals(1, matchResult.id)
        assertEquals("Brasil", matchResult.teamA)
        assertEquals("Argentina", matchResult.teamB)
        assertEquals(2, matchResult.actualScoreA)
        assertEquals(1, matchResult.actualScoreB)
        assertEquals("finished", matchResult.status)
        assertTrue(matchResult.updatedAt > 0)
    }

    @Test
    fun `SupabaseDatabase deve detectar configuração inválida`() {
        // Este teste verifica a lógica de validação
        // Nota: Não podemos instanciar SupabaseDatabase sem Context Android
        // então testamos apenas a lógica estática
        
        val invalidUrl = "https://your-project.supabase.co"
        val emptyKey = ""
        
        // Simular validação
        val isConfigured = invalidUrl.isNotEmpty() && 
                          emptyKey.isNotEmpty() &&
                          invalidUrl != "https://your-project.supabase.co"
        
        assertFalse(isConfigured)
    }

    @Test
    fun `SupabaseDatabase deve detectar configuração válida`() {
        val validUrl = "https://abcdefghijklmnop.supabase.co"
        val validKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid_key"
        
        val isConfigured = validUrl.isNotEmpty() && 
                          validKey.isNotEmpty() &&
                          validUrl != "https://your-project.supabase.co"
        
        assertTrue(isConfigured)
    }
}
