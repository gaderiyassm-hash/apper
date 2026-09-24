package com.example.core.model

enum class MarketCategory {
    ALL,
    CRYPTO,
    FOREX,
    INDICES,
    COMMODITIES
}

enum class MarketStatus {
    LIVE,
    CLOSED,
    SUSPENDED,
    DATA_UNAVAILABLE
}

data class MarketAsset(
    val id: String,
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val currentPrice: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: String,
    val payoutRate: Double = 0.85, // e.g., 85% return on winning trade
    val status: MarketStatus = MarketStatus.LIVE,
    val isFavorite: Boolean = false,
    val priceDecimals: Int = 2
) {
    fun formatPrice(price: Double): String {
        return when (priceDecimals) {
            0 -> String.format("%,.0f", price)
            2 -> String.format("%,.2f", price)
            4 -> String.format("%,.4f", price)
            5 -> String.format("%,.5f", price)
            else -> String.format("%,.2f", price)
        }
    }
}
