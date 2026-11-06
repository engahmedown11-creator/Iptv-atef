package com.example.data.parser

import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.VodItemEntity
import com.example.data.local.entity.VodType
import com.example.data.repository.AiMetadataEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class XtreamAuthResult(
    val isValid: Boolean,
    val status: String = "",
    val expDate: String = "",
    val maxConnections: Int = 1,
    val serverName: String = "",
    val errorMessage: String? = null
)

class XtreamClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private fun normalizeServerUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    suspend fun authenticate(serverUrl: String, user: String, pass: String): XtreamAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val server = normalizeServerUrl(serverUrl)
                val url = "$server/player_api.php?username=$user&password=$pass"
                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext XtreamAuthResult(
                            isValid = false,
                            errorMessage = "تعذر الاتصال بالخادم: كود ${response.code}"
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isEmpty()) {
                        return@withContext XtreamAuthResult(isValid = false, errorMessage = "رد الخادم فارغ")
                    }

                    val json = JSONObject(body)
                    val userInfo = json.optJSONObject("user_info")
                    if (userInfo == null) {
                        return@withContext XtreamAuthResult(isValid = false, errorMessage = "بيانات الاعتماد غير صالحة")
                    }

                    val auth = userInfo.optInt("auth", 0)
                    val status = userInfo.optString("status", "Active")
                    val expTimestamp = userInfo.optLong("exp_date", 0L)
                    val maxConns = userInfo.optInt("max_connections", 1)

                    val expDateStr = if (expTimestamp > 0) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        sdf.format(Date(expTimestamp * 1000L))
                    } else {
                        "غير محدد / غير محدود"
                    }

                    val serverInfo = json.optJSONObject("server_info")
                    val serverName = serverInfo?.optString("url", server) ?: server

                    if (auth == 1 || status.equals("Active", ignoreCase = true)) {
                        XtreamAuthResult(
                            isValid = true,
                            status = status,
                            expDate = expDateStr,
                            maxConnections = maxConns,
                            serverName = serverName
                        )
                    } else {
                        XtreamAuthResult(
                            isValid = false,
                            status = status,
                            errorMessage = "الحساب غير مفعل أو منتهي الصلاحية"
                        )
                    }
                }
            } catch (e: Exception) {
                XtreamAuthResult(
                    isValid = false,
                    errorMessage = "خطأ في الاتصال: ${e.localizedMessage ?: "تأكد من صحة الرابط والإنترنت"}"
                )
            }
        }

    suspend fun fetchLiveStreams(
        serverUrl: String,
        user: String,
        pass: String,
        subId: Long
    ): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val server = normalizeServerUrl(serverUrl)
        val channels = mutableListOf<ChannelEntity>()
        try {
            // First get categories map
            val categoriesMap = fetchCategoriesMap(server, user, pass, "get_live_categories")

            val url = "$server/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val streamId = item.optString("stream_id")
                    val name = item.optString("name", "قناة بدون اسم")
                    val logo = item.optString("stream_icon", "")
                    val categoryId = item.optString("category_id", "")
                    val categoryName = categoriesMap[categoryId] ?: "عام"
                    val epgId = item.optString("epg_channel_id", "")
                    val streamUrl = "$server/live/$user/$pass/$streamId.m3u8"

                    channels.add(
                        ChannelEntity(
                            subscriptionId = subId,
                            channelId = streamId,
                            name = name,
                            logoUrl = logo,
                            streamUrl = streamUrl,
                            categoryName = categoryName,
                            epgChannelId = epgId
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        channels
    }

    suspend fun fetchVodMovies(
        serverUrl: String,
        user: String,
        pass: String,
        subId: Long
    ): List<VodItemEntity> = withContext(Dispatchers.IO) {
        val server = normalizeServerUrl(serverUrl)
        val movies = mutableListOf<VodItemEntity>()
        try {
            val categoriesMap = fetchCategoriesMap(server, user, pass, "get_vod_categories")
            val url = "$server/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val streamId = item.optString("stream_id")
                    val originalTitle = item.optString("name", "فيلم")
                    val categoryId = item.optString("category_id", "")
                    val categoryName = categoriesMap[categoryId] ?: "أفلام عامة"
                    val poster = item.optString("stream_icon", "")
                    val rating = item.optString("rating", "")
                    val extension = item.optString("container_extension", "mp4")
                    val streamUrl = "$server/movie/$user/$pass/$streamId.$extension"

                    movies.add(
                        VodItemEntity(
                            subscriptionId = subId,
                            streamId = streamId,
                            type = VodType.MOVIE,
                            originalTitle = originalTitle,
                            arabicOverview = "فيلم بجودة عالية ضمن تصنيف $categoryName.",
                            categoryName = categoryName,
                            posterUrl = poster,
                            rating = rating,
                            streamUrl = streamUrl,
                            extension = extension
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        movies
    }

    suspend fun fetchSeries(
        serverUrl: String,
        user: String,
        pass: String,
        subId: Long
    ): List<VodItemEntity> = withContext(Dispatchers.IO) {
        val server = normalizeServerUrl(serverUrl)
        val seriesList = mutableListOf<VodItemEntity>()
        try {
            val categoriesMap = fetchCategoriesMap(server, user, pass, "get_series_categories")
            val url = "$server/player_api.php?username=$user&password=$pass&action=get_series"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val seriesId = item.optString("series_id")
                    val originalTitle = item.optString("name", "مسلسل")
                    val categoryId = item.optString("category_id", "")
                    val categoryName = categoriesMap[categoryId] ?: "مسلسلات عامة"
                    val poster = item.optString("cover", "")
                    val backdrop = item.optString("backdrop_path", "")
                    val rating = item.optString("rating", "")
                    val releaseDate = item.optString("releaseDate", "")
                    val year = if (releaseDate.length >= 4) releaseDate.take(4) else ""
                    val plot = item.optString("plot", "").trim()
                    val arabicOverview = if (plot.isNotEmpty() && AiMetadataEngine.isGenuinelyArabic(plot)) {
                        plot
                    } else {
                        "مسلسل درامي مميز مصنف ضمن $categoryName."
                    }

                    seriesList.add(
                        VodItemEntity(
                            subscriptionId = subId,
                            streamId = seriesId,
                            type = VodType.SERIES,
                            originalTitle = originalTitle,
                            arabicOverview = arabicOverview,
                            categoryName = categoryName,
                            posterUrl = poster,
                            backdropUrl = backdrop,
                            releaseYear = year,
                            rating = rating
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        seriesList
    }

    suspend fun fetchSeriesEpisodes(
        serverUrl: String,
        user: String,
        pass: String,
        subId: Long,
        seriesId: String
    ): List<EpisodeEntity> = withContext(Dispatchers.IO) {
        val server = normalizeServerUrl(serverUrl)
        val episodes = mutableListOf<EpisodeEntity>()
        try {
            val url = "$server/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val episodesObj = json.optJSONObject("episodes") ?: return@withContext emptyList()

                val seasonKeys = episodesObj.keys()
                while (seasonKeys.hasNext()) {
                    val seasonKey = seasonKeys.next()
                    val seasonNum = seasonKey.toIntOrNull() ?: 1
                    val epArray = episodesObj.optJSONArray(seasonKey) ?: continue

                    for (i in 0 until epArray.length()) {
                        val ep = epArray.getJSONObject(i)
                        val epId = ep.optString("id")
                        val epNum = ep.optInt("episode_num", i + 1)
                        val originalTitle = ep.optString("title", "الحلقة $epNum")
                        val extension = ep.optString("container_extension", "mp4")
                        val streamUrl = "$server/series/$user/$pass/$epId.$extension"
                        val info = ep.optJSONObject("info")
                        val plot = info?.optString("plot", "") ?: ""
                        val rating = info?.optString("rating", "") ?: ""
                        val durationSec = info?.optInt("duration_secs", 0) ?: 0
                        val poster = info?.optString("movie_image", "") ?: ""

                        episodes.add(
                            EpisodeEntity(
                                subscriptionId = subId,
                                seriesStreamId = seriesId,
                                seasonNumber = seasonNum,
                                episodeNumber = epNum,
                                originalTitle = originalTitle,
                                arabicOverview = if (plot.isNotEmpty() && AiMetadataEngine.isGenuinelyArabic(plot)) {
                                    plot
                                } else {
                                    "تفاصيل أحداث الحلقة $epNum من الموسم $seasonNum."
                                },
                                streamUrl = streamUrl,
                                durationMinutes = if (durationSec > 0) durationSec / 60 else 45,
                                rating = rating,
                                posterUrl = poster
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) { }
        episodes
    }

    private suspend fun fetchCategoriesMap(
        server: String,
        user: String,
        pass: String,
        action: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            val url = "$server/player_api.php?username=$user&password=$pass&action=$action"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("category_id")
                        val name = obj.optString("category_name")
                        if (id.isNotEmpty() && name.isNotEmpty()) {
                            map[id] = name
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        map
    }
}
