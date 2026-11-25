package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AuraAiRepository
import com.example.audio.AuraAudioController
import com.example.audio.PlaybackNotificationBridge
import com.example.billing.BillingProductOption
import com.example.billing.BillingUiState
import com.example.billing.PlayBillingManager
import com.example.data.local.AuraDatabase
import com.example.data.local.ListeningBehaviorRepository
import com.example.data.local.LocalMediaScanner
import com.example.data.local.TrackEntity
import com.example.data.local.UserPreferencesRepository
import com.example.model.AiGenerationUiState
import com.example.model.AudioSettingsState
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
import com.example.model.ListeningBehaviorProfile
import com.example.model.SavedPlaylist
import com.example.model.Track
import com.example.notifications.EmmaProactiveScheduler
import com.example.notifications.ProactiveEmmaCoordinator
import com.example.notifications.ProactiveEvaluationOutcome
import com.example.permissions.AndroidPermissionStatus
import com.example.permissions.PermissionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = AuraDatabase.getInstance(appContext)
    private val trackDao = database.trackDao()
    private val preferencesRepo = UserPreferencesRepository(database.userPreferenceDao())
    private val behaviorRepository = ListeningBehaviorRepository(trackDao, database.listeningBehaviorDao())
    private val aiRepository = AuraAiRepository(appContext)
    private val audioController = AuraAudioController(appContext, viewModelScope)
    private val billingManager = PlayBillingManager(appContext, viewModelScope)

    private val proUnlockedForNow = true

    val billingState: StateFlow<BillingUiState> = billingManager.uiState
    val isProUnlocked: StateFlow<Boolean> = billingState
        .map { proUnlockedForNow || it.isProUnlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), proUnlockedForNow)

    private val _showPaywallDialog = MutableStateFlow(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog.asStateFlow()

    val tracks: StateFlow<List<Track>> = trackDao.observeAllTracks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioSettings: StateFlow<AudioSettingsState> = preferencesRepo.audioSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioSettingsState())

    val behaviorProfile: StateFlow<ListeningBehaviorProfile> = behaviorRepository.observeBehaviorProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningBehaviorProfile())

    val savedPlaylists: StateFlow<List<SavedPlaylist>> = behaviorRepository.observeSavedPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTrackId = MutableStateFlow(-1L)
    val currentTrack: StateFlow<Track?> = combine(tracks, _currentTrackId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isPlaying: StateFlow<Boolean> = audioController.isPlaying
    val currentPositionSec: StateFlow<Float> = audioController.currentPositionSec
    val currentDurationSec: StateFlow<Int> = audioController.durationSec
    val volume: StateFlow<Float> = audioController.volume
    val isMuted: StateFlow<Boolean> = audioController.isMuted
    val isShuffle: StateFlow<Boolean> = audioController.isShuffle
    val isRepeat: StateFlow<Boolean> = audioController.isRepeat
    val spectrumBars: StateFlow<List<Float>> = audioController.spectrumBars
    val dspStatus: StateFlow<String> = audioController.dspStatus

    private val _homeMoodFilter = MutableStateFlow("all")
    val homeMoodFilter: StateFlow<String> = _homeMoodFilter.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _aiPlaylistState = MutableStateFlow<AiGenerationUiState<GeneratedMixResult>>(
        AiGenerationUiState.Idle
    )
    val aiPlaylistState: StateFlow<AiGenerationUiState<GeneratedMixResult>> = _aiPlaylistState.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Bonjour ! Je suis Emma, votre IA dédiée uniquement à la musique. Parlez-moi de vos titres, d'ambiances d'écoute ou demandez-moi une playlist."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isCopilotTyping = MutableStateFlow(false)
    val isCopilotTyping: StateFlow<Boolean> = _isCopilotTyping.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _permissionStatuses = MutableStateFlow(PermissionManager.getAllStatuses(appContext))
    val permissionStatuses: StateFlow<List<AndroidPermissionStatus>> = _permissionStatuses.asStateFlow()

    private val _toastBanner = MutableStateFlow<Pair<String, String>?>(null)
    val toastBanner: StateFlow<Pair<String, String>?> = _toastBanner.asStateFlow()

    private var toastJob: Job? = null
    private var copilotJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastAutoScanAtMs: Long = 0L
    private var lastPlayedTrackForTransition: Track? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            trackDao.deleteLegacyDemoTracks()
            preferencesRepo.ensureInitialized()
            database.userPreferenceDao().getPreferencesOnce()?.let { saved ->
                _currentTrackId.value = saved.lastTrackId
                _homeMoodFilter.value = saved.preferredMoodFilter
                EmmaProactiveScheduler.ensureScheduled(appContext, saved.proactiveNotificationsEnabled)
                if (saved.hasCachedMix && saved.cachedPlaylistTitle.isNotBlank()) {
                    val cachedIds = saved.cachedPlaylistTrackIdsCsv
                        .split(",")
                        .mapNotNull { it.toLongOrNull() }
                    if (cachedIds.isNotEmpty()) {
                        _aiPlaylistState.value = AiGenerationUiState.Success(
                            GeneratedMixResult(
                                title = saved.cachedPlaylistTitle,
                                rationale = saved.cachedPlaylistRationale,
                                trackIds = cachedIds
                            )
                        )
                    }
                }
            }
            refreshPermissions()
            if (areAllPermissionsGrantedNow()) {
                lastAutoScanAtMs = System.currentTimeMillis()
                scanDeviceAudioInternal(silent = true)
            }
        }

        viewModelScope.launch {
            currentTrack.collect { track ->
                audioController.syncSelectedTrack(track)
            }
        }

        viewModelScope.launch {
            audioSettings.collect { settings ->
                audioController.updateAudioSettings(settings)
            }
        }

        audioController.onTrackFinishedCallback = {
            viewModelScope.launch {
                nextTrack()
            }
        }

        audioController.onPlaybackErrorCallback = { message ->
            showToast("Erreur de lecture", message)
        }

        audioController.onDurationResolvedCallback = { trackId, realSec ->
            viewModelScope.launch(Dispatchers.IO) {
                trackDao.updateDuration(trackId, realSec)
            }
        }

        PlaybackNotificationBridge.onPlayAction = {
            viewModelScope.launch {
                if (!isPlaying.value) {
                    currentTrack.value?.let { audioController.resume(it) }
                }
            }
        }
        PlaybackNotificationBridge.onPauseAction = {
            viewModelScope.launch {
                if (isPlaying.value) {
                    audioController.pause()
                }
            }
        }
        PlaybackNotificationBridge.onPlayPauseAction = {
            viewModelScope.launch { togglePlayPause() }
        }
        PlaybackNotificationBridge.onNextAction = {
            viewModelScope.launch { nextTrack() }
        }
        PlaybackNotificationBridge.onPrevAction = {
            viewModelScope.launch { previousTrack() }
        }
        PlaybackNotificationBridge.onSeekToAction = { posMs ->
            viewModelScope.launch { seekToSeconds(posMs / 1000f) }
        }
        PlaybackNotificationBridge.onStopAction = {
            viewModelScope.launch { audioController.pauseWithoutNotification() }
        }
    }

    fun openPaywall() {
        _showPaywallDialog.value = true
    }

    fun dismissPaywall() {
        _showPaywallDialog.value = false
    }

    fun launchProPurchase(activity: Activity, option: BillingProductOption) {
        billingManager.launchPurchaseFlow(activity, option)
    }

    fun restoreProPurchases() {
        billingManager.refreshPurchases()
        showToast("Vérification des achats", "Recherche en cours...")
    }

    fun refreshBilling() {
        billingManager.refreshPurchases()
    }

    private fun requireProEntitlement(): Boolean {
        if (proUnlockedForNow || billingState.value.isProUnlocked) return true
        _showPaywallDialog.value = true
        return false
    }

    fun showToast(title: String, subtitle: String = "") {
        toastJob?.cancel()
        _toastBanner.value = title to subtitle
        toastJob = viewModelScope.launch {
            delay(2600L)
            _toastBanner.value = null
        }
    }

    fun playTrack(track: Track) {
        val previousTrack = lastPlayedTrackForTransition
        lastPlayedTrackForTransition = track
        _currentTrackId.value = track.id
        audioController.playTrack(track)
        viewModelScope.launch(Dispatchers.IO) {
            val nowMs = System.currentTimeMillis()
            preferencesRepo.saveLastTrackId(track.id)
            trackDao.incrementPlayCount(track.id, nowMs)
            behaviorRepository.recordPlayback(
                previousTrack = previousTrack,
                currentTrack = track,
                timestampMs = nowMs
            )
            if ((track.bitrateKbps == 0 || track.year.isBlank()) && !track.localUri.isNullOrBlank()) {
                LocalMediaScanner.readBitrateAndYear(appContext, track.localUri)?.let { (br, yr) ->
                    trackDao.updateAudioDetails(
                        trackId = track.id,
                        bitrateKbps = if (br > 0) br else track.bitrateKbps,
                        year = yr.ifBlank { track.year }
                    )
                }
            }
        }
        showToast(track.title, track.artist)
    }

    fun playTrackById(trackId: Long) {
        val found = tracks.value.find { it.id == trackId } ?: return
        playTrack(found)
    }

    fun togglePlayPause() {
        val active = currentTrack.value
        if (active == null) {
            showToast("Aucun morceau", "Touchez Scanner ou Importer pour ajouter vos sons.")
            return
        }
        if (isPlaying.value) {
            audioController.pause()
        } else {
            audioController.resume(active)
        }
    }

    fun nextTrack() {
        val list = tracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == (currentTrack.value?.id ?: -1L) }.coerceAtLeast(0)
        val nextIdx = if (isShuffle.value && list.size > 1) {
            list.indices.filter { it != currentIdx }.shuffled().first()
        } else {
            (currentIdx + 1) % list.size
        }
        playTrack(list[nextIdx])
    }

    fun previousTrack() {
        if (currentPositionSec.value > 4f) {
            audioController.seekToSeconds(0f)
            return
        }
        val list = tracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == (currentTrack.value?.id ?: -1L) }.coerceAtLeast(0)
        val prevIdx = (currentIdx - 1 + list.size) % list.size
        playTrack(list[prevIdx])
    }

    fun seekToFraction(fraction: Float) = audioController.seekToFraction(fraction)
    fun seekToSeconds(sec: Float) = audioController.seekToSeconds(sec)
    fun setVolume(vol: Float) = audioController.setVolume(vol)

    fun toggleMute() {
        audioController.toggleMute()
        showToast(if (isMuted.value) "Son coupé" else "Son rétabli")
    }

    fun toggleShuffle() {
        audioController.toggleShuffle()
        showToast(if (isShuffle.value) "Aléatoire activé" else "Aléatoire désactivé")
    }

    fun toggleRepeat() {
        audioController.toggleRepeat()
        showToast(if (isRepeat.value) "Répétition activée" else "Répétition désactivée")
    }

    fun toggleLike(track: Track) {
        val nextLiked = !track.isLiked
        viewModelScope.launch(Dispatchers.IO) {
            trackDao.updateLiked(track.id, nextLiked)
        }
        showToast(if (nextLiked) "Ajouté aux favoris" else "Retiré des favoris", track.title)
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            trackDao.deleteTrack(track.id)
        }
        showToast("Retiré de la bibliothèque", track.title)
    }

    fun setHomeMoodFilter(filter: String) {
        _homeMoodFilter.value = filter
        viewModelScope.launch {
            preferencesRepo.savePreferredMood(filter)
        }
    }

    fun setGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
    }

    fun cycleSleepTimer() {
        val next = when (_sleepTimerMinutes.value) {
            0 -> 15
            15 -> 30
            30 -> 45
            else -> 0
        }
        _sleepTimerMinutes.value = next
        sleepTimerJob?.cancel()
        if (next > 0) {
            showToast("Minuteur activé", "Arrêt automatique dans $next min")
            sleepTimerJob = viewModelScope.launch {
                while (isActive && _sleepTimerMinutes.value > 0) {
                    delay(60_000L)
                    val remaining = (_sleepTimerMinutes.value - 1).coerceAtLeast(0)
                    _sleepTimerMinutes.value = remaining
                    if (remaining == 0) {
                        audioController.pause()
                        showToast("Minuteur terminé", "Lecture mise en pause")
                        break
                    }
                }
            }
        } else {
            showToast("Minuteur désactivé")
        }
    }

    fun triggerInstantAiMix(onMixReady: () -> Unit = {}) {
        if (!requireProEntitlement()) return
        val catalog = tracks.value
        if (catalog.isEmpty()) {
            showToast("Bibliothèque vide", "Scannez ou importez d'abord vos musiques.")
            return
        }
        val active = currentTrack.value
        val prompt = if (active != null) {
            "Mix cohérent autour de ${active.title} (${active.artist})"
        } else {
            "Sélection harmonieuse à partir de mes musiques"
        }
        viewModelScope.launch {
            _aiPlaylistState.value = AiGenerationUiState.Loading
            try {
                val snapshot = withContext(Dispatchers.IO) { behaviorRepository.buildProfileSnapshot() }
                val result = aiRepository.generateCustomPlaylist(
                    prompt = prompt,
                    targetBpm = 104,
                    emotionalIntensity = 75,
                    catalog = catalog,
                    behaviorProfile = snapshot
                )
                _aiPlaylistState.value = AiGenerationUiState.Success(result)
                preferencesRepo.saveCachedMix(prompt, result)
                withContext(Dispatchers.IO) {
                    behaviorRepository.savePlaylist(
                        title = result.title,
                        description = result.rationale,
                        trackIds = result.trackIds,
                        isAiGenerated = true
                    )
                }
                result.trackIds.firstOrNull()?.let { playTrackById(it) }
                onMixReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Erreur lors de la création du mix."
                _aiPlaylistState.value = AiGenerationUiState.Error(msg)
                showToast("Erreur Mix IA", msg)
            }
        }
    }

    fun isolateVocals() {
        applyEqPreset("vocal")
        if (!isPlaying.value && currentTrack.value != null) togglePlayPause()
        showToast("Mode Voix activé")
    }

    fun harmonicDjTransition() {
        val list = tracks.value
        if (list.size <= 1) {
            showToast("Transition impossible", "Ajoutez au moins 2 morceaux.")
            return
        }
        val current = currentTrack.value
        val candidates = list.filter { it.id != current?.id }
        val next = candidates.firstOrNull() ?: return
        showToast("Transition vers ${next.title}", next.artist)
        audioController.fadeOutAndSwitch {
            playTrack(next)
        }
    }

    fun generateAiPlaylist(prompt: String, targetBpm: Int, emotionalIntensity: Int) {
        if (!requireProEntitlement()) return
        val catalog = tracks.value
        if (catalog.isEmpty()) {
            _aiPlaylistState.value = AiGenerationUiState.Error("Ajoutez d'abord vos musiques locales.")
            return
        }
        val cleanPrompt = prompt.trim().ifBlank { "Sélection équilibrée pour écouter maintenant" }
        viewModelScope.launch {
            _aiPlaylistState.value = AiGenerationUiState.Loading
            try {
                val snapshot = withContext(Dispatchers.IO) { behaviorRepository.buildProfileSnapshot() }
                val result = aiRepository.generateCustomPlaylist(
                    prompt = cleanPrompt,
                    targetBpm = targetBpm,
                    emotionalIntensity = emotionalIntensity,
                    catalog = catalog,
                    behaviorProfile = snapshot
                )
                _aiPlaylistState.value = AiGenerationUiState.Success(result)
                preferencesRepo.saveCachedMix(cleanPrompt, result)
                withContext(Dispatchers.IO) {
                    behaviorRepository.savePlaylist(
                        title = result.title,
                        description = result.rationale,
                        trackIds = result.trackIds,
                        isAiGenerated = true
                    )
                }
                showToast("Playlist créée", "${result.trackIds.size} titres sélectionnés")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiPlaylistState.value = AiGenerationUiState.Error(
                    e.localizedMessage ?: "Erreur lors de la création de la playlist."
                )
            }
        }
    }

    fun playGeneratedMix() {
        val state = _aiPlaylistState.value
        if (state is AiGenerationUiState.Success) {
            state.data.trackIds.firstOrNull()?.let { playTrackById(it) }
        }
    }

    fun createCustomPlaylist(title: String, trackIds: List<Long>) {
        if (trackIds.isEmpty()) {
            showToast("Playlist vide", "Sélectionnez au moins un morceau.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            behaviorRepository.savePlaylist(
                title = title,
                description = "Playlist personnalisée (${trackIds.size} titres)",
                trackIds = trackIds,
                isAiGenerated = false
            )
            showToast("Playlist enregistrée", title)
        }
    }

    fun playSavedPlaylist(playlist: SavedPlaylist) {
        val firstTrack = playlist.trackIds.firstOrNull()?.let { id -> tracks.value.find { it.id == id } }
        if (firstTrack == null) {
            showToast("Titres introuvables", "Scannez à nouveau votre bibliothèque.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            behaviorRepository.incrementPlaylistPlayCount(playlist.id)
        }
        playTrack(firstTrack)
    }

    fun deleteSavedPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            behaviorRepository.deletePlaylist(playlistId)
        }
        showToast("Playlist supprimée")
    }

    fun sendCopilotMessage(userText: String) {
        if (!requireProEntitlement()) return
        val clean = userText.trim()
        if (clean.isEmpty()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = clean
        )
        val aiMsgId = UUID.randomUUID().toString()
        val placeholderAi = ChatMessage(
            id = aiMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )
        _chatMessages.value = _chatMessages.value + userMsg + placeholderAi
        _isCopilotTyping.value = true

        copilotJob?.cancel()
        copilotJob = viewModelScope.launch {
            try {
                val catalog = tracks.value
                val historySnapshot = _chatMessages.value.dropLast(2)
                val snapshot = withContext(Dispatchers.IO) { behaviorRepository.buildProfileSnapshot() }
                var latestText = ""
                aiRepository.streamCopilotReply(
                    userMessage = clean,
                    currentTrack = currentTrack.value,
                    catalog = catalog,
                    recentHistory = historySnapshot,
                    behaviorProfile = snapshot
                ).collect { partial ->
                    latestText = partial
                    val matchedTrack = catalog.firstOrNull {
                        partial.contains(it.title, ignoreCase = true)
                    }
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) {
                            msg.copy(
                                text = partial,
                                recommendedTrackId = matchedTrack?.id,
                                isStreaming = true
                            )
                        } else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) msg.copy(text = latestText, isStreaming = false) else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = e.localizedMessage ?: "Erreur de connexion avec Emma.",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isCopilotTyping.value = false
            }
        }
    }

    fun clearCopilotChat() {
        copilotJob?.cancel()
        _isCopilotTyping.value = false
        _chatMessages.value = listOf(
            ChatMessage(
                id = "reset",
                isUser = false,
                text = "Discussion réinitialisée. Je suis Emma, uniquement là pour parler musique avec vous !"
            )
        )
    }

    fun updateEqBand(bandIndex: Int, gainDb: Float) {
        val current = audioSettings.value.eqGainsDb.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb.coerceIn(-12f, 12f)
            viewModelScope.launch {
                preferencesRepo.saveEqGains(current, "custom")
            }
        }
    }

    fun applyEqPreset(presetName: String) {
        val gains = when (presetName) {
            "bass" -> listOf(8f, 6f, 2f, 0f, 0f, 2f, 3f)
            "zouk" -> listOf(6f, 4f, -1f, 1f, 3f, 5f, 4f)
            "studio" -> listOf(3f, 2f, 0f, 1f, 4f, 5f, 6f)
            "vocal" -> listOf(-2f, -1f, 2f, 6f, 5f, 2f, 0f)
            else -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
        viewModelScope.launch {
            preferencesRepo.saveEqGains(gains, presetName)
        }
        showToast("Égaliseur : ${presetName.uppercase()}")
    }

    fun toggleCrossfade(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveCrossfade(enabled) }
        showToast("Fondu enchaîné : ${if (enabled) "Activé" else "Désactivé"}")
    }

    fun toggleNormalize(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveNormalize(enabled) }
        showToast("Normalisation : ${if (enabled) "Activée" else "Désactivée"}")
    }

    fun toggleSpatial3d(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveSpatial3d(enabled) }
        showToast("Audio 3D : ${if (enabled) "Activé" else "Désactivé"}")
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveHapticFeedback(enabled) }
        showToast("Vibrations : ${if (enabled) "Activées" else "Désactivées"}")
    }

    fun toggleProactiveNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.saveProactiveNotificationsEnabled(enabled)
            EmmaProactiveScheduler.ensureScheduled(appContext, enabled)
        }
        showToast("Suggestions d'Emma : ${if (enabled) "Activées" else "Désactivées"}")
    }

    fun triggerProactiveNotificationNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val outcome = ProactiveEmmaCoordinator.createDefault(appContext)
                .evaluateAndNotifyIfDue(forceTest = true)
            withContext(Dispatchers.Main) {
                when (outcome) {
                    is ProactiveEvaluationOutcome.Notified ->
                        showToast("Notification d'Emma envoyée", outcome.draft.message)
                    ProactiveEvaluationOutcome.SkippedOffline ->
                        showToast("Hors ligne", "Connectez-vous à Internet pour générer une alerte Emma.")
                    ProactiveEvaluationOutcome.SkippedNoPermission ->
                        showToast("Autorisation requise", "Activez les notifications dans Autorisations.")
                    ProactiveEvaluationOutcome.SkippedNoHabitSignal ->
                        showToast("Bibliothèque vide", "Scannez ou écoutez un titre d'abord.")
                    ProactiveEvaluationOutcome.SkippedAiUnavailable ->
                        showToast("Emma indisponible", "Impossible de joindre Emma pour le moment.")
                    else -> Unit
                }
            }
        }
    }

    fun handleProactiveNotificationLaunch(message: String?, suggestedTrackId: Long?) {
        val clean = message?.trim().orEmpty()
        if (clean.isBlank()) return
        val alreadyExists = _chatMessages.value.any { !it.isUser && it.text == clean }
        if (!alreadyExists) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                id = "notif_${System.currentTimeMillis()}",
                isUser = false,
                text = clean,
                recommendedTrackId = suggestedTrackId?.takeIf { it > 0L },
                isStreaming = false
            )
        }
    }

    private fun areAllPermissionsGrantedNow(): Boolean {
        return _permissionStatuses.value.all { it.isGranted }
    }

    fun refreshPermissions() {
        _permissionStatuses.value = PermissionManager.getAllStatuses(appContext)
    }

    fun onAppResumed() {
        refreshBilling()
        refreshPermissions()
        triggerAutoScanIfAllGranted(forceIfEmpty = true)
    }

    fun triggerAutoScanIfAllGranted(forceIfEmpty: Boolean = false) {
        refreshPermissions()
        if (!areAllPermissionsGrantedNow()) return
        val now = System.currentTimeMillis()
        val cooledDown = now - lastAutoScanAtMs >= 15_000L
        if (_isScanning.value) return
        if (cooledDown || (forceIfEmpty && tracks.value.isEmpty())) {
            lastAutoScanAtMs = now
            viewModelScope.launch(Dispatchers.IO) {
                scanDeviceAudioInternal(silent = tracks.value.isNotEmpty())
            }
        }
    }

    fun scanDeviceAudioFiles() {
        refreshPermissions()
        if (!PermissionManager.hasAudioLibraryPermission(appContext)) {
            showToast("Autorisation requise", "Autorisez l'accès musique pour scanner.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            lastAutoScanAtMs = System.currentTimeMillis()
            scanDeviceAudioInternal(silent = false)
        }
    }

    private suspend fun scanDeviceAudioInternal(silent: Boolean) {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            val found = LocalMediaScanner.scanDeviceAudio(appContext)
            if (found.isEmpty()) {
                if (!silent) {
                    showToast("Scan terminé", "Aucun fichier audio trouvé dans le stockage.")
                }
            } else {
                val existingById = trackDao.getAllTracksOnce().associateBy { it.id }
                val merged = found.mapIndexed { idx, scanned ->
                    val old = existingById[scanned.id]
                    val preserved = if (old != null) {
                        scanned.copy(
                            isLiked = old.isLiked,
                            playCount = old.playCount,
                            lastPlayedAt = old.lastPlayedAt,
                            durationSec = if (old.durationSec > 0 && old.durationSec != 180) old.durationSec else scanned.durationSec,
                            bitrateKbps = if (old.bitrateKbps > 0) old.bitrateKbps else scanned.bitrateKbps,
                            year = old.year.ifBlank { scanned.year }
                        )
                    } else {
                        scanned
                    }
                    TrackEntity.fromDomain(preserved, sortOrder = 20 + idx)
                }
                trackDao.insertTracks(merged)
                if (!silent || existingById.isEmpty()) {
                    showToast("${found.size} titres trouvés", "Bibliothèque mise à jour")
                }
            }
        } catch (e: Exception) {
            if (!silent) {
                showToast("Erreur de scan", "Impossible d'analyser le stockage audio.")
            }
        } finally {
            _isScanning.value = false
        }
    }

    fun importAudioUriSaf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val track = LocalMediaScanner.extractTrackFromUri(appContext, uri)
                trackDao.insertTrack(TrackEntity.fromDomain(track, sortOrder = 1))
                withContext(Dispatchers.Main) {
                    playTrack(track)
                }
            } catch (e: Exception) {
                showToast("Erreur d'importation", "Impossible d'ouvrir ce fichier audio.")
            }
        }
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        audioController.release()
        billingManager.destroy()
        super.onCleared()
    }
}
