package com.example.services

/**
 * Interface para serviços de pagamento
 * Implementações: Stripe, PIX, etc.
 */
interface PaymentService {
    suspend fun processPayment(amount: Double, description: String): PaymentResult
    suspend fun refundPayment(transactionId: String): PaymentResult
    suspend fun getPaymentStatus(transactionId: String): PaymentStatus
}

/**
 * Resultado de uma operação de pagamento
 */
data class PaymentResult(
    val success: Boolean,
    val transactionId: String?,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Status de um pagamento
 */
enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}

/**
 * Tipo de método de pagamento
 */
enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PIX,
    BANK_TRANSFER,
    WALLET
}
