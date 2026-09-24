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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.NotificationCategory
import com.example.core.model.NotificationItem
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(NotificationCategory.ALL) }

    val filteredList = remember(notifications, selectedCategory) {
        if (selectedCategory == NotificationCategory.ALL) notifications
        else notifications.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("notification_center_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            IconButton(onClick = onMarkAllAsRead, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DoneAll, contentDescription = "Mark All As Read", tint = AccentGold)
            }
        }

        // Category Filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(NotificationCategory.values()) { cat ->
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

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            EmptyPlaceholder(
                title = "No Notifications",
                subtitle = "You are all caught up with security and market alerts."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    NotificationCard(item = item, onClick = { onMarkAsRead(item.id) })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isRead) SurfaceCard else Color(0xFF192233), RoundedCornerShape(10.dp))
            .border(1.dp, if (!item.isRead) AccentGold.copy(alpha = 0.5f) else SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("notification_item_${item.id}"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AccentGold.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (item.category) {
                    NotificationCategory.SECURITY -> Icons.Default.Security
                    NotificationCategory.TRADING -> Icons.Default.TrendingUp
                    NotificationCategory.PAYMENT -> Icons.Default.Payment
                    NotificationCategory.ACCOUNT -> Icons.Default.AccountCircle
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(dateStr, fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(item.message, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
