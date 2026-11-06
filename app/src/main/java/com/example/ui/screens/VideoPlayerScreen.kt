package com.example.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.player.AspectRatioMode
import com.example.player.PlaybackStatus
import com.example.ui.components.tvFocusable
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentItem by playerViewModel.currentItem.collectAsState()
    val playbackStatus by playerViewModel.mediaEngine.playbackStatus.collectAsState()
    val errorMessage by playerViewModel.mediaEngine.errorMessage.collectAsState()
    val currentPositionMs by playerViewModel.mediaEngine.currentPositionMs.collectAsState()
    val durationMs by playerViewModel.mediaEngine.durationMs.collectAsState()
    val aspectRatioMode by playerViewModel.mediaEngine.aspectRatioMode.collectAsState()

    val showControls by playerViewModel.showControls.collectAsState()
    val isLocked by playerViewModel.isLocked.collectAsState()
    val showDrawer by playerViewModel.showChannelDrawer.collectAsState()
    val channelList by playerViewModel.channelList.collectAsState()

    val showAspectDialog by playerViewModel.showAspectDialog.collectAsState()
    val showQualityDialog by playerViewModel.showQualityDialog.collectAsState()
    val showTrackDialog by playerViewModel.showTrackDialog.collectAsState()

    var aspectHudText by remember { mutableStateOf<String?>(null) }
    var isInitialAspectLaunch by remember { mutableStateOf(true) }

    val playerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            playerFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(aspectRatioMode) {
        if (isInitialAspectLaunch) {
            isInitialAspectLaunch = false
        } else {
            aspectHudText = "نسخة العرض: ${aspectRatioMode.labelAr}"
            kotlinx.coroutines.delay(1800L)
            aspectHudText = null
        }
    }

    BackHandler {
        if (showDrawer) {
            playerViewModel.setChannelDrawerVisible(false)
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!showControls) {
                                playerViewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showControls) {
                                if (currentItem?.isLive == true) {
                                    playerViewModel.previousChannel()
                                } else {
                                    playerViewModel.mediaEngine.rewind10()
                                }
                                playerViewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showControls) {
                                if (currentItem?.isLive == true) {
                                    playerViewModel.nextChannel()
                                } else {
                                    playerViewModel.mediaEngine.forward10()
                                }
                                playerViewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!showControls) {
                                playerViewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!showControls) {
                                playerViewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            playerViewModel.togglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            playerViewModel.mediaEngine.resume()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            playerViewModel.mediaEngine.pause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            playerViewModel.mediaEngine.forward10()
                            playerViewModel.setControlsVisible(true)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            playerViewModel.mediaEngine.rewind10()
                            playerViewModel.setControlsVisible(true)
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP,
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (currentItem?.isLive == true) {
                                playerViewModel.nextChannel()
                                playerViewModel.setControlsVisible(true)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (currentItem?.isLive == true) {
                                playerViewModel.previousChannel()
                                playerViewModel.setControlsVisible(true)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .clickable { playerViewModel.toggleControls() }
            .testTag("video_player_screen")
    ) {
        // ExoPlayer Native View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerViewModel.mediaEngine.exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playerViewModel.mediaEngine.exoPlayer
                // Apply Aspect Ratio Mode to PlayerView
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL, AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Aspect Ratio / Display Version HUD Overlay
        val currentHud = aspectHudText
        if (currentHud != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentHud,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Buffering Indicator
        if (playbackStatus == PlaybackStatus.BUFFERING) {
            CircularProgressIndicator(
                color = GoldPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
            )
        }

        // Error Banner
        if (playbackStatus == PlaybackStatus.ERROR && errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD1F2937))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تعذر تشغيل البث",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF87171)
                    )
                }
            }
        }

        // Translucent Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .tvFocusable(shape = CircleShape, focusScale = 1.15f)
                                .testTag("player_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentItem?.title ?: "ATEF IPTV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentItem?.subtitle ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Action Icons with Display Version Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Display Version (نسخة العرض) Interactive Chip
                        Row(
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.08f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { playerViewModel.setAspectDialogVisible(true) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "نسخة العرض",
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "نسخة العرض: ${aspectRatioMode.labelAr}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { playerViewModel.setQualityDialogVisible(true) },
                            modifier = Modifier.tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.HighQuality, contentDescription = "الجودة", tint = Color.White)
                        }
                        IconButton(
                            onClick = { playerViewModel.setTrackDialogVisible(true) },
                            modifier = Modifier.tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "الصوت والترجمة", tint = Color.White)
                        }
                        IconButton(
                            onClick = { playerViewModel.toggleLock() },
                            modifier = Modifier.tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "قفل الشاشة",
                                tint = if (isLocked) GoldPrimary else Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Channel Flipping
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (currentItem?.isLive == true) {
                        IconButton(
                            onClick = { playerViewModel.previousChannel() },
                            modifier = Modifier
                                .size(50.dp)
                                .tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "القناة السابقة", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { playerViewModel.mediaEngine.rewind10() },
                            modifier = Modifier.tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.Replay10, contentDescription = "تأخير 10 ثوان", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .tvFocusable(shape = CircleShape, focusScale = 1.15f)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .clickable { playerViewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackStatus == PlaybackStatus.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل / إيقاف",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    if (currentItem?.isLive == true) {
                        IconButton(
                            onClick = { playerViewModel.nextChannel() },
                            modifier = Modifier
                                .size(50.dp)
                                .tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "القناة التالية", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { playerViewModel.mediaEngine.forward10() },
                            modifier = Modifier.tvFocusable(shape = CircleShape, focusScale = 1.15f)
                        ) {
                            Icon(imageVector = Icons.Default.Forward10, contentDescription = "تقديم 10 ثوان", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                // Bottom Control Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Time or Live indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentItem?.isLive == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LiveGreen)
                                    )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بث حي مباشر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Drawer toggle button
                            Row(
                                modifier = Modifier
                                    .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.08f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { playerViewModel.setChannelDrawerVisible(true) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("قائمة القنوات", color = Color.White, fontSize = 12.sp)
                            }
                        } else {
                            Text(formatTime(currentPositionMs), color = Color.White, fontSize = 12.sp)
                            Text(formatTime(durationMs), color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // VOD Progress Slider
                    if (currentItem?.isLive != true && durationMs > 0L) {
                        Slider(
                            value = currentPositionMs.toFloat(),
                            onValueChange = { pos -> playerViewModel.mediaEngine.seekTo(pos.toLong()) },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        // Fast Channel Switching Drawer (قائمة القنوات السريعة)
        AnimatedVisibility(
            visible = showDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00F172A))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "القنوات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        IconButton(onClick = { playerViewModel.setChannelDrawerVisible(false) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(channelList) { ch ->
                            val isPlaying = ch.channelId == currentItem?.channelId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPlaying) GoldPrimary.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        playerViewModel.playChannel(ch)
                                        playerViewModel.setChannelDrawerVisible(false)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(LiveGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = ch.name,
                                    color = if (isPlaying) GoldPrimary else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Aspect Ratio Selection Dialog (نسخة العرض وأبعاد الشاشة)
    if (showAspectDialog) {
        AlertDialog(
            onDismissRequest = { playerViewModel.setAspectDialogVisible(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخة العرض وأبعاد الشاشة (Display Mode)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val aspectOptions = listOf(
                        AspectRatioMode.FIT to "ملاءمة متناسقة (Fit) - ملائم لجميع الشاشات دون قص",
                        AspectRatioMode.FILL to "ملء الشاشة بالكامل (Fill) - تمديد العرض لكامل الشاشة",
                        AspectRatioMode.ZOOM to "تكبير وتوسيط (Zoom) - إخفاء الأشرطة السوداء الجانبية",
                        AspectRatioMode.ASPECT_16_9 to "شاشة عريضة سينمائية (16:9)",
                        AspectRatioMode.ASPECT_4_3 to "شاشة كلاسيكية قديمة (4:3)",
                        AspectRatioMode.ORIGINAL to "الحجم الأصلي الدقيق لمصدر البث (Original)"
                    )

                    aspectOptions.forEach { (mode, desc) ->
                        val isSelected = aspectRatioMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    if (isSelected) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { playerViewModel.setAspectRatio(mode) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mode.labelAr,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        val qualities by playerViewModel.mediaEngine.availableQualities.collectAsState()
        AlertDialog(
            onDismissRequest = { playerViewModel.setQualityDialogVisible(false) },
            title = { Text("جودة الفيديو") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    qualities.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (q.isSelected) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { playerViewModel.selectQuality(q) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q.displayLabel,
                                fontWeight = if (q.isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (q.isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Audio & Subtitle Dialog
    if (showTrackDialog) {
        val audioTracks by playerViewModel.mediaEngine.availableAudioTracks.collectAsState()
        val subtitleTracks by playerViewModel.mediaEngine.availableSubtitleTracks.collectAsState()

        AlertDialog(
            onDismissRequest = { playerViewModel.setTrackDialogVisible(false) },
            title = { Text("مسارات الصوت والترجمة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مسار الصوت:", fontWeight = FontWeight.Bold, color = GoldPrimary)
                    if (audioTracks.isEmpty()) {
                        Text("مسار الصوت الافتراضي من المصدر", fontSize = 12.sp)
                    } else {
                        audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                                    .clickable { playerViewModel.selectAudioTrack(track.id) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(track.name, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ملف الترجمة:", fontWeight = FontWeight.Bold, color = GoldPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                            .clickable { playerViewModel.selectSubtitleTrack("OFF") }
                            .padding(vertical = 4.dp)
                    ) {
                        Text("إيقاف الترجمة", fontSize = 13.sp)
                    }
                    subtitleTracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.04f)
                                .clickable { playerViewModel.selectSubtitleTrack(track.id) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(track.name, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
