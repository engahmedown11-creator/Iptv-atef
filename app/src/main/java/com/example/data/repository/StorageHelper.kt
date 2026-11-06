package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageMetrics(
    val totalDeviceBytes: Long,
    val freeDeviceBytes: Long,
    val appUsedBytes: Long,
    val appCacheBytes: Long,
    val appDownloadBytes: Long,
    val isStorageLow: Boolean
)

data class DownloadedFileItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val isMovie: Boolean
)

object StorageHelper {

    fun getStorageMetrics(context: Context): StorageMetrics {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize

        val cacheDir = context.cacheDir
        val cacheBytes = getFolderSize(cacheDir)

        val downloadsDir = getDownloadsDirectory(context)
        val downloadBytes = getFolderSize(downloadsDir)

        val appDataDir = context.filesDir
        val filesBytes = getFolderSize(appDataDir)

        val appUsed = cacheBytes + downloadBytes + filesBytes
        val isLow = freeBytes < (1024L * 1024L * 500L) // less than 500MB is low

        return StorageMetrics(
            totalDeviceBytes = totalBytes,
            freeDeviceBytes = freeBytes,
            appUsedBytes = appUsed,
            appCacheBytes = cacheBytes,
            appDownloadBytes = downloadBytes,
            isStorageLow = isLow
        )
    }

    fun getDownloadsDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "atef_downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadedFiles(context: Context): List<DownloadedFileItem> {
        val dir = getDownloadsDirectory(context)
        val files = dir.listFiles() ?: return emptyList()

        return files.filter { it.isFile }.map { f ->
            val size = f.length()
            val isMovie = !f.name.contains("ep_", ignoreCase = true)
            DownloadedFileItem(
                file = f,
                name = f.nameWithoutExtension,
                sizeBytes = size,
                formattedSize = formatBytes(size),
                lastModified = f.lastModified(),
                isMovie = isMovie
            )
        }.sortedByDescending { it.lastModified }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }

    fun clearAppCache(context: Context): Boolean {
        return try {
            deleteFolderRecursively(context.cacheDir)
        } catch (_: Exception) {
            false
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 ميجابايت"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f جيجابايت", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f ميجابايت", mb)
            else -> String.format(java.util.Locale.US, "%.0f كيلوبايت", kb)
        }
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size: Long = 0
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    private fun deleteFolderRecursively(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFolderRecursively(it) }
        }
        return file.delete()
    }
}
