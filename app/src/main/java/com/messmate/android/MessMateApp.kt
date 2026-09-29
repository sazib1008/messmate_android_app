package com.messmate.android

import android.app.Application
import com.messmate.android.notification.PushNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MessMateApp : Application() {

    @Inject
    lateinit var pushNotificationManager: PushNotificationManager

    override fun onCreate() {
        super.onCreate()
        pushNotificationManager.init()
    }
}
