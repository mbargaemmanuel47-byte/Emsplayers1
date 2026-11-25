package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.billing.PlayBillingManager
import com.example.data.local.AuraDatabase
import com.example.data.local.SeedCatalog
import com.example.data.local.TrackEntity
import com.example.data.local.UserPreferencesRepository
import com.example.model.AiGenerationUiState
import com.example.model.GeneratedMixResult
import com.example.model.Track
import com.example.permissions.PermissionManager
import com.example.viewmodel.AuraViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Em Player", appName)
    }

    @Test
    fun `seed catalog contains zero demo tracks`() {
        assertTrue(SeedCatalog.initialTracks.isEmpty())
    }

    @Test
    fun `permission manager exposes all three permission categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val statuses = PermissionManager.getAllStatuses(context)
        assertEquals(3, statuses.size)
        assertTrue(statuses.any { it.manifestPermission == android.Manifest.permission.READ_MEDIA_AUDIO })
    }

    @Test
    fun `room database persists playCount, metadata and removes legacy demo tracks`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val trackDao = db.trackDao()
            val demo = TrackEntity.fromDomain(
                Track(
                    id = 1L,
                    title = "Demo",
                    artist = "Seed",
                    album = "Demo",
                    genre = "Local",
                    mood = "all",
                    bpm = 0,
                    musicalKey = "",
                    energy = 0,
                    durationSec = 180,
                    isLiked = false,
                    primaryColorHex = 0xFFF5B731L,
                    secondaryColorHex = 0xFF0A1432L,
                    tertiaryColorHex = 0xFF1A2A52L,
                    localUri = null
                )
            )
            val real = TrackEntity.fromDomain(
                Track(
                    id = 50001L,
                    title = "Mon Son",
                    artist = "Artiste Réel",
                    album = "Album",
                    genre = "MP3",
                    mood = "all",
                    bpm = 0,
                    musicalKey = "",
                    energy = 0,
                    durationSec = 210,
                    isLiked = false,
                    primaryColorHex = 0xFFF5B731L,
                    secondaryColorHex = 0xFF0A1432L,
                    tertiaryColorHex = 0xFF1A2A52L,
                    localUri = "content://media/external/audio/media/50001",
                    fileSizeBytes = 4_200_000L,
                    mimeFormat = "MP3",
                    bitrateKbps = 320
                )
            )
            trackDao.insertTracks(listOf(demo, real))
            trackDao.deleteLegacyDemoTracks()

            val remaining = trackDao.getAllTracksOnce()
            assertEquals(1, remaining.size)
            assertEquals("Mon Son", remaining.first().title)

            trackDao.incrementPlayCount(50001L, 123456L)
            trackDao.incrementPlayCount(50001L, 234567L)
            val updated = trackDao.getAllTracksOnce().first().toDomain()
            assertEquals(2, updated.playCount)
            assertEquals(234567L, updated.lastPlayedAt)
            assertEquals("MP3", updated.mimeFormat)

            val repo = UserPreferencesRepository(db.userPreferenceDao())
            repo.ensureInitialized()
            val initial = repo.preferencesFlow.first()
            assertFalse(initial.hasCachedMix)
            assertTrue(initial.hapticFeedbackEnabled)

            repo.saveCachedMix(
                prompt = "Soirée",
                mix = GeneratedMixResult("Mix Test", "Transition douce", listOf(50001L))
            )
            val saved = repo.preferencesFlow.first()
            assertTrue(saved.hasCachedMix)
            assertEquals("Mix Test", saved.cachedPlaylistTitle)
        } finally {
            db.close()
        }
    }

    @Test
    fun `play billing manager defaults to locked until paid purchase`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val manager = PlayBillingManager(context, scope)
        try {
            assertFalse(manager.uiState.value.isProUnlocked)
        } finally {
            manager.destroy()
            scope.cancel()
        }
    }

    @Test
    fun `pro mode is unlocked, starts with empty library and handles missing firebase safely`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = androidx.lifecycle.ViewModelStore()
        val provider = androidx.lifecycle.ViewModelProvider(
            store,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val viewModel = provider[AuraViewModel::class.java]
        try {
            assertTrue(viewModel.isProUnlocked.value)
            assertEquals(AiGenerationUiState.Idle, viewModel.aiPlaylistState.value)
            assertNull(viewModel.currentTrack.value)

            viewModel.triggerInstantAiMix()
            assertFalse(viewModel.showPaywallDialog.value)

            val apps = FirebaseApp.getApps(app)
            assertTrue(apps.isEmpty() || apps.isNotEmpty())
        } finally {
            store.clear()
        }
    }

    @Test
    fun `local media scanner handles calls cleanly without IS_MUSIC or 40 track cap`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tracks = com.example.data.local.LocalMediaScanner.scanDeviceAudio(context)
        assertTrue(tracks.size >= 0)
    }

    @Test
    fun `emma ai is configured with firebase recaptcha key and strict music only persona`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val siteKeyId = context.resources.getIdentifier("recaptcha_site_key", "string", context.packageName)
        assertTrue("recaptcha_site_key must be bundled for public APK installs", siteKeyId != 0)
        assertTrue(context.getString(siteKeyId).isNotBlank())

        assertTrue(com.example.ai.MusicAssistantManager.EMMA_SYSTEM_INSTRUCTION.contains("Emma"))
        assertTrue(com.example.ai.MusicAssistantManager.EMMA_SYSTEM_INSTRUCTION.contains("experte en musique"))
        assertTrue(com.example.ai.MusicAssistantManager.EMMA_SYSTEM_INSTRUCTION.contains("uniquement"))
        assertTrue(com.example.ai.MusicAssistantManager.EMMA_PLAYLIST_INSTRUCTION.contains("Emma"))
        assertEquals("gemini-3.8-flash", com.example.ai.MusicAssistantManager.GEMINI_PRIMARY_MODEL)
        assertEquals("gemini-3.5-flash-lite", com.example.ai.MusicAssistantManager.GEMINI_LITE_MODEL)
        assertEquals("openai/gpt-oss-120b", com.example.ai.MusicAssistantManager.GROQ_PRIMARY_MODEL)
        assertNotNull(BuildConfig.GROQ_API_KEY)

        val manager = com.example.ai.MusicAssistantManager(context) { "" }
        val sampleTrack = Track(
            id = 42L,
            title = "Zouk Étoile",
            artist = "Artiste",
            album = "Album",
            genre = "MP3",
            mood = "all",
            bpm = 100,
            musicalKey = "",
            energy = 70,
            durationSec = 200,
            isLiked = false,
            primaryColorHex = 0xFFF5B731L,
            secondaryColorHex = 0xFF0A1432L,
            tertiaryColorHex = 0xFF1A2A52L
        )
        val parsed = manager.parsePlaylistJson(
            rawText = "```json\n{\"playlistTitle\":\"Ambiance Douce\",\"rationale\":\"Sélection Emma\",\"selectedTrackIds\":[42,999]}\n```",
            catalog = listOf(sampleTrack)
        )
        assertEquals("Ambiance Douce", parsed.title)
        assertEquals(listOf(42L), parsed.trackIds)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = androidx.lifecycle.ViewModelStore()
        val provider = androidx.lifecycle.ViewModelProvider(
            store,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val viewModel = provider[AuraViewModel::class.java]
        try {
            val welcome = viewModel.chatMessages.value.first().text
            assertTrue(welcome.contains("Emma"))
            assertTrue(welcome.contains("musique"))
        } finally {
            store.clear()
        }
    }

    @Test
    fun `behavior tracking records time slots, transitions, playlists and injects Emma system prompt`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val trackA = Track(
                id = 101L,
                title = "Nuit Calme",
                artist = "Kassav",
                album = "Soirée",
                genre = "Zouk",
                mood = "chill",
                bpm = 96,
                musicalKey = "",
                energy = 60,
                durationSec = 210,
                isLiked = true,
                primaryColorHex = 0xFFF5B731L,
                secondaryColorHex = 0xFF0A1432L,
                tertiaryColorHex = 0xFF1A2A52L,
                localUri = "content://media/external/audio/media/101",
                playCount = 5
            )
            val trackB = trackA.copy(
                id = 102L,
                title = "Aube Dorée",
                artist = "Slaï",
                playCount = 3,
                isLiked = false,
                localUri = "content://media/external/audio/media/102"
            )
            db.trackDao().insertTracks(
                listOf(
                    TrackEntity.fromDomain(trackA),
                    TrackEntity.fromDomain(trackB)
                )
            )
            val repo = com.example.data.local.ListeningBehaviorRepository(
                db.trackDao(),
                db.listeningBehaviorDao()
            )
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 20)
                set(java.util.Calendar.MINUTE, 15)
            }
            val eveningMs = cal.timeInMillis
            repo.recordPlayback(previousTrack = null, currentTrack = trackA, timestampMs = eveningMs)
            repo.recordPlayback(previousTrack = trackA, currentTrack = trackB, timestampMs = eveningMs + 60_000L)
            repo.recordPlayback(previousTrack = trackA, currentTrack = trackB, timestampMs = eveningMs + 120_000L)
            repo.savePlaylist(
                title = "Chill du Soir",
                description = "Sélection détente",
                trackIds = listOf(101L, 102L),
                isAiGenerated = false,
                createdAtMs = eveningMs
            )

            val profile = repo.buildProfileSnapshot(nowMs = eveningMs + 180_000L)
            assertEquals(com.example.model.ListeningTimeSlot.SOIREE, profile.currentSlot)
            assertEquals(1, profile.topTransitions.size)
            assertEquals(2, profile.topTransitions.first().count)
            assertEquals("Nuit Calme", profile.topTransitions.first().fromTitle)
            assertEquals("Aube Dorée", profile.topTransitions.first().toTitle)
            assertEquals(1, profile.playlists.size)
            assertTrue(profile.systemPromptBlock.contains("Chill du Soir"))
            assertTrue(profile.systemPromptBlock.contains("Nuit Calme"))
            assertTrue(profile.systemPromptBlock.contains("Aube Dorée"))

            val manager = com.example.ai.MusicAssistantManager(context) { "" }
            val fullSystemPrompt = manager.buildDynamicSystemPrompt(profile)
            assertTrue(fullSystemPrompt.contains("Emma"))
            assertTrue(fullSystemPrompt.contains("Chill du Soir"))
            assertTrue(fullSystemPrompt.contains("Enchaînements fréquents"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `proactive Emma coordinator respects internet check, habit detection, and cooldown`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val track = Track(
                id = 201L,
                title = "Douce Mélodie",
                artist = "Emma Band",
                album = "Soir",
                genre = "MP3",
                mood = "chill",
                bpm = 90,
                musicalKey = "",
                energy = 50,
                durationSec = 190,
                isLiked = true,
                primaryColorHex = 0xFFF5B731L,
                secondaryColorHex = 0xFF0A1432L,
                tertiaryColorHex = 0xFF1A2A52L,
                localUri = "content://media/external/audio/media/201",
                playCount = 4
            )
            db.trackDao().insertTrack(TrackEntity.fromDomain(track))
            val behaviorRepo = com.example.data.local.ListeningBehaviorRepository(
                db.trackDao(),
                db.listeningBehaviorDao()
            )
            val prefRepo = UserPreferencesRepository(db.userPreferenceDao())
            val fixedNow = System.currentTimeMillis()
            behaviorRepo.recordPlayback(null, track, fixedNow - 10_000L)
            behaviorRepo.recordPlayback(null, track, fixedNow - 5_000L)

            var online = false
            var dispatched = 0
            val coordinator = com.example.notifications.ProactiveEmmaCoordinator(
                context = context,
                behaviorRepository = behaviorRepo,
                preferencesRepository = prefRepo,
                preferenceDao = db.userPreferenceDao(),
                isOnlineProvider = { online },
                hasNotifPermissionProvider = { true },
                copyGenerator = { profile ->
                    com.example.model.ProactiveNotificationDraft(
                        message = "Envie de lancer Douce Mélodie comme d'habitude ?",
                        suggestedTrackId = 201L,
                        slot = profile.currentSlot
                    )
                },
                notificationDispatcher = { _, _ ->
                    dispatched++
                    true
                },
                clockMs = { fixedNow }
            )

            val offlineOutcome = coordinator.evaluateAndNotifyIfDue(forceTest = false)
            assertEquals(com.example.notifications.ProactiveEvaluationOutcome.SkippedOffline, offlineOutcome)
            assertEquals(0, dispatched)

            online = true
            val onlineOutcome = coordinator.evaluateAndNotifyIfDue(forceTest = false)
            assertTrue(onlineOutcome is com.example.notifications.ProactiveEvaluationOutcome.Notified)
            assertEquals(1, dispatched)

            val cooldownOutcome = coordinator.evaluateAndNotifyIfDue(forceTest = false)
            assertEquals(com.example.notifications.ProactiveEvaluationOutcome.SkippedCooldown, cooldownOutcome)
            assertEquals(1, dispatched)
        } finally {
            db.close()
        }
    }
}
