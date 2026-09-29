package com.messmate.android.ui.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messmate.android.domain.model.MealSession
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta
import com.messmate.android.ui.theme.WarningAmber
import com.messmate.android.util.DhakaDateUtils

@Composable
fun ManagerOverviewScreen(
    state: ManagerUiState,
    onNavigateToExpenses: () -> Unit,
    onNavigateToDeposits: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPauseDay: (date: String, session: MealSession?, reason: String) -> Unit,
    onFinalizeCycle: (confirmForfeit: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPauseDayDialog by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var surplusWarningText by remember { mutableStateOf<String?>(null) }
    var confirmForfeitChecked by remember { mutableStateOf(false) }

    val pendingDepositsCount = state.deposits.count { it.status == com.messmate.android.domain.model.DepositStatus.PENDING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cycle Progression Card
        item {
            CycleProgressionCard(
                activeCycle = state.activeCycle,
                onPauseDayClick = { showPauseDayDialog = true },
                onFinalizeClick = {
                    surplusWarningText = null
                    confirmForfeitChecked = false
                    showFinalizeDialog = true
                }
            )
        }

        // 2. Financial Metrics Row: Total Expenses, Meal Rate (with Volatility Guard), Treasury
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Cycle Expenses",
                        value = "৳${"%,.1f".format(state.totalExpensesAmount)}",
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        iconTint = Terracotta,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Treasury Balance",
                        value = "৳${"%,.1f".format(state.treasuryBalance)}",
                        icon = Icons.Rounded.AccountBalance,
                        iconTint = if (state.treasuryBalance >= 0) SageGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Meal Rate with Volatility Guard Card
                MealRateCard(
                    mealRate = state.mealRate,
                    isRateVolatile = state.isRateVolatile,
                    volatilityReason = state.volatilityReason
                )
            }
        }

        // 3. Quick Action Cards
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Expenses",
                    subtitle = "${state.expenses.size} recorded",
                    icon = Icons.Rounded.AddShoppingCart,
                    onClick = onNavigateToExpenses,
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    title = "Deposits",
                    subtitle = if (pendingDepositsCount > 0) "$pendingDepositsCount pending" else "All caught up",
                    icon = Icons.Rounded.Savings,
                    badgeCount = pendingDepositsCount,
                    onClick = onNavigateToDeposits,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Member Roster",
                    subtitle = "${state.members.size} members",
                    icon = Icons.Rounded.People,
                    badgeCount = state.joinRequests.size,
                    onClick = onNavigateToMembers,
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    title = "Menu Planner",
                    subtitle = "7-Day schedule",
                    icon = Icons.Rounded.RestaurantMenu,
                    onClick = onNavigateToMenu,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Recent Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.expenses.size > 5) {
                    TextButton(onClick = onNavigateToExpenses) {
                        Text("View all (${state.expenses.size})")
                    }
                }
            }
        }

        if (state.recentExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No expenses recorded yet in this cycle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.recentExpenses) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                                text = expense.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(
                                    text = expense.category.name.replace("_", " "),
                                    color = Terracotta
                                )
                                Text(
                                    text = DhakaDateUtils.formatDhakaDateHeader(expense.expenseDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "৳${"%,.2f".format(expense.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Terracotta
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Dialog: Pause a Day / Holiday
    if (showPauseDayDialog) {
        PauseDayDialog(
            isPausing = state.isPausingDay,
            onDismiss = { showPauseDayDialog = false },
            onConfirm = { date, session, reason ->
                onPauseDay(date, session, reason)
                showPauseDayDialog = false
            }
        )
    }

    // Dialog: Finalize / Close Cycle Confirmation
    if (showFinalizeDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isFinalizingCycle) showFinalizeDialog = false },
            title = {
                Text(
                    text = "Close Cycle #${state.activeCycle?.cycleNumber ?: ""}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to conclude and finalize this billing cycle? All expenses, meal counts, and balances will be calculated and settled.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Summary stats
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Total Expenses: ৳${"%,.2f".format(state.totalExpensesAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Current Meal Rate: ৳${"%,.2f".format(state.mealRate)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Surplus Warning (if backend previously warned or flagged)
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
                                    modifier = Modifier.clickable { confirmForfeitChecked = !confirmForfeitChecked }
                                ) {
                                    Checkbox(
                                        checked = confirmForfeitChecked,
                                        onCheckedChange = { confirmForfeitChecked = it }
                                    )
                                    Text(
                                        text = "Confirm forfeit of surplus balances (carry forward is disabled)",
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
                        onFinalizeCycle(confirmForfeitChecked)
                    },
                    enabled = !state.isFinalizingCycle && (surplusWarningText == null || confirmForfeitChecked),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    if (state.isFinalizingCycle) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Finalize & Close")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalizeDialog = false },
                    enabled = !state.isFinalizingCycle
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CycleProgressionCard(
    activeCycle: com.messmate.android.data.remote.dto.ActiveCycleDtoRemote?,
    onPauseDayClick: () -> Unit,
    onFinalizeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cycle #${activeCycle?.cycleNumber ?: 1}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Terracotta
                    )
                    Text(
                        text = "Active Billing Cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    text = activeCycle?.status ?: "ACTIVE",
                    color = if (activeCycle?.status == "SETTLED") MaterialTheme.colorScheme.outline else SageGreen
                )
            }

            // Progression Stats
            val targetDays = activeCycle?.targetActiveDays ?: 30
            val countedDays = activeCycle?.countedActiveDays ?: 0
            val pausedDaysCount = activeCycle?.pausedDays?.size ?: 0
            val progress = (countedDays.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Day $countedDays of $targetDays",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Terracotta,
                    trackColor = Terracotta.copy(alpha = 0.2f)
                )
            }

            // Key Dates and Paused Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Started",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeCycle?.startDate?.let { DhakaDateUtils.formatDhakaDateHeader(it) } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Paused Holidays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$pausedDaysCount day(s)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Scheduled End",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeCycle?.scheduledEndDate?.let { DhakaDateUtils.formatDhakaDateHeader(it) } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPauseDayClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause Day", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onFinalizeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Conclude Cycle", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MealRateCard(
    mealRate: Double,
    isRateVolatile: Boolean,
    volatilityReason: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRateVolatile) WarningAmber.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isRateVolatile) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Current Meal Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isRateVolatile) {
                    StatusBadge(
                        text = "Provisional — early cycle",
                        color = WarningAmber
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "৳${"%,.2f".format(mealRate)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Terracotta
                )
                Text(
                    text = "/ meal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (isRateVolatile) {
                Text(
                    text = volatilityReason ?: "Rate is highly volatile during the first days as initial expenses are divided by few meals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Terracotta.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = Terracotta, modifier = Modifier.size(20.dp))
                    }
                }
                if (badgeCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("$badgeCount", color = Color.White)
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PauseDayDialog(
    isPausing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (date: String, session: MealSession?, reason: String) -> Unit
) {
    var dateStr by remember { mutableStateOf(DhakaDateUtils.todayDateString()) }
    var selectedSession by remember { mutableStateOf<MealSession?>(null) } // null = All Day
    var reason by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause Day / Holiday", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Meals will not be counted or billed during this paused duration. Cycle target days will be adjusted automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD, Asia/Dhaka)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Session selector (All Day, Breakfast, Lunch, Dinner)
                Text("Session (optional)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSession == null,
                        onClick = { selectedSession = null },
                        label = { Text("All Day") }
                    )
                    MealSession.values().forEach { session ->
                        FilterChip(
                            selected = selectedSession == session,
                            onClick = { selectedSession = session },
                            label = { Text(session.name.take(1) + session.name.drop(1).lowercase()) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Reason (e.g. Eid Holiday, Mess Closed)") },
                    isError = hasError,
                    supportingText = if (hasError) { { Text("Reason is required") } } else null,
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
                    Text("Record Pause")
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
