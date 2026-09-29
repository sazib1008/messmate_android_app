package com.messmate.android.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.messmate.android.ui.auth.AuthUiState
import com.messmate.android.ui.auth.AuthViewModel
import com.messmate.android.domain.model.UserRole

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    authViewModel: AuthViewModel
) {
    val authState = authViewModel.authState

    LaunchedEffect(Unit) {
        authViewModel.checkSession()
    }

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthUiState.Success -> {
                val role = state.authState.membership?.role
                val hasMembership = state.authState.membership != null
                val destination = when {
                    role?.isManager == true -> "manager_home"
                    role == UserRole.CHEF -> "chef_terminal"
                    hasMembership -> "student_home"
                    state.authState.pendingJoinRequest != null -> "join_mess"
                    else -> "join_mess"
                }
                onNavigate(destination)
            }
            is AuthUiState.Idle, is AuthUiState.Error -> {
                onNavigate("login")
            }
            else -> {} // still loading
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("MessMate", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(onLogout: () -> Unit, authViewModel: AuthViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MessMate") },
                actions = {
                    IconButton(onClick = { authViewModel.logout(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(Icons.Rounded.HourglassEmpty, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Manager & Chef App", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    "The Manager and Chef experience is coming soon.\nPlease use the web app at this time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = { authViewModel.logout(); onLogout() }) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out")
                }
            }
        }
    }
}
