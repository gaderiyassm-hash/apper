package com.example.core.model

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRADE_PROFIT,
    TRADE_INVESTMENT,
    DEMO_REFILL
}

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED
}

enum class PaymentMethod(val displayName: String, val iconRes: String, val processingTime: String) {
    INSTANT_BANK_TRANSFER("Bank Transfer", "bank", "10-30 mins"),
    CRYPTO_USDT("Tether (USDT TRC20)", "crypto", "Instant"),
    CREDIT_DEBIT_CARD("Visa / MasterCard", "card", "Instant"),
    UPI_FAST_PAY("Fast UPI / Pay", "upi", "Instant")
}

data class WalletBalance(
    val available: Double,
    val locked: Double,
    val total: Double = available + locked,
    val currency: String = "USD",
    val isDemo: Boolean = false
)

data class TransactionItem(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String = "USD",
    val status: TransactionStatus,
    val reference: String,
    val timestamp: Long,
    val paymentMethod: PaymentMethod? = null,
    val note: String = ""
)
