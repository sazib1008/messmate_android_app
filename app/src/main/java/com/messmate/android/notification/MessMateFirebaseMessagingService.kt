package com.messmate.android.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.repository.ApiResult
import com.messmate.android.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MessMateFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenDataStore: TokenDataStore

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "MessMateFCM"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "FCM onNewToken callback received: $token")

        serviceScope.launch {
            tokenDataStore.saveFcmToken(token)
            // If user has an active session, register token with backend endpoint
            val jwt = tokenDataStore.token.first()
            if (!jwt.isNullOrBlank()) {
                Log.d(tag, "Active session detected. Registering new token with backend...")
                when (val result = notificationRepository.registerDeviceToken(token)) {
                    is ApiResult.Success -> {
                        Log.i(tag, "Successfully registered new FCM token with backend: $token")
                    }
                    is ApiResult.Error -> {
                        Log.w(tag, "Failed to register new FCM token with backend: ${result.message}")
                    }
                }
            } else {
                Log.d(tag, "No active session present; FCM token saved locally for post-login registration: $token")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        Log.d(tag, "FCM message received from: ${remoteMessage.from}")
        Log.d(tag, "FCM payload data: $data")
        Log.d(tag, "FCM notification title: ${notification?.title}, body: ${notification?.body}")

        // 1. Resolve title and body
        val title = notification?.title
            ?: data["title"]
            ?: "MessMate Notice"

        val body = notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "You have an update in your mess."

        // 2. Resolve screen and event routing
        // Can be specified as "screen": "wallet", "screen": "deposits", or "type": "DEPOSIT_APPROVED", etc.
        val rawScreen = data["screen"] ?: data["route"] ?: data["target_route"]
        val eventType = data["type"] ?: "GENERAL"

        val (channelId, resolvedRoute) = when {
            rawScreen.equals("wallet", ignoreCase = true) -> {
                MessMateNotificationHelper.CHANNEL_FINANCIAL to "student_home_wallet"
            }
            rawScreen.equals("deposits", ignoreCase = true) -> {
                MessMateNotificationHelper.CHANNEL_FINANCIAL to "manager_home_deposits"
            }
            rawScreen.equals("meals", ignoreCase = true) || rawScreen.equals("dashboard", ignoreCase = true) -> {
                MessMateNotificationHelper.CHANNEL_MEALS to "student_home_dashboard"
            }
            rawScreen.equals("join_requests", ignoreCase = true) -> {
                MessMateNotificationHelper.CHANNEL_GENERAL to "manager_home_join_requests"
            }
            rawScreen.equals("chef", ignoreCase = true) -> {
                MessMateNotificationHelper.CHANNEL_MEALS to "chef_terminal"
            }
            eventType == "DEPOSIT_APPROVED" || eventType == "DEPOSIT_REJECTED" -> {
                MessMateNotificationHelper.CHANNEL_FINANCIAL to "student_home_wallet"
            }
            eventType == "NEW_JOIN_REQUEST" -> {
                MessMateNotificationHelper.CHANNEL_GENERAL to "manager_home_join_requests"
            }
            eventType == "JOIN_REQUEST_APPROVED" -> {
                MessMateNotificationHelper.CHANNEL_GENERAL to "student_home"
            }
            eventType == "MEAL_CUTOFF" || eventType == "CUTOFF_APPROACHING" -> {
                MessMateNotificationHelper.CHANNEL_MEALS to "student_home_dashboard"
            }
            eventType == "CYCLE_CONCLUDED" || eventType == "NEW_CYCLE_STARTED" -> {
                MessMateNotificationHelper.CHANNEL_GENERAL to "student_home_wallet"
            }
            !rawScreen.isNullOrBlank() -> {
                MessMateNotificationHelper.CHANNEL_DEFAULT to rawScreen
            }
            else -> {
                MessMateNotificationHelper.CHANNEL_DEFAULT to null
            }
        }

        MessMateNotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            targetRoute = resolvedRoute,
            screen = rawScreen
        )
    }
}
