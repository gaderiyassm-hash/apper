package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.core.model.Trade
import com.example.core.model.TradeOutcome
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettledTradeDialog(
    trade: Trade,
    onDismiss: () -> Unit
) {
    val isWin = trade.outcome == TradeOutcome.WIN
    val isTie = trade.outcome == TradeOutcome.TIE
    val statusColor = when {
        isWin -> TradeUpGreen
        isTie -> AccentGold
        else -> TradeDownRed
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("settled_trade_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trade Settled",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Outcome Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWin) Icons.Default.CheckCircle else Icons.Default.SentimentVeryDissatisfied,
                        contentDescription = trade.outcome.name,
                        tint = statusColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isWin -> "CONGRATULATIONS!"
                        isTie -> "REFUNDED / TIE"
                        else -> "TRADE EXPIRED"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                val profitOrLoss = when {
                    isWin -> "+$${String.format("%.2f", trade.payoutAmount)}"
                    isTie -> "$0.00"
                    else -> "-$${String.format("%.2f", trade.amount)}"
                }
                Text(
                    text = profitOrLoss,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Settlement details grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Trade ID", trade.id)
                    DetailRow("Asset", "${trade.assetSymbol} (${trade.direction.name})")
                    DetailRow("Entry Price", String.format("%.2f", trade.entryPrice))
                    DetailRow("Settlement Price", String.format("%.2f", trade.settlementPrice ?: 0.0))
                    DetailRow("Invested Amount", "$${String.format("%.2f", trade.amount)}")
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trade.serverExpiryTimestamp))
                    DetailRow("Settlement Time", timeStr)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_settled_dialog_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continue Trading", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}
