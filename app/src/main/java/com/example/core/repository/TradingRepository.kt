package com.example.core.repository

import com.example.core.backend.AuthoritativeBackendServer
import com.example.core.backend.model.BackendTradeOrder
import com.example.core.backend.model.BackendTradeState
import com.example.core.model.AuthState
import com.example.core.model.KycStatus
import com.example.core.model.MarketAsset
import com.example.core.model.NotificationCategory
import com.example.core.model.NotificationItem
import com.example.core.model.PaymentMethod
import com.example.core.model.Trade
import com.example.core.model.TradeDirection
import com.example.core.model.TradeOutcome
import com.example.core.model.TradeStatus
import com.example.core.model.TransactionItem
import com.example.core.model.TransactionStatus
import com.example.core.model.TransactionType
import com.example.core.model.UserProfile
import com.example.core.model.WalletBalance
import com.example.core.network.MarketPriceEngine
import com.example.core.network.WsConnectionState
import com.example.core.storage.AppDatabase
import com.example.core.storage.NotificationEntity
import com.example.core.storage.TradeEntity
import com.example.core.storage.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed class TradeSubmissionResult {
    data class Success(val trade: Trade) : TradeSubmissionResult()
    data class Error(val message: String, val category: String = "VALIDATION_ERROR") : TradeSubmissionResult()
}

/**
 * Section 5 & 73: Repository Layer bridging Presentation and Authoritative Backend.
 * Strictly adheres to Master Rule 74: Frontend never settles trades or increases wallet directly.
 * All financial transactions, authorizations, risk checks, and settlements are delegated to
 * the authoritative backend modular monolith engine.
 */
class TradingRepository(
    private val database: AppDatabase,
    val priceEngine: MarketPriceEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Authoritative Backend Server Monolith Core
    val backendServer = AuthoritativeBackendServer(scope)

    // Auth & User State
    private val _authState = MutableStateFlow(AuthState.AUTHENTICATED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Balances: Derived authoritatively from Double-Entry Ledger (Exact minor unit cents)
    private val _demoBalance = MutableStateFlow(WalletBalance(available = 10000.0, locked = 0.0, isDemo = true))
    val demoBalance: StateFlow<WalletBalance> = _demoBalance.asStateFlow()

    private val _realBalance = MutableStateFlow(WalletBalance(available = 1250.0, locked = 0.0, isDemo = false))
    val realBalance: StateFlow<WalletBalance> = _realBalance.asStateFlow()

    // Mode Toggle: Demo vs Real
    private val _isDemoMode = MutableStateFlow(true)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // Recently settled trade event for modal popup
    private val _lastSettledTrade = MutableStateFlow<Trade?>(null)
    val lastSettledTrade: StateFlow<Trade?> = _lastSettledTrade.asStateFlow()

    init {
        seedInitialDataIfEmpty()
        bindAuthoritativeLedgerBalances()
        bindAuthoritativeSettlementWatcher()
    }

    /**
     * Binds UI wallet balances to the Authoritative Double-Entry Ledger balances.
     */
    private fun bindAuthoritativeLedgerBalances() {
        scope.launch {
            backendServer.getDemoBalance().collect { ledgerBalance ->
                _demoBalance.value = WalletBalance(
                    available = ledgerBalance.availableDollars(),
                    locked = ledgerBalance.lockedDollars(),
                    isDemo = true
                )
            }
        }
        scope.launch {
            backendServer.getRealBalance().collect { ledgerBalance ->
                _realBalance.value = WalletBalance(
                    available = ledgerBalance.availableDollars(),
                    locked = ledgerBalance.lockedDollars(),
                    isDemo = false
                )
            }
        }
    }

    /**
     * Listens to the backend authoritative settlement engine events and persists outcomes.
     */
    private fun bindAuthoritativeSettlementWatcher() {
        scope.launch {
            backendServer.tradingEngine.recentlySettledTrade.collect { backendTrade ->
                if (backendTrade != null) {
                    val outcome = when (backendTrade.outcome) {
                        "WIN" -> TradeOutcome.WIN
                        "TIE" -> TradeOutcome.TIE
                        else -> TradeOutcome.LOSS
                    }

                    val tradeDomain = Trade(
                        id = backendTrade.id,
                        idempotencyKey = backendTrade.idempotencyKey,
                        assetSymbol = backendTrade.assetSymbol,
                        assetName = priceEngine.markets.value.find { it.symbol == backendTrade.assetSymbol }?.name ?: backendTrade.assetSymbol,
                        direction = TradeDirection.valueOf(backendTrade.direction),
                        amount = backendTrade.amountCents / 100.0,
                        entryPrice = backendTrade.entryPrice,
                        durationSeconds = backendTrade.durationSeconds,
                        createdAt = backendTrade.serverAcceptedTimestamp,
                        serverExpiryTimestamp = backendTrade.serverExpiryTimestamp,
                        status = TradeStatus.SETTLED,
                        settlementPrice = backendTrade.settlementPrice,
                        payoutRate = backendTrade.payoutRate,
                        payoutAmount = backendTrade.potentialPayoutCents / 100.0,
                        outcome = outcome,
                        isDemo = backendTrade.isDemo
                    )

                    // Update local Room database cache
                    database.tradeDao().updateTrade(TradeEntity.fromDomain(tradeDomain))

                    // Record settlement transaction
                    database.transactionDao().insertTransaction(
                        TransactionEntity(
                            id = "tx_settle_${backendTrade.id}",
                            type = if (outcome == TradeOutcome.WIN) TransactionType.TRADE_PROFIT.name else TransactionType.TRADE_INVESTMENT.name,
                            amount = if (outcome == TradeOutcome.WIN) (tradeDomain.payoutAmount - tradeDomain.amount) else -tradeDomain.amount,
                            currency = "USD",
                            status = TransactionStatus.COMPLETED.name,
                            reference = "SETTLE-${backendTrade.id}",
                            timestamp = System.currentTimeMillis(),
                            paymentMethod = null,
                            note = "${tradeDomain.assetSymbol} ${tradeDomain.direction} settlement: $outcome"
                        )
                    )

                    _lastSettledTrade.value = tradeDomain
                }
            }
        }
    }

    private fun seedInitialDataIfEmpty() {
        scope.launch {
            val existingNotifications = database.notificationDao().getAllNotifications().firstOrNull()
            if (existingNotifications.isNullOrEmpty()) {
                val initialNotifs = listOf(
                    NotificationEntity(
                        id = "notif_1",
                        title = "Welcome to Apper Trading",
                        message = "Authoritative backend initialized. Demo balance credited with $10,000.",
                        category = NotificationCategory.ACCOUNT.name,
                        timestamp = System.currentTimeMillis() - 7200000,
                        isRead = false,
                        actionRoute = null
                    ),
                    NotificationEntity(
                        id = "notif_2",
                        title = "Security & Compliance Active",
                        message = "Double-entry financial ledger and Risk & Fraud engine online.",
                        category = NotificationCategory.SECURITY.name,
                        timestamp = System.currentTimeMillis() - 3600000,
                        isRead = true,
                        actionRoute = null
                    ),
                    NotificationEntity(
                        id = "notif_3",
                        title = "Market Data Ingestion Live",
                        message = "Validating real-time feeds across Crypto, Forex, and Commodities.",
                        category = NotificationCategory.TRADING.name,
                        timestamp = System.currentTimeMillis() - 1800000,
                        isRead = false,
                        actionRoute = null
                    )
                )
                database.notificationDao().insertNotifications(initialNotifs)
            }

            val existingTx = database.transactionDao().getAllTransactions().firstOrNull()
            if (existingTx.isNullOrEmpty()) {
                val initialTx = listOf(
                    TransactionEntity(
                        id = "tx_init_1",
                        type = TransactionType.DEPOSIT.name,
                        amount = 1250.0,
                        currency = "USD",
                        status = TransactionStatus.COMPLETED.name,
                        reference = "DEP-INIT-REAL",
                        timestamp = System.currentTimeMillis() - 86400000 * 2,
                        paymentMethod = PaymentMethod.CREDIT_DEBIT_CARD.name,
                        note = "Initial bank card deposit"
                    ),
                    TransactionEntity(
                        id = "tx_init_2",
                        type = TransactionType.DEMO_REFILL.name,
                        amount = 10000.0,
                        currency = "USD",
                        status = TransactionStatus.COMPLETED.name,
                        reference = "DEMO-INIT",
                        timestamp = System.currentTimeMillis() - 86400000 * 3,
                        paymentMethod = null,
                        note = "Demo practice balance"
                    )
                )
                initialTx.forEach { database.transactionDao().insertTransaction(it) }
            }
        }
    }

    fun setDemoMode(isDemo: Boolean) {
        _isDemoMode.value = isDemo
        _userProfile.update { it.copy(isDemoMode = isDemo) }
    }

    fun refillDemoBalance() {
        val idempotencyKey = UUID.randomUUID().toString()
        val result = backendServer.refillDemoBalance(idempotencyKey)
        if (result.success) {
            scope.launch {
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        id = "tx_${System.currentTimeMillis()}",
                        type = TransactionType.DEMO_REFILL.name,
                        amount = 10000.0,
                        currency = "USD",
                        status = TransactionStatus.COMPLETED.name,
                        reference = "DEMO-REFILL-${System.currentTimeMillis().toString().takeLast(4)}",
                        timestamp = System.currentTimeMillis(),
                        paymentMethod = null,
                        note = "Demo account reset to $10,000 via Ledger"
                    )
                )
            }
        }
    }

    // Trade flows from Room Database
    val allTrades: Flow<List<Trade>> = database.tradeDao().getAllTrades().map { list ->
        list.map { it.toDomain() }
    }

    val activeTrades: Flow<List<Trade>> = database.tradeDao().getActiveTrades().map { list ->
        list.map { it.toDomain() }
    }

    val allTransactions: Flow<List<TransactionItem>> = database.transactionDao().getAllTransactions().map { list ->
        list.map { it.toDomain() }
    }

    val notifications: Flow<List<NotificationItem>> = database.notificationDao().getAllNotifications().map { list ->
        list.map { entity ->
            NotificationItem(
                id = entity.id,
                title = entity.title,
                message = entity.message,
                category = runCatching { NotificationCategory.valueOf(entity.category) }.getOrDefault(NotificationCategory.ALL),
                timestamp = entity.timestamp,
                isRead = entity.isRead,
                actionRoute = entity.actionRoute
            )
        }
    }

    /**
     * Executes trade submission through the Authoritative Backend Server.
     */
    suspend fun submitTrade(
        asset: MarketAsset,
        direction: TradeDirection,
        amount: Double,
        durationSeconds: Long
    ): TradeSubmissionResult {
        // Section 35: Client-side offline sanity check
        if (priceEngine.connectionState.value == WsConnectionState.DISCONNECTED) {
            return TradeSubmissionResult.Error("Cannot submit trade while offline. Please check your connection.", "NETWORK_ERROR")
        }

        // Section 20: Client-side preliminary validation
        if (amount < 1.0) {
            return TradeSubmissionResult.Error("Minimum trade amount is $1.00", "VALIDATION_ERROR")
        }

        val idempotencyKey = UUID.randomUUID().toString()

        // Sync authoritative price tick into backend market engine
        backendServer.marketDataService.ingestTick(
            symbol = asset.symbol,
            price = asset.currentPrice,
            timestamp = System.currentTimeMillis()
        )

        // Delegate execution to Authoritative Backend Server
        val backendResponse = backendServer.submitTrade(
            userId = "usr_default_demo_1",
            assetSymbol = asset.symbol,
            direction = direction.name,
            amountDollars = amount,
            durationSeconds = durationSeconds,
            payoutRate = asset.payoutRate,
            idempotencyKey = idempotencyKey,
            isDemo = _isDemoMode.value
        )

        if (!backendResponse.success || backendResponse.data == null) {
            val err = backendResponse.error
            return TradeSubmissionResult.Error(
                err?.message ?: "Trade execution rejected by backend",
                err?.code ?: "BACKEND_ERROR"
            )
        }

        val backendOrder = backendResponse.data
        val trade = Trade(
            id = backendOrder.id,
            idempotencyKey = backendOrder.idempotencyKey,
            assetSymbol = backendOrder.assetSymbol,
            assetName = asset.name,
            direction = direction,
            amount = amount,
            entryPrice = backendOrder.entryPrice,
            durationSeconds = backendOrder.durationSeconds,
            createdAt = backendOrder.serverAcceptedTimestamp,
            serverExpiryTimestamp = backendOrder.serverExpiryTimestamp,
            status = TradeStatus.ACTIVE,
            settlementPrice = null,
            payoutRate = backendOrder.payoutRate,
            payoutAmount = backendOrder.potentialPayoutCents / 100.0,
            outcome = TradeOutcome.PENDING,
            isDemo = backendOrder.isDemo
        )

        // Persist to local Room database for reactive UI binding
        database.tradeDao().insertTrade(TradeEntity.fromDomain(trade))

        return TradeSubmissionResult.Success(trade)
    }

    fun dismissLastSettledTrade() {
        _lastSettledTrade.value = null
        backendServer.tradingEngine.dismissRecentlySettled()
    }

    // Wallet actions: Deposit & Withdrawal
    suspend fun executeDeposit(amount: Double, method: PaymentMethod): Boolean {
        delay(400) // Simulated network roundtrip
        val ref = "DEP-${System.currentTimeMillis().toString().takeLast(6)}"
        val response = backendServer.submitDeposit(amount, method.name, ref)

        if (response.success) {
            val tx = TransactionEntity(
                id = "dep_${System.currentTimeMillis()}",
                type = TransactionType.DEPOSIT.name,
                amount = amount,
                currency = "USD",
                status = TransactionStatus.COMPLETED.name,
                reference = ref,
                timestamp = System.currentTimeMillis(),
                paymentMethod = method.name,
                note = "Deposit via ${method.displayName}"
            )
            database.transactionDao().insertTransaction(tx)

            database.notificationDao().insertNotifications(listOf(
                NotificationEntity(
                    id = "notif_dep_${System.currentTimeMillis()}",
                    title = "Deposit Successful",
                    message = "Your deposit of $${String.format("%,.2f", amount)} has been credited via ${method.displayName}.",
                    category = NotificationCategory.PAYMENT.name,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    actionRoute = null
                )
            ))
            return true
        }
        return false
    }

    suspend fun executeWithdrawal(amount: Double, destination: String, method: PaymentMethod): Boolean {
        val idempotencyKey = UUID.randomUUID().toString()
        val response = backendServer.submitWithdrawal(
            userId = "usr_default_demo_1",
            amountDollars = amount,
            destinationAddress = destination,
            method = method.name,
            idempotencyKey = idempotencyKey
        )

        if (response.success) {
            val tx = TransactionEntity(
                id = "wdr_${System.currentTimeMillis()}",
                type = TransactionType.WITHDRAWAL.name,
                amount = -amount,
                currency = "USD",
                status = TransactionStatus.PROCESSING.name,
                reference = response.data ?: "WDR-REF",
                timestamp = System.currentTimeMillis(),
                paymentMethod = method.name,
                note = "Withdrawal to $destination"
            )
            database.transactionDao().insertTransaction(tx)
            return true
        }
        return false
    }

    suspend fun markNotificationAsRead(id: String) {
        database.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        database.notificationDao().markAllAsRead()
    }

    fun updateKycStatus(status: KycStatus) {
        _userProfile.update { it.copy(kycStatus = status) }
    }

    fun toggleMfa(enabled: Boolean) {
        _userProfile.update { it.copy(mfaEnabled = enabled) }
    }

    fun logout() {
        _authState.value = AuthState.UNAUTHENTICATED
    }

    fun login(email: String, isDemo: Boolean = false) {
        _userProfile.update { it.copy(email = email) }
        _authState.value = AuthState.AUTHENTICATED
        setDemoMode(isDemo)
    }
}
