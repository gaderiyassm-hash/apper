package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.model.MarketAsset
import com.example.core.model.MarketCategory
import com.example.core.model.Trade
import com.example.core.model.WalletBalance
import com.example.ui.components.ActiveTradeCard
import com.example.ui.components.RiskDisclosureCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusPending
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen

@Composable
fun HomeScreen(
    isDemo: Boolean,
    walletBalance: WalletBalance,
    markets: List<MarketAsset>,
    activeTrades: List<Trade>,
    onToggleDemo: () -> Unit,
    onRefillDemo: () -> Unit,
    onNavigateToTrade: (MarketAsset) -> Unit,
    onNavigateToMarkets: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(MarketCategory.ALL) }

    val filteredMarkets = remember(markets, selectedCategory) {
        if (selectedCategory == MarketCategory.ALL) markets
        else markets.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Summary Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDemo) "DEMO PRACTICE PORTFOLIO" else "REAL TRADING PORTFOLIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemo) StatusPending else AccentGold,
                            letterSpacing = 1.sp
                        )
                        if (isDemo) {
                            Row(
                                modifier = Modifier
                                    .clickable { onRefillDemo() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Balance",
                                    tint = AccentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refill $10k", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$${String.format("%,.2f", walletBalance.total)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", walletBalance.available)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TradeUpGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("In Active Trades", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", walletBalance.locked)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (walletBalance.locked > 0) AccentGold else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Mode", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                if (isDemo) "Simulated" else "Live Margin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToWallet,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("quick_deposit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deposit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onToggleDemo,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("toggle_account_mode_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isDemo) "Go Real" else "Go Demo", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Active Trades Horizontal Carousel if any
        if (activeTrades.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Positions (${activeTrades.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTrades.take(3).forEach { trade ->
                        val asset = markets.find { it.symbol == trade.assetSymbol }
                        val currentPrice = asset?.currentPrice ?: trade.entryPrice
                        ActiveTradeCard(trade = trade, currentMarketPrice = currentPrice)
                    }
                }
            }
        }

        // Market Overview / Watchlist Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Markets Overview", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "See All",
                    fontSize = 12.sp,
                    color = AccentGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToMarkets() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MarketCategory.values()) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) AccentGold else SurfaceBorder
                        )
                    )
                }
            }
        }

        // Market Asset Cards
        items(filteredMarkets.take(5), key = { it.symbol }) { asset ->
            MarketItemCard(
                asset = asset,
                onClick = { onNavigateToTrade(asset) },
                onToggleFavorite = { onToggleFavorite(asset.symbol) }
            )
        }

        // Mandatory Risk Disclosure Banner
        item {
            RiskDisclosureCard(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MarketItemCard(
    asset: MarketAsset,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isPositive = asset.changePercent24h >= 0
    val trendColor = if (isPositive) TradeUpGreen else TradeDownRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("market_item_${asset.symbol}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left info
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (asset.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Favorite",
                    tint = if (asset.isFavorite) AccentGold else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(asset.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F141E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${(asset.payoutRate * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                }
                Text(asset.name, fontSize = 11.sp, color = TextSecondary)
            }
        }

        // Right info (Price & 24h change pill)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                asset.formatPrice(asset.currentPrice),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .background(trendColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", asset.changePercent24h)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
