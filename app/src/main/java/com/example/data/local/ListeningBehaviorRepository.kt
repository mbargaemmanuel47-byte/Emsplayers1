package com.example.data.local

import com.example.model.ListeningBehaviorProfile
import com.example.model.ListeningTimeSlot
import com.example.model.SavedPlaylist
import com.example.model.TimeSlotHabit
import com.example.model.Track
import com.example.model.TrackTransitionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.TimeZone

class ListeningBehaviorRepository(
    private val trackDao: TrackDao,
    private val behaviorDao: ListeningBehaviorDao
) {

    companion object {
        private const val MAX_STORED_EVENTS = 1000
    }

    suspend fun recordPlayback(
        previousTrack: Track?,
        currentTrack: Track,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = timestampMs
        }
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val slot = ListeningTimeSlot.fromHour(hourOfDay)

        behaviorDao.insertListeningEvent(
            ListeningEventEntity(
                trackId = currentTrack.id,
                trackTitle = currentTrack.title,
                trackArtist = currentTrack.artist,
                trackAlbum = currentTrack.album,
                genre = currentTrack.genre.ifBlank { currentTrack.mimeFormat },
                playedAtMs = timestampMs,
                hourOfDay = hourOfDay,
                timeSlot = slot.code,
                dayOfWeek = dayOfWeek
            )
        )

        if (previousTrack != null && previousTrack.id != currentTrack.id) {
            val existing = behaviorDao.getTransition(previousTrack.id, currentTrack.id)
            val nextCount = (existing?.transitionCount ?: 0) + 1
            behaviorDao.upsertTransition(
                TrackTransitionEntity(
                    fromTrackId = previousTrack.id,
                    fromTitle = previousTrack.title,
                    toTrackId = currentTrack.id,
                    toTitle = currentTrack.title,
                    transitionCount = nextCount,
                    lastTransitionAtMs = timestampMs
                )
            )
        }

        if (behaviorDao.getEventCount() > MAX_STORED_EVENTS + 100) {
            behaviorDao.pruneOldEvents(MAX_STORED_EVENTS)
        }
    }

    suspend fun savePlaylist(
        title: String,
        description: String,
        trackIds: List<Long>,
        isAiGenerated: Boolean,
        createdAtMs: Long = System.currentTimeMillis()
    ): Long {
        val cleanIds = trackIds.distinct()
        if (cleanIds.isEmpty()) return -1L
        val existing = behaviorDao.getSavedPlaylistsOnce()
            .firstOrNull { it.title.equals(title.trim(), ignoreCase = true) }
        if (existing != null) {
            behaviorDao.insertPlaylist(
                existing.copy(
                    description = description.ifBlank { existing.description },
                    trackIdsCsv = cleanIds.joinToString(","),
                    createdAtMs = createdAtMs,
                    isAiGenerated = isAiGenerated
                )
            )
            return existing.id
        }
        return behaviorDao.insertPlaylist(
            SavedPlaylistEntity(
                title = title.trim().ifBlank { "Ma sélection" },
                description = description.trim(),
                trackIdsCsv = cleanIds.joinToString(","),
                createdAtMs = createdAtMs,
                isAiGenerated = isAiGenerated,
                playCount = 0
            )
        )
    }

    suspend fun incrementPlaylistPlayCount(playlistId: Long) {
        behaviorDao.incrementPlaylistPlayCount(playlistId)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        behaviorDao.deletePlaylist(playlistId)
    }

    fun observeSavedPlaylists(): Flow<List<SavedPlaylist>> {
        return combine(
            behaviorDao.observeSavedPlaylists(),
            trackDao.observeAllTracks()
        ) { entities, _ ->
            entities.map { it.toDomain() }
        }
    }

    fun observeBehaviorProfile(): Flow<ListeningBehaviorProfile> {
        return combine(
            trackDao.observeAllTracks(),
            behaviorDao.observeRecentEvents(400),
            behaviorDao.observeTopTransitions(15),
            behaviorDao.observeSavedPlaylists()
        ) { trackEntities, events, transitions, playlists ->
            synthesizeProfile(
                tracks = trackEntities.map { it.toDomain() },
                events = events,
                transitions = transitions,
                playlists = playlists.map { it.toDomain() },
                nowMs = System.currentTimeMillis()
            )
        }
    }

    suspend fun buildProfileSnapshot(nowMs: Long = System.currentTimeMillis()): ListeningBehaviorProfile {
        val tracks = trackDao.getAllTracksOnce().map { it.toDomain() }
        val events = behaviorDao.getRecentEventsOnce(400)
        val transitions = behaviorDao.getTopTransitionsOnce(15)
        val playlists = behaviorDao.getSavedPlaylistsOnce().map { it.toDomain() }
        return synthesizeProfile(
            tracks = tracks,
            events = events,
            transitions = transitions,
            playlists = playlists,
            nowMs = nowMs
        )
    }

    internal fun synthesizeProfile(
        tracks: List<Track>,
        events: List<ListeningEventEntity>,
        transitions: List<TrackTransitionEntity>,
        playlists: List<SavedPlaylist>,
        nowMs: Long
    ): ListeningBehaviorProfile {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = nowMs }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentSlot = ListeningTimeSlot.fromHour(currentHour)
        val approachingSlot = ListeningTimeSlot.approachingFromHour(currentHour)

        val tracksById = tracks.associateBy { it.id }
        val topTracks = tracks
            .filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(8)
        val likedTracks = tracks
            .filter { it.isLiked }
            .sortedByDescending { it.playCount }
            .take(10)

        val transitionSummaries = transitions.map {
            val fromTrack = tracksById[it.fromTrackId]
            val toTrack = tracksById[it.toTrackId]
            TrackTransitionSummary(
                fromTrackId = it.fromTrackId,
                fromTitle = fromTrack?.title ?: it.fromTitle,
                toTrackId = it.toTrackId,
                toTitle = toTrack?.title ?: it.toTitle,
                count = it.transitionCount
            )
        }

        val eventsBySlot = events.groupBy { ListeningTimeSlot.fromCode(it.timeSlot) }
        val slotHabits = ListeningTimeSlot.entries.map { slot ->
            val slotEvents = eventsBySlot[slot].orEmpty()
            val topGrouped = slotEvents
                .groupBy { it.trackId }
                .maxByOrNull { it.value.size }
            val topEvent = topGrouped?.value?.firstOrNull()
            val peakHour = slotEvents
                .groupBy { it.hourOfDay }
                .maxByOrNull { it.value.size }
                ?.key
            TimeSlotHabit(
                slot = slot,
                playCount = slotEvents.size,
                topTrackId = topGrouped?.key,
                topTrackTitle = topGrouped?.key?.let { tracksById[it]?.title } ?: topEvent?.trackTitle,
                topTrackArtist = topGrouped?.key?.let { tracksById[it]?.artist } ?: topEvent?.trackArtist,
                peakHour = peakHour
            )
        }

        val totalTrackedPlays = maxOf(events.size, tracks.sumOf { it.playCount })
        val currentHabit = slotHabits.firstOrNull { it.slot == currentSlot }
        val nextHabit = slotHabits.firstOrNull { it.slot == approachingSlot }
        val dominantWindowHabit = listOfNotNull(currentHabit, nextHabit)
            .filter { it.playCount > 0 }
            .maxByOrNull { it.playCount }
            ?: slotHabits.filter { it.playCount > 0 }.maxByOrNull { it.playCount }

        val systemPromptBlock = buildSystemPromptBlock(
            currentHour = currentHour,
            currentSlot = currentSlot,
            approachingSlot = approachingSlot,
            totalLibraryTracks = tracks.size,
            totalTrackedPlays = totalTrackedPlays,
            topTracks = topTracks,
            likedTracks = likedTracks,
            playlists = playlists,
            transitions = transitionSummaries,
            slotHabits = slotHabits
        )

        val dominantSlotLabel = slotHabits.filter { it.playCount > 0 }.maxByOrNull { it.playCount }?.slot?.label
        val compactBadgeSummary = buildList {
            add("$totalTrackedPlays écoute(s)")
            if (likedTracks.isNotEmpty()) add("${likedTracks.size} favori(s)")
            if (playlists.isNotEmpty()) add("${playlists.size} playlist(s)")
            if (dominantSlotLabel != null) add("Pic : $dominantSlotLabel")
            if (transitionSummaries.isNotEmpty()) add("${transitionSummaries.size} enchaînement(s)")
        }.joinToString(" • ")

        return ListeningBehaviorProfile(
            totalTrackedPlays = totalTrackedPlays,
            totalLibraryTracks = tracks.size,
            topTracks = topTracks,
            likedTracks = likedTracks,
            playlists = playlists,
            topTransitions = transitionSummaries,
            slotHabits = slotHabits,
            currentSlot = currentSlot,
            approachingSlot = approachingSlot,
            dominantHabitForWindow = dominantWindowHabit,
            systemPromptBlock = systemPromptBlock,
            compactBadgeSummary = compactBadgeSummary
        )
    }

    private fun buildSystemPromptBlock(
        currentHour: Int,
        currentSlot: ListeningTimeSlot,
        approachingSlot: ListeningTimeSlot,
        totalLibraryTracks: Int,
        totalTrackedPlays: Int,
        topTracks: List<Track>,
        likedTracks: List<Track>,
        playlists: List<SavedPlaylist>,
        transitions: List<TrackTransitionSummary>,
        slotHabits: List<TimeSlotHabit>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== PROFIL COMPORTEMENTAL D'ÉCOUTE DE L'UTILISATEUR ===")
        sb.appendLine(
            "Moment actuel : ${currentHour}h (${currentSlot.label}, ${currentSlot.hoursLabel}) ; " +
                "créneau suivant : ${approachingSlot.label} (${approachingSlot.hoursLabel})."
        )
        sb.appendLine(
            "Statistiques globales : $totalLibraryTracks titres locaux, $totalTrackedPlays écoutes suivies, " +
                "${likedTracks.size} favoris, ${playlists.size} playlists."
        )

        if (topTracks.isNotEmpty()) {
            val topLine = topTracks.take(6).joinToString(" ; ") {
                "\"${it.title}\" (${it.artist}, ${it.playCount} écoutes)"
            }
            sb.appendLine("Titres les plus écoutés : $topLine.")
        }

        if (likedTracks.isNotEmpty()) {
            val likedLine = likedTracks.take(6).joinToString(" ; ") {
                "\"${it.title}\" — ${it.artist}"
            }
            sb.appendLine("Titres likés : $likedLine.")
        }

        if (playlists.isNotEmpty()) {
            val plLine = playlists.take(5).joinToString(" ; ") {
                "\"${it.title}\" (${it.trackIds.size} titres)"
            }
            sb.appendLine("Playlists de l'utilisateur : $plLine.")
        }

        if (transitions.isNotEmpty()) {
            val seqLine = transitions.take(5).joinToString(" ; ") {
                "\"${it.fromTitle}\" -> \"${it.toTitle}\" (${it.count}x)"
            }
            sb.appendLine("Enchaînements fréquents : $seqLine.")
        }

        val activeSlots = slotHabits.filter { it.playCount > 0 }
        if (activeSlots.isNotEmpty()) {
            val slotLine = activeSlots.joinToString(" ; ") { habit ->
                val fav = habit.topTrackTitle?.let { " | titre phare : \"$it\"" }.orEmpty()
                val peak = habit.peakHour?.let { " | pic vers ${it}h" }.orEmpty()
                "${habit.slot.label} (${habit.playCount} écoutes$peak$fav)"
            }
            sb.appendLine("Habitudes horaires : $slotLine.")
        }

        sb.append(
            "Utilise ce profil comportemental réel de façon naturelle et personnalisée quand l'utilisateur te demande " +
                "quoi écouter, ses goûts, ses habitudes ou la suite de sa file d'attente."
        )
        return sb.toString()
    }
}
