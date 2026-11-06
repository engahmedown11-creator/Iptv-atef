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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
fun MoviesScreen(
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
    var selectedMovie by remember { mutableStateOf<VodItemEntity?>(null) }

    val movies by if (activeSubscription != null) {
        if (searchQuery.isNotEmpty()) {
            appViewModel.repository.searchVod(activeSubscription!!.id, VodType.MOVIE, searchQuery)
                .collectAsState(initial = emptyList())
        } else if (onlyFavorites) {
            appViewModel.repository.getFavoriteVod(activeSubscription!!.id, VodType.MOVIE)
                .collectAsState(initial = emptyList())
        } else if (selectedCategory == "المفضلة ⭐") {
            appViewModel.repository.getFavoriteVod(activeSubscription!!.id, VodType.MOVIE)
                .collectAsState(initial = emptyList())
        } else if (selectedCategory != "الكل") {
            appViewModel.repository.getVodByCategory(activeSubscription!!.id, VodType.MOVIE, selectedCategory)
                .collectAsState(initial = emptyList())
        } else {
            appViewModel.repository.getVodItems(activeSubscription!!.id, VodType.MOVIE)
                .collectAsState(initial = emptyList())
        }
    } else {
        remember { mutableStateOf(emptyList<VodItemEntity>()) }
    }

    val categories by if (activeSubscription != null) {
        appViewModel.repository.getVodCategories(activeSubscription!!.id, VodType.MOVIE)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<String>()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("movies_screen")
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
                    .testTag("movie_search_input"),
                placeholder = { Text("بحث عن اسم الفيلم باللغة الأصلية...", fontSize = 13.sp) },
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
                    .testTag("movie_favorites_toggle_btn")
            ) {
                Icon(
                    imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "المفضلة فقط",
                    tint = if (onlyFavorites) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Side-by-side Layout: Category Sidebar on the side + Movies Grid
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
                    totalCount = movies.size
                )
            }

            // Movies Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (movies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (activeSubscription == null) "يرجى إضافة اشتراك لعرض مكتبة الأفلام"
                                else if (onlyFavorites) "قائمة الأفلام المفضلة فارغة (اضغط مطولاً على أي فيلم لإضافته)"
                                else "لم يتم العثور على أفلام في هذا التصنيف",
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
                        items(movies, key = { it.localId }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { selectedMovie = movie },
                                onToggleFavorite = {
                                    appViewModel.toggleVodFavorite(movie.localId, !movie.isFavorite)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Movie Detail Dialog
    if (selectedMovie != null) {
        val movie = selectedMovie!!
        MovieDetailDialog(
            movie = movie,
            appViewModel = appViewModel,
            onDismiss = { selectedMovie = null },
            onPlay = { resume ->
                val pos = if (resume) movie.resumePositionMs else 0L
                playerViewModel.playVod(
                    title = movie.originalTitle,
                    category = movie.categoryName,
                    streamUrl = movie.streamUrl,
                    posterUrl = movie.posterUrl,
                    subId = movie.subscriptionId,
                    refId = movie.localId,
                    isMovie = true,
                    resumePositionMs = pos
                )
                selectedMovie = null
                onNavigate(Screen.Player)
            },
            onToggleFavorite = {
                scope.launch {
                    val newFav = !movie.isFavorite
                    appViewModel.toggleVodFavorite(movie.localId, newFav)
                    selectedMovie = movie.copy(isFavorite = newFav)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviePosterCard(
    movie: VodItemEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.08f,
                focusedBorderWidth = 3.dp,
                focusedBorderColor = GoldPrimary
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleFavorite
            )
            .testTag("movie_item_${movie.localId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (movie.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.originalTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Favorite Star Button on Top-Start (Direct tap or indicates long-click status)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (movie.isFavorite) GoldPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (movie.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (movie.isFavorite) "في المفضلة" else "إضافة للمفضلة",
                        tint = if (movie.isFavorite) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Rating Badge on Top-End
                if (movie.rating.isNotEmpty() && movie.rating != "0") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = movie.rating, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                // Strictly preserved original name
                Text(
                    text = movie.originalTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movie.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (movie.isFavorite) {
                        Text(
                            text = "★ مفضل",
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieDetailDialog(
    movie: VodItemEntity,
    appViewModel: AppViewModel,
    onDismiss: () -> Unit,
    onPlay: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayedSynopsis by remember(movie.localId) {
        mutableStateOf(
            if (AiMetadataEngine.isGenuinelyArabic(movie.arabicOverview)) movie.arabicOverview
            else "جاري ترجمة الملخص إلى اللغة العربية..."
        )
    }
    var castMembers by remember(movie.localId) { mutableStateOf<List<String>>(emptyList()) }
    var keyThemes by remember(movie.localId) { mutableStateOf<List<String>>(emptyList()) }
    var isAiLoading by remember(movie.localId) { mutableStateOf(false) }
    var isAiGenerated by remember(movie.localId) { mutableStateOf(false) }

    // Download Tracking
    val downloadTasks by appViewModel.downloadTasks.collectAsState()
    val movieTask = downloadTasks.find { it.id == movie.streamId }
    val isCompleted = movieTask?.status == DownloadStatus.COMPLETED
    val isDownloading = movieTask?.status == DownloadStatus.DOWNLOADING

    // Automatic accurate Arabic lookup on launch:
    LaunchedEffect(movie.localId) {
        isAiLoading = true
        val res = AiMetadataEngine.getOrGenerateArabicSynopsis(
            title = movie.originalTitle,
            category = movie.categoryName,
            year = movie.releaseYear,
            existingPlot = movie.arabicOverview,
            isSeries = false
        )
        displayedSynopsis = res.arabicOverview
        castMembers = res.cast
        keyThemes = res.keyThemes
        isAiGenerated = res.isGeneratedByAi
        isAiLoading = false
        if (movie.arabicOverview != res.arabicOverview && AiMetadataEngine.isGenuinelyArabic(res.arabicOverview)) {
            appViewModel.updateVodOverview(movie.localId, res.arabicOverview)
        }
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
                    text = movie.originalTitle, // Preserved Original Title
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
                // Poster and Meta Info
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(width = 105.dp, height = 150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (movie.posterUrl.isNotEmpty()) {
                            AsyncImage(
                                model = movie.posterUrl,
                                contentDescription = movie.originalTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = GoldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التصنيف: ${movie.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (movie.releaseYear.isNotEmpty()) {
                            Text(
                                text = "سنة الإنتاج: ${movie.releaseYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (movie.rating.isNotEmpty()) {
                            Text(
                                text = "التقييم: ★ ${movie.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (movie.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "المفضلة",
                                    tint = if (movie.isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (movie.isFavorite) "في المفضلة ★ (اضغط مطولاً للإزالة)" else "إضافة للمفضلة",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (movie.isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (movie.isFavorite) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Search & Translation Action Button
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
                                    title = movie.originalTitle,
                                    category = movie.categoryName,
                                    year = movie.releaseYear,
                                    existingPlot = movie.arabicOverview
                                )
                                displayedSynopsis = res.arabicOverview
                                castMembers = res.cast
                                keyThemes = res.keyThemes
                                isAiGenerated = res.isGeneratedByAi
                                isAiLoading = false
                                if (AiMetadataEngine.isGenuinelyArabic(res.arabicOverview)) {
                                    appViewModel.updateVodOverview(movie.localId, res.arabicOverview)
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
                            text = if (isAiGenerated) "تم جلب القصة والأبطال بالذكاء الاصطناعي (محدث)" else "بحث بالاسم وجلب الوصف الدقيق والأبطال",
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

                Spacer(modifier = Modifier.height(12.dp))

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
                    Spacer(modifier = Modifier.height(12.dp))
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

                // Full Arabic Overview (مكان مخصص ومريح للقراءة الكاملة بدون حرق)
                Text(
                    text = "ملخص وقصة الفيلم (دقيق بدون حرق الأحداث):",
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
                        text = displayedSynopsis.ifEmpty { "فيلم سينمائي عالي الجودة متاح للبث المباشر عبر مشغل ATEF IPTV." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Download Button for Movies (إضافة التحميلات)
                OutlinedButton(
                    onClick = {
                        appViewModel.startDownload(
                            id = movie.streamId,
                            title = movie.originalTitle,
                            streamUrl = movie.streamUrl,
                            isMovie = true
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movie_download_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isCompleted) LiveGreen else GoldPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isCompleted) LiveGreen else GoldPrimary)
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = "تحميل الفيلم",
                        tint = if (isCompleted) LiveGreen else GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isCompleted -> "تم التحميل بنجاح (متاح بدون إنترنت) ✓"
                            isDownloading -> "جاري التنزيل (${((movieTask?.progress ?: 0f) * 100).toInt()}%)"
                            else -> "تحميل الفيلم للمشاهدة بدون إنترنت"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (movie.resumePositionMs > 5000L) {
                    Button(
                        onClick = { onPlay(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("movie_resume_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استكمال المشاهدة", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onPlay(false) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("movie_play_start_button")
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("البدء من البداية")
                    }
                } else {
                    Button(
                        onClick = { onPlay(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("movie_play_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهدة الفيلم الآن", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
