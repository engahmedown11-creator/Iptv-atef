package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

data class DownloadTask(
    val id: String,
    val title: String,
    val streamUrl: String,
    val destinationPath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String? = null
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

class DownloadRepository(private val context: Context) {
    companion object {
        @Volatile
        private var instance: DownloadRepository? = null

        fun getInstance(context: Context): DownloadRepository {
            return instance ?: synchronized(this) {
                instance ?: DownloadRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client = OkHttpClient()
    private val activeTasks = ConcurrentHashMap<String, DownloadTask>()
    private val _tasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow: StateFlow<List<DownloadTask>> = _tasksFlow.asStateFlow()

    private val cancelFlags = ConcurrentHashMap<String, Boolean>()

    suspend fun startDownload(id: String, title: String, streamUrl: String, isMovie: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            // Live streams or HLS playlists are not downloadable as flat files
            if (streamUrl.contains(".m3u8") || streamUrl.contains(".mpd") || streamUrl.contains("action=get_live_streams")) {
                return@withContext false
            }

            val downloadsDir = StorageHelper.getDownloadsDirectory(context)
            val extension = if (streamUrl.contains(".mkv")) "mkv" else "mp4"
            val sanitizedName = title.replace("[^a-zA-Z0-9ء-ي]".toRegex(), "_")
            val fileName = "${if (isMovie) "mov_" else "ep_"}${id}_$sanitizedName.$extension"
            val targetFile = File(downloadsDir, fileName)

            if (targetFile.exists() && targetFile.length() > 0) {
                // Already downloaded
                val task = DownloadTask(
                    id = id,
                    title = title,
                    streamUrl = streamUrl,
                    destinationPath = targetFile.absolutePath,
                    totalBytes = targetFile.length(),
                    downloadedBytes = targetFile.length(),
                    progress = 1f,
                    status = DownloadStatus.COMPLETED
                )
                activeTasks[id] = task
                _tasksFlow.value = activeTasks.values.toList()
                return@withContext true
            }

            cancelFlags[id] = false
            val initialTask = DownloadTask(
                id = id,
                title = title,
                streamUrl = streamUrl,
                destinationPath = targetFile.absolutePath,
                status = DownloadStatus.DOWNLOADING
            )
            activeTasks[id] = initialTask
            _tasksFlow.value = activeTasks.values.toList()

            try {
                val request = Request.Builder().url(streamUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        activeTasks[id] = initialTask.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "فشل التحميل من المصدر (كود ${response.code})"
                        )
                        _tasksFlow.value = activeTasks.values.toList()
                        return@withContext false
                    }

                    val body = response.body ?: run {
                        activeTasks[id] = initialTask.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "لا يوجد محتوى للتحميل"
                        )
                        _tasksFlow.value = activeTasks.values.toList()
                        return@withContext false
                    }

                    val totalLength = body.contentLength()
                    var input: InputStream? = null
                    var output: FileOutputStream? = null

                    try {
                        input = body.byteStream()
                        output = FileOutputStream(targetFile)
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (cancelFlags[id] == true) {
                                output.flush()
                                activeTasks[id] = initialTask.copy(
                                    downloadedBytes = downloaded,
                                    totalBytes = totalLength,
                                    progress = if (totalLength > 0) downloaded.toFloat() / totalLength else 0f,
                                    status = DownloadStatus.PAUSED
                                )
                                _tasksFlow.value = activeTasks.values.toList()
                                return@withContext false
                            }

                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val progress = if (totalLength > 0) downloaded.toFloat() / totalLength else 0f
                            activeTasks[id] = initialTask.copy(
                                downloadedBytes = downloaded,
                                totalBytes = totalLength,
                                progress = progress,
                                status = DownloadStatus.DOWNLOADING
                            )
                            _tasksFlow.value = activeTasks.values.toList()
                        }

                        output.flush()
                        activeTasks[id] = initialTask.copy(
                            downloadedBytes = downloaded,
                            totalBytes = totalLength,
                            progress = 1f,
                            status = DownloadStatus.COMPLETED
                        )
                        _tasksFlow.value = activeTasks.values.toList()
                        true
                    } finally {
                        input?.close()
                        output?.close()
                    }
                }
            } catch (e: Exception) {
                activeTasks[id] = initialTask.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "حدث خطأ أثناء تنزيل الملف"
                )
                _tasksFlow.value = activeTasks.values.toList()
                false
            }
        }

    fun pauseDownload(id: String) {
        cancelFlags[id] = true
    }

    fun removeDownload(id: String) {
        cancelFlags[id] = true
        val task = activeTasks.remove(id)
        if (task != null) {
            val file = File(task.destinationPath)
            if (file.exists()) {
                file.delete()
            }
        }
        _tasksFlow.value = activeTasks.values.toList()
    }
}
