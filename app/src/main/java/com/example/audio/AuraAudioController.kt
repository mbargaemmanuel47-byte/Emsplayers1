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

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _spectrumBars = MutableStateFlow(List(42) { 0.06f })
    val spectrumBars: StateFlow<List<Float>> = _spectrumBars.asStateFlow()

    private val _dspStatus = MutableStateFlow("Prêt (Appuyez sur Lecture)")
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

    private val synthEngine = ProceduralSynthEngine(
        scope = scope,
        onSpectrumUpdate = { bars -> _spectrumBars.value = bars },
        onSessionIdReady = { sessionId -> attachAudioEffects(sessionId) },
        onError = { msg ->
            _isPlaying.value = false
            _dspStatus.value = "Erreur AudioTrack"
            onPlaybackErrorCallback?.invoke(msg)
        }
    )

    fun playTrack(track: Track, startPositionSec: Float = 0f) {
        transitionJob?.cancel()
        activeTrack = track
        _currentPositionSec.value = startPositionSec
        analyzedSpectrum = null
        analyzerJob?.cancel()

        if (!requestAudioFocus()) {
            _isPlaying.value = false
            _dspStatus.value = "Focus audio refusé par le système"
            onPlaybackErrorCallback?.invoke("Impossible d'obtenir le focus audio système.")
            return
        }

        stopEnginesOnly()
        _isPlaying.value = true
        currentCrossfadeGain = if (currentAudioSettings.crossfadeEnabled) 0.20f else 1.0f

        if (track.localUri != null) {
            startLocalMediaPlayer(track, startPositionSec)
        } else {
            synthEngine.updateVolume(if (_isMuted.value) 0f else _volume.value)
            synthEngine.updateCrossfadeGain(currentCrossfadeGain)
            synthEngine.updateEqGains(currentAudioSettings.eqGainsDb)
            synthEngine.updateDspOptions(
                spatial = currentAudioSettings.spatial3dEnabled,
                normalize = currentAudioSettings.normalizeEnabled
            )
            synthEngine.start(track, startPositionSec)
            _dspStatus.value = "Actif • Synthèse 48kHz DSP"
        }

        startProgressLoop()
        AuraPlaybackService.updateNotification(context, track.title, track.artist, true)
    }

    fun pause() {
        transitionJob?.cancel()
        _isPlaying.value = false
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
        synthEngine.stop()
        progressJob?.cancel()
        _spectrumBars.value = List(42) { 0.06f }
        _dspStatus.value = "En pause • DSP en veille"
        activeTrack?.let {
            AuraPlaybackService.updateNotification(context, it.title, it.artist, false)
        }
    }

    fun resume(fallbackTrack: Track?) {
        val target = activeTrack ?: fallbackTrack ?: return
        if (!requestAudioFocus()) {
            onPlaybackErrorCallback?.invoke("Focus audio indisponible.")
            return
        }

        if (target.localUri != null && mediaPlayer != null) {
            _isPlaying.value = true
            runCatching { mediaPlayer?.start() }
            startProgressLoop()
            _dspStatus.value = "Actif • MediaPlayer AudioFX"
            AuraPlaybackService.updateNotification(context, target.title, target.artist, true)
        } else {
            val savedPos = _currentPositionSec.value
            playTrack(target, startPositionSec = savedPos)
        }
    }

    fun seekToFraction(fraction: Float) {
        val track = activeTrack ?: return
        val newSec = (fraction.coerceIn(0f, 1f) * track.durationSec)
        seekToSeconds(newSec)
    }

    fun seekToSeconds(sec: Float) {
        val track = activeTrack ?: return
        val clamped = sec.coerceIn(0f, track.durationSec.toFloat().coerceAtLeast(1f))
        _currentPositionSec.value = clamped
        if (track.localUri != null) {
            mediaPlayer?.let { mp ->
                runCatching { mp.seekTo((clamped * 1000).toInt()) }
            }
        } else {
            synthEngine.seekToSeconds(clamped)
        }
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
        synthEngine.updateEqGains(settings.eqGainsDb)
        synthEngine.updateDspOptions(
            spatial = settings.spatial3dEnabled,
            normalize = settings.normalizeEnabled
        )
        applyHardwareEffects(settings)
    }

    fun updateActiveTrackDuration(newDurationSec: Int) {
        activeTrack = activeTrack?.copy(durationSec = newDurationSec)
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
        synthEngine.updateVolume(base)
        synthEngine.updateCrossfadeGain(currentCrossfadeGain)
        runCatching { mediaPlayer?.setVolume(effective, effective) }
    }

    private fun startLocalMediaPlayer(track: Track, startPositionSec: Float) {
        val uri = Uri.parse(track.localUri)

        // Decode real PCM spectrum timeline asynchronously without requiring RECORD_AUDIO
        analyzerJob = scope.launch {
            analyzedSpectrum = LocalAudioSpectrumAnalyzer.analyze(context, uri)
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
                    if (startPositionSec > 0f) {
                        runCatching { player.seekTo((startPositionSec * 1000).toInt()) }
                    }
                    player.start()
                    _dspStatus.value = "Actif • Fichier Local AudioFX"
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
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra for uri=$uri")
                    _isPlaying.value = false
                    _spectrumBars.value = List(42) { 0.06f }
                    _dspStatus.value = "Erreur lecture fichier ($what)"
                    onPlaybackErrorCallback?.invoke("Impossible de lire ce fichier audio (code $what).")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open local audio URI: $uri", e)
            _isPlaying.value = false
            _spectrumBars.value = List(42) { 0.06f }
            _dspStatus.value = "Fichier audio inaccessible"
            onPlaybackErrorCallback?.invoke(e.localizedMessage ?: "Fichier audio introuvable ou autorisation révoquée.")
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
                val mp = mediaPlayer

                val posSec = if (track.localUri != null && mp != null) {
                    val ms = runCatching {
                        if (mp.isPlaying) mp.currentPosition else (_currentPositionSec.value * 1000f).toInt()
                    }.getOrDefault(0)
                    val sec = ms / 1000f
                    _currentPositionSec.value = sec

                    // Drive visualizer from decoded real PCM energy timeline
                    val timeline = analyzedSpectrum
                    if (timeline != null) {
                        _spectrumBars.value = timeline.barsAt(ms.toLong())
                    } else {
                        val level = ((if (_isMuted.value) 0f else _volume.value) * 0.45f).coerceIn(0.08f, 0.6f)
                        _spectrumBars.value = List(42) { level }
                    }
                    sec
                } else {
                    val nextPos = _currentPositionSec.value + 0.18f
                    if (nextPos >= track.durationSec) {
                        if (_isRepeat.value) {
                            _currentPositionSec.value = 0f
                            synthEngine.seekToSeconds(0f)
                            0f
                        } else {
                            onTrackFinishedCallback?.invoke()
                            break
                        }
                    } else {
                        _currentPositionSec.value = nextPos
                        nextPos
                    }
                }

                // Real-time crossfade gain envelope (2.0s fade-in, 3.5s fade-out)
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
        synthEngine.stop()
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
