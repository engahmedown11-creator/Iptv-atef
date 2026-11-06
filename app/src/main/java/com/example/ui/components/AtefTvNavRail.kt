package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.SubscriptionEntity
import com.example.ui.navigation.Screen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen

@Composable
fun AtefTvNavRail(
    currentRoute: String,
    activeSubscription: SubscriptionEntity?,
    onNavigate: (Screen) -> Unit,
    onRefreshSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(110.dp)
            .testTag("atef_tv_nav_rail"),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TV Logo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.atef_iptv_icon_1793775078559),
                        contentDescription = "ATEF IPTV",
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ATEF IPTV",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )

                // Active Subscription Status & Refresh on TV
                if (activeSubscription != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LiveGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeSubscription.name,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Refresh Subscription Button for TV
                    IconButton(
                        onClick = onRefreshSubscription,
                        modifier = Modifier
                            .size(32.dp)
                            .tvFocusable(shape = CircleShape, focusScale = 1.15f)
                            .padding(top = 2.dp)
                            .testTag("tv_refresh_sub_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث بيانات الاشتراك",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main Navigation destinations
            BottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.screen.route
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onNavigate(item.screen) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.screen.titleAr,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.screen.titleAr,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = GoldPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .tvFocusable(shape = RoundedCornerShape(12.dp), focusScale = 1.08f)
                        .testTag("${item.testTag}_tv")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subscriptions Manager Shortcut (Formerly in top bar)
            val isAddSubSelected = currentRoute == Screen.AddSubscription.route
            NavigationRailItem(
                selected = isAddSubSelected,
                onClick = { onNavigate(Screen.AddSubscription) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "الاشتراكات",
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { Text("الاشتراكات", fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .tvFocusable(shape = RoundedCornerShape(12.dp), focusScale = 1.08f)
                    .testTag("nav_add_sub_tv")
            )

            // Support & Help Shortcut (Formerly in top bar)
            val isSupportSelected = currentRoute == Screen.Support.route
            NavigationRailItem(
                selected = isSupportSelected,
                onClick = { onNavigate(Screen.Support) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "الدعم الفني",
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { Text("الدعم الفني", fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .tvFocusable(shape = RoundedCornerShape(12.dp), focusScale = 1.08f)
                    .testTag("nav_support_tv")
            )
        }
    }
}
