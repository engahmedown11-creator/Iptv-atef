package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class RemoteAppConfig(
    val currentInstalledVersion: String = "1.0.0",
    val latestVersion: String = "1.1.0",
    val isUpdateRequired: Boolean = false,
    val isNewVersionAvailable: Boolean = true,
    val updateTitle: String = "تحديث جديد متاح لتطبيق ATEF IPTV",
    val updateMessage: String = "يتضمن هذا التحديث تسريع تشغيل البث المباشر HLS/DASH/RTSP، ودعم استمرارية تشغيل الصوت في الخلفية، وتشفير بيانات الدخول AES-256.",
    val apkDownloadUrl: String = "https://github.com/atef-iptv/releases/releases/download/v1.1.0/atef_iptv_latest.apk",
    val minSupportedVersion: Int = 1,
    val supportHotline: String = "0581283398",
    val supportEmail: String = "eng.ibrahimabdalfatah@gmail.com",
    val lastFetchSuccessful: Boolean = false
)

class RemoteConfigManager(private val context: Context) {

    private val _configState = MutableStateFlow(RemoteAppConfig())
    val configState: StateFlow<RemoteAppConfig> = _configState.asStateFlow()

    private var remoteConfig: FirebaseRemoteConfig? = null

    init {
        initFirebaseRemoteConfig()
    }

    private fun initFirebaseRemoteConfig() {
        try {
            // Ensure FirebaseApp is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("atef-iptv")
                    .setApiKey("AIzaSyDummyAtefIptvKeyForRemoteConfig")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            val frc = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // Instant fetch for live updates
                .build()

            frc.setConfigSettingsAsync(configSettings)
            frc.setDefaultsAsync(R.xml.remote_config_defaults)
            remoteConfig = frc

            updateStateFromConfig(frc, fetchSuccess = false)
        } catch (e: Exception) {
            // Fallback gracefully without crash
            _configState.value = RemoteAppConfig(lastFetchSuccessful = false)
        }
    }

    suspend fun fetchAndActivate(): Boolean = withContext(Dispatchers.IO) {
        val frc = remoteConfig ?: return@withContext false
        try {
            val task = frc.fetchAndActivate()
            // Wait for completion synchronously in IO dispatcher
            var completed = false
            var success = false
            task.addOnCompleteListener { t ->
                success = t.isSuccessful
                completed = true
            }

            var waitCount = 0
            while (!completed && waitCount < 30) {
                kotlinx.coroutines.delay(100L)
                waitCount++
            }

            updateStateFromConfig(frc, fetchSuccess = success)
            success
        } catch (_: Exception) {
            false
        }
    }

    private fun updateStateFromConfig(frc: FirebaseRemoteConfig, fetchSuccess: Boolean) {
        val latest = frc.getString("latest_app_version").ifEmpty { "1.1.0" }
        val req = frc.getBoolean("is_update_required")
        val title = frc.getString("update_title").ifEmpty { "تحديث جديد متاح لتطبيق ATEF IPTV" }
        val msg = frc.getString("update_message").ifEmpty {
            "يتضمن هذا التحديث تسريع تشغيل البث المباشر HLS/DASH/RTSP، ودعم استمرارية تشغيل الصوت في الخلفية، وتشفير بيانات الدخول AES-256."
        }
        val apkUrl = frc.getString("apk_download_url").ifEmpty {
            "https://github.com/atef-iptv/releases/releases/download/v1.1.0/atef_iptv_latest.apk"
        }
        val minVer = frc.getLong("min_supported_version").toInt().coerceAtLeast(1)
        val phone = frc.getString("support_hotline").ifEmpty { "0581283398" }
        val email = frc.getString("support_email").ifEmpty { "eng.ibrahimabdalfatah@gmail.com" }

        val current = "1.0.0"
        val isNew = isVersionNewer(latest, current)

        _configState.value = RemoteAppConfig(
            currentInstalledVersion = current,
            latestVersion = latest,
            isUpdateRequired = req,
            isNewVersionAvailable = isNew,
            updateTitle = title,
            updateMessage = msg,
            apkDownloadUrl = apkUrl,
            minSupportedVersion = minVer,
            supportHotline = phone,
            supportEmail = email,
            lastFetchSuccessful = fetchSuccess
        )
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        val remoteClean = remote.replace("[^0-9.]".toRegex(), "")
        val localClean = local.replace("[^0-9.]".toRegex(), "")
        val rParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = localClean.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until Math.max(rParts.size, lParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
