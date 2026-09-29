package com.messmate.android.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messmate.android.ui.common.MessMateButton
import com.messmate.android.ui.common.StatusBadge
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinMessScreen(
    onJoinSubmitted: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val joinState by viewModel.joinState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Check if there's already a pending join request
    val pendingJoinRequest = (authState as? AuthUiState.Success)?.authState?.pendingJoinRequest

    var messCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var successSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(joinState) {
        when (val state = joinState) {
            is AuthUiState.Loading -> isLoading = true
            is AuthUiState.Success -> {
                isLoading = false
                successSubmitted = true
            }
            is AuthUiState.Error -> {
                isLoading = false
                errorMsg = state.message
                viewModel.resetJoinState()
            }
            else -> isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join a Mess") },
                actions = {
                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pending state
            if (pendingJoinRequest != null && !successSubmitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.HourglassEmpty, null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Approval Pending", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Text("You have a pending request to join ${pendingJoinRequest.messName}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        StatusBadge("PENDING", color = Color(0xFFF59E0B))
                        Spacer(Modifier.height(4.dp))
                        Text("The manager will review your request shortly. Once approved, come back and you'll be automatically taken to your dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        OutlinedButton(onClick = onJoinSubmitted, modifier = Modifier.fillMaxWidth()) {
                            Text("Check Dashboard")
                        }
                    }
                }
                HorizontalDivider()
                Text("Or join a different mess below:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Success state
            if (successSubmitted) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp))
                        Text("Request Submitted!", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary)
                        Text("Your request is awaiting manager approval.", textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Button(onClick = onJoinSubmitted, modifier = Modifier.fillMaxWidth()) {
                            Text("Continue to App")
                        }
                    }
                }
            } else {
                // Join form
                Icon(Icons.Rounded.Group, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp))
                Text("Enter your mess join code", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Ask your mess manager for the 6-character join code (e.g. MM8K91).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (errorMsg != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                OutlinedTextField(
                    value = messCode,
                    onValueChange = { messCode = it.uppercase().trim(); errorMsg = null },
                    label = { Text("Mess Join Code") },
                    leadingIcon = { Icon(Icons.Rounded.Pin, null) },
                    placeholder = { Text("e.g. MM8K91") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room Number (optional)") },
                    leadingIcon = { Icon(Icons.Rounded.MeetingRoom, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                MessMateButton(
                    text = "Submit Join Request",
                    onClick = {
                        viewModel.submitJoinRequest(
                            messCode.trim(),
                            notes.takeIf { it.isNotBlank() },
                            roomNumber.takeIf { it.isNotBlank() }
                        )
                    },
                    isLoading = isLoading,
                    enabled = messCode.length >= 4
                )
            }
        }
    }
}
