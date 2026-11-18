package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.SynthProfile
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMediaScanner {

    suspend fun scanDeviceAudio(context: Context, limit: Int = 40): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val mediaId = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol).orEmpty().ifBlank { "Piste locale" }
                    val artist = cursor.getString(artistCol).orEmpty().let {
                        if (it.isBlank() || it == "<unknown>") "Artiste inconnu" else it
                    }
                    val album = cursor.getString(albumCol).orEmpty().ifBlank { "Bibliothèque Appareil" }
                    val durationMs = cursor.getLong(durCol).coerceAtLeast(1000L)
                    val contentUri = ContentUris.withAppendedId(collection, mediaId)

                    tracks.add(
                        buildLocalTrack(
                            id = 10_000L + mediaId,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = "Audio Local",
                            durationSec = (durationMs / 1000L).toInt().coerceAtLeast(1),
                            uriString = contentUri.toString()
                        )
                    )
                    count++
                }
            }
        }
        tracks
    }

    suspend fun extractTrackFromUri(context: Context, uri: Uri): Track = withContext(Dispatchers.IO) {
        var title = "Audio Importé"
        var artist = "Artiste inconnu"
        var album = "Import SAF"
        var genre = "Audio Local"
        var durationSec = 180

        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        title = c.getString(idx).substringBeforeLast(".")
                    }
                }
            }
        }

        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                title = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                artist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                album = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() }?.let {
                genre = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { ms ->
                durationSec = (ms / 1000L).toInt().coerceAtLeast(1)
            }
        }
        runCatching { retriever.release() }

        buildLocalTrack(
            id = System.currentTimeMillis(),
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            durationSec = durationSec,
            uriString = uri.toString()
        )
    }

    private fun buildLocalTrack(
        id: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        durationSec: Int,
        uriString: String
    ): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            mood = "chill",
            bpm = 0,
            musicalKey = "—",
            energy = 0,
            durationSec = durationSec,
            isLiked = false,
            primaryColorHex = 0xFFD4AF37,
            secondaryColorHex = 0xFF1D4ED8,
            tertiaryColorHex = 0xFF06070B,
            synthProfile = SynthProfile(
                scaleHz = emptyList(),
                bassHz = emptyList(),
                waveType = "pcm",
                rhythmPattern = emptyList(),
                groove = 0,
                warmth = 0,
                clarity = 0
            ),
            aiInsight = "Fichier audio local lu nativement via MediaPlayer et traité par l'égaliseur matériel Android.",
            lyrics = emptyList(),
            localUri = uriString,
            isAiGenerated = false
        )
    }
}
