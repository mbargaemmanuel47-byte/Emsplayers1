package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.R
import com.example.model.ProactiveNotificationDraft
import com.example.permissions.PermissionManager

object ProactiveNotificationHelper {

    const val CHANNEL_ID = "em_player_emma_proactive"
    const val NOTIFICATION_ID = 4301

    const val EXTRA_OPEN_ROUTE = "extra_open_route"
    const val EXTRA_PROACTIVE_MESSAGE = "extra_proactive_message"
    const val EXTRA_SUGGESTED_TRACK_ID = "extra_suggested_track_id"
    const val ROUTE_EMMA_CHAT = "studio"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.emma_notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.emma_notif_channel_desc)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun showProactiveNotification(context: Context, draft: ProactiveNotificationDraft): Boolean {
        val notifPerm = PermissionManager.notificationsPermissionOrNull()
        if (!PermissionManager.isPermissionGranted(context, notifPerm)) {
            return false
        }

        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ROUTE, ROUTE_EMMA_CHAT)
            putExtra(EXTRA_PROACTIVE_MESSAGE, draft.message)
            draft.suggestedTrackId?.let { putExtra(EXTRA_SUGGESTED_TRACK_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            4301,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_em_player)
            .setContentTitle("Emma • ${draft.slot.label}")
            .setContentText(draft.message)
            .setStyle(android.app.Notification.BigTextStyle().bigText(draft.message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFF5B731.toInt())
            .build()

        return runCatching {
            nm.notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }
}
