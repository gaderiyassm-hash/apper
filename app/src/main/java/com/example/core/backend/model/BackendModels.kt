package com.example.core.backend.model

import java.util.UUID

// Section 43: API Response Standard
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val requestId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T, requestId: String = UUID.randomUUID().toString()): ApiResponse<T> =
            ApiResponse(success = true, data = data, error = null, requestId = requestId)

        fun <T> error(code: String, message: String, requestId: String = UUID.randomUUID().toString()): ApiResponse<T> =
            ApiResponse(success = false, data = null, error = ApiError(code, message, requestId), requestId = requestId)
    }
}

data class ApiError(
    val code: String,
    val message: String,
    val requestId: String
)

// Section 10: Role-Based Access Control (RBAC)
enum class UserRole {
    USER,
    ADMIN,
    COMPLIANCE,
    FINANCE,
    RISK,
    SUPPORT,
    SUPER_ADMIN
}

// Section 16: Market Status
enum class BackendMarketStatus {
    OPEN,
    CLOSED,
    SUSPENDED,
    DATA_ERROR,
    MAINTENANCE
}

// Section 21: Trade State Machine
enum class BackendTradeState {
    CREATED,
    VALIDATING,
    ACCEPTED,
    ACTIVE,
    EXPIRING,
    SETTLED,
    REJECTED,
    FAILED,
    CANCELLED,
    UNDER_REVIEW
}

// Section 22: Trade Event
data class BackendTradeEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val tradeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val previousState: BackendTradeState?,
    val newState: BackendTradeState,
    val metadata: Map<String, String> = emptyMap()
)

// Section 33-35: Risk Engine & Actions
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class RiskAction {
    ALLOW,
    STEP_UP,
    LIMIT,
    HOLD,
    REVIEW,
    BLOCK
}

data class RiskEvaluationResult(
    val score: Int, // 0 - 100
    val level: RiskLevel,
    val action: RiskAction,
    val reasons: List<String>
)

// Section 26-28: Ledger & Balance Minor Units (Cents / Micros for exact arithmetic)
// Minor units = cents (1 USD = 100 cents, stored as Long to eliminate float inaccuracies)
@JvmInline
value class MoneyMinor(val cents: Long) {
    fun toDouble(): Double = cents / 100.0

    operator fun plus(other: MoneyMinor): MoneyMinor = MoneyMinor(this.cents + other.cents)
    operator fun minus(other: MoneyMinor): MoneyMinor = MoneyMinor(this.cents - other.cents)
    operator fun compareTo(other: MoneyMinor): Int = this.cents.compareTo(other.cents)

    companion object {
        val ZERO = MoneyMinor(0L)
        fun fromDouble(amount: Double): MoneyMinor = MoneyMinor((amount * 100.0 + 0.5).toLong())
        fun fromCents(cents: Long): MoneyMinor = MoneyMinor(cents)
    }
}

enum class LedgerEntryDirection {
    DEBIT,
    CREDIT
}

enum class LedgerEventType {
    INITIAL_CREDIT,
    DEMO_REFILL,
    DEPOSIT_CONFIRMED,
    WITHDRAWAL_REQUESTED,
    WITHDRAWAL_SETTLED,
    WITHDRAWAL_REJECTED,
    TRADE_INVESTMENT_LOCK,
    TRADE_SETTLEMENT_WIN,
    TRADE_SETTLEMENT_REFUND,
    FEE_COLLECTION
}

data class BackendLedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val accountId: String,
    val currency: String = "USD",
    val amountCents: Long,
    val direction: LedgerEntryDirection,
    val eventType: LedgerEventType,
    val reference: String,
    val idempotencyKey: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Section 38: Core Authoritative Entities
data class BackendUser(
    val id: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val role: UserRole = UserRole.USER,
    val isDemoActive: Boolean = true,
    val isBlocked: Boolean = false,
    val isMfaEnabled: Boolean = false,
    val kycStatus: String = "VERIFIED",
    val createdAt: Long = System.currentTimeMillis()
)

data class BackendAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 900 // 15 mins
)

data class BackendTradeOrder(
    val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String,
    val userId: String,
    val assetSymbol: String,
    val direction: String, // UP / DOWN (CALL / PUT)
    val amountCents: Long,
    val durationSeconds: Long,
    val entryPrice: Double,
    val payoutRate: Double,
    val potentialPayoutCents: Long,
    val state: BackendTradeState,
    val serverAcceptedTimestamp: Long,
    val serverExpiryTimestamp: Long,
    val settlementPrice: Double? = null,
    val outcome: String? = null, // WIN, LOSS, TIE
    val isDemo: Boolean
)
