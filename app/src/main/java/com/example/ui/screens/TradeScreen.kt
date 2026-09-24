package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CandleStick
import com.example.core.model.MarketAsset
import com.example.core.model.TimeFrame
import com.example.core.model.Trade
import com.example.core.model.TradeDirection
import com.example.core.network.MarketPriceEngine
import com.example.core.network.OrderBookDepth
import com.example.core.repository.TradeSubmissionResult
import com.example.ui.components.ActiveTradeCard
import com.example.ui.components.InteractiveCandleChart
import com.example.ui.components.OrderBookPreview
import com.example.ui.components.TradeConfirmationSheet
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    currentAsset: MarketAsset,
    allMarkets: List<MarketAsset>,
    availableBalance: Double,
    candles: List<CandleStick>,
    orderBook: OrderBookDepth,
    activeTrades: List<Trade>,
    onSelectAsset: (MarketAsset) -> Unit,
    onTimeframeChange: (TimeFrame) -> Unit,
    onSubmitTrade: suspend (MarketAsset, TradeDirection, Double, Long) -> TradeSubmissionResult,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(TimeFrame.TF_1M) }
    var selectedDurationSeconds by remember { mutableLongStateOf(60L) } // default 1m
    var amountInput by remember { mutableStateOf("50") }
    var showAssetDropdown by remember { mutableStateOf(false) }
    var showOrderBook by remember { mutableStateOf(false) }

    // Trade confirmation state
    var pendingTradeDirection by remember { mutableStateOf<TradeDirection?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Flash price background color on price ticks
    var prevPrice by remember { mutableDoubleStateOf(currentAsset.currentPrice) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(currentAsset.currentPrice) {
        if (currentAsset.currentPrice > prevPrice) {
            flashColor = TradeUpGreen.copy(alpha = 0.25f)
        } else if (currentAsset.currentPrice < prevPrice) {
            flashColor = TradeDownRed.copy(alpha = 0.25f)
        }
        prevPrice = currentAsset.currentPrice
        kotlinx.coroutines.delay(400)
        flashColor = Color.Transparent
    }

    val animatedFlash by animateColorAsState(targetValue = flashColor, animationSpec = tween(300))

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val potentialProfit = parsedAmount * currentAsset.payoutRate
    val potentialPayout = parsedAmount + potentialProfit

    val durations = listOf(
        30L to "30s",
        60L to "1m",
        120L to "2m",
        300L to "5m",
        900L to "15m"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .testTag("trade_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Asset Picker Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .background(SurfaceCard, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { showAssetDropdown = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("asset_picker_dropdown"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentAsset.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Asset", tint = TextSecondary)
                        }

                        DropdownMenu(
                            expanded = showAssetDropdown,
                            onDismissRequest = { showAssetDropdown = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            allMarkets.forEach { asset ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(asset.symbol, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                asset.formatPrice(asset.currentPrice),
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectAsset(asset)
                                        showAssetDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Ticking Price Strip
                    Row(
                        modifier = Modifier
                            .background(animatedFlash, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentAsset.formatPrice(currentAsset.currentPrice),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Action buttons (Depth toggle & favorite)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showOrderBook = !showOrderBook },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = "Order Depth",
                                tint = if (showOrderBook) AccentGold else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onToggleFavorite(currentAsset.symbol) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (currentAsset.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Favorite",
                                tint = if (currentAsset.isFavorite) AccentGold else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Chart
            item {
                InteractiveCandleChart(
                    candles = candles,
                    currentPrice = currentAsset.currentPrice,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeSelected = { tf ->
                        selectedTimeframe = tf
                        onTimeframeChange(tf)
                    },
                    priceDecimals = currentAsset.priceDecimals,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }

            // Collapsible Order Book Depth
            if (showOrderBook) {
                item {
                    OrderBookPreview(depth = orderBook, priceDecimals = currentAsset.priceDecimals)
                }
            }

            // Duration Selector Strip
            item {
                Column {
                    Text("Duration / Expiry", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durations.forEach { (sec, label) ->
                            val isSelected = sec == selectedDurationSeconds
                            Surface(
                                onClick = { selectedDurationSeconds = sec },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AccentGold else SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentGold else SurfaceBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Investment Amount Input & Quick Percentage Buttons
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Investment Amount", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Available: $${String.format("%,.2f", availableBalance)}",
                            fontSize = 11.sp,
                            color = TradeUpGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { input ->
                            // Allow numbers and decimal point only
                            if (input.all { it.isDigit() || it == '.' }) {
                                amountInput = input
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trade_amount_input"),
                        prefix = { Text("$", color = AccentGold, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickAmounts = listOf(10, 50, 100, 250)
                        quickAmounts.forEach { q ->
                            Surface(
                                onClick = { amountInput = q.toString() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+$q", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                        Surface(
                            onClick = {
                                val half = (availableBalance / 2).toInt()
                                if (half > 0) amountInput = half.toString()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("50%", fontSize = 11.sp, color = AccentGold)
                            }
                        }
                    }
                }
            }

            // Potential Payout Calculation Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F141E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Payout Rate", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            "+${(currentAsset.payoutRate * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Potential Return", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            "$${String.format("%,.2f", potentialPayout)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TradeUpGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Primary Execution Buttons: CALL (UP) & PUT (DOWN)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CALL (UP) Button
                    Button(
                        onClick = { pendingTradeDirection = TradeDirection.CALL },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("trade_call_up_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TradeUpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("CALL", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("UP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    // PUT (DOWN) Button
                    Button(
                        onClick = { pendingTradeDirection = TradeDirection.PUT },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("trade_put_down_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TradeDownRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Put", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("PUT", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("DOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // Active Positions for this asset
            val currentAssetActiveTrades = activeTrades.filter { it.assetSymbol == currentAsset.symbol }
            if (currentAssetActiveTrades.isNotEmpty()) {
                item {
                    Text(
                        "Active Positions (${currentAssetActiveTrades.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                items(currentAssetActiveTrades) { trade ->
                    ActiveTradeCard(
                        trade = trade,
                        currentMarketPrice = currentAsset.currentPrice
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }

    // Trade Confirmation Modal Sheet
    pendingTradeDirection?.let { direction ->
        TradeConfirmationSheet(
            sheetState = sheetState,
            asset = currentAsset,
            direction = direction,
            amount = parsedAmount,
            durationSeconds = selectedDurationSeconds,
            isSubmitting = isSubmitting,
            onConfirm = {
                coroutineScope.launch {
                    isSubmitting = true
                    val result = onSubmitTrade(currentAsset, direction, parsedAmount, selectedDurationSeconds)
                    isSubmitting = false
                    pendingTradeDirection = null

                    when (result) {
                        is TradeSubmissionResult.Success -> {
                            snackbarHostState.showSnackbar("Trade submitted successfully! Order #${result.trade.id}")
                        }
                        is TradeSubmissionResult.Error -> {
                            snackbarHostState.showSnackbar("Error: ${result.message}")
                        }
                    }
                }
            },
            onDismiss = {
                if (!isSubmitting) pendingTradeDirection = null
            }
        )
    }
}
