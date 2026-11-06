package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.VodItemEntity
import com.example.data.local.entity.VodType
import com.example.data.repository.AiMetadataEngine
import com.example.data.repository.DownloadStatus
import com.example.ui.components.CategorySidebar
import com.example.ui.components.tvFocusable
import com.example.ui.navigation.Screen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun SeriesScreen(
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubscription by appViewModel.activeSubscription.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var selectedSeries by remember { mutableStateOf<VodItemEntity?>(null) }

    val seriesList by if (activeSubscription != null) {
        if (searchQuery.isNotEmpty()) {
            appViewModel.repository.searchVod(activeSubscription!!.id, VodType.SERIES, searchQuery)
                .collectAsState(initial = emptyList())
        } else if (onlyFavorites || selectedCategory == "المفضلة ⭐") {
            appViewModel.repository.getFavoriteVod(activeSubscription!!.id, VodType.SERIES)
                .collectAsState(initial = emptyList())
        } else if (selectedCategory != "الكل") {
            appViewModel.repository.getVodByCategory(activeSubscription!!.id, VodType.SERIES, selectedCategory)
                .collectAsState(initial = emptyList())
        } else {
            appViewModel.repository.getVodItems(activeSubscription!!.id, VodType.SERIES)
                .collectAsState(initial = emptyList())
        }
    } else {
        remember { mutableStateOf(emptyList<VodItemEntity>()) }
    }

    val categories by if (activeSubscription != null) {
        appViewModel.repository.getVodCategories(activeSubscription!!.id, VodType.SERIES)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<String>()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("series_screen")
    ) {
        // Search & Favorites Bar
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
                    .testTag("series_search_input"),
                placeholder = { Text("بحث عن اسم المسلسل باللغة الأصلية...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "بحث", tint = GoldPrimary)
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

            // Favorites Filter Toggle Button
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
                    .testTag("series_favorites_toggle_btn")
            ) {
                Icon(
                    imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "المفضلة فقط",
                    tint = if (onlyFavorites) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Side-by-side Layout: Category Sidebar on the side + Series Grid
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
                    totalCount = seriesList.size
                )
            }

            // Series Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (seriesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (activeSubscription == null) "يرجى إضافة اشتراك لعرض مكتبة المسلسلات"
                                else if (onlyFavorites) "قائمة المسلسلات المفضلة فارغة (اضغط مطولاً على أي مسلسل لإضافته)"
                                else "لم يتم العثور على مسلسلات في هذا التصنيف",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(seriesList, key = { it.localId }) { series ->
                            MoviePosterCard(
                                movie = series,
                                onClick = { selectedSeries = series },
                                onToggleFavorite = {
                                    appViewModel.toggleVodFavorite(series.localId, !series.isFavorite)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Series Detail Dialog with Seasons & Episodes
    if (selectedSeries != null) {
        val series = selectedSeries!!
        SeriesDetailDialog(
            series = series,
            activeSubscription = activeSubscription,
            appViewModel = appViewModel,
            onDismiss = { selectedSeries = null },
            onPlayEpisode = { episode ->
                playerViewModel.playVod(
                    title = "${series.originalTitle} - ${episode.originalTitle}",
                    category = "الموسم ${episode.seasonNumber} - الحلقة ${episode.episodeNumber}",
                    streamUrl = episode.streamUrl,
                    posterUrl = episode.posterUrl.ifEmpty { series.posterUrl },
                    subId = series.subscriptionId,
                    refId = episode.localId,
                    isMovie = false,
                    resumePositionMs = episode.resumePositionMs
                )
                selectedSeries = null
                onNavigate(Screen.Player)
            }
        )
    }
}

@Composable
fun SeriesDetailDialog(
    series: VodItemEntity,
    activeSubscription: com.example.data.local.entity.SubscriptionEntity?,
    appViewModel: AppViewModel,
    onDismiss: () -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayedSynopsis by remember(series.localId) {
        mutableStateOf(
            if (AiMetadataEngine.isGenuinelyArabic(series.arabicOverview)) series.arabicOverview
            else "جاري ترجمة الملخص إلى اللغة العربية..."
        )
    }
    var castMembers by remember(series.localId) { mutableStateOf<List<String>>(emptyList()) }
    var keyThemes by remember(series.localId) { mutableStateOf<List<String>>(emptyList()) }
    var isAiLoading by remember(series.localId) { mutableStateOf(false) }
    var isAiGenerated by remember(series.localId) { mutableStateOf(false) }
    var isFavorite by remember(series.localId) { mutableStateOf(series.isFavorite) }

    val downloadTasks by appViewModel.downloadTasks.collectAsState()

    // Automatic accurate Arabic lookup on launch:
    LaunchedEffect(series.localId) {
        isAiLoading = true
        val res = AiMetadataEngine.getOrGenerateArabicSynopsis(
            title = series.originalTitle,
            category = series.categoryName,
            year = series.releaseYear,
            existingPlot = series.arabicOverview,
            isSeries = true
        )
        displayedSynopsis = res.arabicOverview
        castMembers = res.cast
        keyThemes = res.keyThemes
        isAiGenerated = res.isGeneratedByAi
        isAiLoading = false
        if (series.arabicOverview != res.arabicOverview && AiMetadataEngine.isGenuinelyArabic(res.arabicOverview)) {
            appViewModel.updateVodOverview(series.localId, res.arabicOverview)
        }
    }

    LaunchedEffect(series.streamId) {
        if (activeSubscription != null) {
            appViewModel.repository.fetchSeriesEpisodesIfEmpty(activeSubscription, series.streamId)
        }
    }

    val episodes by appViewModel.repository.getEpisodesForSeries(series.subscriptionId, series.streamId)
        .collectAsState(initial = emptyList())

    val seasons = remember(episodes) {
        episodes.map { it.seasonNumber }.distinct().sorted()
    }

    var selectedSeason by remember(seasons) {
        mutableStateOf(seasons.firstOrNull() ?: 1)
    }

    val seasonEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.seasonNumber == selectedSeason }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = series.originalTitle, // Preserved Original Title
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Meta Row (Poster & Details)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 145.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (series.posterUrl.isNotEmpty()) {
                            AsyncImage(
                                model = series.posterUrl,
                                contentDescription = series.originalTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = GoldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التصنيف: ${series.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (series.releaseYear.isNotEmpty()) {
                            Text(
                                text = "سنة الإنتاج: ${series.releaseYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (series.rating.isNotEmpty()) {
                            Text(
                                text = "التقييم: ★ ${series.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "عدد المواسم: ${seasons.size.coerceAtLeast(1)} • إجمالي الحلقات: ${episodes.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val newFav = !isFavorite
                                isFavorite = newFav
                                appViewModel.toggleVodFavorite(series.localId, newFav)
                            }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "المفضلة",
                                    tint = if (isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isFavorite) "في المفضلة ★" else "إضافة للمفضلة",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Synopsis Action Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAiLoading) {
                            scope.launch {
                                isAiLoading = true
                                val res = AiMetadataEngine.getOrGenerateArabicSynopsis(
                                    title = series.originalTitle,
                                    category = series.categoryName,
                                    year = series.releaseYear,
                                    existingPlot = series.arabicOverview,
                                    isSeries = true
                                )
                                displayedSynopsis = res.arabicOverview
                                castMembers = res.cast
                                keyThemes = res.keyThemes
                                isAiGenerated = res.isGeneratedByAi
                                isAiLoading = false
                                if (AiMetadataEngine.isGenuinelyArabic(res.arabicOverview)) {
                                    appViewModel.updateVodOverview(series.localId, res.arabicOverview)
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "الذكاء الاصطناعي",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAiGenerated) "تم جلب قصة المسلسل والأبطال بالذكاء الاصطناعي (محدث)" else "بحث بالاسم وجلب الوصف الدقيق والأبطال",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GoldPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cast Members Section (أبطال العمل وطاقم التمثيل)
                if (castMembers.isNotEmpty()) {
                    Text(
                        text = "أبطال العمل وطاقم التمثيل:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(castMembers) { actor ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = actor,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Key Themes Tags
                if (keyThemes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keyThemes.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("#$tag", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Dedicated Arabic Reading Card (مكان مخصص ومريح للقراءة الكاملة)
                Text(
                    text = "ملخص وقصة المسلسل (دقيق بدون حرق الأحداث):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = displayedSynopsis.ifEmpty { "مسلسل درامي مشوق متاح بأعلى دقة عبر مشغل ATEF IPTV." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seasons Tabs
                if (seasons.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = seasons.indexOf(selectedSeason).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        seasons.forEach { seasonNum ->
                            Tab(
                                selected = selectedSeason == seasonNum,
                                onClick = { selectedSeason = seasonNum },
                                text = { Text("الموسم $seasonNum") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Episodes Section Header
                Text(
                    text = "حلقات الموسم $selectedSeason (${seasonEpisodes.size} حلقة):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Episodes List with Download Buttons
                if (seasonEpisodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "جاري تحميل قائمة الحلقات أو لم تتوفر بعد...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        seasonEpisodes.forEach { ep ->
                            val epTaskId = "ep_${series.streamId}_${ep.localId}"
                            val epTask = downloadTasks.find { it.id == epTaskId }
                            val isEpCompleted = epTask?.status == DownloadStatus.COMPLETED
                            val isEpDownloading = epTask?.status == DownloadStatus.DOWNLOADING

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(shape = RoundedCornerShape(8.dp), focusScale = 1.03f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPlayEpisode(ep) }
                                    .testTag("episode_item_${ep.localId}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.originalTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (ep.arabicOverview.isNotBlank() && ep.arabicOverview.any { it in '\u0600'..'\u06FF' }) ep.arabicOverview
                                            else "حلقة شيقة من مسلسل ${series.originalTitle} بجودة عالية.",
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Episode Download Button
                                    IconButton(
                                        onClick = {
                                            appViewModel.startDownload(
                                                id = epTaskId,
                                                title = "${series.originalTitle} - S${ep.seasonNumber}E${ep.episodeNumber}",
                                                streamUrl = ep.streamUrl,
                                                isMovie = false
                                            )
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isEpCompleted) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = "تحميل الحلقة",
                                            tint = if (isEpCompleted) LiveGreen else if (isEpDownloading) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل الحلقة",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}
