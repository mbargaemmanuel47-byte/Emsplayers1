package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "LocalSpectrumAnalyzer"

data class AnalyzedSpectrumTimeline(
    val durationMs: Long,
    val frames: List<List<Float>>
) {
    fun barsAt(positionMs: Long): List<Float> {
        if (frames.isEmpty() || durationMs <= 0L) return List(42) { 0.08f }
        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val index = (fraction * (frames.size - 1)).toInt().coerceIn(0, frames.lastIndex)
        return frames[index]
    }
}

/**
 * Decodes real PCM samples from a local audio URI via MediaExtractor + MediaCodec
 * so the 42-band spectrum visualizer displays genuine audio energy rather than fake sine waves.
 */
object LocalAudioSpectrumAnalyzer {

    suspend fun analyze(
        context: Context,
        uri: Uri,
        barCount: Int = 42,
        targetFrames: Int = 96
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

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val frames = mutableListOf<List<Float>>()
            val bufferInfo = MediaCodec.BufferInfo()
            val stepUs = (durationUs / targetFrames.coerceAtLeast(1)).coerceAtLeast(150_000L)

            for (sampleIdx in 0 until targetFrames) {
                val seekUs = (sampleIdx * stepUs).coerceAtMost((durationUs - 10_000L).coerceAtLeast(0L))
                extractor.seekTo(seekUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.flush()

                val pcmWindow = ShortArray(2048)
                var collected = 0
                var attempts = 0

                while (collected < pcmWindow.size && attempts < 14) {
                    attempts++
                    val inIdx = codec.dequeueInputBuffer(8_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val size = extractor.readSampleData(inBuf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 8_000L)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            while (shortBuf.hasRemaining() && collected < pcmWindow.size) {
                                pcmWindow[collected++] = shortBuf.get()
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    }
                }

                frames.add(computeSpectrumBarsFromPcm(pcmWindow, collected, barCount))
            }

            AnalyzedSpectrumTimeline(
                durationMs = durationUs / 1000L,
                frames = frames
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to decode local PCM spectrum for $uri: ${e.message}")
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun computeSpectrumBarsFromPcm(
        samples: ShortArray,
        sampleCount: Int,
        barCount: Int
    ): List<Float> {
        if (sampleCount <= 0) return List(barCount) { 0.08f }
        val chunk = (sampleCount / barCount).coerceAtLeast(1)
        return List(barCount) { band ->
            val start = (band * chunk).coerceAtMost(sampleCount - 1)
            val end = ((band + 1) * chunk).coerceAtMost(sampleCount)
            var sumSq = 0.0
            var peak = 0f
            val count = (end - start).coerceAtLeast(1)
            for (i in start until end) {
                val norm = samples[i] / 32768.0
                sumSq += norm * norm
                val absNorm = abs(norm.toFloat())
                if (absNorm > peak) peak = absNorm
            }
            val rms = sqrt(sumSq / count).toFloat()
            val combined = (rms * 2.2f + peak * 0.45f).coerceIn(0.08f, 1.0f)
            combined
        }
    }
}
