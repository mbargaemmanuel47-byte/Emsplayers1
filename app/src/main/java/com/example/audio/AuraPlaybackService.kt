package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.example.MainActivity
import com.example.R

object PlaybackNotificationBridge {
    var onPlayAction: (() -> Unit)? = null
    var onPauseAction: (() -> Unit)? = null
    var onPlayPauseAction: (() -> Unit)? = null
    var onNextAction: (() -> Unit)? = null
    var onPrevAction: (() -> Unit)? = null
    var onSeekToAction: ((Long) -> Unit)? = null
    var onStopAction: (() -> Unit)? = null
}

class AuraPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "em_player_playback_channel"
        const val NOTIFICATION_ID = 4201

        const val ACTION_UPDATE = "com.example.emplayer.ACTION_UPDATE"
        const val ACTION_PROGRESS = "com.example.emplayer.ACTION_PROGRESS"
        const val ACTION_PLAY_PAUSE = "com.example.emplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.emplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.example.emplayer.ACTION_PREV"
        const val ACTION_STOP = "com.example.emplayer.ACTION_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_ALBUM = "extra_album"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_PRIMARY_COLOR = "extra_primary_color"
        const val EXTRA_SECONDARY_COLOR = "extra_secondary_color"
        const val EXTRA_TERTIARY_COLOR = "extra_tertiary_color"

        @Volatile
        private var isForegroundActive: Boolean = false

        fun updateNotification(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            album: String = "",
            durationMs: Long = 0L,
            positionMs: Long = 0L,
            primaryColorHex: Long = 0xFFFACC15,
            secondaryColorHex: Long = 0xFF0E1D47,
            tertiaryColorHex: Long = 0xFF040B25
        ) {
            val intent = Intent(context, AuraPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_ALBUM, album)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_PRIMARY_COLOR, primaryColorHex)
                putExtra(EXTRA_SECONDARY_COLOR, secondaryColorHex)
                putExtra(EXTRA_TERTIARY_COLOR, tertiaryColorHex)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.startService(intent) }
            }
        }

        fun syncProgress(
            context: Context,
            positionMs: Long,
            durationMs: Long,
            isPlaying: Boolean
        ) {
            if (!isForegroundActive) return
            val intent = Intent(context, AuraPlaybackService::class.java).apply {
                action = ACTION_PROGRESS
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            runCatching { context.startService(intent) }
        }

        fun stopService(context: Context) {
            isForegroundActive = false
            runCatching { context.stopService(Intent(context, AuraPlaybackService::class.java)) }
        }
    }

    private var mediaSession: MediaSession? = null
    private var currentTitle: String = "Em Player"
    private var currentArtist: String = "Lecteur Audio"
    private var currentAlbum: String = ""
    private var currentDurationMs: Long = 0L
    private var currentPositionMs: Long = 0L
    private var currentlyPlaying: Boolean = false
    private var currentPrimaryColor: Long = 0xFFFACC15
    private var currentSecondaryColor: Long = 0xFF0E1D47
    private var currentTertiaryColor: Long = 0xFF040B25

    private var cachedArtworkKey: String = ""
    private var cachedArtwork: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSession(this, "EmPlayerSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    PlaybackNotificationBridge.onPlayAction?.invoke()
                        ?: PlaybackNotificationBridge.onPlayPauseAction?.invoke()
                }

                override fun onPause() {
                    PlaybackNotificationBridge.onPauseAction?.invoke()
                        ?: PlaybackNotificationBridge.onPlayPauseAction?.invoke()
                }

                override fun onSkipToNext() {
                    PlaybackNotificationBridge.onNextAction?.invoke()
                }

                override fun onSkipToPrevious() {
                    PlaybackNotificationBridge.onPrevAction?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    PlaybackNotificationBridge.onSeekToAction?.invoke(pos.coerceAtLeast(0L))
                }

                override fun onStop() {
                    PlaybackNotificationBridge.onStopAction?.invoke()
                    stopForegroundCompat()
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlaybackNotificationBridge.onPlayPauseAction?.invoke()
            ACTION_NEXT -> PlaybackNotificationBridge.onNextAction?.invoke()
            ACTION_PREV -> PlaybackNotificationBridge.onPrevAction?.invoke()
            ACTION_STOP -> {
                PlaybackNotificationBridge.onStopAction?.invoke()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PROGRESS -> {
                currentPositionMs = intent.getLongExtra(EXTRA_POSITION_MS, currentPositionMs).coerceAtLeast(0L)
                val dur = intent.getLongExtra(EXTRA_DURATION_MS, currentDurationMs).coerceAtLeast(0L)
                if (dur > 0L) currentDurationMs = dur
                currentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, currentlyPlaying)
                updateMediaSessionState(getOrCreateArtworkBitmap())
                return START_NOT_STICKY
            }
            ACTION_UPDATE, null -> {
                currentTitle = intent?.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: currentTitle
                currentArtist = intent?.getStringExtra(EXTRA_ARTIST)?.takeIf { it.isNotBlank() } ?: currentArtist
                currentAlbum = intent?.getStringExtra(EXTRA_ALBUM) ?: currentAlbum
                currentDurationMs = intent?.getLongExtra(EXTRA_DURATION_MS, currentDurationMs)?.coerceAtLeast(0L) ?: currentDurationMs
                currentPositionMs = intent?.getLongExtra(EXTRA_POSITION_MS, currentPositionMs)?.coerceAtLeast(0L) ?: currentPositionMs
                currentlyPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, currentlyPlaying) ?: currentlyPlaying
                currentPrimaryColor = intent?.getLongExtra(EXTRA_PRIMARY_COLOR, currentPrimaryColor) ?: currentPrimaryColor
                currentSecondaryColor = intent?.getLongExtra(EXTRA_SECONDARY_COLOR, currentSecondaryColor) ?: currentSecondaryColor
                currentTertiaryColor = intent?.getLongExtra(EXTRA_TERTIARY_COLOR, currentTertiaryColor) ?: currentTertiaryColor
            }
        }

        val artwork = getOrCreateArtworkBitmap()
        updateMediaSessionState(artwork)
        val notification = buildNotification(artwork)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForegroundActive = true

        return START_NOT_STICKY
    }

    private fun updateMediaSessionState(artwork: Bitmap) {
        val stateCode = if (currentlyPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackSpeed = if (currentlyPlaying) 1.0f else 0.0f
        val state = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP
            )
            .setState(stateCode, currentPositionMs, playbackSpeed, SystemClock.elapsedRealtime())
            .build()

        mediaSession?.setPlaybackState(state)

        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, currentAlbum.ifBlank { "Em Player" })
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artwork)
        if (currentDurationMs > 0L) {
            metaBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, currentDurationMs)
        }
        mediaSession?.setMetadata(metaBuilder.build())
    }

    private fun buildNotification(artwork: Bitmap): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = servicePendingIntent(ACTION_PREV, 101)
        val playPausePending = servicePendingIntent(ACTION_PLAY_PAUSE, 102)
        val nextPending = servicePendingIntent(ACTION_NEXT, 103)
        val stopPending = servicePendingIntent(ACTION_STOP, 104)

        val playPauseIcon = if (currentlyPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseLabel = if (currentlyPlaying) "Pause" else "Lecture"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setColorized(true)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_prev),
            "Précédent",
            prevPending
        ).build()
        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, playPauseIcon),
            playPauseLabel,
            playPausePending
        ).build()
        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_next),
            "Suivant",
            nextPending
        ).build()
        val closeAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_close),
            "Fermer",
            stopPending
        ).build()

        val subTextDisplay = if (currentAlbum.isNotBlank() && currentAlbum != "Stockage local") {
            currentAlbum
        } else {
            "Em Player"
        }

        return builder
            .setSmallIcon(R.drawable.ic_stat_em_player)
            .setLargeIcon(artwork)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText(subTextDisplay)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopPending)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setColor(0xFF0A1432.toInt())
            .setOngoing(currentlyPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(closeAction)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun getOrCreateArtworkBitmap(): Bitmap {
        val key = "$currentTitle|$currentPrimaryColor|$currentSecondaryColor|$currentTertiaryColor"
        cachedArtwork?.let { existing ->
            if (cachedArtworkKey == key && !existing.isRecycled) return existing
        }

        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val primary = (currentPrimaryColor or 0xFF000000L).toInt()
        val secondary = (currentSecondaryColor or 0xFF000000L).toInt()
        val tertiary = (currentTertiaryColor or 0xFF000000L).toInt()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                size.toFloat(),
                size.toFloat(),
                intArrayOf(secondary, tertiary, 0xFF040B25.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.35f,
                size * 0.30f,
                size * 0.55f,
                intArrayOf((primary and 0x00FFFFFF) or (0x99 shl 24), 0x00000000),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size * 0.35f, size * 0.30f, size * 0.55f, glowPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = (primary and 0x00FFFFFF) or (0x88 shl 24)
        }
        canvas.drawCircle(size * 0.5f, size * 0.5f, size * 0.34f, ringPaint)

        // Subtle gold wave curve
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            color = primary
        }
        val wave = Path().apply {
            moveTo(size * 0.22f, size * 0.52f)
            quadTo(size * 0.36f, size * 0.32f, size * 0.50f, size * 0.52f)
            quadTo(size * 0.64f, size * 0.72f, size * 0.78f, size * 0.52f)
        }
        canvas.drawPath(wave, wavePaint)

        // Three precision equalizer bars
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFDE68A.toInt()
        }
        val barW = 14f
        val gap = 12f
        val startX = (size - (barW * 3 + gap * 2)) / 2f
        val barHeights = floatArrayOf(48f, 78f, 56f)
        for (i in 0..2) {
            val left = startX + i * (barW + gap)
            val top = (size - barHeights[i]) / 2f
            canvas.drawRoundRect(
                RectF(left, top, left + barW, top + barHeights[i]),
                barW / 2f,
                barW / 2f,
                barPaint
            )
        }

        cachedArtworkKey = key
        cachedArtwork = bmp
        return bmp
    }

    private fun servicePendingIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(this, AuraPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundCompat() {
        isForegroundActive = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        isForegroundActive = false
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
