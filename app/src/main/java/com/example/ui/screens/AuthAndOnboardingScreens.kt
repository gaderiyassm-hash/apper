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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeUpGreen

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    val steps = listOf(
        Triple("Real-Time Financial Markets", "Stream institutional-grade market data with ultra-low latency WebSocket charts.", Icons.Default.CandlestickChart),
        Triple("Risk-Free $10,000 Demo Practice", "Master algorithmic and manual market timing before risking your capital. Refill anytime.", Icons.Default.Speed),
        Triple("Institutional Security & KYC", "Two-factor authentication, end-to-end encryption, and compliant identity verification.", Icons.Default.Shield)
    )

    val current = steps[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinishOnboarding) {
                Text("Skip", color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(AccentGold.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, AccentGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = current.third,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = current.first,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = current.second,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == step) 20.dp else 8.dp, 8.dp)
                            .background(if (index == step) AccentGold else SurfaceBorder, CircleShape)
                    )
                }
            }
        }

        // Bottom CTA
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (step < steps.size - 1) {
                        step++
                    } else {
                        onFinishOnboarding()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_next_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (step < steps.size - 1) "Next" else "Start Trading",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trading involves financial risk. Never invest funds you cannot afford to lose.",
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AuthScreen(
    onLoginSuccess: (email: String, isDemo: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var emailInput by remember { mutableStateOf("trader@apper.io") }
    var passwordInput by remember { mutableStateOf("SecureTrade2026!") }
    var referralCode by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(20.dp)
            .testTag("auth_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Crest
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AccentGold, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("A", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("APPER TRADING", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = 2.sp)
        Text("Institutional Speed • Retail Precision", fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCardElevated,
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
                        text = { Text("Log In", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Register", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email
                Text("Email Address / Mobile", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_field"),
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                Text("Password", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_field"),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    visualTransformation = PasswordVisualTransformation(),
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

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Referral Code (Optional)", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it },
                        placeholder = { Text("e.g. VIP-888", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentGold)
                        )
                        Text("I agree to Trading Terms & Risk Disclosure", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit button
                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                            onLoginSuccess(emailInput, false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("auth_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) "Sign In to Real Account" else "Create Real Trading Account",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Practice Demo Button
                Button(
                    onClick = {
                        onLoginSuccess("demo.trader@apper.io", true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("auth_demo_access_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TradeUpGreen.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TradeUpGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Instant Demo Practice ($10,000)", color = TradeUpGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
