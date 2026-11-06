package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.ui.components.SupportInfoCard
import com.example.ui.components.tvFocusable
import com.example.ui.navigation.Screen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PlayerViewModel

@Composable
fun DashboardScreen(
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubscription by appViewModel.activeSubscription.collectAsState()

    val continueWatching by if (activeSubscription != null) {
        appViewModel.repository.getContinueWatching(activeSubscription!!.id).collectAsState(initial = emptyList())
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<PlaybackHistoryEntity>()) }
    }

    val favoriteChannels by if (activeSubscription != null) {
        appViewModel.repository.getFavoriteChannels(activeSubscription!!.id).collectAsState(initial = emptyList())
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<ChannelEntity>()) }
    }

    val recentChannels by if (activeSubscription != null) {
        appViewModel.repository.getRecentChannels(activeSubscription!!.id).collectAsState(initial = emptyList())
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<ChannelEntity>()) }
    }

    val remoteConfig by appViewModel.remoteConfig.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Remote Config Alert Banner for new versions or required updates
        if (remoteConfig.isNewVersionAvailable) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(Screen.Support) }
                        .testTag("remote_config_update_banner"),
                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = remoteConfig.updateTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الإصدار المتاح: ${remoteConfig.latestVersion} • اضغط هنا لتنزيل APK والتثبيت المباشر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Zero-State: When no subscription is added
        if (activeSubscription == null) {
            item {
                NoSubscriptionWelcomeCard(
                    onAddSubscription = { onNavigate(Screen.AddSubscription) },
                    onContactSupport = { onNavigate(Screen.Support) }
                )
            }
        } else {
            // Subscription Overview Hero Card
            item {
                SubscriptionHeroCard(
                    subscription = activeSubscription!!,
                    onNavigateLive = { onNavigate(Screen.LiveTv) },
                    onNavigateMovies = { onNavigate(Screen.Movies) },
                    onNavigateSeries = { onNavigate(Screen.Series) },
                    onRefresh = { appViewModel.refreshActiveSubscription() }
                )
            }

            // Continue Watching Section (استكمال المشاهدة)
            if (continueWatching.isNotEmpty()) {
                item {
                    SectionHeader(title = "متابعة المشاهدة", icon = Icons.Default.PlayArrow)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        items(continueWatching, key = { it.historyKey }) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = {
                                    playerViewModel.playVod(
                                        title = item.title,
                                        category = item.subtitle,
                                        streamUrl = item.streamUrl,
                                        posterUrl = item.posterUrl,
                                        subId = item.subscriptionId,
                                        refId = item.referenceId,
                                        isMovie = item.contentType == "MOVIE",
                                        resumePositionMs = item.resumePositionMs
                                    )
                                    onNavigate(Screen.Player)
                                }
                            )
                        }
                    }
                }
            }

            // Favorite Channels Section (القنوات المفضلة)
            if (favoriteChannels.isNotEmpty()) {
                item {
                    SectionHeader(title = "القنوات المفضلة", icon = Icons.Default.Star)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        items(favoriteChannels, key = { it.localId }) { channel ->
                            ChannelBadgeCard(
                                channel = channel,
                                onClick = {
                                    playerViewModel.playChannel(channel, favoriteChannels)
                                    onNavigate(Screen.Player)
                                }
                            )
                        }
                    }
                }
            }

            // Recent Channels Section (آخر القنوات المشاهدة)
            if (recentChannels.isNotEmpty()) {
                item {
                    SectionHeader(title = "آخر القنوات المشاهدة", icon = Icons.Default.LiveTv)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        items(recentChannels, key = { it.localId }) { channel ->
                            ChannelBadgeCard(
                                channel = channel,
                                onClick = {
                                    playerViewModel.playChannel(channel, recentChannels)
                                    onNavigate(Screen.Player)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Official Support Card at the bottom of Dashboard
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SupportInfoCard()
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SubscriptionHeroCard(
    subscription: SubscriptionEntity,
    onNavigateLive: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateSeries: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("subscription_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary.copy(alpha = 0.4f))
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نوع المصدر: ${subscription.type.name} • الصلاحية: ${subscription.expiryDate.ifEmpty { "ساري المفعول" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.08f)
                            .testTag("hero_refresh_sub_btn"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث الاشتراك",
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث البيانات", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LiveGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("نشط ومحدث", color = LiveGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Category Shortcuts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryStatButton(
                    title = "بث مباشر",
                    count = subscription.totalChannels,
                    icon = Icons.Default.LiveTv,
                    onClick = onNavigateLive,
                    modifier = Modifier.weight(1f)
                )
                CategoryStatButton(
                    title = "أفلام",
                    count = subscription.totalMovies,
                    icon = Icons.Default.Movie,
                    onClick = onNavigateMovies,
                    modifier = Modifier.weight(1f)
                )
                CategoryStatButton(
                    title = "مسلسلات",
                    count = subscription.totalSeries,
                    icon = Icons.Default.Tv,
                    onClick = onNavigateSeries,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CategoryStatButton(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .tvFocusable(shape = RoundedCornerShape(12.dp), focusScale = 1.08f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = GoldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count عنصر",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: PlaybackHistoryEntity,
    onClick: () -> Unit
) {
    val progress = if (item.totalDurationMs > 0) {
        (item.resumePositionMs.toFloat() / item.totalDurationMs).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .width(200.dp)
            .tvFocusable(shape = RoundedCornerShape(14.dp), focusScale = 1.06f)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("continue_item_${item.historyKey}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (item.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "استكمال",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = GoldPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.subtitle.isNotEmpty()) item.subtitle else "استكمال المشاهدة",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChannelBadgeCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .tvFocusable(shape = RoundedCornerShape(12.dp), focusScale = 1.08f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("channel_card_${channel.localId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = channel.categoryName,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoSubscriptionWelcomeCard(
    onAddSubscription: () -> Unit,
    onContactSupport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("no_subscription_welcome_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary.copy(alpha = 0.5f))
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "أهلاً بك في مشغل ATEF IPTV",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "التطبيق عبارة عن مشغل وسائط احترافي فقط، ولا يحتوي على قنوات أو أفلام مسبقة. للبدء، يرجى إضافة اشتراكك الشخصي (M3U أو Xtream Codes).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddSubscription,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_add_sub_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إضافة اشتراك جديد الآن",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
