package com.example.core.model

enum class TradeDirection {
    CALL, // UP
    PUT   // DOWN
}

enum class TradeStatus {
    SUBMITTING,
    ACCEPTED,
    ACTIVE,
    EXPIRING,
    SETTLED,
    REJECTED,
    FAILED,
    UNDER_REVIEW
}

enum class TradeOutcome {
    PENDING,
    WIN,
    LOSS,
    TIE
}

data class Trade(
    val id: String,
    val idempotencyKey: String,
    val assetSymbol: String,
    val assetName: String,
    val direction: TradeDirection,
    val amount: Double,
    val entryPrice: Double,
    val durationSeconds: Long,
    val createdAt: Long,
    val serverExpiryTimestamp: Long,
    val status: TradeStatus = TradeStatus.ACTIVE,
    val settlementPrice: Double? = null,
    val payoutRate: Double = 0.85,
    val payoutAmount: Double = 0.0,
    val outcome: TradeOutcome = TradeOutcome.PENDING,
    val isDemo: Boolean = true
) {
    fun remainingSeconds(currentTimeMs: Long, serverTimeOffsetMs: Long = 0): Long {
        val adjustedNow = currentTimeMs + serverTimeOffsetMs
        val diffMs = serverExpiryTimestamp - adjustedNow
        return if (diffMs > 0) (diffMs / 1000) else 0L
    }
}
