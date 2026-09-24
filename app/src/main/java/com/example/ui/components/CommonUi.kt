package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.example.core.network.WsConnectionState
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppHeader(
    isDemo: Boolean,
    availableBalance: Double,
    connectionState: WsConnectionState,
    unreadNotificationCount: Int,
    onToggleDemoMode: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Brand & Connection Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(AccentGold, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "APPER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                ConnectionIndicator(state = connectionState)
            }
        }

        // Balance & Account Mode Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(20.dp))
                    .border(1.dp, if (isDemo) StatusPending.copy(alpha = 0.5f) else StatusSuccess.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable { onToggleDemoMode() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("account_mode_toggle")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isDemo) StatusPending else StatusSuccess, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDemo) "DEMO" else "REAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDemo) StatusPending else StatusSuccess
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$${String.format("%,.2f", availableBalance)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Notification Bell with badge
            BadgedBox(
                badge = {
                    if (unreadNotificationCount > 0) {
                        Badge(containerColor = AccentGold) {
                            Text(
                                text = unreadNotificationCount.toString(),
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("notification_bell_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionIndicator(state: WsConnectionState) {
    val (dotColor, text) = when (state) {
        WsConnectionState.CONNECTED -> StatusSuccess to "LIVE"
        WsConnectionState.CONNECTING -> StatusPending to "CONNECTING"
        WsConnectionState.RECONNECTING -> StatusPending to "RECONNECTING"
        WsConnectionState.DISCONNECTED -> StatusError to "OFFLINE"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 9.sp,
            color = dotColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RiskDisclosureCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Risk Info",
            tint = TextSecondary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Risk Warning: Financial instrument trading involves substantial risk of losing capital. Past performance is no guarantee of future results.",
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun EmptyPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 11.sp, color = TextSecondary)
    }
}
