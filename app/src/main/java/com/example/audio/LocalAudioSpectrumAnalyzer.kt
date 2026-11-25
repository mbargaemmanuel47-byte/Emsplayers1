package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "LocalSpectrumAnalyzer"
private const val FFT_SIZE = 512

data class AnalyzedSpectrumTimeline(
    val durationMs: Long,
    val barCount: Int,
    val frames: List<List<Float>>
) {
    fun barsAt(positionMs: Long): List<Float> {
        if (frames.isEmpty() || durationMs <= 0L) return List(barCount) { 0.08f }
        if (frames.size == 1) return frames[0]

        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val exactIndex = fraction * (frames.size - 1)
        val idx0 = exactIndex.toInt().coerceIn(0, frames.lastIndex)
        val idx1 = (idx0 + 1).coerceIn(0, frames.lastIndex)
        val t = (exactIndex - idx0).coerceIn(0f, 1f)

        val f0 = frames[idx0]
        val f1 = frames[idx1]
        val count = minOf(f0.size, f1.size)
        return List(count) { i ->
            val v0 = f0[i]
            val v1 = f1[i]
            (v0 + (v1 - v0) * t).coerceIn(0.08f, 1.0f)
        }
    }
}

/**
 * Decodes real PCM audio frames via MediaExtractor + MediaCodec and computes
 * logarithmic multi-band FFT energy across the track so the spectrum visualizer
 * responds authentically without collapsing into flat static dots.
 */
object LocalAudioSpectrumAnalyzer {

    suspend fun analyze(
        context: Context,
        uri: Uri,
        barCount: Int = 36,
        targetFrames: Int = 220
    ): AnalyzedSpectrumTimeline? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return@withContext null

            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            if (durationUs <= 0L) return@withContext null

            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            } else {
                2
            }
            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else {
                44100
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val rawFrames = arrayOfNulls<List<Float>>(targetFrames)
            val bufferInfo = MediaCodec.BufferInfo()
            val frameIntervalUs = (durationUs / targetFrames.coerceAtLeast(1)).coerceAtLeast(80_000L)
            var validFramesCount = 0
            var nonSilentFramesCount = 0

            val monoWindow = FloatArray(FFT_SIZE)

            for (sampleIdx in 0 until targetFrames) {
                currentCoroutineContext().ensureActive()

                val seekUs = (sampleIdx * frameIntervalUs).coerceAtMost((durationUs - 15_000L).coerceAtLeast(0L))
                extractor.seekTo(seekUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.flush()

                var collectedMono = 0
                var attempts = 0
                var inputEos = false
                var outputEos = false
                var channelAccum = 0f
                var channelPhase = 0

                while (collectedMono < FFT_SIZE && attempts < 45 && !outputEos) {
                    attempts++

                    if (!inputEos) {
                        val inIdx = codec.dequeueInputBuffer(4_000L)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val size = extractor.readSampleData(inBuf, 0)
                                if (size < 0) {
                                    codec.queueInputBuffer(
                                        inIdx,
                                        0,
                                        0,
                                        0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputEos = true
                                } else {
                                    codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 4_000L)
                    when {
                        outIdx >= 0 -> {
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null && bufferInfo.size > 0) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                while (shortBuf.hasRemaining() && collectedMono < FFT_SIZE) {
                                    channelAccum += shortBuf.get() / 32768f
                                    channelPhase++
                                    if (channelPhase >= channelCount) {
                                        monoWindow[collectedMono++] = channelAccum / channelCount.toFloat()
                                        channelAccum = 0f
                                        channelPhase = 0
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputEos = true
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFmt = codec.outputFormat
                            if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                            }
                            if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
                            }
                            // Format change does not count against decode progress
                            attempts--
                        }
                    }
                }

                if (collectedMono >= 128) {
                    val bands = computeFftBands(
                        samples = monoWindow,
                        sampleCount = collectedMono,
                        sampleRate = sampleRate,
                        barCount = barCount
                    )
                    rawFrames[sampleIdx] = bands
                    validFramesCount++
                    if (bands.any { it > 0.14f }) {
                        nonSilentFramesCount++
                    }
                }
            }

            // If hardware decoder couldn't decode sufficient real frames (e.g. exotic stream),
            // return null so live harmonic visualizer takes over seamlessly instead of freezing flat.
            if (validFramesCount < (targetFrames / 5).coerceAtLeast(8) || nonSilentFramesCount < 3) {
                Log.w(TAG, "Insufficient decoded PCM frames ($validFramesCount/$targetFrames); using live harmonic mode.")
                return@withContext null
            }

            val repairedFrames = interpolateMissingFrames(rawFrames, barCount)
            AnalyzedSpectrumTimeline(
                durationMs = durationUs / 1000L,
                barCount = barCount,
                frames = repairedFrames
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to decode local PCM spectrum for $uri: ${e.message}")
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun interpolateMissingFrames(
        rawFrames: Array<List<Float>?>,
        barCount: Int
    ): List<List<Float>> {
        val result = MutableList(rawFrames.size) { List(barCount) { 0.12f } }
        var lastValidIdx = -1
        for (i in rawFrames.indices) {
            val frame = rawFrames[i]
            if (frame != null) {
                result[i] = frame
                if (lastValidIdx == -1 && i > 0) {
                    for (fill in 0 until i) {
                        result[fill] = frame
                    }
                } else if (lastValidIdx >= 0 && i - lastValidIdx > 1) {
                    val prev = result[lastValidIdx]
                    val span = (i - lastValidIdx).toFloat()
                    for (fill in (lastValidIdx + 1) until i) {
                        val t = (fill - lastValidIdx) / span
                        result[fill] = List(barCount) { b ->
                            (prev[b] + (frame[b] - prev[b]) * t).coerceIn(0.08f, 1.0f)
                        }
                    }
                }
                lastValidIdx = i
            }
        }
        if (lastValidIdx in 0 until rawFrames.lastIndex) {
            val tail = result[lastValidIdx]
            for (fill in (lastValidIdx + 1) until rawFrames.size) {
                result[fill] = tail
            }
        }
        return result
    }

    internal fun computeFftBands(
        samples: FloatArray,
        sampleCount: Int,
        sampleRate: Int,
        barCount: Int
    ): List<Float> {
        val n = FFT_SIZE
        val real = FloatArray(n)
        val imag = FloatArray(n)
        val usable = sampleCount.coerceIn(1, n)

        var rmsSum = 0f
        for (i in 0 until usable) {
            val s = samples[i]
            rmsSum += s * s
            // Hann window prevents spectral leakage
            val hann = (0.5 * (1.0 - cos(2.0 * PI * i / (usable - 1).coerceAtLeast(1)))).toFloat()
            real[i] = s * hann
        }
        val frameRms = sqrt(rmsSum / usable)
        if (frameRms < 0.0025f) {
            return List(barCount) { 0.08f }
        }

        // In-place iterative radix-2 Cooley-Tukey FFT
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val half = len shr 1
            val angle = (-2.0 * PI / len)
            val wLenR = cos(angle).toFloat()
            val wLenI = sin(angle).toFloat()
            var start = 0
            while (start < n) {
                var wR = 1f
                var wI = 0f
                for (k in 0 until half) {
                    val uR = real[start + k]
                    val uI = imag[start + k]
                    val idx2 = start + k + half
                    val vR = real[idx2] * wR - imag[idx2] * wI
                    val vI = real[idx2] * wI + imag[idx2] * wR
                    real[start + k] = uR + vR
                    imag[start + k] = uI + vI
                    real[idx2] = uR - vR
                    imag[idx2] = uI - vI
                    val nextWR = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = nextWR
                }
                start += len
            }
            len = len shl 1
        }

        val halfBins = n / 2
        val nyquist = (sampleRate / 2f).coerceAtLeast(4000f)
        val minFreq = 48f
        val maxFreq = minOf(nyquist * 0.85f, 12500f).coerceAtLeast(2500f)
        val normFactor = n * 0.32f

        val rawBands = FloatArray(barCount)
        var maxBand = 0.0001f

        for (b in 0 until barCount) {
            val fLow = minFreq * (maxFreq / minFreq).toDouble().pow(b.toDouble() / barCount)
            val fHigh = minFreq * (maxFreq / minFreq).toDouble().pow((b + 1).toDouble() / barCount)
            val binStart = ((fLow / nyquist) * halfBins).toInt().coerceIn(1, halfBins - 1)
            val binEnd = ((fHigh / nyquist) * halfBins).toInt().coerceIn(binStart, halfBins - 1)

            var magSum = 0f
            var binCount = 0
            for (bin in binStart..binEnd) {
                val mag = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin]) / normFactor
                magSum += mag
                binCount++
            }
            val avgMag = magSum / binCount.coerceAtLeast(1)
            // Perceptual weighting: boost upper mids/highs slightly so right side stays lively
            val tiltBoost = 1f + (b.toFloat() / barCount) * 1.15f
            val perceived = (ln(1f + avgMag * tiltBoost * 24f) / 3.4f).coerceAtLeast(0f)
            rawBands[b] = perceived
            if (perceived > maxBand) maxBand = perceived
        }

        val loudnessScale = (frameRms * 3.4f).coerceIn(0.32f, 1.0f)
        return List(barCount) { b ->
            val rel = (rawBands[b] / maxBand.coerceAtLeast(0.08f)).coerceIn(0f, 1f)
            (0.10f + rel * 0.86f * loudnessScale).coerceIn(0.10f, 1.0f)
        }
    }
}
