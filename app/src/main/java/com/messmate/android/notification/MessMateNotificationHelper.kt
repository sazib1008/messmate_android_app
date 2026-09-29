package com.messmate.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.messmate.android.MainActivity
import com.messmate.android.R

object MessMateNotificationHelper {

    const val CHANNEL_DEFAULT = "messmate_channel_default"
    const val CHANNEL_FINANCIAL = "messmate_channel_financial"
    const val CHANNEL_MEALS = "messmate_channel_meals"
    const val CHANNEL_GENERAL = "messmate_channel_general"

    const val EXTRA_TARGET_ROUTE = "target_route"
    const val EXTRA_SCREEN = "screen"

    private const val TAG = "MessMateNotification"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard MessMate alerts and general notices"
                enableVibration(true)
            }

            val financialChannel = NotificationChannel(
                CHANNEL_FINANCIAL,
                "Financial & Deposits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates on deposit verification, balance changes, and cycle settlement"
                enableVibration(true)
            }

            val mealsChannel = NotificationChannel(
                CHANNEL_MEALS,
                "Meals & Cutoffs",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for meal cutoffs, dining changes, and kitchen prep"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Announcements & Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Join requests, mess notices, and cycle status alerts"
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, financialChannel, mealsChannel, generalChannel)
            )
            Log.d(TAG, "Notification channels registered successfully (Default, Financial, Meals, General)")
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_DEFAULT,
        targetRoute: String? = null,
        screen: String? = null,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetRoute.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            }
            if (!screen.isNullOrBlank()) {
                putExtra(EXTRA_SCREEN, screen)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channelId == CHANNEL_FINANCIAL || channelId == CHANNEL_MEALS)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Notification displayed successfully: [$title] -> route=$targetRoute, screen=$screen")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot display notification: POST_NOTIFICATIONS permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }
}
