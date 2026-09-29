package com.messmate.android.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.repository.ApiResult
import com.messmate.android.data.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenDataStore: TokenDataStore,
    private val notificationRepository: NotificationRepository
) {
    private val tag = "MessMateFCM"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init() {
        MessMateNotificationHelper.createNotificationChannels(context)
        Log.i(tag, "PushNotificationManager initialized and channels created.")
    }

    /**
     * Fetches current FCM token and registers it immediately with the backend
     * if an active JWT session is present.
     */
    fun syncDeviceToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(tag, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (!token.isNullOrBlank()) {
                    Log.i(tag, "Current FCM device token fetched: $token")
                    launchTokenRegistration(token)
                } else {
                    Log.w(tag, "FCM token returned null or blank.")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error accessing FirebaseMessaging instance", e)
        }
    }

    fun fetchCurrentToken(onTokenReady: ((String) -> Unit)? = null) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    val token = task.result
                    Log.i(tag, "Fetched active FCM token: $token")
                    onTokenReady?.invoke(token)
                } else {
                    Log.w(tag, "Failed to retrieve active token", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching active FCM token", e)
        }
    }

    private fun launchTokenRegistration(token: String) {
        scope.launch {
            tokenDataStore.saveFcmToken(token)
            val jwt = tokenDataStore.token.first()
            if (!jwt.isNullOrBlank()) {
                Log.d(tag, "Active JWT present. Sending token to backend /api/notifications/device-token ...")
                when (val res = notificationRepository.registerDeviceToken(token)) {
                    is ApiResult.Success -> {
                        Log.i(tag, "Device token successfully registered with MessMate backend.")
                    }
                    is ApiResult.Error -> {
                        Log.w(tag, "Backend token registration response: ${res.message}")
                    }
                }
            } else {
                Log.d(tag, "User not logged in yet. Token stored locally for subsequent login registration.")
            }
        }
    }

    fun unregisterDeviceToken() {
        scope.launch {
            try {
                val token = tokenDataStore.fcmToken.first()
                if (!token.isNullOrBlank()) {
                    Log.d(tag, "Unregistering device token from backend...")
                    notificationRepository.unregisterDeviceToken(token)
                    tokenDataStore.saveFcmToken("")
                    Log.i(tag, "Device token unregistered.")
                }
            } catch (e: Exception) {
                Log.w(tag, "Error unregistering token from backend", e)
            }
        }
    }
}
