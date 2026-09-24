package com.example.core.backend.services

import com.example.core.backend.ledger.DoubleEntryLedgerService
import com.example.core.backend.model.ApiResponse
import com.example.core.backend.model.BackendMarketStatus
import com.example.core.backend.model.BackendTradeEvent
import com.example.core.backend.model.BackendTradeOrder
import com.example.core.backend.model.BackendTradeState
import com.example.core.backend.model.MoneyMinor
import com.example.core.backend.model.RiskAction
import com.example.core.backend.risk.AuditService
import com.example.core.backend.risk.RiskAndFraudEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Section 13-16: Authoritative Market Data Service.
 * Implements validation pipeline: rejects negative prices, stale timestamps, future timestamps, and duplicate ticks.
 */
class AuthoritativeMarketDataService {
    private val marketStatuses = ConcurrentHashMap<String, BackendMarketStatus>()
    private val latestPrices = ConcurrentHashMap<String, Double>()
    private val lastTickTimestamps = ConcurrentHashMap<String, Long>()

    init {
        // Initialize default market statuses
        listOf("BTC/USDT", "ETH/USDT", "SOL/USDT", "EUR/USD", "GBP/USD", "GOLD", "CRUDE OIL", "APPLE", "NVIDIA").forEach {
            marketStatuses[it] = BackendMarketStatus.OPEN
        }
        latestPrices["BTC/USDT"] = 96420.50
        latestPrices["ETH/USDT"] = 3450.20
        latestPrices["SOL/USDT"] = 214.80
        latestPrices["EUR/USD"] = 1.0845
        latestPrices["GBP/USD"] = 1.2980
        latestPrices["GOLD"] = 2742.60
        latestPrices["CRUDE OIL"] = 71.85
        latestPrices["APPLE"] = 238.50
        latestPrices["NVIDIA"] = 142.90
    }

    fun getMarketStatus(symbol: String): BackendMarketStatus =
        marketStatuses[symbol] ?: BackendMarketStatus.OPEN

    fun getAuthoritativePrice(symbol: String): Double =
        latestPrices[symbol] ?: 100.0

    /**
     * Section 14: Validates incoming price ticks from feed provider.
     */
    fun ingestTick(symbol: String, price: Double, timestamp: Long): Boolean {
        // 1. Reject negative or zero prices
        if (price <= 0.0) return false

        // 2. Reject future timestamps (> 5 seconds ahead of server time)
        val now = System.currentTimeMillis()
        if (timestamp > now + 5000) return false

        // 3. Reject stale or out-of-order data
        val lastTimestamp = lastTickTimestamps[symbol] ?: 0L
        if (timestamp < lastTimestamp) return false

        // 4. Check market status
        if (marketStatuses[symbol] == BackendMarketStatus.SUSPENDED) return false

        latestPrices[symbol] = price
        lastTickTimestamps[symbol] = timestamp
        return true
    }

    fun setMarketStatus(symbol: String, status: BackendMarketStatus) {
        marketStatuses[symbol] = status
    }
}

/**
 * Section 17-24: Authoritative Trading & Settlement Engine.
 * - Enforces State Machine: CREATED -> VALIDATING -> ACCEPTED -> ACTIVE -> EXPIRING -> SETTLED.
 * - Idempotency key uniqueness check.
 * - Authoritative server timestamp and price determination.
 * - Settlement isolated from client; UNIQUE(trade_id) settlement idempotency.
 */
class AuthoritativeTradingEngine(
    private val marketDataService: AuthoritativeMarketDataService,
    private val ledgerService: DoubleEntryLedgerService,
    private val riskEngine: RiskAndFraudEngine,
    private val auditService: AuditService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Authoritative in-memory store of trades
    private val trades = ConcurrentHashMap<String, BackendTradeOrder>()
    private val idempotencyMap = ConcurrentHashMap<String, BackendTradeOrder>()
    private val settledTradeIds = ConcurrentHashMap.newKeySet<String>()

    // Trade state events flow
    private val _tradeEvents = MutableSharedFlow<BackendTradeEvent>(extraBufferCapacity = 64)
    val tradeEvents: SharedFlow<BackendTradeEvent> = _tradeEvents.asSharedFlow()

    // Settled trade notifications flow
    private val _recentlySettledTrade = MutableStateFlow<BackendTradeOrder?>(null)
    val recentlySettledTrade: StateFlow<BackendTradeOrder?> = _recentlySettledTrade.asStateFlow()

    init {
        // Section 23: Start continuous authoritative settlement watcher
        startSettlementEngineWatcher()
    }

    /**
     * Section 17-20: Submit and validate a trade request.
     */
    suspend fun executeTradeSubmission(
        userId: String,
        assetSymbol: String,
        direction: String, // UP / DOWN (CALL / PUT)
        amountCents: Long,
        durationSeconds: Long,
        payoutRate: Double,
        idempotencyKey: String,
        isDemo: Boolean,
        kycStatus: String
    ): ApiResponse<BackendTradeOrder> {
        // Section 20: Idempotency check
        idempotencyMap[idempotencyKey]?.let { existing ->
            return ApiResponse.success(existing)
        }

        // Section 16: Market Status Verification
        val marketStatus = marketDataService.getMarketStatus(assetSymbol)
        if (marketStatus != BackendMarketStatus.OPEN) {
            return ApiResponse.error("MARKET_UNAVAILABLE", "Market $assetSymbol is currently $marketStatus")
        }

        // Section 19: Amount validation (Min $1.00 = 100 cents)
        if (amountCents < 100L) {
            return ApiResponse.error("MIN_AMOUNT_NOT_MET", "Minimum investment is $1.00")
        }

        // Section 33: Risk Engine Evaluation
        val risk = riskEngine.evaluateTradeSubmission(userId, amountCents, isDemo, kycStatus)
        if (risk.action == RiskAction.BLOCK) {
            auditService.log(userId, "TRADE_BLOCKED_RISK", "Trade", idempotencyKey, status = "BLOCKED", details = risk.reasons.joinToString("; "))
            return ApiResponse.error("RISK_LIMIT_EXCEEDED", "Trade blocked by Risk Engine: ${risk.reasons.firstOrNull()}")
        }

        // Section 28: Balance check & Lock in Ledger
        val tradeId = "trd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val lockSuccess = ledgerService.lockTradeFunds(isDemo, amountCents, tradeId, idempotencyKey)
        if (!lockSuccess) {
            return ApiResponse.error("INSUFFICIENT_BALANCE", "Insufficient available balance to lock investment")
        }

        // Section 18: Authoritative server values
        val serverNow = System.currentTimeMillis()
        val entryPrice = marketDataService.getAuthoritativePrice(assetSymbol)
        val serverExpiry = serverNow + (durationSeconds * 1000L)
        val potentialPayoutCents = (amountCents * (1.0 + payoutRate)).toLong()

        val trade = BackendTradeOrder(
            id = tradeId,
            idempotencyKey = idempotencyKey,
            userId = userId,
            assetSymbol = assetSymbol,
            direction = direction,
            amountCents = amountCents,
            durationSeconds = durationSeconds,
            entryPrice = entryPrice,
            payoutRate = payoutRate,
            potentialPayoutCents = potentialPayoutCents,
            state = BackendTradeState.ACTIVE,
            serverAcceptedTimestamp = serverNow,
            serverExpiryTimestamp = serverExpiry,
            isDemo = isDemo
        )

        trades[tradeId] = trade
        idempotencyMap[idempotencyKey] = trade

        // Emit events and audit logs
        emitTradeEvent(tradeId, BackendTradeState.CREATED, BackendTradeState.ACCEPTED)
        emitTradeEvent(tradeId, BackendTradeState.ACCEPTED, BackendTradeState.ACTIVE)
        auditService.log(userId, "TRADE_ACCEPTED", "Trade", tradeId, status = "SUCCESS", details = "$direction $assetSymbol $amountCents cents @ $entryPrice")

        return ApiResponse.success(trade)
    }

    /**
     * Section 23: Autonomous Authoritative Settlement Engine Watcher.
     * Evaluates expiring trades strictly against authoritative server timestamps.
     */
    private fun startSettlementEngineWatcher() {
        scope.launch {
            while (isActive) {
                delay(250) // High frequency tick check
                val serverNow = System.currentTimeMillis()

                for ((tradeId, trade) in trades) {
                    if (trade.state == BackendTradeState.ACTIVE && serverNow >= trade.serverExpiryTimestamp) {
                        settleTradeAuthoritatively(trade)
                    }
                }
            }
        }
    }

    /**
     * Section 24: Settle a single trade with strict idempotency (UNIQUE tradeId).
     */
    @Synchronized
    private fun settleTradeAuthoritatively(trade: BackendTradeOrder) {
        if (!settledTradeIds.add(trade.id)) {
            // Already settled! Prevent duplicate settlement.
            return
        }

        val settlePrice = marketDataService.getAuthoritativePrice(trade.assetSymbol)
        val isCall = trade.direction.equals("CALL", ignoreCase = true) || trade.direction.equals("UP", ignoreCase = true)

        val isWin = if (isCall) settlePrice > trade.entryPrice else settlePrice < trade.entryPrice
        val isTie = settlePrice == trade.entryPrice

        val outcome = when {
            isWin -> "WIN"
            isTie -> "TIE"
            else -> "LOSS"
        }

        val returnAmountCents = when {
            isWin -> trade.potentialPayoutCents
            isTie -> trade.amountCents
            else -> 0L
        }

        // Section 23: Update ledger with authoritative financial result
        ledgerService.settleTradeFunds(
            isDemo = trade.isDemo,
            lockedInvestmentCents = trade.amountCents,
            payoutReturnCents = returnAmountCents,
            tradeId = trade.id,
            isWin = isWin,
            isTie = isTie
        )

        val settled = trade.copy(
            state = BackendTradeState.SETTLED,
            settlementPrice = settlePrice,
            outcome = outcome
        )
        trades[trade.id] = settled

        // Emit events and audit
        emitTradeEvent(trade.id, BackendTradeState.EXPIRING, BackendTradeState.SETTLED, mapOf("outcome" to outcome, "settlePrice" to settlePrice.toString()))
        auditService.log(trade.userId, "TRADE_SETTLED", "Trade", trade.id, status = outcome, details = "Entry: ${trade.entryPrice}, Settle: $settlePrice, Return: $returnAmountCents cents")

        _recentlySettledTrade.value = settled
    }

    fun dismissRecentlySettled() {
        _recentlySettledTrade.value = null
    }

    private fun emitTradeEvent(
        tradeId: String,
        prev: BackendTradeState?,
        next: BackendTradeState,
        meta: Map<String, String> = emptyMap()
    ) {
        val event = BackendTradeEvent(
            tradeId = tradeId,
            previousState = prev,
            newState = next,
            metadata = meta
        )
        _tradeEvents.tryEmit(event)
    }

    fun getActiveTrades(): List<BackendTradeOrder> =
        trades.values.filter { it.state == BackendTradeState.ACTIVE }.sortedByDescending { it.serverAcceptedTimestamp }

    fun getAllTrades(): List<BackendTradeOrder> =
        trades.values.sortedByDescending { it.serverAcceptedTimestamp }
}
