package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.repository.DownloadRepository
import com.example.data.repository.DownloadTask
import com.example.data.repository.IptvRepository
import com.example.data.repository.StorageHelper
import com.example.data.repository.StorageMetrics
import com.example.data.repository.SyncProgressState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DeviceMode(val titleAr: String) {
    AUTO("تلقائي (اكتشاف ذكي)"),
    PHONE("هاتف ذكي"),
    TABLET("جهاز لوحي (تابلت)"),
    TV("تلفزيون (Android TV / Box)")
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val repository = IptvRepository(application)
    private val prefs = application.getSharedPreferences("atef_iptv_prefs", Context.MODE_PRIVATE)

    val downloadRepository = DownloadRepository.getInstance(application)
    val downloadTasks: StateFlow<List<DownloadTask>> = downloadRepository.tasksFlow

    val activeSubscription: StateFlow<SubscriptionEntity?> = repository.activeSubscriptionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remoteConfigManager = com.example.data.repository.RemoteConfigManager(application)
    val remoteConfig: StateFlow<com.example.data.repository.RemoteAppConfig> = remoteConfigManager.configState

    init {
        viewModelScope.launch {
            remoteConfigManager.fetchAndActivate()
        }
    }

    private val _deviceMode = MutableStateFlow(loadDeviceMode())
    val deviceMode: StateFlow<DeviceMode> = _deviceMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showFirstRunDialog = MutableStateFlow(!prefs.getBoolean("first_run_completed", false))
    val showFirstRunDialog: StateFlow<Boolean> = _showFirstRunDialog.asStateFlow()

    private val _storageMetrics = MutableStateFlow(StorageHelper.getStorageMetrics(application))
    val storageMetrics: StateFlow<StorageMetrics> = _storageMetrics.asStateFlow()

    private val _syncState = MutableStateFlow<SyncProgressState>(SyncProgressState.Idle)
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    fun setDeviceMode(mode: DeviceMode) {
        _deviceMode.value = mode
        prefs.edit().putString("device_mode", mode.name).apply()
    }

    fun completeFirstRun(selectedMode: DeviceMode) {
        setDeviceMode(selectedMode)
        _showFirstRunDialog.value = false
        prefs.edit().putBoolean("first_run_completed", true).apply()
    }

    fun toggleTheme() {
        val newVal = !_isDarkTheme.value
        _isDarkTheme.value = newVal
        prefs.edit().putBoolean("dark_theme", newVal).apply()
    }

    fun refreshStorage() {
        _storageMetrics.value = StorageHelper.getStorageMetrics(getApplication())
    }

    fun clearAppCache() {
        StorageHelper.clearAppCache(getApplication())
        refreshStorage()
    }

    fun refreshActiveSubscription() {
        val sub = activeSubscription.value ?: return
        refreshSubscription(sub.id)
    }

    fun refreshSubscription(subId: Long) {
        viewModelScope.launch {
            repository.refreshSubscription(subId) { state ->
                _syncState.value = state
            }
            refreshStorage()
        }
    }

    fun addM3u(name: String, url: String, isStream: Boolean) {
        viewModelScope.launch {
            repository.addM3uSubscription(name, url, isStream) { state ->
                _syncState.value = state
            }
            refreshStorage()
        }
    }

    fun addXtream(name: String, server: String, user: String, pass: String) {
        viewModelScope.launch {
            repository.addXtreamSubscription(name, server, user, pass) { state ->
                _syncState.value = state
            }
            refreshStorage()
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncProgressState.Idle
    }

    fun switchSubscription(subId: Long) {
        viewModelScope.launch {
            repository.setActiveSubscription(subId)
        }
    }

    fun deleteSubscription(subId: Long) {
        viewModelScope.launch {
            repository.deleteSubscription(subId)
            refreshStorage()
        }
    }

    fun startDownload(id: String, title: String, streamUrl: String, isMovie: Boolean) {
        viewModelScope.launch {
            downloadRepository.startDownload(id, title, streamUrl, isMovie)
            refreshStorage()
        }
    }

    fun toggleVodFavorite(localId: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleVodFavorite(localId, isFav)
        }
    }

    fun updateVodOverview(localId: Long, overview: String) {
        viewModelScope.launch {
            repository.updateVodOverview(localId, overview)
        }
    }

    private fun loadDeviceMode(): DeviceMode {
        val saved = prefs.getString("device_mode", null)
        if (saved != null) {
            return try {
                DeviceMode.valueOf(saved)
            } catch (_: Exception) {
                DeviceMode.AUTO
            }
        }
        // Auto detection
        val uiModeManager = getApplication<Application>().packageManager
        val isTv = uiModeManager.hasSystemFeature("android.software.leanback")
        return if (isTv) DeviceMode.TV else DeviceMode.AUTO
    }
}
