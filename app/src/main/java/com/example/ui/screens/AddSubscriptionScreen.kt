package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SubscriptionType
import com.example.data.repository.SyncProgressState
import com.example.ui.components.SupportInfoCard
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun AddSubscriptionScreen(
    appViewModel: AppViewModel,
    onSubscriptionAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Xtream Codes, 1: M3U URL, 2: Active List
    val syncState by appViewModel.syncState.collectAsState()
    val allSubscriptions by appViewModel.allSubscriptions.collectAsState()

    // Xtream State
    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // M3U State
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_subscription_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "إدارة الاشتراكات والمصادر",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = GoldPrimary
            )
            Text(
                text = "يدعم التطبيق Xtream Codes API وقوائم تشغيل M3U/M3U8 المشفرة والآمنة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; appViewModel.resetSyncState() },
                    text = { Text("Xtream Codes") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; appViewModel.resetSyncState() },
                    text = { Text("رابط M3U") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; appViewModel.resetSyncState() },
                    text = { Text("الاشتراكات (${allSubscriptions.size})") }
                )
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            // Xtream Codes Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = { Text("اسم الاشتراك (اختياري)") },
                            placeholder = { Text("مثال: اشتراكي الخاص") },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("رابط الخادم (Server URL)") },
                            placeholder = { Text("http://example.com:8080") },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = GoldPrimary) },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_server_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("اسم المستخدم (Username)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_user_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("كلمة المرور (Password)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار كلمة المرور"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("xtream_pass_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                appViewModel.addXtream(xtreamName, xtreamServer, xtreamUser, xtreamPass)
                            },
                            enabled = xtreamServer.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank() && syncState !is SyncProgressState.Progress,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("submit_xtream_btn")
                        ) {
                            Text("ربط ومزامنة الاشتراك", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // M3U URL Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = { Text("اسم الاشتراك (اختياري)") },
                            placeholder = { Text("مثال: قائمة قنواتي M3U") },
                            modifier = Modifier.fillMaxWidth().testTag("m3u_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("رابط ملف M3U أو M3U8") },
                            placeholder = { Text("http://example.com/playlist.m3u") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary) },
                            modifier = Modifier.fillMaxWidth().testTag("m3u_url_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                appViewModel.addM3u(m3uName, m3uUrl, isStream = true)
                            },
                            enabled = m3uUrl.isNotBlank() && syncState !is SyncProgressState.Progress,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("submit_m3u_btn")
                        ) {
                            Text("تحميل وحفظ القائمة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        } else {
            // All Subscriptions List
            if (allSubscriptions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد اشتراكات مضافة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(allSubscriptions, key = { it.id }) { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sub.isActive) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (sub.isActive) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary)
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (sub.isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(LiveGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("نشط", color = LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "النوع: ${sub.type.name} • ${sub.totalChannels} قناة • ${sub.totalMovies} فيلم • ${sub.totalSeries} مسلسل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { appViewModel.refreshSubscription(sub.id) },
                                    modifier = Modifier.testTag("refresh_sub_${sub.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحديث بيانات الاشتراك",
                                        tint = GoldPrimary
                                    )
                                }
                                if (!sub.isActive) {
                                    Button(
                                        onClick = { appViewModel.switchSubscription(sub.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                    ) {
                                        Text("تفعيل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                IconButton(onClick = { appViewModel.deleteSubscription(sub.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الاشتراك", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sync Progress / Feedback Banner
        item {
            when (val state = syncState) {
                is SyncProgressState.Progress -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GoldPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.step, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                is SyncProgressState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiveGreen.copy(alpha = 0.15f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LiveGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                is SyncProgressState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                SyncProgressState.Idle -> {}
            }
        }

        // Support Info
        item {
            SupportInfoCard()
        }
    }
}
