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

    private var activeTrackDurationMs: Long = 0L

    fun playTrack(track: Track, startPositionSec: Float = 0f) {
        transitionJob?.cancel()
        val uriStr = track.localUri
        if (uriStr.isNullOrBlank()) {
            _isPlaying.value = false
            _dspStatus.value = "Fichier introuvable"
            onPlaybackErrorCallback?.invoke("Erreur : ce fichier audio n'est pas disponible.")
            return
        }

        activeTrack = track
        activeTrackDurationMs = (track.durationSec.toLong().coerceAtLeast(0L)) * 1000L
        _currentPositionSec.value = startPositionSec
        analyzedSpectrum = null
        analyzerJob?.cancel()

        if (!requestAudioFocus()) {
            _isPlaying.value = false
            _dspStatus.value = "Son bloqué"
            onPlaybackErrorCallback?.invoke("Erreur : le son est utilisé par une autre application.")
            return
        }

        stopEnginesOnly()
        _isPlaying.value = true
        currentCrossfadeGain = if (currentAudioSettings.crossfadeEnabled) 0.25f else 1.0f

        startLocalMediaPlayer(track, uriStr, startPositionSec)
        startProgressLoop()
        pushNotificationUpdate(track, isPlaying = true, positionMs = (startPositionSec * 1000f).toLong())
    }

    fun pause() {
        transitionJob?.cancel()
        _isPlaying.value = false
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
        progressJob?.cancel()
        _spectrumBars.value = List(36) { 0.08f }
        _dspStatus.value = "En pause"
        activeTrack?.let {
            pushNotificationUpdate(it, isPlaying = false, positionMs = (_currentPositionSec.value * 1000f).toLong())
        }
    }

    fun pauseWithoutNotification() {
        transitionJob?.cancel()
        _isPlaying.value = false
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
        progressJob?.cancel()
        _spectrumBars.value = List(36) { 0.08f }
        _dspStatus.value = "En pause"
    }

    fun resume(fallbackTrack: Track?) {
        val target = activeTrack ?: fallbackTrack ?: return
        if (!requestAudioFocus()) {
            onPlaybackErrorCallback?.invoke("Erreur d'accès à la sortie audio.")
            return
        }

        if (mediaPlayer != null) {
            _isPlaying.value = true
            runCatching { mediaPlayer?.start() }
            startProgressLoop()
            _dspStatus.value = "Lecture"
            pushNotificationUpdate(target, isPlaying = true, positionMs = (_currentPositionSec.value * 1000f).toLong())
        } else {
            val savedPos = _currentPositionSec.value
            playTrack(target, startPositionSec = savedPos)
        }
    }

    fun seekToFraction(fraction: Float) {
        val track = activeTrack ?: return
        val effectiveDurationSec = if (track.durationSec > 0) track.durationSec else (activeTrackDurationMs / 1000L).toInt()
        val newSec = (fraction.coerceIn(0f, 1f) * effectiveDurationSec.coerceAtLeast(1))
        seekToSeconds(newSec)
    }

    fun seekToSeconds(sec: Float) {
        val track = activeTrack ?: return
        val maxSec = maxOf(track.durationSec.toFloat(), activeTrackDurationMs / 1000f, 1f)
        val clamped = sec.coerceIn(0f, maxSec)
        _currentPositionSec.value = clamped
        mediaPlayer?.let { mp ->
            runCatching { mp.seekTo((clamped * 1000).toInt()) }
        }
        pushNotificationUpdate(track, isPlaying = _isPlaying.value, positionMs = (clamped * 1000f).toLong())
    }

    private fun pushNotificationUpdate(track: Track, isPlaying: Boolean, positionMs: Long) {
        val durationMs = if (activeTrackDurationMs > 0L) activeTrackDurationMs else (track.durationSec.toLong().coerceAtLeast(0L) * 1000L)
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

    private fun startLocalMediaPlayer(track: Track, uriStr: String, startPositionSec: Float) {
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
                setDataSource(context, uri)
                val base = if (_isMuted.value) 0f else _volume.value
                val effective = base * currentCrossfadeGain
                setVolume(effective, effective)
                setOnPreparedListener { player ->
                    attachAudioEffects(player.audioSessionId)
                    val realDuration = runCatching { player.duration.toLong() }.getOrDefault(0L)
                    if (realDuration > 0L) {
                        activeTrackDurationMs = realDuration
                    }
                    if (startPositionSec > 0f) {
                        runCatching { player.seekTo((startPositionSec * 1000).toInt()) }
                    }
                    player.start()
                    _dspStatus.value = "Lecture"
                    pushNotificationUpdate(track, isPlaying = true, positionMs = (startPositionSec * 1000f).toLong())
                }
                setOnCompletionListener { player ->
                    if (_isRepeat.value) {
                        _currentPositionSec.value = 0f
                        runCatching {
                            player.seekTo(0)
                            player.start()
                        }
                    } else {
                        onTrackFinishedCallback?.invoke()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error what=$what extra=$extra for uri=$uri")
                    _isPlaying.value = false
                    _spectrumBars.value = List(36) { 0.08f }
                    _dspStatus.value = "Erreur lecture"
                    onPlaybackErrorCallback?.invoke("Erreur de lecture de ce fichier audio.")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open local audio URI: $uri", e)
            _isPlaying.value = false
            _spectrumBars.value = List(36) { 0.08f }
            _dspStatus.value = "Fichier inaccessible"
            onPlaybackErrorCallback?.invoke("Erreur d'ouverture : fichier introuvable ou accès refusé.")
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
            while (isActive && _isPlaying.value) {
                delay(180L)
                val track = activeTrack ?: continue
                val mp = mediaPlayer ?: continue

                val ms = runCatching {
                    if (mp.isPlaying) mp.currentPosition else (_currentPositionSec.value * 1000f).toInt()
                }.getOrDefault(0)
                val posSec = ms / 1000f
                _currentPositionSec.value = posSec

                val timeline = analyzedSpectrum
                if (timeline != null) {
                    _spectrumBars.value = timeline.barsAt(ms.toLong())
                } else {
                    val level = ((if (_isMuted.value) 0f else _volume.value) * 0.42f).coerceIn(0.08f, 0.55f)
                    _spectrumBars.value = List(36) { level }
                }

                if (currentAudioSettings.crossfadeEnabled && track.durationSec > 8) {
                    val fadeIn = (posSec / 2.0f).coerceIn(0.15f, 1.0f)
                    val remaining = (track.durationSec - posSec).coerceAtLeast(0f)
                    val fadeOut = if (remaining < 3.5f) (remaining / 3.5f).coerceIn(0.10f, 1.0f) else 1.0f
                    currentCrossfadeGain = minOf(fadeIn, fadeOut)
                    applyEffectiveVolume()
                } else if (currentCrossfadeGain != 1.0f) {
                    currentCrossfadeGain = 1.0f
                    applyEffectiveVolume()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun stopEnginesOnly() {
        progressJob?.cancel()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
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
