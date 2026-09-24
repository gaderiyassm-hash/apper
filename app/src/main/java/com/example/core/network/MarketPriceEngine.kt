package com.example.core.network

import com.example.core.model.CandleStick
import com.example.core.model.MarketAsset
import com.example.core.model.MarketCategory
import com.example.core.model.MarketStatus
import com.example.core.model.TimeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class WsConnectionState {
    CONNECTED,
    CONNECTING,
    RECONNECTING,
    DISCONNECTED
}

data class PriceTick(
    val symbol: String,
    val price: Double,
    val changePercent: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderBookEntry(
    val price: Double,
    val size: Double,
    val total: Double
)

data class OrderBookDepth(
    val symbol: String,
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val spread: Double
)

class MarketPriceEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _connectionState = MutableStateFlow(WsConnectionState.CONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _ticks = MutableSharedFlow<PriceTick>(extraBufferCapacity = 64)
    val ticks: SharedFlow<PriceTick> = _ticks.asSharedFlow()

    private val _markets = MutableStateFlow<List<MarketAsset>>(emptyList())
    val markets: StateFlow<List<MarketAsset>> = _markets.asStateFlow()

    // Cache of candle history per (symbol, timeframe)
    private val candleCache = ConcurrentHashMap<String, MutableList<CandleStick>>()

    private var simulationJob: Job? = null

    init {
        initializeInitialMarkets()
        startRealtimeStreaming()
    }

    private fun initializeInitialMarkets() {
        val initial = listOf(
            MarketAsset(
                id = "btc_usdt",
                symbol = "BTC/USDT",
                name = "Bitcoin",
                category = MarketCategory.CRYPTO,
                currentPrice = 96420.50,
                changePercent24h = 3.42,
                high24h = 97800.00,
                low24h = 94250.00,
                volume24h = "2.84B",
                payoutRate = 0.88,
                status = MarketStatus.LIVE,
                isFavorite = true,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "eth_usdt",
                symbol = "ETH/USDT",
                name = "Ethereum",
                category = MarketCategory.CRYPTO,
                currentPrice = 3450.80,
                changePercent24h = -1.18,
                high24h = 3560.00,
                low24h = 3380.00,
                volume24h = "1.15B",
                payoutRate = 0.86,
                status = MarketStatus.LIVE,
                isFavorite = true,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "sol_usdt",
                symbol = "SOL/USDT",
                name = "Solana",
                category = MarketCategory.CRYPTO,
                currentPrice = 186.45,
                changePercent24h = 5.62,
                high24h = 191.00,
                low24h = 176.20,
                volume24h = "640M",
                payoutRate = 0.85,
                status = MarketStatus.LIVE,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "eur_usd",
                symbol = "EUR/USD",
                name = "Euro / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = 1.08450,
                changePercent24h = 0.24,
                high24h = 1.08720,
                low24h = 1.08180,
                volume24h = "14.2B",
                payoutRate = 0.82,
                status = MarketStatus.LIVE,
                priceDecimals = 4
            ),
            MarketAsset(
                id = "gbp_usd",
                symbol = "GBP/USD",
                name = "British Pound / USD",
                category = MarketCategory.FOREX,
                currentPrice = 1.29820,
                changePercent24h = -0.31,
                high24h = 1.30400,
                low24h = 1.29450,
                volume24h = "8.6B",
                payoutRate = 0.83,
                status = MarketStatus.LIVE,
                priceDecimals = 4
            ),
            MarketAsset(
                id = "xau_usd",
                symbol = "XAU/USD",
                name = "Gold Spot",
                category = MarketCategory.COMMODITIES,
                currentPrice = 2748.60,
                changePercent24h = 1.15,
                high24h = 2758.00,
                low24h = 2724.00,
                volume24h = "3.2B",
                payoutRate = 0.87,
                status = MarketStatus.LIVE,
                isFavorite = true,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "spx_500",
                symbol = "S&P 500",
                name = "US 500 Index",
                category = MarketCategory.INDICES,
                currentPrice = 5880.25,
                changePercent24h = 0.68,
                high24h = 5905.00,
                low24h = 5840.00,
                volume24h = "5.1B",
                payoutRate = 0.84,
                status = MarketStatus.LIVE,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "nasdaq_100",
                symbol = "NDX 100",
                name = "US Tech 100",
                category = MarketCategory.INDICES,
                currentPrice = 20340.50,
                changePercent24h = 1.45,
                high24h = 20490.00,
                low24h = 20080.00,
                volume24h = "4.7B",
                payoutRate = 0.85,
                status = MarketStatus.LIVE,
                priceDecimals = 2
            ),
            MarketAsset(
                id = "crude_oil",
                symbol = "WTI/OIL",
                name = "Crude Oil WTI",
                category = MarketCategory.COMMODITIES,
                currentPrice = 71.85,
                changePercent24h = -0.84,
                high24h = 73.10,
                low24h = 71.20,
                volume24h = "1.8B",
                payoutRate = 0.80,
                status = MarketStatus.LIVE,
                priceDecimals = 2
            )
        )
        _markets.value = initial
    }

    private fun startRealtimeStreaming() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var loopCount = 0
            while (isActive) {
                delay(750) // Frequent live ticks
                loopCount++

                // Intermittent brief reconnect simulation every 200 ticks to verify fault-tolerance
                if (loopCount % 350 == 0) {
                    _connectionState.value = WsConnectionState.RECONNECTING
                    delay(1200)
                    _connectionState.value = WsConnectionState.CONNECTED
                }

                _markets.update { currentList ->
                    currentList.map { asset ->
                        // Stochastic Brownian walk
                        val volatility = when (asset.category) {
                            MarketCategory.CRYPTO -> 0.0009
                            MarketCategory.FOREX -> 0.00012
                            MarketCategory.COMMODITIES -> 0.00035
                            MarketCategory.INDICES -> 0.00025
                            else -> 0.0003
                        }
                        val deltaPercent = (Random.nextDouble() - 0.495) * volatility
                        val newPrice = max(0.0001, asset.currentPrice * (1.0 + deltaPercent))
                        val newChange = asset.changePercent24h + (deltaPercent * 100 * 0.1)

                        // Update current candle in cache
                        updateCandleCacheWithTick(asset.symbol, newPrice)

                        val updated = asset.copy(
                            currentPrice = newPrice,
                            changePercent24h = newChange,
                            high24h = max(asset.high24h, newPrice),
                            low24h = min(asset.low24h, newPrice)
                        )

                        _ticks.tryEmit(
                            PriceTick(
                                symbol = updated.symbol,
                                price = newPrice,
                                changePercent = newChange
                            )
                        )

                        updated
                    }
                }
            }
        }
    }

    fun getCandles(symbol: String, timeframe: TimeFrame, count: Int = 40): List<CandleStick> {
        val key = "$symbol:${timeframe.name}"
        return candleCache.getOrPut(key) {
            generateSyntheticCandles(symbol, timeframe, count)
        }
    }

    private fun updateCandleCacheWithTick(symbol: String, price: Double) {
        if (candleCache.isEmpty()) return
        val prefix = "$symbol:"
        for ((key, list) in candleCache) {
            if (key.startsWith(prefix) && list.isNotEmpty()) {
                val last = list.last()
                val now = System.currentTimeMillis()
                val tfName = key.substringAfter(prefix)
                val tf = runCatching { TimeFrame.valueOf(tfName) }.getOrNull() ?: TimeFrame.TF_1M
                val intervalMs = tf.seconds * 1000L
                if (now - last.timestamp < intervalMs) {
                    // Update active candle
                    val updated = last.copy(
                        high = max(last.high, price),
                        low = min(last.low, price),
                        close = price,
                        volume = last.volume + 0.25
                    )
                    list[list.lastIndex] = updated
                } else {
                    // New candle
                    val newCandle = CandleStick(
                        timestamp = now,
                        open = last.close,
                        high = max(last.close, price),
                        low = min(last.close, price),
                        close = price,
                        volume = 2.5
                    )
                    list.add(newCandle)
                    if (list.size > 80) list.removeAt(0)
                }
            }
        }
    }

    private fun generateSyntheticCandles(symbol: String, timeframe: TimeFrame, count: Int): MutableList<CandleStick> {
        val asset = _markets.value.find { it.symbol == symbol }
        var basePrice = asset?.currentPrice ?: 100.0
        val candles = mutableListOf<CandleStick>()
        val intervalMs = timeframe.seconds * 1000
        val now = System.currentTimeMillis()
        val startTime = now - (count * intervalMs)

        var currPrice = basePrice * 0.97
        for (i in 0 until count) {
            val ts = startTime + (i * intervalMs)
            val open = currPrice
            val shift = (Random.nextDouble() - 0.49) * (basePrice * 0.006)
            val close = max(0.001, open + shift)
            val high = max(open, close) + Random.nextDouble(0.0, basePrice * 0.003)
            val low = min(open, close) - Random.nextDouble(0.0, basePrice * 0.003)
            val vol = Random.nextDouble(50.0, 500.0)

            candles.add(
                CandleStick(
                    timestamp = ts,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol
                )
            )
            currPrice = close
        }
        return candles
    }

    fun getOrderBook(symbol: String): OrderBookDepth {
        val asset = _markets.value.find { it.symbol == symbol }
        val price = asset?.currentPrice ?: 100.0
        val spread = price * 0.0002

        var bidRunning = 0.0
        val bids = (1..6).map { i ->
            val p = price - (spread * i)
            val size = Random.nextDouble(0.2, 3.5)
            bidRunning += size
            OrderBookEntry(p, size, bidRunning)
        }

        var askRunning = 0.0
        val asks = (1..6).map { i ->
            val p = price + (spread * i)
            val size = Random.nextDouble(0.2, 3.5)
            askRunning += size
            OrderBookEntry(p, size, askRunning)
        }

        return OrderBookDepth(symbol, bids, asks, spread)
    }

    fun toggleFavorite(symbol: String) {
        _markets.update { list ->
            list.map {
                if (it.symbol == symbol) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }
}
