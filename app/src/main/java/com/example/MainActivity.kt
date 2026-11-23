package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.notifications.ProactiveNotificationHelper
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.AuraMiniPlayer
import com.example.ui.components.AuraNavigationRail
import com.example.ui.components.AuraRoutes
import com.example.ui.components.AuraToastOverlay
import com.example.ui.components.AuraTopHeader
import com.example.ui.components.FloatingAmbientBackground
import com.example.ui.components.ProPaywallDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AuraAITheme
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.viewmodel.AuraViewModel

class MainActivity : ComponentActivity() {

    private var pendingDeepLinkState by mutableStateOf<Triple<String?, String?, Long?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val incomingAudioUri = extractAudioUri(intent)
        captureNotificationDeepLink(intent)

        setContent {
            AuraAITheme {
                val viewModel: AuraViewModel = viewModel()

                LaunchedEffect(incomingAudioUri) {
                    if (incomingAudioUri != null) {
                        viewModel.importAudioUriSaf(incomingAudioUri)
                    }
                }

                AuraMainApp(
                    viewModel = viewModel,
                    pendingDeepLink = pendingDeepLinkState,
                    onConsumeDeepLink = { pendingDeepLinkState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
        captureNotificationDeepLink(intent)
    }

    private fun captureNotificationDeepLink(intent: Intent?) {
        val route = intent?.getStringExtra(ProactiveNotificationHelper.EXTRA_OPEN_ROUTE)
        val message = intent?.getStringExtra(ProactiveNotificationHelper.EXTRA_PROACTIVE_MESSAGE)
        val suggestedTrackId = intent?.getLongExtra(ProactiveNotificationHelper.EXTRA_SUGGESTED_TRACK_ID, -1L)
            ?.takeIf { it > 0L }
        if (!route.isNullOrBlank() || !message.isNullOrBlank()) {
            pendingDeepLinkState = Triple(route ?: AuraRoutes.STUDIO, message, suggestedTrackId)
        }
    }

    private fun extractAudioUri(intent: Intent?): android.net.Uri? {
        return if (intent?.action == Intent.ACTION_VIEW) intent.data else null
    }
}

@Composable
fun AuraMainApp(
    viewModel: AuraViewModel,
    pendingDeepLink: Triple<String?, String?, Long?>? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AuraRoutes.HOME

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionSec by viewModel.currentPositionSec.collectAsStateWithLifecycle()
    val durationSec by viewModel.currentDurationSec.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.isRepeat.collectAsStateWithLifecycle()
    val spectrumBars by viewModel.spectrumBars.collectAsStateWithLifecycle()
    val dspStatus by viewModel.dspStatus.collectAsStateWithLifecycle()
    val homeFilter by viewModel.homeMoodFilter.collectAsStateWithLifecycle()
    val globalSearch by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
    val aiPlaylistState by viewModel.aiPlaylistState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isCopilotTyping by viewModel.isCopilotTyping.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val permissionStatuses by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    val toastBanner by viewModel.toastBanner.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val isProUnlocked by viewModel.isProUnlocked.collectAsStateWithLifecycle()
    val showPaywallDialog by viewModel.showPaywallDialog.collectAsStateWithLifecycle()
    val behaviorProfile by viewModel.behaviorProfile.collectAsStateWithLifecycle()
    val savedPlaylists by viewModel.savedPlaylists.collectAsStateWithLifecycle()

    val allPermissionsGranted = permissionStatuses.all { it.isGranted }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            viewModel.triggerAutoScanIfAllGranted(forceIfEmpty = true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        var skippedFirstResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skippedFirstResume) {
                    viewModel.onAppResumed()
                } else {
                    skippedFirstResume = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navigateTo: (String) -> Unit = remember(navController) {
        { route ->
            val activeRoute = navController.currentBackStackEntry?.destination?.route
            if (activeRoute != route) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            val (route, message, suggestedTrackId) = pendingDeepLink
            viewModel.handleProactiveNotificationLaunch(message, suggestedTrackId)
            navigateTo(route ?: AuraRoutes.STUDIO)
            onConsumeDeepLink()
        }
    }

    val accentColor = remember(currentTrack) {
        currentTrack?.let { Color(it.primaryColorHex) } ?: GoldBright
    }

    val showMiniPlayer = currentTrack != null && currentRoute != AuraRoutes.PLAYER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue.copy(alpha = 0.48f), Obsidian, Midnight)
                )
            )
    ) {
        // Animated floating ambient shapes across the backdrop
        FloatingAmbientBackground(
            accentColor = accentColor,
            isPlaying = isPlaying
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (!isCompact) {
                    AuraNavigationRail(
                        currentRoute = currentRoute,
                        onNavigate = navigateTo,
                        onOpenPermissions = { navigateTo(AuraRoutes.PERMISSIONS) },
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // Clean docked vertical layout so header, mini-player, and bottom nav never cover list/player text
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AuraTopHeader(
                        searchQuery = globalSearch,
                        onSearchChange = { q ->
                            viewModel.setGlobalSearchQuery(q)
                            if (q.isNotBlank() && currentRoute != AuraRoutes.LIBRARY) {
                                navigateTo(AuraRoutes.LIBRARY)
                            }
                        },
                        onOpenSearch = { navigateTo(AuraRoutes.LIBRARY) },
                        onInstantAiMix = {
                            viewModel.triggerInstantAiMix {
                                navigateTo(AuraRoutes.PLAYER)
                            }
                        },
                        onOpenPermissions = { navigateTo(AuraRoutes.PERMISSIONS) },
                        allPermissionsGranted = allPermissionsGranted,
                        isProUnlocked = isProUnlocked,
                        isPlaying = isPlaying,
                        showSearchBar = !isCompact
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AuraRoutes.HOME,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(AuraRoutes.HOME) {
                                HomeScreen(
                                    tracks = tracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    isScanning = isScanning,
                                    hapticEnabled = audioSettings.hapticFeedbackEnabled,
                                    selectedFilter = homeFilter,
                                    onFilterSelected = viewModel::setHomeMoodFilter,
                                    onPlayTrack = { track -> viewModel.playTrack(track) },
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onToggleLike = viewModel::toggleLike,
                                    onOpenPlayer = { navigateTo(AuraRoutes.PLAYER) },
                                    onOpenStudio = { navigateTo(AuraRoutes.STUDIO) },
                                    onOpenLibrary = { navigateTo(AuraRoutes.LIBRARY) }
                                )
                            }

                            composable(AuraRoutes.PLAYER) {
                                PlayerScreen(
                                    track = currentTrack,
                                    isPlaying = isPlaying,
                                    positionSec = positionSec,
                                    durationSec = durationSec,
                                    volume = volume,
                                    isMuted = isMuted,
                                    isShuffle = isShuffle,
                                    isRepeat = isRepeat,
                                    isProUnlocked = isProUnlocked,
                                    sleepTimerMinutes = sleepTimerMinutes,
                                    spectrumBars = spectrumBars,
                                    onTogglePlay = viewModel::togglePlayPause,
                                    onNext = viewModel::nextTrack,
                                    onPrevious = viewModel::previousTrack,
                                    onSeekFraction = viewModel::seekToFraction,
                                    onVolumeChange = viewModel::setVolume,
                                    onToggleMute = viewModel::toggleMute,
                                    onToggleShuffle = viewModel::toggleShuffle,
                                    onToggleRepeat = viewModel::toggleRepeat,
                                    onToggleLike = { currentTrack?.let(viewModel::toggleLike) },
                                    onCycleSleepTimer = viewModel::cycleSleepTimer,
                                    onIsolateVocals = viewModel::isolateVocals,
                                    onHarmonicTransition = viewModel::harmonicDjTransition,
                                    onTriggerInstantAiMix = { viewModel.triggerInstantAiMix() },
                                    onOpenLibrary = { navigateTo(AuraRoutes.LIBRARY) }
                                )
                            }

                            composable(AuraRoutes.STUDIO) {
                                StudioScreen(
                                    tracks = tracks,
                                    behaviorProfile = behaviorProfile,
                                    proactiveNotificationsEnabled = audioSettings.proactiveNotificationsEnabled,
                                    aiPlaylistState = aiPlaylistState,
                                    chatMessages = chatMessages,
                                    isCopilotTyping = isCopilotTyping,
                                    isProUnlocked = isProUnlocked,
                                    topOverlayPadding = 0.dp,
                                    bottomOverlayPadding = 0.dp,
                                    onOpenPaywall = viewModel::openPaywall,
                                    onGeneratePlaylist = viewModel::generateAiPlaylist,
                                    onPlayGeneratedMix = {
                                        viewModel.playGeneratedMix()
                                        navigateTo(AuraRoutes.PLAYER)
                                    },
                                    onPlayTrackById = { id ->
                                        viewModel.playTrackById(id)
                                        navigateTo(AuraRoutes.PLAYER)
                                    },
                                    onSendChatMessage = viewModel::sendCopilotMessage,
                                    onClearChat = viewModel::clearCopilotChat,
                                    onToggleProactiveNotifications = viewModel::toggleProactiveNotifications,
                                    onTriggerProactiveNotificationTest = viewModel::triggerProactiveNotificationNow
                                )
                            }

                            composable(AuraRoutes.LIBRARY) {
                                LibraryScreen(
                                    tracks = tracks,
                                    savedPlaylists = savedPlaylists,
                                    currentTrack = currentTrack,
                                    isScanning = isScanning,
                                    globalSearch = globalSearch,
                                    topOverlayPadding = 0.dp,
                                    bottomOverlayPadding = 0.dp,
                                    onPlayTrack = { track -> viewModel.playTrack(track) },
                                    onToggleLike = viewModel::toggleLike,
                                    onDeleteTrack = viewModel::deleteTrack,
                                    onScanDeviceAudio = viewModel::scanDeviceAudioFiles,
                                    onImportSafUri = { uri ->
                                        viewModel.importAudioUriSaf(uri)
                                        navigateTo(AuraRoutes.PLAYER)
                                    },
                                    onCreatePlaylist = viewModel::createCustomPlaylist,
                                    onPlaySavedPlaylist = { playlist ->
                                        viewModel.playSavedPlaylist(playlist)
                                        navigateTo(AuraRoutes.PLAYER)
                                    },
                                    onDeleteSavedPlaylist = viewModel::deleteSavedPlaylist,
                                    onRefreshPermissions = viewModel::refreshPermissions
                                )
                            }

                            composable(AuraRoutes.EQUALIZER) {
                                EqualizerScreen(
                                    settings = audioSettings,
                                    dspStatus = dspStatus,
                                    topOverlayPadding = 0.dp,
                                    bottomOverlayPadding = 0.dp,
                                    onBandChange = viewModel::updateEqBand,
                                    onSelectPreset = viewModel::applyEqPreset,
                                    onToggleCrossfade = viewModel::toggleCrossfade,
                                    onToggleNormalize = viewModel::toggleNormalize,
                                    onToggleSpatial3d = viewModel::toggleSpatial3d,
                                    onToggleHapticFeedback = viewModel::toggleHapticFeedback,
                                    onToggleProactiveNotifications = viewModel::toggleProactiveNotifications
                                )
                            }

                            composable(AuraRoutes.PERMISSIONS) {
                                PermissionsScreen(
                                    permissionStatuses = permissionStatuses,
                                    onRefreshPermissions = viewModel::refreshPermissions,
                                    onScanDeviceAudio = viewModel::scanDeviceAudioFiles
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        AuraMiniPlayer(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            positionSec = positionSec,
                            durationSec = durationSec,
                            onTogglePlay = viewModel::togglePlayPause,
                            onNext = viewModel::nextTrack,
                            onPrevious = viewModel::previousTrack,
                            onOpenPlayer = { navigateTo(AuraRoutes.PLAYER) },
                            modifier = if (!isCompact) Modifier.navigationBarsPadding() else Modifier
                        )
                    }

                    if (isCompact) {
                        AuraBottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateTo
                        )
                    }
                }
            }
        }

        AuraToastOverlay(
            toast = toastBanner,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        if (showPaywallDialog) {
            ProPaywallDialog(
                billingState = billingState,
                onDismiss = viewModel::dismissPaywall,
                onPurchaseOption = viewModel::launchProPurchase,
                onRestorePurchases = viewModel::restoreProPurchases
            )
        }
    }
}
