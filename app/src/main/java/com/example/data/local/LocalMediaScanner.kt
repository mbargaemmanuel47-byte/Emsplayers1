package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object LocalMediaScanner {

    private val artworkPalettes = listOf(
        Triple(0xFFFACC15L, 0xFF1E3A8AL, 0xFF060916L),
        Triple(0xFFF59E0BL, 0xFF312E81L, 0xFF06070BL),
        Triple(0xFF38BDF8L, 0xFF1E293BL, 0xFF040B25L),
        Triple(0xFF34D399L, 0xFF0F172AL, 0xFF05131EL),
        Triple(0xFFFDE68AL, 0xFF1D4ED8L, 0xFF070B18L)
    )

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "amr", "m4b", "3gp", "3gpp", "oga", "aiff"
    )

    private val ignoredFolderNames = setOf(
        "android", ".thumbnails", ".trash", ".trashed", ".cache", "cache", "lost.dir"
    )

    suspend fun scanDeviceAudio(context: Context, limit: Int = Int.MAX_VALUE): List<Track> =
        withContext(Dispatchers.IO) {
            val resultTracks = mutableListOf<Track>()
            val seenPaths = HashSet<String>()
            val seenFingerprints = HashSet<String>()
            val seenIds = HashSet<Long>()

            fun registerIfUnique(path: String?, title: String, sizeBytes: Long, track: Track): Boolean {
                if (resultTracks.size >= limit) return false
                val normPath = path?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
                if (normPath != null && !seenPaths.add(normPath)) {
                    return false
                }
                val normTitle = title.trim().lowercase(Locale.ROOT)
                val fingerprint = if (sizeBytes > 0L) "$normTitle|$sizeBytes" else "$normTitle|${track.durationSec}"
                if (!seenFingerprints.add(fingerprint)) {
                    return false
                }
                if (!seenIds.add(track.id)) {
                    return false
                }
                resultTracks.add(track)
                return true
            }

            // 1. Scan MediaStore.Audio across ALL mounted volumes (internal + SD cards) without IS_MUSIC restriction
            val audioCollections = buildList<Uri> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        MediaStore.getExternalVolumeNames(context).forEach { vol ->
                            add(MediaStore.Audio.Media.getContentUri(vol))
                        }
                    }
                    val unionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    if (unionUri !in this) add(unionUri)
                } else {
                    add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                }
            }

            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_ALARM
            )

            for (collection in audioCollections) {
                if (resultTracks.size >= limit) break
                runCatching {
                    context.contentResolver.query(
                        collection,
                        audioProjection,
                        null,
                        null,
                        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        val displayCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                        val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                        val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        val isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                        val isRingCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                        val isNotifCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                        val isAlarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)

                        if (idCol < 0) return@use

                        while (cursor.moveToNext() && resultTracks.size < limit) {
                            val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                            val lowerPath = path.orEmpty().lowercase(Locale.ROOT)
                            if (isSystemAudioPath(lowerPath)) continue

                            val durationMs = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                            val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                            val isMusic = if (isMusicCol >= 0) cursor.getInt(isMusicCol) != 0 else false
                            val isNotif = if (isNotifCol >= 0) cursor.getInt(isNotifCol) != 0 else false
                            val isAlarm = if (isAlarmCol >= 0) cursor.getInt(isAlarmCol) != 0 else false
                            val isRing = if (isRingCol >= 0) cursor.getInt(isRingCol) != 0 else false

                            // Skip short OS alerts/notifications/ringtones, keep all real music/downloads/recordings
                            if (!isMusic && (isNotif || isAlarm)) continue
                            if (!isMusic && isRing && durationMs in 1L..45_000L && isAlertFolder(lowerPath)) continue
                            if (!isMusic && durationMs in 1L..3_999L && sizeBytes in 1L..80_000L) continue
                            if (sizeBytes in 1L..15_000L) continue

                            val mediaId = cursor.getLong(idCol)
                            val displayName = if (displayCol >= 0) cursor.getString(displayCol).orEmpty() else ""
                            val rawTitle = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else ""
                            val title = resolveDisplayTitle(rawTitle, displayName)
                            val rawArtist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty() else ""
                            val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") "Artiste inconnu" else rawArtist
                            val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol).orEmpty() else ""
                            val album = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") "Stockage local" else rawAlbum
                            val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                            val yearVal = if (yearCol >= 0) cursor.getInt(yearCol) else 0
                            val yearText = if (yearVal in 1900..2100) yearVal.toString() else ""

                            val contentUri = ContentUris.withAppendedId(collection, mediaId)
                            val durationSec = if (durationMs > 0L) (durationMs / 1000L).toInt().coerceAtLeast(1) else 180
                            val estimatedBitrate = if (sizeBytes > 0L && durationSec > 0) {
                                ((sizeBytes * 8L) / durationSec / 1000L).toInt().coerceIn(32, 3200)
                            } else {
                                0
                            }

                            val track = buildLocalTrack(
                                id = 10_000L + mediaId,
                                title = title,
                                artist = artist,
                                album = album,
                                genre = "Local",
                                durationSec = durationSec,
                                uriString = contentUri.toString(),
                                fileSizeBytes = sizeBytes,
                                bitrateKbps = estimatedBitrate,
                                mimeFormat = normalizeAudioFormat(mime, displayName.ifBlank { title }),
                                year = yearText
                            )
                            registerIfUnique(path, title, sizeBytes, track)
                        }
                    }
                }
            }

            // 2. Scan MediaStore.Files for audio MIME types not yet classified under Audio.Media
            if (resultTracks.size < limit) {
                val fileCollections = buildList<Uri> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        runCatching {
                            MediaStore.getExternalVolumeNames(context).forEach { vol ->
                                add(MediaStore.Files.getContentUri(vol))
                            }
                        }
                        val unionFiles = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                        if (unionFiles !in this) add(unionFiles)
                    } else {
                        add(MediaStore.Files.getContentUri("external"))
                    }
                }
                val fileProjection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.TITLE,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATA
                )
                val fileSelection =
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO} " +
                        "OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'audio/%' " +
                        "OR ${MediaStore.Files.FileColumns.MIME_TYPE} = 'application/ogg'"

                for (fileUri in fileCollections) {
                    if (resultTracks.size >= limit) break
                    runCatching {
                        context.contentResolver.query(
                            fileUri,
                            fileProjection,
                            fileSelection,
                            null,
                            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                            val dispCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                            val titleCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                            val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                            if (idCol < 0) return@use
                            while (cursor.moveToNext() && resultTracks.size < limit) {
                                val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                                val lowerPath = path.orEmpty().lowercase(Locale.ROOT)
                                if (isSystemAudioPath(lowerPath) || isAlertFolder(lowerPath)) continue

                                val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                                if (sizeBytes in 1L..15_000L) continue

                                val fileId = cursor.getLong(idCol)
                                val displayName = if (dispCol >= 0) cursor.getString(dispCol).orEmpty() else ""
                                val rawTitle = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else ""
                                val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                                val title = resolveDisplayTitle(rawTitle, displayName)
                                val contentUri = ContentUris.withAppendedId(fileUri, fileId)

                                val track = buildLocalTrack(
                                    id = 5_000_000_000L + fileId,
                                    title = title,
                                    artist = "Artiste inconnu",
                                    album = "Stockage local",
                                    genre = "Local",
                                    durationSec = 180,
                                    uriString = contentUri.toString(),
                                    fileSizeBytes = sizeBytes,
                                    bitrateKbps = 0,
                                    mimeFormat = normalizeAudioFormat(mime, displayName.ifBlank { title }),
                                    year = ""
                                )
                                registerIfUnique(path, title, sizeBytes, track)
                            }
                        }
                    }
                }
            }

            // 3. Recursive filesystem scan across readable internal & SD card roots for any unindexed downloads
            if (resultTracks.size < limit) {
                val roots = collectStorageRoots(context)
                val unindexedPaths = mutableListOf<String>()
                for (root in roots) {
                    if (resultTracks.size >= limit) break
                    scanDirectoryRecursive(
                        dir = root,
                        depth = 0,
                        maxDepth = 10
                    ) { file ->
                        if (resultTracks.size >= limit) return@scanDirectoryRecursive
                        val absPath = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
                        val lower = absPath.lowercase(Locale.ROOT)
                        if (isSystemAudioPath(lower) || isAlertFolder(lower)) return@scanDirectoryRecursive
                        val sizeBytes = file.length()
                        if (sizeBytes < 15_000L) return@scanDirectoryRecursive

                        val title = file.nameWithoutExtension.ifBlank { file.name }
                        val stableId = 9_000_000_000L + (absPath.hashCode().toLong() and 0x7FFFFFFFL)
                        val track = buildLocalTrack(
                            id = stableId,
                            title = title,
                            artist = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Stockage local",
                            album = "Stockage local",
                            genre = "Local",
                            durationSec = 180,
                            uriString = Uri.fromFile(file).toString(),
                            fileSizeBytes = sizeBytes,
                            bitrateKbps = 0,
                            mimeFormat = normalizeAudioFormat("", file.name),
                            year = ""
                        )
                        if (registerIfUnique(absPath, title, sizeBytes, track)) {
                            unindexedPaths.add(absPath)
                        }
                    }
                }
                if (unindexedPaths.isNotEmpty()) {
                    runCatching {
                        MediaScannerConnection.scanFile(
                            context,
                            unindexedPaths.take(100).toTypedArray(),
                            null,
                            null
                        )
                    }
                }
            }

            resultTracks
        }

    private fun isSystemAudioPath(lowerPath: String): Boolean {
        if (lowerPath.isBlank()) return false
        return lowerPath.startsWith("/system/") ||
            lowerPath.startsWith("/product/") ||
            lowerPath.startsWith("/vendor/") ||
            lowerPath.startsWith("/oem/") ||
            lowerPath.startsWith("/apex/")
    }

    private fun isAlertFolder(lowerPath: String): Boolean {
        if (lowerPath.isBlank()) return false
        return lowerPath.contains("/notifications/") ||
            lowerPath.contains("/alarms/") ||
            lowerPath.contains("/ringtones/") ||
            lowerPath.contains("/media/audio/ui/")
    }

    private fun resolveDisplayTitle(rawTitle: String, displayName: String): String {
        val cleanTitle = rawTitle.trim()
        val cleanDisplay = displayName.substringBeforeLast('.').trim()
        return when {
            cleanTitle.isNotBlank() && !cleanTitle.all { it.isDigit() } -> cleanTitle
            cleanDisplay.isNotBlank() -> cleanDisplay
            cleanTitle.isNotBlank() -> cleanTitle
            else -> "Piste audio"
        }
    }

    private fun collectStorageRoots(context: Context): List<File> {
        val roots = LinkedHashMap<String, File>()
        fun addRoot(f: File?) {
            if (f != null && f.exists() && f.isDirectory && f.canRead()) {
                val key = runCatching { f.canonicalPath }.getOrDefault(f.absolutePath)
                roots[key] = f
            }
        }
        @Suppress("DEPRECATION")
        addRoot(runCatching { Environment.getExternalStorageDirectory() }.getOrNull())
        runCatching {
            context.getExternalFilesDirs(null).forEach { appDir ->
                val path = appDir?.absolutePath ?: return@forEach
                val idx = path.indexOf("/Android/")
                if (idx > 0) {
                    addRoot(File(path.substring(0, idx)))
                }
            }
        }
        return roots.values.toList()
    }

    private fun scanDirectoryRecursive(
        dir: File,
        depth: Int,
        maxDepth: Int,
        onAudioFile: (File) -> Unit
    ) {
        if (depth > maxDepth || !dir.exists() || !dir.isDirectory || !dir.canRead()) return
        val nameLower = dir.name.lowercase(Locale.ROOT)
        if (depth > 0 && nameLower in ignoredFolderNames) return

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                scanDirectoryRecursive(child, depth + 1, maxDepth, onAudioFile)
            } else if (child.isFile) {
                val ext = child.extension.lowercase(Locale.ROOT)
                if (ext in audioExtensions) {
                    onAudioFile(child)
                }
            }
        }
    }

    suspend fun extractTrackFromUri(context: Context, uri: Uri): Track = withContext(Dispatchers.IO) {
        var title = "Audio importé"
        var artist = "Artiste inconnu"
        var album = "Importé"
        var genre = "Local"
        var durationSec = 180
        var fileSizeBytes = 0L
        var bitrateKbps = 0
        var year = ""
        val rawMime = context.contentResolver.getType(uri).orEmpty()

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val rawName = c.getString(nameIdx).orEmpty()
                        if (rawName.isNotBlank()) {
                            title = rawName.substringBeforeLast(".")
                        }
                    }
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !c.isNull(sizeIdx)) {
                        fileSizeBytes = c.getLong(sizeIdx).coerceAtLeast(0L)
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
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.let { bps ->
                bitrateKbps = (bps / 1000L).toInt().coerceAtLeast(0)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.takeIf { it.isNotBlank() }?.let {
                year = it.take(4)
            }
        }
        runCatching { retriever.release() }

        if (bitrateKbps == 0 && fileSizeBytes > 0L && durationSec > 0) {
            bitrateKbps = ((fileSizeBytes * 8L) / durationSec / 1000L).toInt().coerceIn(32, 3200)
        }

        buildLocalTrack(
            id = System.currentTimeMillis(),
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            durationSec = durationSec,
            uriString = uri.toString(),
            fileSizeBytes = fileSizeBytes,
            bitrateKbps = bitrateKbps,
            mimeFormat = normalizeAudioFormat(rawMime, title),
            year = year
        )
    }

    suspend fun readBitrateAndYear(context: Context, uriString: String): Pair<Int, String>? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            var result: Pair<Int, String>? = null
            runCatching {
                retriever.setDataSource(context, Uri.parse(uriString))
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLongOrNull()?.let { (it / 1000L).toInt().coerceAtLeast(0) } ?: 0
                val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.trim()?.take(4).orEmpty()
                if (bitrate > 0 || year.isNotEmpty()) {
                    result = bitrate to year
                }
            }
            runCatching { retriever.release() }
            result
        }

    private fun normalizeAudioFormat(mimeType: String, nameOrTitle: String): String {
        val m = mimeType.lowercase(Locale.ROOT)
        return when {
            m.contains("mpeg") || m.contains("mp3") -> "MP3"
            m.contains("flac") -> "FLAC"
            m.contains("mp4") || m.contains("m4a") || m.contains("aac") -> "M4A"
            m.contains("wav") || m.contains("x-wav") -> "WAV"
            m.contains("ogg") || m.contains("opus") -> "OGG"
            m.contains("amr") -> "AMR"
            m.contains("wma") -> "WMA"
            else -> nameOrTitle.substringAfterLast('.', "")
                .uppercase(Locale.ROOT)
                .takeIf { it.length in 2..4 } ?: "AUDIO"
        }
    }

    private fun buildLocalTrack(
        id: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        durationSec: Int,
        uriString: String,
        fileSizeBytes: Long,
        bitrateKbps: Int,
        mimeFormat: String,
        year: String
    ): Track {
        val palette = artworkPalettes[((id and 0x7FFFFFFFL) % artworkPalettes.size).toInt()]
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            mood = "local",
            bpm = 0,
            musicalKey = "—",
            energy = 0,
            durationSec = durationSec,
            isLiked = false,
            primaryColorHex = palette.first,
            secondaryColorHex = palette.second,
            tertiaryColorHex = palette.third,
            aiInsight = "",
            lyrics = emptyList(),
            localUri = uriString,
            isAiGenerated = false,
            playCount = 0,
            lastPlayedAt = 0L,
            fileSizeBytes = fileSizeBytes,
            bitrateKbps = bitrateKbps,
            mimeFormat = mimeFormat,
            year = year
        )
    }
}
