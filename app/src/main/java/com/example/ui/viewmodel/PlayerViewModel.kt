package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelProfileEntity
import com.example.data.repository.IptvRepository
import com.example.player.AspectRatioMode
import com.example.player.MediaEngine
import com.example.player.QualityItem
import com.example.player.SubtitleConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CurrentPlayingItem(
    val title: String,
    val subtitle: String = "",
    val streamUrl: String,
    val logoOrPoster: String = "",
    val isLive: Boolean = true,
    val channelId: String = "",
    val subscriptionId: Long = 0L,
    val referenceId: Long = 0L,
    val contentType: String = "LIVE" // "LIVE", "MOVIE", "SERIES_EPISODE"
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val repository = IptvRepository(application)
    val mediaEngine = MediaEngine(application)

    private val _currentItem = MutableStateFlow<CurrentPlayingItem?>(null)
    val currentItem: StateFlow<CurrentPlayingItem?> = _currentItem.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private val _showChannelDrawer = MutableStateFlow(false)
    val showChannelDrawer: StateFlow<Boolean> = _showChannelDrawer.asStateFlow()

    private val _showTrackDialog = MutableStateFlow(false)
    val showTrackDialog: StateFlow<Boolean> = _showTrackDialog.asStateFlow()

    private val _showQualityDialog = MutableStateFlow(false)
    val showQualityDialog: StateFlow<Boolean> = _showQualityDialog.asStateFlow()

    private val _showSubtitleDialog = MutableStateFlow(false)
    val showSubtitleDialog: StateFlow<Boolean> = _showSubtitleDialog.asStateFlow()

    private val _showAspectDialog = MutableStateFlow(false)
    val showAspectDialog: StateFlow<Boolean> = _showAspectDialog.asStateFlow()

    // Playlist channels for fast channel switching
    private val _channelList = MutableStateFlow<List<ChannelEntity>>(emptyList())
    val channelList: StateFlow<List<ChannelEntity>> = _channelList.asStateFlow()

    private var resumeSyncJob: Job? = null
    private var controlsTimeoutJob: Job? = null

    init {
        startResumeTracking()
    }

    fun setPlaylist(channels: List<ChannelEntity>) {
        _channelList.value = channels
    }

    fun playChannel(channel: ChannelEntity, playlist: List<ChannelEntity>? = null) {
        if (playlist != null) {
            _channelList.value = playlist
        }

        viewModelScope.launch {
            repository.markChannelWatched(channel.localId)
            // Restore per-channel playback profile
            val profile = repository.getChannelProfile(channel.subscriptionId, channel.channelId)

            val aspect = try {
                AspectRatioMode.valueOf(profile.aspectRatio)
            } catch (_: Exception) {
                AspectRatioMode.FIT
            }
            mediaEngine.setAspectRatio(aspect)

            _currentItem.value = CurrentPlayingItem(
                title = channel.name,
                subtitle = channel.categoryName,
                streamUrl = channel.streamUrl,
                logoOrPoster = channel.logoUrl,
                isLive = true,
                channelId = channel.channelId,
                subscriptionId = channel.subscriptionId,
                referenceId = channel.localId,
                contentType = "LIVE"
            )

            mediaEngine.play(channel.streamUrl, title = channel.name, isLive = true)
            resetControlsTimer()
        }
    }

    fun playVod(
        title: String,
        category: String,
        streamUrl: String,
        posterUrl: String,
        subId: Long,
        refId: Long,
        isMovie: Boolean,
        resumePositionMs: Long = 0L
    ) {
        _currentItem.value = CurrentPlayingItem(
            title = title,
            subtitle = category,
            streamUrl = streamUrl,
            logoOrPoster = posterUrl,
            isLive = false,
            subscriptionId = subId,
            referenceId = refId,
            contentType = if (isMovie) "MOVIE" else "SERIES_EPISODE"
        )
        mediaEngine.play(streamUrl, title = title, startPositionMs = resumePositionMs, isLive = false)
        resetControlsTimer()
    }

    fun nextChannel() {
        val current = _currentItem.value ?: return
        if (!current.isLive) return
        val list = _channelList.value
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.channelId == current.channelId }
        val nextIndex = if (currentIndex != -1 && currentIndex < list.size - 1) currentIndex + 1 else 0
        playChannel(list[nextIndex])
    }

    fun previousChannel() {
        val current = _currentItem.value ?: return
        if (!current.isLive) return
        val list = _channelList.value
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.channelId == current.channelId }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playChannel(list[prevIndex])
    }

    fun togglePlayPause() {
        if (mediaEngine.exoPlayer.isPlaying) {
            mediaEngine.pause()
        } else {
            mediaEngine.resume()
        }
        resetControlsTimer()
    }

    fun toggleControls() {
        if (_isLocked.value) return
        _showControls.value = !_showControls.value
        if (_showControls.value) {
            resetControlsTimer()
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        _showControls.value = !_isLocked.value
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        mediaEngine.setAspectRatio(mode)
        _showAspectDialog.value = false
        // Persist to channel profile if it's a channel
        saveCurrentChannelProfile()
    }

    fun selectQuality(quality: QualityItem) {
        mediaEngine.selectVideoQuality(quality)
        _showQualityDialog.value = false
        saveCurrentChannelProfile()
    }

    fun selectAudioTrack(trackId: String) {
        mediaEngine.selectAudioTrack(trackId)
        _showTrackDialog.value = false
        saveCurrentChannelProfile()
    }

    fun selectSubtitleTrack(trackId: String) {
        mediaEngine.selectSubtitleTrack(trackId)
        _showSubtitleDialog.value = false
        saveCurrentChannelProfile()
    }

    fun updateSubtitleConfig(config: SubtitleConfig) {
        mediaEngine.updateSubtitleConfig(config)
        saveCurrentChannelProfile()
    }

    fun setChannelDrawerVisible(visible: Boolean) {
        _showChannelDrawer.value = visible
        if (visible) {
            controlsTimeoutJob?.cancel()
        } else {
            resetControlsTimer()
        }
    }

    fun setTrackDialogVisible(visible: Boolean) { _showTrackDialog.value = visible }
    fun setQualityDialogVisible(visible: Boolean) { _showQualityDialog.value = visible }
    fun setSubtitleDialogVisible(visible: Boolean) { _showSubtitleDialog.value = visible }
    fun setAspectDialogVisible(visible: Boolean) { _showAspectDialog.value = visible }

    fun setControlsVisible(visible: Boolean) {
        if (_isLocked.value && visible) return
        _showControls.value = visible
        if (visible) {
            resetControlsTimer()
        } else {
            controlsTimeoutJob?.cancel()
        }
    }

    private fun saveCurrentChannelProfile() {
        val item = _currentItem.value ?: return
        if (!item.isLive || item.channelId.isEmpty()) return

        viewModelScope.launch {
            val key = "sub_${item.subscriptionId}_ch_${item.channelId}"
            val profile = ChannelProfileEntity(
                profileKey = key,
                aspectRatio = mediaEngine.aspectRatioMode.value.name,
                subtitleSizeSp = mediaEngine.subtitleConfig.value.sizeSp,
                subtitleColorHex = mediaEngine.subtitleConfig.value.textColorHex
            )
            repository.saveChannelProfile(profile)
        }
    }

    private fun resetControlsTimer() {
        controlsTimeoutJob?.cancel()
        controlsTimeoutJob = viewModelScope.launch {
            delay(5000L)
            if (!_showChannelDrawer.value && !_showTrackDialog.value && !_showQualityDialog.value) {
                _showControls.value = false
            }
        }
    }

    private fun startResumeTracking() {
        resumeSyncJob?.cancel()
        resumeSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(4000L)
                val item = _currentItem.value
                if (item != null && !item.isLive && mediaEngine.exoPlayer.isPlaying) {
                    val pos = mediaEngine.exoPlayer.currentPosition
                    val dur = mediaEngine.exoPlayer.duration
                    if (pos > 5000L && dur > 0L) {
                        repository.savePlaybackProgress(
                            subId = item.subscriptionId,
                            title = item.title,
                            subtitle = item.subtitle,
                            posterUrl = item.logoOrPoster,
                            streamUrl = item.streamUrl,
                            contentType = item.contentType,
                            refId = item.referenceId,
                            positionMs = pos,
                            durationMs = dur
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resumeSyncJob?.cancel()
        controlsTimeoutJob?.cancel()
        mediaEngine.release()
    }
}
