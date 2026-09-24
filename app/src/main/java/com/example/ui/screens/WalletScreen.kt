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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.model.PaymentMethod
import com.example.core.model.TransactionItem
import com.example.core.model.WalletBalance
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
fun WalletScreen(
    walletBalance: WalletBalance,
    recentTransactions: List<TransactionItem>,
    onDeposit: suspend (Double, PaymentMethod) -> Boolean,
    onWithdraw: suspend (Double, String, PaymentMethod) -> Boolean,
    modifier: Modifier = Modifier
) {
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("wallet_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Wallet & Balances", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            // Big Balance Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TOTAL ASSET VALUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%,.2f", walletBalance.total)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Available for Trading", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    "$${String.format("%,.2f", walletBalance.available)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TradeUpGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Locked in Active Trades", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    "$${String.format("%,.2f", walletBalance.locked)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Deposit and Withdraw buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showDepositSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("open_deposit_sheet_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deposit", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showWithdrawSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("open_withdraw_sheet_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Recent Transactions List
            item {
                Text("Recent Transactions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            items(recentTransactions.take(8)) { tx ->
                TransactionHistoryItem(item = tx)
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

    // Deposit Bottom Sheet
    if (showDepositSheet) {
        DepositBottomSheet(
            onDismiss = { showDepositSheet = false },
            onExecute = { amount, method ->
                coroutineScope.launch {
                    val success = onDeposit(amount, method)
                    showDepositSheet = false
                    if (success) {
                        snackbarHostState.showSnackbar("Deposit of $${String.format("%.2f", amount)} successfully credited!")
                    }
                }
            }
        )
    }

    // Withdraw Bottom Sheet
    if (showWithdrawSheet) {
        WithdrawBottomSheet(
            availableBalance = walletBalance.available,
            onDismiss = { showWithdrawSheet = false },
            onExecute = { amount, destination, method ->
                coroutineScope.launch {
                    val success = onWithdraw(amount, destination, method)
                    showWithdrawSheet = false
                    if (success) {
                        snackbarHostState.showSnackbar("Withdrawal request of $${String.format("%.2f", amount)} submitted for processing.")
                    } else {
                        snackbarHostState.showSnackbar("Withdrawal failed: Insufficient funds.")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositBottomSheet(
    onDismiss: () -> Unit,
    onExecute: (Double, PaymentMethod) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by remember { mutableStateOf("250") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CREDIT_DEBIT_CARD) }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("deposit_modal_sheet")
        ) {
            Text("Deposit Funds", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Select payment provider and enter amount", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(14.dp))

            // Amount input
            Text("Deposit Amount ($ USD)", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amountInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_amount_field"),
                prefix = { Text("$", color = AccentGold, fontWeight = FontWeight.Bold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 250, 500, 1000).forEach { p ->
                    Surface(
                        onClick = { amountInput = p.toString() },
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        color = SurfaceCard,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$$p", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Payment Method", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))

            // Payment methods list
            PaymentMethod.values().forEach { method ->
                val isSelected = method == selectedMethod
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(SurfaceCard, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSelected) AccentGold else SurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedMethod = method }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (method) {
                                PaymentMethod.INSTANT_BANK_TRANSFER -> Icons.Default.AccountBalance
                                PaymentMethod.CRYPTO_USDT -> Icons.Default.CurrencyBitcoin
                                PaymentMethod.CREDIT_DEBIT_CARD -> Icons.Default.CreditCard
                                PaymentMethod.UPI_FAST_PAY -> Icons.Default.FlashOn
                            },
                            contentDescription = null,
                            tint = if (isSelected) AccentGold else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(method.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Processing: ${method.processingTime}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = AccentGold, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        isProcessing = true
                        onExecute(amount, selectedMethod)
                    }
                },
                enabled = !isProcessing && (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_deposit_action_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text("Proceed to Deposit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawBottomSheet(
    availableBalance: Double,
    onDismiss: () -> Unit,
    onExecute: (Double, String, PaymentMethod) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by remember { mutableStateOf("100") }
    var destinationInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CRYPTO_USDT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("withdraw_modal_sheet")
        ) {
            Text("Withdraw Funds", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Available: $${String.format("%,.2f", availableBalance)}", fontSize = 12.sp, color = TradeUpGreen)

            Spacer(modifier = Modifier.height(14.dp))

            Text("Withdrawal Amount ($ USD)", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amountInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_field"),
                prefix = { Text("$", color = AccentGold, fontWeight = FontWeight.Bold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Payout Destination (Wallet Address / Account)", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = destinationInput,
                onValueChange = { destinationInput = it },
                placeholder = { Text("e.g. TRC20 address or IBAN", fontSize = 11.sp, color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_destination_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Security PIN
            Text("Security PIN / 2FA Confirmation", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                placeholder = { Text("Enter 4 or 6 digit PIN (e.g. 1234)", fontSize = 11.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_security_pin_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && destinationInput.isNotBlank()) {
                        onExecute(amount, destinationInput, selectedMethod)
                    }
                },
                enabled = destinationInput.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_withdraw_action_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Withdrawal Request", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
