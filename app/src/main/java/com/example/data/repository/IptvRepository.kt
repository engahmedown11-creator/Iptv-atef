package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelProfileEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.SubscriptionType
import com.example.data.local.entity.VodItemEntity
import com.example.data.local.entity.VodType
import com.example.data.parser.EpgParser
import com.example.data.parser.M3uParser
import com.example.data.parser.XtreamAuthResult
import com.example.data.parser.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream

sealed class SyncProgressState {
    object Idle : SyncProgressState()
    data class Progress(val step: String, val percent: Float = 0f) : SyncProgressState()
    data class Success(val message: String) : SyncProgressState()
    data class Error(val message: String) : SyncProgressState()
}

class IptvRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val xtreamClient: XtreamClient = XtreamClient()
) {
    private val subscriptionDao = db.subscriptionDao()
    private val channelDao = db.channelDao()
    private val channelProfileDao = db.channelProfileDao()
    private val vodDao = db.vodDao()
    private val playbackHistoryDao = db.playbackHistoryDao()
    private val epgDao = db.epgDao()

    val activeSubscriptionFlow: Flow<SubscriptionEntity?> = subscriptionDao.observeActiveSubscription()
    val allSubscriptionsFlow: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    suspend fun getActiveSubscription(): SubscriptionEntity? = subscriptionDao.getActiveSubscription()

    suspend fun setActiveSubscription(id: Long) {
        subscriptionDao.deactivateAllSubscriptions()
        subscriptionDao.setActiveSubscription(id)
    }

    suspend fun deleteSubscription(id: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsBySubscription(id)
        vodDao.deleteVodBySubscription(id)
        vodDao.deleteEpisodesBySubscription(id)
        playbackHistoryDao.clearHistoryForSubscription(id)
        subscriptionDao.deleteSubscriptionById(id)
    }

    // Channels
    fun getChannels(subId: Long): Flow<List<ChannelEntity>> = channelDao.getChannelsBySubscription(subId)
    fun getCategories(subId: Long): Flow<List<String>> = channelDao.getCategoriesBySubscription(subId)
    fun getChannelsByCategory(subId: Long, category: String): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByCategory(subId, category)
    fun getFavoriteChannels(subId: Long): Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels(subId)
    fun getRecentChannels(subId: Long): Flow<List<ChannelEntity>> = channelDao.getRecentChannels(subId)
    fun searchChannels(subId: Long, query: String): Flow<List<ChannelEntity>> = channelDao.searchChannels(subId, query)

    suspend fun toggleChannelFavorite(localId: Long, isFav: Boolean) {
        channelDao.setFavorite(localId, isFav)
    }

    suspend fun markChannelWatched(localId: Long) {
        channelDao.updateLastWatched(localId, System.currentTimeMillis())
    }

    // Per-Channel Playback Profile
    suspend fun getChannelProfile(subId: Long, channelId: String): ChannelProfileEntity {
        val key = "sub_${subId}_ch_$channelId"
        return channelProfileDao.getProfile(key) ?: ChannelProfileEntity(profileKey = key)
    }

    suspend fun saveChannelProfile(profile: ChannelProfileEntity) {
        channelProfileDao.saveProfile(profile)
    }

    // VOD
    fun getVodItems(subId: Long, type: VodType): Flow<List<VodItemEntity>> = vodDao.getVodItemsByType(subId, type)
    fun getVodCategories(subId: Long, type: VodType): Flow<List<String>> = vodDao.getVodCategories(subId, type)
    fun getVodByCategory(subId: Long, type: VodType, cat: String): Flow<List<VodItemEntity>> =
        vodDao.getVodByCategory(subId, type, cat)
    fun getFavoriteVod(subId: Long, type: VodType): Flow<List<VodItemEntity>> = vodDao.getFavoriteVod(subId, type)
    fun searchVod(subId: Long, type: VodType, query: String): Flow<List<VodItemEntity>> =
        vodDao.searchVod(subId, type, query)
    fun searchAllVod(subId: Long, query: String): Flow<List<VodItemEntity>> =
        vodDao.searchAllVod(subId, query)

    suspend fun getVodById(localId: Long): VodItemEntity? = vodDao.getVodById(localId)
    suspend fun toggleVodFavorite(localId: Long, isFav: Boolean) = vodDao.setVodFavorite(localId, isFav)
    suspend fun updateVodOverview(localId: Long, overview: String) = vodDao.updateVodOverview(localId, overview)

    // Series episodes
    fun getEpisodesForSeries(subId: Long, seriesStreamId: String): Flow<List<EpisodeEntity>> =
        vodDao.getEpisodesForSeries(subId, seriesStreamId)

    suspend fun fetchSeriesEpisodesIfEmpty(sub: SubscriptionEntity, seriesStreamId: String) = withContext(Dispatchers.IO) {
        if (sub.type == SubscriptionType.XTREAM) {
            val episodes = xtreamClient.fetchSeriesEpisodes(
                sub.serverUrl,
                sub.getDecryptedUsername(),
                sub.getDecryptedPassword(),
                sub.id,
                seriesStreamId
            )
            if (episodes.isNotEmpty()) {
                vodDao.insertEpisodes(episodes)
            }
        }
    }

    // Playback History & Resume
    fun getContinueWatching(subId: Long): Flow<List<PlaybackHistoryEntity>> = playbackHistoryDao.getContinueWatching(subId)

    suspend fun savePlaybackProgress(
        subId: Long,
        title: String,
        subtitle: String,
        posterUrl: String,
        streamUrl: String,
        contentType: String,
        refId: Long,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (positionMs <= 5000L || durationMs <= 0L) return@withContext

        val key = "${contentType.lowercase()}_$refId"
        val item = PlaybackHistoryEntity(
            historyKey = key,
            subscriptionId = subId,
            title = title,
            subtitle = subtitle,
            posterUrl = posterUrl,
            streamUrl = streamUrl,
            contentType = contentType,
            referenceId = refId,
            resumePositionMs = positionMs,
            totalDurationMs = durationMs,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        playbackHistoryDao.saveHistory(item)

        if (contentType == "MOVIE") {
            vodDao.updateVodResume(refId, positionMs, durationMs, System.currentTimeMillis())
        } else if (contentType == "SERIES_EPISODE") {
            vodDao.updateEpisodeResume(refId, positionMs, durationMs, System.currentTimeMillis())
        }
    }

    // EPG
    suspend fun getUpcomingPrograms(epgChannelId: String) =
        epgDao.getUpcomingPrograms(epgChannelId, System.currentTimeMillis())

    suspend fun getCurrentProgram(epgChannelId: String) =
        epgDao.getCurrentProgram(epgChannelId, System.currentTimeMillis())

    // Subscription Imports
    suspend fun addM3uSubscription(
        name: String,
        m3uUrlOrContent: String,
        isStreamUrl: Boolean,
        onProgress: (SyncProgressState) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        try {
            onProgress(SyncProgressState.Progress("جاري حفظ بيانات الاشتراك...", 0.1f))
            subscriptionDao.deactivateAllSubscriptions()

            val sub = SubscriptionEntity(
                name = name.ifEmpty { "اشتراك M3U" },
                type = SubscriptionType.M3U,
                serverUrl = if (isStreamUrl) m3uUrlOrContent else "مستند محلي",
                m3uUrl = if (isStreamUrl) m3uUrlOrContent else "",
                isActive = true
            )
            val subId = subscriptionDao.insertSubscription(sub)

            onProgress(SyncProgressState.Progress("جاري جلب وتحليل قائمة M3U...", 0.3f))
            val stream: InputStream = if (isStreamUrl) {
                val req = Request.Builder().url(m3uUrlOrContent).build()
                val resp = OkHttpClient().newCall(req).execute()
                if (!resp.isSuccessful) {
                    onProgress(SyncProgressState.Error("فشل تحميل قائمة القنوات (كود ${resp.code})"))
                    return@withContext subId
                }
                resp.body?.byteStream() ?: throw Exception("استجابة الرابط فارغة")
            } else {
                m3uUrlOrContent.byteInputStream()
            }

            val parsed = M3uParser.parse(stream, subId)
            stream.close()

            onProgress(SyncProgressState.Progress("جاري حفظ القنوات في قاعدة البيانات...", 0.7f))
            channelDao.insertChannels(parsed.channels)
            vodDao.insertVodItems(parsed.vodItems)

            val updatedSub = sub.copy(
                id = subId,
                totalChannels = parsed.channels.size,
                totalMovies = parsed.vodItems.count { it.type == VodType.MOVIE },
                totalSeries = parsed.vodItems.count { it.type == VodType.SERIES }
            )
            subscriptionDao.updateSubscription(updatedSub)

            onProgress(SyncProgressState.Success("تمت إضافة ${parsed.channels.size} قناة و ${parsed.vodItems.size} محتوى بنجاح!"))
            subId
        } catch (e: Exception) {
            onProgress(SyncProgressState.Error("خطأ في إضافة الاشتراك: ${e.localizedMessage ?: "تأكد من صحة الرابط"}"))
            -1L
        }
    }

    suspend fun addXtreamSubscription(
        name: String,
        serverUrl: String,
        user: String,
        pass: String,
        onProgress: (SyncProgressState) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        try {
            onProgress(SyncProgressState.Progress("جاري التحقق من بيانات الاتصال...", 0.1f))
            val auth = xtreamClient.authenticate(serverUrl, user, pass)
            if (!auth.isValid) {
                onProgress(SyncProgressState.Error(auth.errorMessage ?: "فشل تسجيل الدخول للاشتراك"))
                return@withContext -1L
            }

            subscriptionDao.deactivateAllSubscriptions()
            val sub = SubscriptionEntity(
                name = name.ifEmpty { "اشتراك Xtream" },
                type = SubscriptionType.XTREAM,
                serverUrl = serverUrl,
                username = com.example.data.security.CryptoManager.encrypt(user),
                password = com.example.data.security.CryptoManager.encrypt(pass),
                isActive = true,
                expiryDate = auth.expDate,
                maxConnections = auth.maxConnections
            )
            val subId = subscriptionDao.insertSubscription(sub)

            onProgress(SyncProgressState.Progress("جاري تحميل قائمة البث المباشر...", 0.3f))
            val liveChannels = xtreamClient.fetchLiveStreams(serverUrl, user, pass, subId)
            channelDao.insertChannels(liveChannels)

            onProgress(SyncProgressState.Progress("جاري تحميل قائمة الأفلام...", 0.6f))
            val movies = xtreamClient.fetchVodMovies(serverUrl, user, pass, subId)
            vodDao.insertVodItems(movies)

            onProgress(SyncProgressState.Progress("جاري تحميل قائمة المسلسلات...", 0.85f))
            val series = xtreamClient.fetchSeries(serverUrl, user, pass, subId)
            vodDao.insertVodItems(series)

            val updatedSub = sub.copy(
                id = subId,
                totalChannels = liveChannels.size,
                totalMovies = movies.size,
                totalSeries = series.size
            )
            subscriptionDao.updateSubscription(updatedSub)

            onProgress(SyncProgressState.Success("تم ربط الاشتراك بنجاح: ${liveChannels.size} قناة، ${movies.size} فيلم، ${series.size} مسلسل."))
            subId
        } catch (e: Exception) {
            onProgress(SyncProgressState.Error("خطأ: ${e.localizedMessage ?: "تعذر إكمال مزامنة الاشتراك"}"))
            -1L
        }
    }

    suspend fun syncEpgForActiveSubscription(epgUrl: String, onProgress: (SyncProgressState) -> Unit) = withContext(Dispatchers.IO) {
        try {
            onProgress(SyncProgressState.Progress("جاري تنزيل دليل البرامج XMLTV...", 0.3f))
            val req = Request.Builder().url(epgUrl).build()
            val resp = OkHttpClient().newCall(req).execute()
            if (!resp.isSuccessful) {
                onProgress(SyncProgressState.Error("فشل تحميل دليل البرامج (كود ${resp.code})"))
                return@withContext
            }
            val stream = resp.body?.byteStream() ?: return@withContext
            onProgress(SyncProgressState.Progress("جاري معالجة برامج الدليل...", 0.6f))
            val programs = EpgParser.parse(stream)
            stream.close()
            if (programs.isNotEmpty()) {
                epgDao.insertPrograms(programs)
                onProgress(SyncProgressState.Success("تم تحديث دليل البرامج بنجاح (${programs.size} برنامج)."))
            } else {
                onProgress(SyncProgressState.Error("لم يتم العثور على برامج صالحة في ملف EPG."))
            }
        } catch (e: Exception) {
            onProgress(SyncProgressState.Error("خطأ في تحديث EPG: ${e.localizedMessage}"))
        }
    }

    suspend fun refreshSubscription(subId: Long, onProgress: (SyncProgressState) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val sub = subscriptionDao.getSubscriptionById(subId) ?: run {
                onProgress(SyncProgressState.Error("لم يتم العثور على الاشتراك المحدد"))
                return@withContext
            }

            onProgress(SyncProgressState.Progress("جاري الاتصال بالسيرفر والتحقق من التحديثات...", 0.1f))

            if (sub.type == SubscriptionType.XTREAM) {
                val user = sub.getDecryptedUsername()
                val pass = sub.getDecryptedPassword()
                val auth = xtreamClient.authenticate(sub.serverUrl, user, pass)
                if (!auth.isValid) {
                    onProgress(SyncProgressState.Error(auth.errorMessage ?: "فشل التحقق من بيانات الاشتراك"))
                    return@withContext
                }

                onProgress(SyncProgressState.Progress("جاري تحديث قائمة البث المباشر...", 0.3f))
                val liveChannels = xtreamClient.fetchLiveStreams(sub.serverUrl, user, pass, subId)
                channelDao.deleteChannelsBySubscription(subId)
                channelDao.insertChannels(liveChannels)

                onProgress(SyncProgressState.Progress("جاري تحديث قائمة الأفلام...", 0.6f))
                val movies = xtreamClient.fetchVodMovies(sub.serverUrl, user, pass, subId)
                vodDao.deleteVodBySubscription(subId)
                vodDao.insertVodItems(movies)

                onProgress(SyncProgressState.Progress("جاري تحديث قائمة المسلسلات...", 0.85f))
                val series = xtreamClient.fetchSeries(sub.serverUrl, user, pass, subId)
                vodDao.insertVodItems(series)

                val updatedSub = sub.copy(
                    expiryDate = auth.expDate,
                    maxConnections = auth.maxConnections,
                    totalChannels = liveChannels.size,
                    totalMovies = movies.size,
                    totalSeries = series.size,
                    lastUpdated = System.currentTimeMillis()
                )
                subscriptionDao.updateSubscription(updatedSub)

                onProgress(SyncProgressState.Success("تم تحديث البيانات بنجاح: ${liveChannels.size} قناة، ${movies.size} فيلم، ${series.size} مسلسل."))
            } else {
                if (sub.m3uUrl.isNotEmpty() && (sub.m3uUrl.startsWith("http://") || sub.m3uUrl.startsWith("https://"))) {
                    onProgress(SyncProgressState.Progress("جاري تنزيل قائمة M3U المحدثة...", 0.3f))
                    val req = Request.Builder().url(sub.m3uUrl).build()
                    val resp = OkHttpClient().newCall(req).execute()
                    if (!resp.isSuccessful) {
                        onProgress(SyncProgressState.Error("فشل تحميل قائمة القنوات (كود ${resp.code})"))
                        return@withContext
                    }
                    val stream = resp.body?.byteStream() ?: throw Exception("استجابة الخادم فارغة")
                    val parsed = M3uParser.parse(stream, subId)
                    stream.close()

                    channelDao.deleteChannelsBySubscription(subId)
                    channelDao.insertChannels(parsed.channels)
                    vodDao.deleteVodBySubscription(subId)
                    vodDao.insertVodItems(parsed.vodItems)

                    val updatedSub = sub.copy(
                        totalChannels = parsed.channels.size,
                        totalMovies = parsed.vodItems.count { it.type == VodType.MOVIE },
                        totalSeries = parsed.vodItems.count { it.type == VodType.SERIES },
                        lastUpdated = System.currentTimeMillis()
                    )
                    subscriptionDao.updateSubscription(updatedSub)
                    onProgress(SyncProgressState.Success("تم تحديث ${parsed.channels.size} قناة و ${parsed.vodItems.size} محتوى بنجاح!"))
                } else {
                    onProgress(SyncProgressState.Error("لا يمكن تحديث القائمة المحلية تلقائياً؛ يرجى إعادة إدخالها."))
                }
            }
        } catch (e: Exception) {
            onProgress(SyncProgressState.Error("حدث خطأ أثناء التحديث: ${e.localizedMessage ?: "تأكد من اتصال الإنترنت"}"))
        }
    }
}
