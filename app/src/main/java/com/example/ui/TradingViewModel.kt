package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AuthState
import com.example.core.model.CandleStick
import com.example.core.model.KycStatus
import com.example.core.model.MarketAsset
import com.example.core.model.NotificationItem
import com.example.core.model.PaymentMethod
import com.example.core.model.TimeFrame
import com.example.core.model.Trade
import com.example.core.model.TradeDirection
import com.example.core.model.TransactionItem
import com.example.core.model.UserProfile
import com.example.core.model.WalletBalance
import com.example.core.network.MarketPriceEngine
import com.example.core.network.OrderBookDepth
import com.example.core.network.WsConnectionState
import com.example.core.repository.TradeSubmissionResult
import com.example.core.repository.TradingRepository
import com.example.core.storage.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val priceEngine = MarketPriceEngine(viewModelScope)
    val repository = TradingRepository(database, priceEngine, viewModelScope)

    // Connection & Markets
    val connectionState: StateFlow<WsConnectionState> = priceEngine.connectionState
    val markets: StateFlow<List<MarketAsset>> = priceEngine.markets

    // Selected Asset for Trading Screen
    private val _selectedAsset = MutableStateFlow<MarketAsset?>(null)
    val selectedAsset: StateFlow<MarketAsset?> = _selectedAsset.asStateFlow()

    // Selected Timeframe for Charting
    private val _selectedTimeframe = MutableStateFlow(TimeFrame.TF_1M)
    val selectedTimeframe: StateFlow<TimeFrame> = _selectedTimeframe.asStateFlow()

    // Chart Candles State
    private val _candles = MutableStateFlow<List<CandleStick>>(emptyList())
    val candles: StateFlow<List<CandleStick>> = _candles.asStateFlow()

    // Order Book Depth State
    private val _orderBook = MutableStateFlow(priceEngine.getOrderBook("BTC/USDT"))
    val orderBook: StateFlow<OrderBookDepth> = _orderBook.asStateFlow()

    // Mode: Demo vs Real
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode

    // Balances
    val activeBalance: StateFlow<WalletBalance> = combine(
        repository.isDemoMode,
        repository.demoBalance,
        repository.realBalance
    ) { isDemo, demo, real ->
        if (isDemo) demo else real
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WalletBalance(10000.0, 0.0, isDemo = true))

    // Trades
    val allTrades: StateFlow<List<Trade>> = repository.allTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrades: StateFlow<List<Trade>> = repository.activeTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastSettledTrade: StateFlow<Trade?> = repository.lastSettledTrade

    // Transactions
    val allTransactions: StateFlow<List<TransactionItem>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Profile & Auth
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val authState: StateFlow<AuthState> = repository.authState

    init {
        // Observe price updates to refresh chart and current asset
        viewModelScope.launch {
            priceEngine.markets.collect { list ->
                if (_selectedAsset.value == null && list.isNotEmpty()) {
                    _selectedAsset.value = list.first()
                } else if (_selectedAsset.value != null) {
                    val updated = list.find { it.symbol == _selectedAsset.value?.symbol }
                    if (updated != null) {
                        _selectedAsset.value = updated
                    }
                }

                // Update candles & depth for current asset
                val current = _selectedAsset.value
                if (current != null) {
                    _candles.value = priceEngine.getCandles(current.symbol, _selectedTimeframe.value)
                    _orderBook.value = priceEngine.getOrderBook(current.symbol)
                }
            }
        }
    }

    fun selectAsset(asset: MarketAsset) {
        _selectedAsset.value = asset
        _candles.value = priceEngine.getCandles(asset.symbol, _selectedTimeframe.value)
        _orderBook.value = priceEngine.getOrderBook(asset.symbol)
    }

    fun setTimeframe(tf: TimeFrame) {
        _selectedTimeframe.value = tf
        val current = _selectedAsset.value
        if (current != null) {
            _candles.value = priceEngine.getCandles(current.symbol, tf)
        }
    }

    fun toggleFavorite(symbol: String) {
        priceEngine.toggleFavorite(symbol)
    }

    fun toggleDemoMode() {
        repository.setDemoMode(!repository.isDemoMode.value)
    }

    fun refillDemoBalance() {
        repository.refillDemoBalance()
    }

    suspend fun submitTrade(
        asset: MarketAsset,
        direction: TradeDirection,
        amount: Double,
        durationSeconds: Long
    ): TradeSubmissionResult {
        return repository.submitTrade(asset, direction, amount, durationSeconds)
    }

    fun dismissLastSettledTrade() {
        repository.dismissLastSettledTrade()
    }

    suspend fun deposit(amount: Double, method: PaymentMethod): Boolean {
        return repository.executeDeposit(amount, method)
    }

    suspend fun withdraw(amount: Double, destination: String, method: PaymentMethod): Boolean {
        return repository.executeWithdrawal(amount, destination, method)
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch { repository.markNotificationAsRead(id) }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch { repository.markAllNotificationsAsRead() }
    }

    fun updateKycStatus(status: KycStatus) {
        repository.updateKycStatus(status)
    }

    fun toggleMfa(enabled: Boolean) {
        repository.toggleMfa(enabled)
    }

    fun logout() {
        repository.logout()
    }

    fun login(email: String, isDemo: Boolean) {
        repository.login(email, isDemo)
    }

    // Authoritative Backend Introspection
    fun getLedgerEntries(): List<com.example.core.backend.model.BackendLedgerEntry> =
        repository.backendServer.getLedgerEntries()

    fun getAuditTrail(): List<com.example.core.backend.risk.AuditLogRecord> =
        repository.backendServer.getAuditTrail()

    fun getFeatureFlags(): Map<String, Boolean> =
        repository.backendServer.featureFlagService.getAllFlags()
}
