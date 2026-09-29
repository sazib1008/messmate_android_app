package com.messmate.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.messmate.android.navigation.MessMateNavGraph
import com.messmate.android.notification.MessMateNotificationHelper
import com.messmate.android.notification.PushNotificationManager
import com.messmate.android.ui.theme.MessMateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pushNotificationManager: PushNotificationManager

    private var targetRouteState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pushNotificationManager.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pushNotificationManager.init()

        requestNotificationPermission()
        handleIntent(intent)

        setContent {
            MessMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val targetRoute by targetRouteState
                    MessMateNavGraph(initialTargetRoute = targetRoute)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent?.getStringExtra(MessMateNotificationHelper.EXTRA_TARGET_ROUTE)
            ?: intent?.getStringExtra(MessMateNotificationHelper.EXTRA_SCREEN)
            ?: intent?.getStringExtra("screen")
            ?: intent?.getStringExtra("route")
            ?: intent?.getStringExtra("target_route")

        if (!target.isNullOrBlank()) {
            val resolved = when (target.lowercase()) {
                "wallet" -> "student_home_wallet"
                "deposits" -> "manager_home_deposits"
                "meals", "dashboard" -> "student_home_dashboard"
                "join_requests" -> "manager_home_join_requests"
                "chef" -> "chef_terminal"
                else -> target
            }
            targetRouteState.value = resolved
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

