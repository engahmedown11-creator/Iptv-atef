package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeviceModeDialog
import com.example.ui.components.SupportInfoCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.DeviceMode

@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by appViewModel.isDarkTheme.collectAsState()
    val deviceMode by appViewModel.deviceMode.collectAsState()
    var showDeviceModeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "الإعدادات العامة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "تخصيص المشغل، وضع العرض، الحساب والخصوصية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Display & UI
        item {
            SettingsCategoryHeader(title = "المظهر وواجهة المستخدم")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    // Dark Mode Toggle
                    SettingsRow(
                        title = "الوضع الداكن (Cinematic Luxury Dark)",
                        subtitle = if (isDarkTheme) "الوضع الليلي الفاخر مفعّل" else "الوضع الفاتح مفعّل",
                        icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        trailing = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { appViewModel.toggleTheme() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldPrimary,
                                    checkedTrackColor = GoldPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    )

                    // Device Mode Selection
                    SettingsRow(
                        title = "نوع واجهة الجهاز",
                        subtitle = deviceMode.titleAr,
                        icon = Icons.Default.Devices,
                        onClick = { showDeviceModeDialog = true }
                    )

                    // Language
                    SettingsRow(
                        title = "لغة التطبيق",
                        subtitle = "العربية (RTL متكامل)",
                        icon = Icons.Default.Language
                    )
                }
            }
        }

        // Section: Media & Storage
        item {
            SettingsCategoryHeader(title = "المشغل والتخزين")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "إدارة مساحة التخزين (Storage Manager)",
                        subtitle = "مراقبة الذاكرة، حجم الكاش والملفات المحملة",
                        icon = Icons.Default.Storage,
                        onClick = { onNavigate(Screen.Storage) }
                    )

                    SettingsRow(
                        title = "إدارة الاشتراكات والمصادر",
                        subtitle = "إضافة وتعديل وحذف اشتراكات M3U و Xtream",
                        icon = Icons.Default.Subscriptions,
                        onClick = { onNavigate(Screen.AddSubscription) }
                    )

                    SettingsRow(
                        title = "إعدادات محرك المشغل (Media3 ExoPlayer)",
                        subtitle = "تدفق سريع، بفر تكيفي، استعادة تلقائية عند التقطع",
                        icon = Icons.Default.PlayCircle
                    )
                }
            }
        }

        // Section: Privacy & About
        item {
            SettingsCategoryHeader(title = "الأمان والخصوصية")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "الخصوصية وحماية البيانات",
                        subtitle = "بيانات اشتراكاتك وكلمات المرور مخزنة محلياً ومشفرة",
                        icon = Icons.Default.Security
                    )

                    SettingsRow(
                        title = "حول تطبيق ATEF IPTV",
                        subtitle = "الإصدار 1.0.0 (Premium) • مشغل وسائط احترافي",
                        icon = Icons.Default.Info,
                        onClick = { onNavigate(Screen.Support) }
                    )
                }
            }
        }

        // Official Support Card
        item {
            SettingsCategoryHeader(title = "المساعدة والدعم الفني")
        }

        item {
            SupportInfoCard()
        }
    }

    if (showDeviceModeDialog) {
        DeviceModeDialog(
            initialMode = deviceMode,
            onConfirm = { mode ->
                appViewModel.setDeviceMode(mode)
                showDeviceModeDialog = false
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = GoldPrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
