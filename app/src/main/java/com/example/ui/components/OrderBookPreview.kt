package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.OrderBookDepth
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen

@Composable
fun OrderBookPreview(
    depth: OrderBookDepth,
    modifier: Modifier = Modifier,
    priceDecimals: Int = 2
) {
    Column(
        modifier = modifier
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("order_book_preview")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Order Book Depth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Spread: ${String.format("%.4f", depth.spread)}",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Bids (Green / Buyers)
            Column(modifier = Modifier.weight(1f)) {
                Text("Bids (Qty)", fontSize = 10.sp, color = TradeUpGreen, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                depth.bids.take(4).forEach { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    ) {
                        // Depth bar fill
                        val fillRatio = (entry.size / 4.0).toFloat().coerceIn(0.1f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fillRatio)
                                .align(Alignment.CenterStart)
                                .background(TradeUpGreen.copy(alpha = 0.15f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                String.format("%.2f", entry.price),
                                fontSize = 10.sp,
                                color = TradeUpGreen,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                String.format("%.2f", entry.size),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Asks (Red / Sellers)
            Column(modifier = Modifier.weight(1f)) {
                Text("Asks (Qty)", fontSize = 10.sp, color = TradeDownRed, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                depth.asks.take(4).forEach { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    ) {
                        val fillRatio = (entry.size / 4.0).toFloat().coerceIn(0.1f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fillRatio)
                                .align(Alignment.CenterEnd)
                                .background(TradeDownRed.copy(alpha = 0.15f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                String.format("%.2f", entry.size),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                String.format("%.2f", entry.price),
                                fontSize = 10.sp,
                                color = TradeDownRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
