package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.core.model.MarketAsset
import com.example.core.model.MarketCategory
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MarketsScreen(
    markets: List<MarketAsset>,
    onSelectAsset: (MarketAsset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarketCategory.ALL) }
    var onlyFavorites by remember { mutableStateOf(false) }

    val filteredList = remember(markets, searchQuery, selectedCategory, onlyFavorites) {
        markets.filter { asset ->
            val matchesCategory = selectedCategory == MarketCategory.ALL || asset.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    asset.symbol.contains(searchQuery, ignoreCase = true) ||
                    asset.name.contains(searchQuery, ignoreCase = true)
            val matchesFav = !onlyFavorites || asset.isFavorite
            matchesCategory && matchesSearch && matchesFav
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("markets_screen")
    ) {
        Text("Trading Markets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("market_search_input"),
            placeholder = { Text("Search by symbol or asset name...", fontSize = 12.sp, color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onlyFavorites = !onlyFavorites },
                    label = { Text("★ Favorites", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = onlyFavorites,
                        borderColor = if (onlyFavorites) AccentGold else SurfaceBorder
                    )
                )
            }

            items(MarketCategory.values()) { cat ->
                val selected = cat == selectedCategory && !onlyFavorites
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedCategory = cat
                        onlyFavorites = false
                    },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Market Card List
        if (filteredList.isEmpty()) {
            EmptyPlaceholder(
                title = "No Markets Found",
                subtitle = "Try adjusting your search query or filter category."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList, key = { it.symbol }) { asset ->
                    MarketItemCard(
                        asset = asset,
                        onClick = { onSelectAsset(asset) },
                        onToggleFavorite = { onToggleFavorite(asset.symbol) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
