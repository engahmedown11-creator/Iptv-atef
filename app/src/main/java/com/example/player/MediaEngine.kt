package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MediaEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _availableAudioTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val availableAudioTracks: StateFlow<List<TrackItem>> = _availableAudioTracks.asStateFlow()

    private val _availableSubtitleTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<TrackItem>> = _availableSubtitleTracks.asStateFlow()

    private val _availableQualities = MutableStateFlow<List<QualityItem>>(emptyList())
    val availableQualities: StateFlow<List<QualityItem>> = _availableQualities.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _subtitleConfig = MutableStateFlow(SubtitleConfig())
    val subtitleConfig: StateFlow<SubtitleConfig> = _subtitleConfig.asStateFlow()

    private var currentMediaUrl: String = ""
    private var reconnectJob: Job? = null
    private var progressTrackingJob: Job? = null

    init {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2_000,  // minBufferMs (fast start for IPTV)
                15_000, // maxBufferMs
                1_000,  // bufferForPlaybackMs
                2_000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
        SharedPlayerHolder.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_IDLE -> _playbackStatus.value = PlaybackStatus.IDLE
                    Player.STATE_BUFFERING -> _playbackStatus.value = PlaybackStatus.BUFFERING
                    Player.STATE_READY -> {
                        _playbackStatus.value = if (exoPlayer.playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                        _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                        _errorMessage.value = null
                    }
                    Player.STATE_ENDED -> _playbackStatus.value = PlaybackStatus.ENDED
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playbackStatus.value = PlaybackStatus.PLAYING
                } else if (_playbackStatus.value == PlaybackStatus.PLAYING) {
                    _playbackStatus.value = PlaybackStatus.PAUSED
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackStatus.value = PlaybackStatus.ERROR
                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "فشل الاتصال بالشبكة أو انقطاع الإنترنت"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "رابط البث غير متاح أو الخادم لا يستجيب"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "تنسيق الفيديو غير مدعوم من جهازك"
                    else -> error.localizedMessage ?: "حدث خطأ غير متوقع أثناء تشغيل البث"
                }
                _errorMessage.value = message
                scheduleAutoReconnect()
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateExtractedTracks(tracks)
            }
        })

        startPositionTracking()
    }

    fun play(url: String, title: String = "ATEF IPTV", startPositionMs: Long = 0L, isLive: Boolean = false) {
        reconnectJob?.cancel()
        currentMediaUrl = url
        _errorMessage.value = null
        _playbackStatus.value = PlaybackStatus.BUFFERING

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("ATEF IPTV")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.stop()
        exoPlayer.setMediaItem(mediaItem)
        if (startPositionMs > 0 && !isLive) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        try {
            val intent = android.content.Intent(context, AtefMediaSessionService::class.java)
            context.startService(intent)
        } catch (_: Exception) {}
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun resume() {
        exoPlayer.playWhenReady = true
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, _durationMs.value))
    }

    fun forward10() {
        val current = exoPlayer.currentPosition
        seekTo(current + 10_000L)
    }

    fun rewind10() {
        val current = exoPlayer.currentPosition
        seekTo((current - 10_000L).coerceAtLeast(0L))
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun selectAudioTrack(trackId: String) {
        selectTrackInternal(C.TRACK_TYPE_AUDIO, trackId)
    }

    fun selectSubtitleTrack(trackId: String) {
        if (trackId == "OFF") {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
        } else {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )
            selectTrackInternal(C.TRACK_TYPE_TEXT, trackId)
        }
    }

    fun selectVideoQuality(quality: QualityItem) {
        if (quality.isAuto) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearVideoSizeConstraints()
            )
        } else {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setMaxVideoSize(quality.width, quality.height)
                    .setMinVideoSize(quality.width, quality.height)
            )
        }
    }

    fun updateSubtitleConfig(config: SubtitleConfig) {
        _subtitleConfig.value = config
    }

    private fun selectTrackInternal(trackType: Int, trackId: String) {
        val currentTracks = exoPlayer.currentTracks
        for (group in currentTracks.groups) {
            if (group.type == trackType) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id == trackId || "${format.label}_$i" == trackId) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                                )
                        )
                        return
                    }
                }
            }
        }
    }

    private fun updateExtractedTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackItem>()
        val textList = mutableListOf<TrackItem>()
        val qualityList = mutableListOf<QualityItem>()

        qualityList.add(QualityItem(width = 0, height = 0, bitrate = 0, isAuto = true, isSelected = true))

        for (group in tracks.groups) {
            val type = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                val id = format.id ?: "${format.label}_$i"

                when (type) {
                    C.TRACK_TYPE_AUDIO -> {
                        val name = format.label ?: format.language ?: "مسار صوت $i"
                        audioList.add(
                            TrackItem(
                                id = id,
                                name = name,
                                language = format.language ?: "",
                                isSelected = isSelected,
                                formatInfo = "${format.sampleRate}Hz"
                            )
                        )
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val name = format.label ?: format.language ?: "ترجمة $i"
                        textList.add(
                            TrackItem(
                                id = id,
                                name = name,
                                language = format.language ?: "",
                                isSelected = isSelected
                            )
                        )
                    }
                    C.TRACK_TYPE_VIDEO -> {
                        if (format.height > 0) {
                            qualityList.add(
                                QualityItem(
                                    width = format.width,
                                    height = format.height,
                                    bitrate = format.bitrate,
                                    isAuto = false,
                                    isSelected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        _availableAudioTracks.value = audioList
        _availableSubtitleTracks.value = textList
        _availableQualities.value = qualityList
    }

    private fun scheduleAutoReconnect() {
        if (currentMediaUrl.isEmpty()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000L)
            if (_playbackStatus.value == PlaybackStatus.ERROR) {
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    private fun startPositionTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500L)
            }
        }
    }

    fun release() {
        progressTrackingJob?.cancel()
        reconnectJob?.cancel()
        exoPlayer.release()
    }
}
