package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Trade
import com.example.core.model.TradeDirection
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen
import kotlinx.coroutines.delay

@Composable
fun ActiveTradeCard(
    trade: Trade,
    currentMarketPrice: Double,
    modifier: Modifier = Modifier
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(trade.id) {
        while (true) {
            delay(250)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val remainingSeconds = trade.remainingSeconds(currentTimeMs)
    val totalSeconds = trade.durationSeconds
    val elapsed = totalSeconds - remainingSeconds
    val progress = (elapsed.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val isCall = trade.direction == TradeDirection.CALL
    val inTheMoney = if (isCall) currentMarketPrice > trade.entryPrice else currentMarketPrice < trade.entryPrice

    val accentColor = if (isCall) TradeUpGreen else TradeDownRed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, if (inTheMoney) TradeUpGreen.copy(alpha = 0.5f) else SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("active_trade_${trade.id}")
    ) {
        // Top Row: Asset, Direction Badge, Remaining time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCall) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = trade.direction.name,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(trade.assetSymbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(trade.id, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }

            // Countdown Pill
            Row(
                modifier = Modifier
                    .background(Color(0xFF0F141E), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = AccentGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${remainingSeconds}s",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Price comparison row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Entry Price", fontSize = 10.sp, color = TextSecondary)
                Text(
                    String.format("%.2f", trade.entryPrice),
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current Price", fontSize = 10.sp, color = TextSecondary)
                Text(
                    String.format("%.2f", currentMarketPrice),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (inTheMoney) TradeUpGreen else TradeDownRed,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Invested", fontSize = 10.sp, color = TextSecondary)
                Text(
                    "$${String.format("%.2f", trade.amount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Countdown visual progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = if (inTheMoney) TradeUpGreen else AccentGold,
            trackColor = Color(0xFF0F141E)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (inTheMoney) "▲ IN THE MONEY" else "▼ OUT OF MONEY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (inTheMoney) TradeUpGreen else TradeDownRed
            )
            Text(
                text = "Payout: +$${String.format("%.2f", trade.payoutAmount)}",
                fontSize = 10.sp,
                color = AccentGold,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
