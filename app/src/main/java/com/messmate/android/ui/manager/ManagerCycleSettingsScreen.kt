package com.messmate.android.ui.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.messmate.android.domain.model.DiningConfig
import com.messmate.android.domain.model.MealSession
import com.messmate.android.domain.model.PastCycleSummary
import com.messmate.android.domain.model.displayName
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta
import com.messmate.android.ui.theme.WarningAmber
import com.messmate.android.util.DhakaDateUtils

@Composable
fun ManagerCycleSettingsScreen(
    state: ManagerUiState,
    onPauseDay: (date: String, session: MealSession?, reason: String) -> Unit,
    onFinalizeCycle: (confirmForfeit: Boolean) -> Unit,
    onStartNewCycle: (targetDays: Int, carryForward: Boolean) -> Unit,
    onUpdateDiningConfig: (DiningConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPauseDayDialog by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var showStartCycleDialog by remember { mutableStateOf(false) }
    var surplusWarningText by remember { mutableStateOf<String?>(null) }
    var confirmForfeitSurplus by remember { mutableStateOf(false) }

    // Dining config local state for editing
    var isEditingConfig by remember { mutableStateOf(false) }
    var editableConfig by remember(state.diningConfig) { mutableStateOf(state.diningConfig ?: DiningConfig()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Active Cycle Status & Conclude Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Billing Cycle Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusBadge(
                            text = state.activeCycle?.status ?: "ACTIVE",
                            color = if (state.activeCycle?.status == "SETTLED") MaterialTheme.colorScheme.outline else SageGreen
                        )
                    }

                    if (state.activeCycle != null) {
                        val cycle = state.activeCycle
                        Text(
                            text = "Cycle #${cycle.cycleNumber}: Day ${cycle.countedActiveDays} of ${cycle.targetActiveDays}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Started: ${DhakaDateUtils.formatDhakaDateHeader(cycle.startDate)} • Scheduled End: ${DhakaDateUtils.formatDhakaDateHeader(cycle.scheduledEndDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPauseDayDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause Day", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                surplusWarningText = null
                                confirmForfeitSurplus = false
                                showFinalizeDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conclude Cycle", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Button to start new cycle
                    OutlinedButton(
                        onClick = { showStartCycleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start New Cycle", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Dining Session Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dining Session Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active sessions, cutoff times, and multipliers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = { isEditingConfig = !isEditingConfig }) {
                            Text(if (isEditingConfig) "Cancel" else "Edit")
                        }
                    }

                    editableConfig.sessions.forEachIndexed { index, sessionConfig ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        SessionConfigRow(
                            session = sessionConfig.session,
                            isActive = sessionConfig.isEnabled,
                            cutoffTime = sessionConfig.cutoffTime,
                            servingWindow = "${sessionConfig.servingStartTime ?: "12:00"} - ${sessionConfig.servingEndTime ?: "14:00"}",
                            weight = sessionConfig.unitValue,
                            isEditing = isEditingConfig,
                            onActiveChange = { active ->
                                val updated = editableConfig.sessions.toMutableList()
                                updated[index] = sessionConfig.copy(isEnabled = active)
                                editableConfig = editableConfig.copy(sessions = updated)
                            },
                            onCutoffChange = { cutoff ->
                                val updated = editableConfig.sessions.toMutableList()
                                updated[index] = sessionConfig.copy(cutoffTime = cutoff)
                                editableConfig = editableConfig.copy(sessions = updated)
                            },
                            onWindowChange = { window ->
                                val parts = window.split("-")
                                val start = parts.getOrNull(0)?.trim()
                                val end = parts.getOrNull(1)?.trim()
                                val updated = editableConfig.sessions.toMutableList()
                                updated[index] = sessionConfig.copy(servingStartTime = start, servingEndTime = end)
                                editableConfig = editableConfig.copy(sessions = updated)
                            },
                            onWeightChange = { weight ->
                                val updated = editableConfig.sessions.toMutableList()
                                updated[index] = sessionConfig.copy(unitValue = weight)
                                editableConfig = editableConfig.copy(sessions = updated)
                            }
                        )
                    }

                    if (isEditingConfig) {
                        Button(
                            onClick = {
                                onUpdateDiningConfig(editableConfig)
                                isEditingConfig = false
                            },
                            enabled = !state.isUpdatingConfig,
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (state.isUpdatingConfig) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save Dining Settings")
                            }
                        }
                    }
                }
            }
        }

        // 3. Past Cycles History
        item {
            Text(
                text = "Past Cycles History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.cycleHistory.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No past cycles recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.cycleHistory) { cycleSummary ->
                PastCycleRow(cycleSummary)
            }
        }
    }

    // Pause a Day / Holiday Dialog
    if (showPauseDayDialog) {
        PauseDayDialogContent(
            isPausing = state.isPausingDay,
            onDismiss = { showPauseDayDialog = false },
            onConfirm = { date, session, reason ->
                onPauseDay(date, session, reason)
                showPauseDayDialog = false
            }
        )
    }

    // Finalize Cycle Dialog with Surplus Guard
    if (showFinalizeDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isFinalizingCycle) showFinalizeDialog = false },
            title = {
                Text("Conclude Cycle #${state.activeCycle?.cycleNumber ?: ""}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Conclude and finalize calculation for the active cycle? This permanently fixes all expense allocations and member balances for this period.")

                    if (surplusWarningText != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = surplusWarningText ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { confirmForfeitSurplus = !confirmForfeitSurplus }
                                ) {
                                    Checkbox(
                                        checked = confirmForfeitSurplus,
                                        onCheckedChange = { confirmForfeitSurplus = it }
                                    )
                                    Text(
                                        text = "Confirm forfeit of surplus balances (carry-forward disabled)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinalizeCycle(confirmForfeitSurplus)
                    },
                    enabled = !state.isFinalizingCycle && (surplusWarningText == null || confirmForfeitSurplus),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    if (state.isFinalizingCycle) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Finalize Cycle")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeDialog = false }, enabled = !state.isFinalizingCycle) {
                    Text("Cancel")
                }
            }
        )
    }

    // Start New Cycle Dialog
    if (showStartCycleDialog) {
        var targetDaysText by remember { mutableStateOf("30") }
        var carryForward by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!state.isStartingNewCycle) showStartCycleDialog = false },
            title = { Text("Start New Cycle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Initialize a new billing cycle starting from today (Asia/Dhaka).")

                    OutlinedTextField(
                        value = targetDaysText,
                        onValueChange = { targetDaysText = it },
                        label = { Text("Target Active Days (e.g. 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { carryForward = !carryForward }
                    ) {
                        Checkbox(
                            checked = carryForward,
                            onCheckedChange = { carryForward = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Carry forward remaining balances to new cycle",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = targetDaysText.toIntOrNull() ?: 30
                        onStartNewCycle(days, carryForward)
                        showStartCycleDialog = false
                    },
                    enabled = !state.isStartingNewCycle,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    if (state.isStartingNewCycle) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Start Cycle")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartCycleDialog = false }, enabled = !state.isStartingNewCycle) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionConfigRow(
    session: MealSession,
    isActive: Boolean,
    cutoffTime: String,
    servingWindow: String,
    weight: Double,
    isEditing: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onCutoffChange: (String) -> Unit,
    onWindowChange: (String) -> Unit,
    onWeightChange: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = session.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(
                    text = if (isActive) "Active" else "Disabled",
                    color = if (isActive) SageGreen else MaterialTheme.colorScheme.outline
                )
            }

            if (isEditing) {
                Switch(
                    checked = isActive,
                    onCheckedChange = onActiveChange
                )
            }
        }

        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cutoffTime,
                    onValueChange = onCutoffChange,
                    label = { Text("Cutoff") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = servingWindow,
                    onValueChange = onWindowChange,
                    label = { Text("Serving Window") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cutoff: $cutoffTime • Window: $servingWindow",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Weight: ${weight}x",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PastCycleRow(cycle: PastCycleSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cycle #${cycle.cycleNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val endStr = cycle.endDate?.let { DhakaDateUtils.formatDhakaDateHeader(it) } ?: "Ongoing"
                Text(
                    text = "${DhakaDateUtils.formatDhakaDateHeader(cycle.startDate)} to $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rate: ৳${"%,.2f".format(cycle.mealRate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Terracotta
                )
                Text(
                    text = "Expenses: ৳${"%,.0f".format(cycle.totalExpenses)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PauseDayDialogContent(
    isPausing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (date: String, session: MealSession?, reason: String) -> Unit
) {
    var dateStr by remember { mutableStateOf(DhakaDateUtils.todayDateString()) }
    var selectedSession by remember { mutableStateOf<MealSession?>(null) }
    var reason by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause Day / Holiday", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select date to pause all meal tracking for the mess (Asia/Dhaka).")

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Reason (e.g. Durga Puja, University Closed)") },
                    isError = hasError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        hasError = true
                    } else {
                        onConfirm(dateStr, selectedSession, reason)
                    }
                },
                enabled = !isPausing,
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                if (isPausing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Pause Day")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPausing) {
                Text("Cancel")
            }
        }
    )
}
