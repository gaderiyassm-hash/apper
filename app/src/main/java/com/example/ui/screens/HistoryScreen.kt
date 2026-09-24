package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.core.model.TradeOutcome
import com.example.core.model.TradeStatus
import com.example.core.model.TransactionItem
import com.example.core.model.TransactionStatus
import com.example.core.model.TransactionType
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    trades: List<Trade>,
    transactions: List<TransactionItem>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trades, 1: Transactions
    var tradeOutcomeFilter by remember { mutableStateOf<TradeOutcome?>(null) }

    val filteredTrades = remember(trades, tradeOutcomeFilter) {
        if (tradeOutcomeFilter == null) trades
        else trades.filter { it.outcome == tradeOutcomeFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("history_screen")
    ) {
        Text("Activity History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = AccentGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Trades (${trades.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Transactions (${transactions.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Trade filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = tradeOutcomeFilter == null,
                        onClick = { tradeOutcomeFilter = null },
                        label = { Text("All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = tradeOutcomeFilter == null,
                            borderColor = if (tradeOutcomeFilter == null) AccentGold else SurfaceBorder
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = tradeOutcomeFilter == TradeOutcome.WIN,
                        onClick = { tradeOutcomeFilter = TradeOutcome.WIN },
                        label = { Text("Won", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TradeUpGreen,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = tradeOutcomeFilter == TradeOutcome.WIN,
                            borderColor = if (tradeOutcomeFilter == TradeOutcome.WIN) TradeUpGreen else SurfaceBorder
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = tradeOutcomeFilter == TradeOutcome.LOSS,
                        onClick = { tradeOutcomeFilter = TradeOutcome.LOSS },
                        label = { Text("Lost", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TradeDownRed,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = tradeOutcomeFilter == TradeOutcome.LOSS,
                            borderColor = if (tradeOutcomeFilter == TradeOutcome.LOSS) TradeDownRed else SurfaceBorder
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = tradeOutcomeFilter == TradeOutcome.PENDING,
                        onClick = { tradeOutcomeFilter = TradeOutcome.PENDING },
                        label = { Text("Active", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPending,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = tradeOutcomeFilter == TradeOutcome.PENDING,
                            borderColor = if (tradeOutcomeFilter == TradeOutcome.PENDING) StatusPending else SurfaceBorder
                        )
                    )
                }
            }

            if (filteredTrades.isEmpty()) {
                EmptyPlaceholder(
                    title = "No Trades Recorded",
                    subtitle = "Execute your first trade in the Trade terminal."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTrades, key = { it.id }) { trade ->
                        TradeHistoryItem(trade = trade)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        } else {
            // Transactions list
            if (transactions.isEmpty()) {
                EmptyPlaceholder(
                    title = "No Transactions Found",
                    subtitle = "Deposits, withdrawals, and settlements will appear here."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionHistoryItem(item = tx)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TradeHistoryItem(trade: Trade) {
    val isCall = trade.direction == TradeDirection.CALL
    val isWin = trade.outcome == TradeOutcome.WIN
    val isTie = trade.outcome == TradeOutcome.TIE
    val isPending = trade.status == TradeStatus.ACTIVE || trade.status == TradeStatus.EXPIRING

    val outcomeColor = when {
        isPending -> StatusPending
        isWin -> TradeUpGreen
        isTie -> AccentGold
        else -> TradeDownRed
    }

    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(trade.createdAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("trade_history_${trade.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (isCall) TradeUpGreen.copy(0.2f) else TradeDownRed.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCall) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = trade.direction.name,
                        tint = if (isCall) TradeUpGreen else TradeDownRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(trade.assetSymbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("$dateStr • ${trade.id}", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }

            // Outcome badge
            Box(
                modifier = Modifier
                    .background(outcomeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = when {
                        isPending -> "ACTIVE"
                        isWin -> "+$${String.format("%.2f", trade.payoutAmount)}"
                        isTie -> "REFUND"
                        else -> "-$${String.format("%.2f", trade.amount)}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = outcomeColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Entry: ${String.format("%.2f", trade.entryPrice)}", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            val settleStr = trade.settlementPrice?.let { String.format("%.2f", it) } ?: "Pending"
            Text("Settle: $settleStr", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Text("Amount: $${String.format("%.2f", trade.amount)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
fun TransactionHistoryItem(item: TransactionItem) {
    val isPositive = item.amount >= 0
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    val statusColor = when (item.status) {
        TransactionStatus.COMPLETED -> StatusSuccess
        TransactionStatus.PENDING, TransactionStatus.PROCESSING -> StatusPending
        TransactionStatus.FAILED, TransactionStatus.REVERSED -> StatusError
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("tx_item_${item.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.type.name.replace("_", " "), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.reference, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Text(dateStr, fontSize = 10.sp, color = TextSecondary)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isPositive) "+" else ""}$${String.format("%,.2f", item.amount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) TradeUpGreen else TradeDownRed,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(item.status.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
    }
}
