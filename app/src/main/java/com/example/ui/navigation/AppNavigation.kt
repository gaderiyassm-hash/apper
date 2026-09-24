package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.model.AuthState
import com.example.ui.TradingViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.SettledTradeDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileKycScreen
import com.example.ui.screens.TradeScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeUpGreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Markets : Screen("markets", "Markets", Icons.Default.ShowChart)
    object Trade : Screen("trade", "Trade", Icons.Default.CandlestickChart)
    object History : Screen("history", "History", Icons.Default.History)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Home)
}

@Composable
fun AppNavigation(
    viewModel: TradingViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isOnboardingCompleted by remember { mutableStateOf(true) }

    val authState by viewModel.authState.collectAsState()
    val isDemo by viewModel.isDemoMode.collectAsState()
    val balance by viewModel.activeBalance.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val markets by viewModel.markets.collectAsState()
    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val orderBook by viewModel.orderBook.collectAsState()
    val allTrades by viewModel.allTrades.collectAsState()
    val activeTrades by viewModel.activeTrades.collectAsState()
    val lastSettledTrade by viewModel.lastSettledTrade.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val unreadNotifications = remember(notifications) {
        notifications.count { !it.isRead }
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(onFinishOnboarding = { isOnboardingCompleted = true })
        return
    }

    if (authState == AuthState.UNAUTHENTICATED) {
        AuthScreen(
            onLoginSuccess = { email, isDemoSelected ->
                viewModel.login(email, isDemoSelected)
            }
        )
        return
    }

    val navItems = listOf(
        Screen.Home,
        Screen.Markets,
        Screen.Trade,
        Screen.History,
        Screen.Wallet,
        Screen.Profile
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceDark,
        topBar = {
            if (currentScreen != Screen.Notifications) {
                AppHeader(
                    isDemo = isDemo,
                    availableBalance = balance.available,
                    connectionState = connectionState,
                    unreadNotificationCount = unreadNotifications,
                    onToggleDemoMode = { viewModel.toggleDemoMode() },
                    onOpenNotifications = { currentScreen = Screen.Notifications },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.Notifications) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .border(1.dp, SurfaceBorder)
                        .testTag("main_bottom_nav_bar")
                ) {
                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                if (screen == Screen.Trade && activeTrades.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = TradeUpGreen) {
                                                Text(activeTrades.size.toString(), color = Color.White, fontSize = 9.sp)
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGold,
                                selectedTextColor = AccentGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceCard
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        isDemo = isDemo,
                        walletBalance = balance,
                        markets = markets,
                        activeTrades = activeTrades,
                        onToggleDemo = { viewModel.toggleDemoMode() },
                        onRefillDemo = { viewModel.refillDemoBalance() },
                        onNavigateToTrade = { asset ->
                            viewModel.selectAsset(asset)
                            currentScreen = Screen.Trade
                        },
                        onNavigateToMarkets = { currentScreen = Screen.Markets },
                        onNavigateToWallet = { currentScreen = Screen.Wallet },
                        onToggleFavorite = { symbol -> viewModel.toggleFavorite(symbol) }
                    )
                }

                Screen.Markets -> {
                    MarketsScreen(
                        markets = markets,
                        onSelectAsset = { asset ->
                            viewModel.selectAsset(asset)
                            currentScreen = Screen.Trade
                        },
                        onToggleFavorite = { symbol -> viewModel.toggleFavorite(symbol) }
                    )
                }

                Screen.Trade -> {
                    val asset = selectedAsset ?: markets.firstOrNull()
                    if (asset != null) {
                        TradeScreen(
                            currentAsset = asset,
                            allMarkets = markets,
                            availableBalance = balance.available,
                            candles = candles,
                            orderBook = orderBook,
                            activeTrades = activeTrades,
                            onSelectAsset = { viewModel.selectAsset(it) },
                            onTimeframeChange = { viewModel.setTimeframe(it) },
                            onSubmitTrade = { a, dir, amt, dur ->
                                viewModel.submitTrade(a, dir, amt, dur)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                }

                Screen.History -> {
                    HistoryScreen(
                        trades = allTrades,
                        transactions = transactions
                    )
                }

                Screen.Wallet -> {
                    WalletScreen(
                        walletBalance = balance,
                        recentTransactions = transactions,
                        onDeposit = { amt, method -> viewModel.deposit(amt, method) },
                        onWithdraw = { amt, dest, method -> viewModel.withdraw(amt, dest, method) }
                    )
                }

                Screen.Profile -> {
                    ProfileKycScreen(
                        user = userProfile,
                        onUpdateKyc = { viewModel.updateKycStatus(it) },
                        onToggleMfa = { viewModel.toggleMfa(it) },
                        onLogout = { viewModel.logout() }
                    )
                }

                Screen.Notifications -> {
                    NotificationCenterScreen(
                        notifications = notifications,
                        onBack = { currentScreen = Screen.Home },
                        onMarkAsRead = { viewModel.markNotificationAsRead(it) },
                        onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }

            // Real-time Settlement Result Notification Popup Dialog
            lastSettledTrade?.let { settledTrade ->
                SettledTradeDialog(
                    trade = settledTrade,
                    onDismiss = { viewModel.dismissLastSettledTrade() }
                )
            }
        }
    }
}
