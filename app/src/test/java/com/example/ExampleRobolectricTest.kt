package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.MarketAsset
import com.example.core.model.MarketCategory
import com.example.core.model.MarketStatus
import com.example.core.model.TradeDirection
import com.example.core.network.MarketPriceEngine
import com.example.core.repository.TradeSubmissionResult
import com.example.core.repository.TradingRepository
import com.example.core.storage.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Apper Trading", appName)
    }

    @Test
    fun testMarketAssetCalculations() {
        val asset = MarketAsset(
            id = "btc_usdt",
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = MarketCategory.CRYPTO,
            currentPrice = 64500.0,
            changePercent24h = 2.45,
            high24h = 65000.0,
            low24h = 62500.0,
            volume24h = "$1.5B",
            payoutRate = 0.85,
            priceDecimals = 2,
            status = MarketStatus.LIVE,
            isFavorite = true
        )

        assertEquals("64,500.00", asset.formatPrice(asset.currentPrice))
        assertTrue(asset.changePercent24h > 0)
    }

    @Test
    fun testTradingRepositoryTradeValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val database = AppDatabase.getInstance(context)
        val engine = MarketPriceEngine(testScope)
        val repo = TradingRepository(database, engine, testScope)

        val asset = MarketAsset(
            id = "eth_usdt",
            symbol = "ETH/USDT",
            name = "Ethereum",
            category = MarketCategory.CRYPTO,
            currentPrice = 3450.0,
            changePercent24h = 1.15,
            high24h = 3500.0,
            low24h = 3380.0,
            volume24h = "$850M",
            payoutRate = 0.82,
            priceDecimals = 2,
            status = MarketStatus.LIVE
        )

        // Test below minimum amount ($1.0)
        val subMinResult = repo.submitTrade(asset, TradeDirection.CALL, 0.5, 60)
        assertTrue(subMinResult is TradeSubmissionResult.Error)

        // Test valid trade on demo account ($10,000 available)
        val validResult = repo.submitTrade(asset, TradeDirection.CALL, 100.0, 60)
        assertTrue(validResult is TradeSubmissionResult.Success)
        val trade = (validResult as TradeSubmissionResult.Success).trade

        assertEquals("ETH/USDT", trade.assetSymbol)
        assertEquals(TradeDirection.CALL, trade.direction)
        assertEquals(100.0, trade.amount, 0.001)
        assertEquals(182.0, trade.payoutAmount, 0.001)
        assertTrue(trade.serverExpiryTimestamp > trade.createdAt)
    }
}
