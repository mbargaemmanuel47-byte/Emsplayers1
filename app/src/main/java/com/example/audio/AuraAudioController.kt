package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.model.AudioSettingsState
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "AuraAudioController"

class AuraAudioController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _spectrumBars = MutableStateFlow(List(36) { 0.08f })
    val spectrumBars: StateFlow<List<Float>> = _spectrumBars.asStateFlow()

    private val _dspStatus = MutableStateFlow("Prêt")
    val dspStatus: StateFlow<String> = _dspStatus.asStateFlow()

    private var activeTrack: Track? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared: Boolean = false
    private var pendingSeekMs: Int? = null
    private var hardwareEq: Equalizer? = null
    private var hardwareVirtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var progressJob: Job? = null
    private var analyzerJob: Job? = null
    private var transitionJob: Job? = null
    private var analyzedSpectrum: AnalyzedSpectrumTimeline? = null
    private var currentAudioSettings = AudioSettingsState()
    private var currentCrossfadeGain = 1.0f
    private var focusRequest: AudioFocusRequest? = null

    var onTrackFinishedCallback: (() -> Unit)? = null
    var onPlaybackErrorCallback: ((String) -> Unit)? = null
    var onDurationResolvedCallback: ((Long, Int) -> Unit)? = null

    private var activeTrackDurationMs: Long = 0L

    fun syncSelectedTrack(track: Track?) {
        if (track == null) {
            if (activeTrack == null) _durationSec.value = 0
            return
        }
        if (activeTrack?.id != track.id && mediaPlayer == null) {
            activeTrack = track
            val initialSec = track.durationSec.coerceAtLeast(0)
            _durationSec.value = initialSec
            activeTrackDurationMs = initialSec * 1000L
            _currentPositionSec.value = 0f
        }
    }

    fun playTrack(track: Track, startPositionSec: Float = 0f) {
        transitionJob?.cancel()
        val uriStr = track.localUri
        if (uriStr.isNullOrBlank()) {
            _isPlaying.value = false
            _dspStatus.value = "Fichier introuvable"
            onPlaybackErrorCallback?.invoke("Ce fichier audio n'est pas disponible.")
            return
        }

        activeTrack = track
        val initialSec = track.durationSec.coerceAtLeast(0)
        _durationSec.value = initialSec
        activeTrackDurationMs = initialSec.toLong() * 1000L
        _currentPositionSec.value = startPositionSec.coerceAtLeast(0f)
        pendingSeekMs = if (startPositionSec > 0f) (startPositionSec * 1000f).toInt() else null
        analyzedSpectrum = null
        analyzerJob?.cancel()

        if (!requestAudioFocus()) {
            _isPlaying.value = false
            _dspStatus.value = "Son bloqué"
            onPlaybackErrorCallback?.invoke("Le son est utilisé par une autre application.")
            return
        }

        stopEnginesOnly()
        _isPlaying.value = true
        _dspStatus.value = "Chargement"
        currentCrossfadeGain = if (currentAudioSettings.crossfadeEnabled) 0.25f else 1.0f

        startLocalMediaPlayer(track, uriStr)
        startProgressLoop()
        pushNotificationUpdate(track, isPlaying = true, positionMs = (_currentPositionSec.value * 1000f).toLong())
    }

    fun pause() {
        transitionJob?.cancel()
        val currentMs = if (isPrepared) {
            runCatching { mediaPlayer?.currentPosition ?: (_currentPositionSec.value * 1000f).toInt() }
                .getOrDefault((_currentPositionSec.value * 1000f).toInt())
        } else {
            (_currentPositionSec.value * 1000f).toInt()
        }
        _currentPositionSec.value = currentMs / 1000f
        _isPlaying.value = false
        runCatching {
            if (isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
        progressJob?.cancel()
        _spectrumBars.value = List(36) { 0.08f }
        _dspStatus.value = "En pause"
        activeTrack?.let {
            pushNotificationUpdate(it, isPlaying = false, positionMs = currentMs.toLong())
        }
    }

    fun pauseWithoutNotification() {
        transitionJob?.cancel()
        _isPlaying.value = false
        runCatching {
            if (isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
        progressJob?.cancel()
        _spectrumBars.value = List(36) { 0.08f }
        _dspStatus.value = "En pause"
    }

    fun resume(fallbackTrack: Track?) {
        val target = activeTrack ?: fallbackTrack ?: return
        if (!requestAudioFocus()) {
            onPlaybackErrorCallback?.invoke("Accès à la sortie audio refusé.")
            return
        }

        val mp = mediaPlayer
        if (mp != null) {
            _isPlaying.value = true
            if (isPrepared) {
                runCatching {
                    val seekTarget = pendingSeekMs
                    if (seekTarget != null) {
                        mp.seekTo(seekTarget)
                        pendingSeekMs = null
                    }
                    mp.start()
                    _dspStatus.value = "Lecture"
                }.onFailure {
                    Log.w(TAG, "Resume failed on existing player; recreating", it)
                    playTrack(target, startPositionSec = _currentPositionSec.value)
                    return
                }
            } else {
                _dspStatus.value = "Chargement"
            }
            startProgressLoop()
            pushNotificationUpdate(target, isPlaying = true, positionMs = (_currentPositionSec.value * 1000f).toLong())
        } else {
            val savedPos = _currentPositionSec.value
            playTrack(target, startPositionSec = savedPos)
        }
    }

    fun seekToFraction(fraction: Float) {
        val track = activeTrack ?: return
        val effectiveDurationSec = maxOf(_durationSec.value, track.durationSec, (activeTrackDurationMs / 1000L).toInt(), 1)
        val newSec = fraction.coerceIn(0f, 1f) * effectiveDurationSec
        seekToSeconds(newSec)
    }

    fun seekToSeconds(sec: Float) {
        val track = activeTrack ?: return
        val maxSec = maxOf(_durationSec.value.toFloat(), track.durationSec.toFloat(), activeTrackDurationMs / 1000f, 1f)
        val clamped = sec.coerceIn(0f, maxSec)
        _currentPositionSec.value = clamped
        val targetMs = (clamped * 1000f).toInt()
        val mp = mediaPlayer
        if (mp != null && isPrepared) {
            pendingSeekMs = null
            runCatching { mp.seekTo(targetMs) }
        } else {
            pendingSeekMs = targetMs
        }
        pushNotificationUpdate(track, isPlaying = _isPlaying.value, positionMs = targetMs.toLong())
    }

    private fun pushNotificationUpdate(track: Track, isPlaying: Boolean, positionMs: Long) {
        val durationMs = if (activeTrackDurationMs > 0L) {
            activeTrackDurationMs
        } else {
            (maxOf(_durationSec.value, track.durationSec, 0).toLong() * 1000L)
        }
        AuraPlaybackService.updateNotification(
            context = context,
            title = track.title,
            artist = track.artist,
            isPlaying = isPlaying,
            album = track.album,
            durationMs = durationMs,
            positionMs = positionMs,
            primaryColorHex = track.primaryColorHex,
            secondaryColorHex = track.secondaryColorHex,
            tertiaryColorHex = track.tertiaryColorHex
        )
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        if (clamped > 0f) _isMuted.value = false
        applyEffectiveVolume()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        applyEffectiveVolume()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun updateAudioSettings(settings: AudioSettingsState) {
        currentAudioSettings = settings
        applyHardwareEffects(settings)
    }

    fun fadeOutAndSwitch(onReadyToSwitch: () -> Unit) {
        transitionJob?.cancel()
        if (!_isPlaying.value) {
            onReadyToSwitch()
            return
        }
        transitionJob = scope.launch {
            val steps = 8
            for (s in steps downTo 1) {
                currentCrossfadeGain = s.toFloat() / steps
                applyEffectiveVolume()
                delay(40L)
            }
            onReadyToSwitch()
        }
    }

    private fun applyEffectiveVolume() {
        val base = if (_isMuted.value) 0f else _volume.value
        val effective = (base * currentCrossfadeGain).coerceIn(0f, 1f)
        runCatching { mediaPlayer?.setVolume(effective, effective) }
    }

    private fun startLocalMediaPlayer(track: Track, uriStr: String) {
        val uri = Uri.parse(uriStr)

        analyzerJob = scope.launch {
            analyzedSpectrum = LocalAudioSpectrumAnalyzer.analyze(context, uri, barCount = 36)
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                var dataSourceSet = false
                runCatching {
                    val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                    if (afd != null) {
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        dataSourceSet = true
                    }
                }
                if (!dataSourceSet) {
                    if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                        setDataSource(uri.path)
                    } else {
                        setDataSource(context, uri)
                    }
                }
                val base = if (_isMuted.value) 0f else _volume.value
                val effective = (base * currentCrossfadeGain).coerceIn(0f, 1f)
                setVolume(effective, effective)

                setOnPreparedListener { player ->
                    isPrepared = true
                    attachAudioEffects(player.audioSessionId)
                    val realDurationMs = runCatching { player.duration.toLong() }.getOrDefault(0L)
                    if (realDurationMs > 0L) {
                        activeTrackDurationMs = realDurationMs
                        val realSec = ((realDurationMs + 500L) / 1000L).toInt().coerceAtLeast(1)
                        _durationSec.value = realSec
                        if (track.durationSec != realSec) {
                            onDurationResolvedCallback?.invoke(track.id, realSec)
                        }
                    }

                    val targetSeek = pendingSeekMs
                    if (targetSeek != null && targetSeek > 0) {
                        pendingSeekMs = null
                        runCatching { player.seekTo(targetSeek) }
                    }

                    if (_isPlaying.value) {
                        player.start()
                        _dspStatus.value = "Lecture"
                        val posMs = runCatching { player.currentPosition.toLong() }.getOrDefault(0L)
                        pushNotificationUpdate(track, isPlaying = true, positionMs = posMs)
                    } else {
                        _dspStatus.value = "En pause"
                    }
                }

                setOnCompletionListener { player ->
                    if (_isRepeat.value) {
                        _currentPositionSec.value = 0f
                        runCatching {
                            player.seekTo(0)
                            player.start()
                        }
                        pushNotificationUpdate(track, isPlaying = true, positionMs = 0L)
                    } else {
                        onTrackFinishedCallback?.invoke()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error what=$what extra=$extra for uri=$uri")
                    isPrepared = false
                    _isPlaying.value = false
                    _spectrumBars.value = List(36) { 0.08f }
                    _dspStatus.value = "Erreur lecture"
                    onPlaybackErrorCallback?.invoke("Impossible de lire ce fichier audio.")
                    true
                }

                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open local audio URI: $uri", e)
            isPrepared = false
            mediaPlayer = null
            _isPlaying.value = false
            _spectrumBars.value = List(36) { 0.08f }
            _dspStatus.value = "Fichier inaccessible"
            onPlaybackErrorCallback?.invoke("Fichier introuvable ou format non pris en charge.")
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        releaseAudioEffects()
        runCatching {
            hardwareEq = Equalizer(0, sessionId).apply { enabled = true }
            hardwareVirtualizer = Virtualizer(0, sessionId).apply {
                enabled = currentAudioSettings.spatial3dEnabled
            }
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = currentAudioSettings.normalizeEnabled
                if (currentAudioSettings.normalizeEnabled) {
                    setTargetGain(250)
                }
            }
            applyHardwareEffects(currentAudioSettings)
        }
    }

    private fun applyHardwareEffects(settings: AudioSettingsState) {
        runCatching {
            hardwareEq?.let { eq ->
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                for (b in 0 until bandCount) {
                    val mappedIdx = ((b.toFloat() / bandCount.coerceAtLeast(1)) * settings.eqGainsDb.size).toInt()
                        .coerceIn(0, settings.eqGainsDb.lastIndex)
                    val targetMb = (settings.eqGainsDb[mappedIdx] * 100f).toInt().coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(b.toShort(), targetMb.toShort())
                }
            }
            hardwareVirtualizer?.let { virt ->
                virt.enabled = settings.spatial3dEnabled
                if (settings.spatial3dEnabled && virt.strengthSupported) {
                    virt.setStrength(750.toShort())
                }
            }
            loudnessEnhancer?.let { le ->
                le.enabled = settings.normalizeEnabled
                if (settings.normalizeEnabled) le.setTargetGain(250)
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastServiceSyncMs = 0L
            while (isActive && _isPlaying.value) {
                val track = activeTrack
                val mp = mediaPlayer
                if (track != null && mp != null && isPrepared) {
                    val playingNow = runCatching { mp.isPlaying }.getOrDefault(false)
                    val posMs = runCatching { mp.currentPosition }.getOrDefault(-1)
                    if (playingNow && posMs >= 0) {
                        val posSec = posMs / 1000f
                        _currentPositionSec.value = posSec

                        val durMs = runCatching { mp.duration.toLong() }.getOrDefault(0L)
                        if (durMs > 0L && activeTrackDurationMs != durMs) {
                            activeTrackDurationMs = durMs
                            _durationSec.value = ((durMs + 500L) / 1000L).toInt().coerceAtLeast(1)
                        }

                        val timeline = analyzedSpectrum
                        if (timeline != null) {
                            _spectrumBars.value = timeline.barsAt(posMs.toLong())
                        } else {
                            val baseLevel = ((if (_isMuted.value) 0f else _volume.value) * 0.44f).coerceIn(0.10f, 0.52f)
                            _spectrumBars.value = List(36) { idx ->
                                val ripple = abs(sin((posMs / 170.0) + idx * 0.48)).toFloat()
                                (baseLevel * 0.65f + ripple * 0.35f).coerceIn(0.08f, 0.90f)
                            }
                        }

                        val totalSec = _durationSec.value.coerceAtLeast(track.durationSec)
                        if (currentAudioSettings.crossfadeEnabled && totalSec > 8) {
                            val fadeIn = (posSec / 2.0f).coerceIn(0.15f, 1.0f)
                            val remaining = (totalSec - posSec).coerceAtLeast(0f)
                            val fadeOut = if (remaining < 3.5f) (remaining / 3.5f).coerceIn(0.10f, 1.0f) else 1.0f
                            currentCrossfadeGain = minOf(fadeIn, fadeOut)
                            applyEffectiveVolume()
                        } else if (currentCrossfadeGain != 1.0f) {
                            currentCrossfadeGain = 1.0f
                            applyEffectiveVolume()
                        }

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastServiceSyncMs >= 3_000L) {
                            lastServiceSyncMs = now
                            AuraPlaybackService.syncProgress(
                                context = context,
                                positionMs = posMs.toLong(),
                                durationMs = activeTrackDurationMs,
                                isPlaying = true
                            )
                        }
                    }
                }
                delay(140L)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause()
                    }
                }
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun stopEnginesOnly() {
        progressJob?.cancel()
        isPrepared = false
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        releaseAudioEffects()
    }

    private fun releaseAudioEffects() {
        runCatching { hardwareEq?.release() }
        runCatching { hardwareVirtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        hardwareEq = null
        hardwareVirtualizer = null
        loudnessEnhancer = null
    }

    fun release() {
        transitionJob?.cancel()
        analyzerJob?.cancel()
        stopEnginesOnly()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
        AuraPlaybackService.stopService(context)
    }
}
