package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MarketAsset
import com.example.core.model.TradeDirection
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeConfirmationSheet(
    sheetState: SheetState,
    asset: MarketAsset,
    direction: TradeDirection,
    amount: Double,
    durationSeconds: Long,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCall = direction == TradeDirection.CALL
    val actionColor = if (isCall) TradeUpGreen else TradeDownRed
    val payoutPotential = amount * (1.0 + asset.payoutRate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("trade_confirmation_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(actionColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCall) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = direction.name,
                        tint = actionColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Confirm ${if (isCall) "CALL (UP)" else "PUT (DOWN)"} Trade",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${asset.symbol} • ${asset.name}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryItem("Current Price", asset.formatPrice(asset.currentPrice))
                SummaryItem("Investment Amount", "$${String.format("%.2f", amount)}")
                SummaryItem("Duration", "${durationSeconds} seconds")
                SummaryItem("Platform Fee", "$0.00 (Zero Fee)")
                SummaryItem("Profit Multiplier", "+${(asset.payoutRate * 100).toInt()}%")
                SummaryItem(
                    label = "Potential Payout",
                    value = "$${String.format("%.2f", payoutPotential)}",
                    isHighlight = true,
                    highlightColor = TradeUpGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Risk Disclosure Box (Mandatory financial warning per specification)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF231B0D), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Risk Warning",
                    tint = AccentGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trading involves capital risk. Only trade with funds you can afford to lose.",
                    fontSize = 10.sp,
                    color = AccentGold.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }

                Button(
                    onClick = onConfirm,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("confirm_submit_trade_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Execute Trade",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = if (isHighlight) 14.sp else 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) highlightColor else TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
