package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Real-time 48kHz Stereo PCM Procedural Synthesizer using Android AudioTrack (API 23+).
 * Supports sample-accurate seeking, 7-band tonal shaping, crossfade gain ramping, and 3D stereo widening.
 */
class ProceduralSynthEngine(
    private val scope: CoroutineScope,
    private val onSpectrumUpdate: (List<Float>) -> Unit,
    private val onSessionIdReady: (Int) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private val sampleRate = 48000
    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null
    private val sampleCursor = AtomicLong(0L)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentTrack: Track? = null

    @Volatile
    private var volume: Float = 0.8f

    @Volatile
    private var crossfadeGain: Float = 1.0f

    @Volatile
    private var eqGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)

    @Volatile
    private var spatial3d: Boolean = false

    @Volatile
    private var normalizeLufs: Boolean = true

    fun start(track: Track, startPositionSec: Float = 0f) {
        currentTrack = track
        stop()
        sampleCursor.set((startPositionSec.coerceAtLeast(0f) * sampleRate).toLong())
        isPlaying = true

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val trackOut = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull()

        if (trackOut == null || trackOut.state != AudioTrack.STATE_INITIALIZED) {
            isPlaying = false
            runCatching { trackOut?.release() }
            onError("Impossible d'initialiser le moteur audio AudioTrack 48kHz sur cet appareil.")
            return
        }

        audioTrack = trackOut
        onSessionIdReady(trackOut.audioSessionId)
        trackOut.play()

        renderJob = scope.launch(Dispatchers.Default) {
            val framesPerChunk = 1024
            val pcm = ShortArray(framesPerChunk * 2)
            var lowFilterL = 0.0
            var lowFilterR = 0.0
            var highPrevL = 0.0
            var highPrevR = 0.0
            var lastInL = 0.0
            var lastInR = 0.0

            val bars = MutableList(42) { 0.08f }

            while (isActive && isPlaying) {
                val activeTrack = currentTrack ?: break
                val cursorStart = sampleCursor.get()
                val profile = activeTrack.synthProfile
                val bpm = activeTrack.bpm.coerceIn(60, 180)
                val stepDurationSamples = (sampleRate * 60.0 / bpm / 2.0).toLong().coerceAtLeast(2400L)

                val gains = eqGainsDb
                val lowGain = dbToLinear((gains.getOrElse(0) { 0f } + gains.getOrElse(1) { 0f }) * 0.4f)
                val midGain = dbToLinear((gains.getOrElse(2) { 0f } + gains.getOrElse(3) { 0f } + gains.getOrElse(4) { 0f }) * 0.25f)
                val highGain = dbToLinear((gains.getOrElse(5) { 0f } + gains.getOrElse(6) { 0f }) * 0.4f)

                val masterVol = (volume * crossfadeGain * if (normalizeLufs) 0.55f else 0.48f).toDouble()
                val scale = profile.scaleHz.ifEmpty { listOf(220.0, 261.63, 329.63, 392.0) }
                val bass = profile.bassHz.ifEmpty { listOf(87.31, 65.41) }
                val pattern = profile.rhythmPattern.ifEmpty { listOf(1f, 0f, 0.5f, 1f) }

                var lowEnergy = 0.0
                var midEnergy = 0.0
                var highEnergy = 0.0

                for (i in 0 until framesPerChunk) {
                    val globalSample = cursorStart + i
                    val stepIndex = (globalSample / stepDurationSamples).toInt()
                    val step = stepIndex % pattern.size
                    val stepTimeSec = (globalSample % stepDurationSamples).toDouble() / sampleRate
                    val velocity = pattern[step].toDouble()

                    var kickSample = 0.0
                    if (step % 4 == 0 || (activeTrack.mood == "zouk" && step % 8 == 3)) {
                        if (stepTimeSec < 0.26) {
                            val kickFreq = 42.0 + 83.0 * exp(-stepTimeSec * 22.0)
                            val kickEnv = exp(-stepTimeSec * 14.0)
                            kickSample = sin(2.0 * PI * kickFreq * stepTimeSec) * kickEnv * 0.45
                        }
                    }

                    var bassSample = 0.0
                    if (velocity > 0.0) {
                        val barIndex = (stepIndex / 8) % bass.size
                        val bassFreq = bass[barIndex]
                        val bassEnv = exp(-stepTimeSec * 5.5) * velocity
                        val phase = (globalSample.toDouble() * bassFreq / sampleRate) % 1.0
                        val tri = 4.0 * abs(phase - 0.5) - 1.0
                        bassSample = tri * bassEnv * 0.30
                    }

                    var leadSample = 0.0
                    if (step % 2 == 0 || velocity >= 0.7) {
                        val noteIdx = (stepIndex + stepIndex / 4) % scale.size
                        val freq = scale[noteIdx]
                        val attack = (stepTimeSec / 0.025).coerceAtMost(1.0)
                        val decay = exp(-stepTimeSec * 3.8)
                        val amp = (if (activeTrack.mood == "focus") 0.16 else 0.20) * (if (velocity > 0.0) velocity else 0.5) * attack * decay

                        val phase = (globalSample.toDouble() * freq / sampleRate) % 1.0
                        val fifthPhase = (globalSample.toDouble() * (freq * 1.498) / sampleRate) % 1.0
                        val osc = when (profile.waveType) {
                            "sawtooth" -> (2.0 * phase - 1.0) * 0.65 + sin(2.0 * PI * fifthPhase) * 0.25
                            "square" -> (if (phase < 0.5) 0.55 else -0.55) + sin(2.0 * PI * phase) * 0.35
                            "triangle" -> (4.0 * abs(phase - 0.5) - 1.0) * 0.75 + sin(2.0 * PI * fifthPhase) * 0.20
                            else -> sin(2.0 * PI * phase) * 0.8 + sin(2.0 * PI * fifthPhase) * 0.22
                        }
                        leadSample = osc * amp
                    }

                    var hatSample = 0.0
                    if (step % 2 == 1 && stepTimeSec < 0.045) {
                        val hatEnv = exp(-stepTimeSec * 70.0)
                        hatSample = (Random.nextDouble() * 2.0 - 1.0) * hatEnv * 0.055
                    }

                    val monoRaw = kickSample + bassSample + leadSample + hatSample

                    val pan = if (spatial3d) sin(globalSample.toDouble() / sampleRate * 1.4) * 0.42 else 0.08 * sin(globalSample.toDouble() / sampleRate * 0.7)
                    val rawL = monoRaw * (1.0 - pan * 0.45) + leadSample * 0.12 * cos(globalSample.toDouble() / sampleRate * 2.1)
                    val rawR = monoRaw * (1.0 + pan * 0.45) - leadSample * 0.12 * cos(globalSample.toDouble() / sampleRate * 2.1)

                    lowFilterL += 0.045 * (rawL - lowFilterL)
                    lowFilterR += 0.045 * (rawR - lowFilterR)
                    highPrevL = 0.85 * (highPrevL + rawL - lastInL)
                    highPrevR = 0.85 * (highPrevR + rawR - lastInR)
                    lastInL = rawL
                    lastInR = rawR

                    val midL = rawL - lowFilterL - highPrevL * 0.4
                    val midR = rawR - lowFilterR - highPrevR * 0.4

                    lowEnergy = maxOf(lowEnergy, abs(lowFilterL * lowGain))
                    midEnergy = maxOf(midEnergy, abs(midL * midGain))
                    highEnergy = maxOf(highEnergy, abs(highPrevL * 0.4 * highGain))

                    val shapedL = tanh((lowFilterL * lowGain + midL * midGain + highPrevL * 0.4 * highGain) * masterVol)
                    val shapedR = tanh((lowFilterR * lowGain + midR * midGain + highPrevR * 0.4 * highGain) * masterVol)

                    pcm[i * 2] = (shapedL.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
                    pcm[i * 2 + 1] = (shapedR.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
                }

                sampleCursor.compareAndSet(cursorStart, cursorStart + framesPerChunk)

                // Derive 42 spectrum bars from real low/mid/high DSP energy bands
                for (b in bars.indices) {
                    val ratio = b.toFloat() / bars.lastIndex.coerceAtLeast(1)
                    val bandEnergy = when {
                        ratio < 0.30f -> lowEnergy.toFloat() * 1.9f
                        ratio < 0.72f -> midEnergy.toFloat() * 1.8f
                        else -> highEnergy.toFloat() * 2.2f
                    }
                    val target = (bandEnergy * masterVol.toFloat() * 1.8f).coerceIn(0.06f, 1.0f)
                    bars[b] = bars[b] * 0.55f + target * 0.45f
                }
                onSpectrumUpdate(bars.toList())

                runCatching {
                    trackOut.write(pcm, 0, pcm.size)
                }
            }
        }
    }

    fun seekToSeconds(sec: Float) {
        sampleCursor.set((sec.coerceAtLeast(0f) * sampleRate).toLong())
    }

    fun updateVolume(newVol: Float) {
        volume = newVol.coerceIn(0f, 1f)
    }

    fun updateCrossfadeGain(gain: Float) {
        crossfadeGain = gain.coerceIn(0f, 1f)
    }

    fun updateEqGains(gainsDb: List<Float>) {
        eqGainsDb = gainsDb
    }

    fun updateDspOptions(spatial: Boolean, normalize: Boolean) {
        spatial3d = spatial
        normalizeLufs = normalize
    }

    fun stop() {
        isPlaying = false
        renderJob?.cancel()
        renderJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        onSpectrumUpdate(List(42) { 0.06f })
    }

    private fun dbToLinear(db: Float): Double {
        return Math.pow(10.0, (db / 20.0).toDouble()).coerceIn(0.35, 2.6)
    }
}
