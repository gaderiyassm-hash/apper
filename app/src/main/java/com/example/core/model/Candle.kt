package com.example.core.model

enum class TimeFrame(val label: String, val seconds: Long) {
    TF_1M("1m", 60),
    TF_5M("5m", 300),
    TF_15M("15m", 900),
    TF_1H("1h", 3600),
    TF_4H("4h", 14400),
    TF_1D("1D", 86400)
}

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
) {
    val isBullish: Boolean get() = close >= open
}
