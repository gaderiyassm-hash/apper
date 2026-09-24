package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CandleStick
import com.example.core.model.TimeFrame
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class ChartType {
    CANDLESTICK,
    LINE
}

@Composable
fun InteractiveCandleChart(
    candles: List<CandleStick>,
    currentPrice: Double,
    selectedTimeframe: TimeFrame,
    onTimeframeSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier,
    priceDecimals: Int = 2
) {
    var chartType by remember { mutableStateOf(ChartType.CANDLESTICK) }
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .testTag("interactive_candle_chart")
    ) {
        // Controls Row: Timeframes & Chart Type Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeFrame.values().forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    Surface(
                        onClick = { onTimeframeSelected(tf) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) AccentGold else Color.Transparent,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = tf.label,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Mode Toggle (Candles vs Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { chartType = ChartType.CANDLESTICK },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Candlestick Chart",
                        tint = if (chartType == ChartType.CANDLESTICK) AccentGold else TextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { chartType = ChartType.LINE },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Line Chart",
                        tint = if (chartType == ChartType.LINE) AccentGold else TextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Crosshair tooltip if user taps
        val inspectCandle = selectedCandleIndex?.let { idx ->
            if (idx in candles.indices) candles[idx] else null
        }
        if (inspectCandle != null) {
            val dateFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(inspectCandle.timestamp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F141E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("T: $dateFmt", fontSize = 10.sp, color = TextSecondary)
                Text("O: ${formatPrice(inspectCandle.open, priceDecimals)}", fontSize = 10.sp, color = TextPrimary)
                Text("H: ${formatPrice(inspectCandle.high, priceDecimals)}", fontSize = 10.sp, color = TradeUpGreen)
                Text("L: ${formatPrice(inspectCandle.low, priceDecimals)}", fontSize = 10.sp, color = TradeDownRed)
                Text("C: ${formatPrice(inspectCandle.close, priceDecimals)}", fontSize = 10.sp, color = if (inspectCandle.isBullish) TradeUpGreen else TradeDownRed)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val candleWidth = size.width / max(1, candles.size)
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedCandleIndex = if (selectedCandleIndex == index) null else index
                            }
                        )
                    }
            ) {
                if (candles.isEmpty()) return@Canvas

                val chartWidth = size.width - 64.dp.toPx() // Reserve 64dp for price axis on the right
                val chartHeight = size.height
                val volumeAreaHeight = chartHeight * 0.18f
                val priceAreaHeight = chartHeight - volumeAreaHeight - 12.dp.toPx()

                // Calculate price bounds
                var minP = candles.minOf { it.low }
                var maxP = candles.maxOf { it.high }
                minP = min(minP, currentPrice)
                maxP = max(maxP, currentPrice)

                val padding = (maxP - minP) * 0.08
                val minPrice = minP - padding
                val maxPrice = maxP + padding
                val priceRange = max(0.0001, maxPrice - minPrice)

                val maxVolume = max(1.0, candles.maxOf { it.volume })

                fun priceToY(price: Double): Float {
                    val ratio = (price - minPrice) / priceRange
                    return (priceAreaHeight * (1f - ratio.toFloat()))
                }

                // Draw Horizontal Price Grid Lines & Labels
                val gridLevels = 4
                for (i in 0..gridLevels) {
                    val p = minPrice + (priceRange * (i.toDouble() / gridLevels))
                    val y = priceToY(p)

                    drawLine(
                        color = SurfaceBorder,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Draw right-axis price text
                    val priceStr = formatPrice(p, priceDecimals)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = priceStr,
                        topLeft = Offset(chartWidth + 6.dp.toPx(), y - 7.sp.toPx()),
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                val count = candles.size
                val stepX = chartWidth / max(1, count)
                val candleWidth = max(2f, stepX * 0.65f)

                // Draw Volume bars at bottom
                candles.forEachIndexed { i, candle ->
                    val x = (i * stepX) + (stepX / 2f)
                    val volRatio = (candle.volume / maxVolume).toFloat().coerceIn(0f, 1f)
                    val vHeight = volumeAreaHeight * volRatio
                    val vTop = chartHeight - vHeight
                    val color = if (candle.isBullish) TradeUpGreen.copy(alpha = 0.35f) else TradeDownRed.copy(alpha = 0.35f)

                    drawRect(
                        color = color,
                        topLeft = Offset(x - (candleWidth / 2f), vTop),
                        size = Size(candleWidth, vHeight)
                    )
                }

                if (chartType == ChartType.CANDLESTICK) {
                    // Draw Candlesticks (Wick + Body)
                    candles.forEachIndexed { i, candle ->
                        val x = (i * stepX) + (stepX / 2f)
                        val highY = priceToY(candle.high)
                        val lowY = priceToY(candle.low)
                        val openY = priceToY(candle.open)
                        val closeY = priceToY(candle.close)

                        val isUp = candle.isBullish
                        val candleColor = if (isUp) TradeUpGreen else TradeDownRed

                        // Draw Wick
                        drawLine(
                            color = candleColor,
                            start = Offset(x, highY),
                            end = Offset(x, lowY),
                            strokeWidth = 1.5f
                        )

                        // Draw Body
                        val bodyTop = min(openY, closeY)
                        val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x - (candleWidth / 2f), bodyTop),
                            size = Size(candleWidth, bodyHeight)
                        )
                    }
                } else {
                    // Line chart mode with smooth gradient fill
                    val linePath = Path()
                    val areaPath = Path()

                    candles.forEachIndexed { i, candle ->
                        val x = (i * stepX) + (stepX / 2f)
                        val y = priceToY(candle.close)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, priceAreaHeight)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }

                    val lastX = ((count - 1) * stepX) + (stepX / 2f)
                    areaPath.lineTo(lastX, priceAreaHeight)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        color = AccentGold.copy(alpha = 0.12f)
                    )
                    drawPath(
                        path = linePath,
                        color = AccentGold,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw Current Live Price Dashed Indicator & Pill
                val liveY = priceToY(currentPrice)
                drawLine(
                    color = AccentGold,
                    start = Offset(0f, liveY),
                    end = Offset(chartWidth, liveY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                // Live price badge on the right
                val liveBadgeWidth = 56.dp.toPx()
                val liveBadgeHeight = 16.dp.toPx()
                drawRect(
                    color = AccentGold,
                    topLeft = Offset(chartWidth + 2.dp.toPx(), liveY - (liveBadgeHeight / 2f)),
                    size = Size(liveBadgeWidth, liveBadgeHeight)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = formatPrice(currentPrice, priceDecimals),
                    topLeft = Offset(chartWidth + 6.dp.toPx(), liveY - 6.sp.toPx()),
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )

                // Draw inspection crosshair line if tapped
                selectedCandleIndex?.let { idx ->
                    if (idx in candles.indices) {
                        val inspectX = (idx * stepX) + (stepX / 2f)
                        drawLine(
                            color = TextPrimary.copy(alpha = 0.7f),
                            start = Offset(inspectX, 0f),
                            end = Offset(inspectX, chartHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPrice(price: Double, decimals: Int): String {
    return when (decimals) {
        0 -> String.format("%,.0f", price)
        2 -> String.format("%,.2f", price)
        4 -> String.format("%,.4f", price)
        else -> String.format("%,.2f", price)
    }
}
