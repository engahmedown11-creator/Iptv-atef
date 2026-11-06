package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChannelEntity
import com.example.ui.components.CategorySidebar
import com.example.ui.components.tvFocusable
import com.example.ui.navigation.Screen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubscription by appViewModel.activeSubscription.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var onlyFavorites by remember { mutableStateOf(false) }

    val channels by if (activeSubscription != null) {
        if (searchQuery.isNotEmpty()) {
            appViewModel.repository.searchChannels(activeSubscription!!.id, searchQuery)
                .collectAsState(initial = emptyList())
        } else if (onlyFavorites) {
            appViewModel.repository.getFavoriteChannels(activeSubscription!!.id)
                .collectAsState(initial = emptyList())
        } else if (selectedCategory != "الكل") {
            appViewModel.repository.getChannelsByCategory(activeSubscription!!.id, selectedCategory)
                .collectAsState(initial = emptyList())
        } else {
            appViewModel.repository.getChannels(activeSubscription!!.id)
                .collectAsState(initial = emptyList())
        }
    } else {
        remember { mutableStateOf(emptyList<ChannelEntity>()) }
    }

    val categories by if (activeSubscription != null) {
        appViewModel.repository.getCategories(activeSubscription!!.id)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<String>()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_tv_screen")
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("channel_search_input"),
                placeholder = { Text("بحث عن اسم القناة...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Favorites Filter Toggle
            IconButton(
                onClick = { onlyFavorites = !onlyFavorites },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (onlyFavorites) GoldPrimary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (onlyFavorites) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("toggle_favorites_btn")
            ) {
                Icon(
                    imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "المفضلة فقط",
                    tint = if (onlyFavorites) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Side-by-side Layout: Category Sidebar on the side + Channel List
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dedicated Vertical Category Sidebar on the side
            if (categories.isNotEmpty() && searchQuery.isEmpty()) {
                CategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onlyFavorites = onlyFavorites,
                    onSelectAll = {
                        onlyFavorites = false
                        selectedCategory = "الكل"
                    },
                    onSelectFavorites = {
                        onlyFavorites = true
                        selectedCategory = "المفضلة ⭐"
                    },
                    onSelectCategory = { cat ->
                        onlyFavorites = false
                        selectedCategory = cat
                    },
                    totalCount = channels.size
                )
            }

            // Channel List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (activeSubscription == null) "يرجى إضافة اشتراك لعرض القنوات المباشرة"
                                else if (onlyFavorites) "لا توجد قنوات مضافة إلى المفضلة بعد"
                                else "لم يتم العثور على قنوات تطابق البحث",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels, key = { it.localId }) { channel ->
                            ChannelListItem(
                                channel = channel,
                                onPlay = {
                                    playerViewModel.playChannel(channel, channels)
                                    onNavigate(Screen.Player)
                                },
                                onToggleFavorite = {
                                    scope.launch {
                                        appViewModel.repository.toggleChannelFavorite(channel.localId, !channel.isFavorite)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    channel: ChannelEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(14.dp),
                focusScale = 1.04f,
                focusedBorderWidth = 3.dp,
                focusedBorderColor = GoldPrimary
            )
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onToggleFavorite
            )
            .testTag("channel_item_${channel.localId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Logo
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Channel Title and Category
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LiveGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = channel.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_channel_${channel.localId}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "تبديل المفضلة",
                    tint = if (channel.isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
