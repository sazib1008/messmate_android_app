package com.messmate.android.ui.chef

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messmate.android.domain.model.ChefDailyHeadcount
import com.messmate.android.domain.model.ChefSessionHeadcount
import com.messmate.android.domain.model.MealSession
import com.messmate.android.domain.model.displayName
import com.messmate.android.domain.model.getActiveSessionList
import com.messmate.android.ui.common.ErrorState
import com.messmate.android.ui.common.LoadingScreen
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta
import com.messmate.android.ui.theme.WarningAmber
import com.messmate.android.util.DhakaDateUtils
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefPrepStationScreen(
    onLogout: () -> Unit,
    onBackToManager: (() -> Unit)? = null,
    viewModel: ChefViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Live clock in Asia/Dhaka
    var currentDhakaTime by remember { mutableStateOf(DhakaDateUtils.formatTimeNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentDhakaTime = DhakaDateUtils.formatTimeNow()
            delay(1000)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val todayDate = remember { DhakaDateUtils.todayDateString() }
    val tomorrowDate = remember { DhakaDateUtils.dateWithOffset(1) }

    val activeSessions = remember(state.diningConfig) {
        state.diningConfig?.getActiveSessionList()
            ?: listOf(MealSession.BREAKFAST, MealSession.LUNCH, MealSession.DINNER)
    }

    // Filter headcount data to only active sessions
    val displaySessions = remember(state.headcountData, activeSessions) {
        val all = state.headcountData?.sessions ?: emptyList()
        all.filter { it.session in activeSessions }
    }

    val totalServings = remember(displaySessions) { displaySessions.sumOf { it.totalHeadcount } }
    val regularServings = remember(displaySessions) { displaySessions.sumOf { it.studentOnCount } }
    val guestServings = remember(displaySessions) { displaySessions.sumOf { it.guestMealCount } }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Kitchen Prep Station",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(text = "LIVE", color = SageGreen)
                        }
                        Text(
                            text = "${state.messName.ifBlank { "Mess Kitchen" }} • Chef ${state.chefName} • $currentDhakaTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Share / Print prep sheet button
                    IconButton(
                        onClick = {
                            state.headcountData?.let { data ->
                                sharePrepSheet(context, data, displaySessions)
                            }
                        },
                        enabled = state.headcountData != null
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = "Print / Share Prep Sheet")
                    }

                    // Manual refresh button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh headcounts")
                        }
                    }

                    // Manager switch back button if accessed by manager/owner
                    if (onBackToManager != null && state.userRole != com.messmate.android.domain.model.UserRole.CHEF) {
                        IconButton(onClick = onBackToManager) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = "Back to Manager Portal", tint = Terracotta)
                        }
                    }

                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        if (state.isLoading && state.headcountData == null) {
            LoadingScreen(message = "Loading kitchen headcounts...", modifier = Modifier.padding(paddingValues))
        } else if (state.errorMessage != null && state.headcountData == null) {
            ErrorState(
                message = state.errorMessage ?: "Failed to connect to kitchen terminal.",
                onRetry = { viewModel.loadInitialData() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                com.messmate.android.ui.common.OfflineStatusBar(isOnline = !state.isOffline)
                com.messmate.android.ui.common.CachedDataBanner(cachedAt = state.cachedAt)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
                ) {
                // 1. Date Navigation: Quick Tabs & Prev/Next
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Quick Tabs: Today / Tomorrow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedDate == todayDate,
                                onClick = { viewModel.selectDate(todayDate) },
                                label = { Text("Today's Kitchen Plan") },
                                leadingIcon = {
                                    if (state.selectedDate == todayDate) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = state.selectedDate == tomorrowDate,
                                onClick = { viewModel.selectDate(tomorrowDate) },
                                label = { Text("Tomorrow's Kitchen Plan") },
                                leadingIcon = {
                                    if (state.selectedDate == tomorrowDate) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Arbitrary Date Navigation Row
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.prevDay() }) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Day")
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { showDatePickerDialog = true }
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Terracotta)
                                    Text(
                                        text = DhakaDateUtils.formatDhakaDateHeader(state.selectedDate),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(onClick = { viewModel.nextDay() }) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Day")
                                }
                            }
                        }
                    }
                }

                // 2. Holiday Banner if Day is Paused
                if (state.headcountData?.isPaused == true) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column {
                                    Text(
                                        text = "Mess Paused / Holiday — No Service",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = state.headcountData?.pauseReason ?: "Scheduled holiday for this mess. Meals are not served.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Top Stat Row: Total Servings, Regular, Guests
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeadcountStatCard(
                            title = "Total Servings",
                            count = totalServings,
                            icon = Icons.Rounded.Restaurant,
                            tint = Terracotta,
                            modifier = Modifier.weight(1f)
                        )
                        HeadcountStatCard(
                            title = "Regular Students",
                            count = regularServings,
                            icon = Icons.Rounded.School,
                            tint = SageGreen,
                            modifier = Modifier.weight(1f)
                        )
                        HeadcountStatCard(
                            title = "Guest Meals",
                            count = guestServings,
                            icon = Icons.Rounded.GroupAdd,
                            tint = WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Per-Session Prep Cards
                items(displaySessions, key = { it.session.name }) { sessionHeadcount ->
                    val sessionConfig = state.diningConfig?.sessions?.firstOrNull { it.session == sessionHeadcount.session }
                    val cutoffTime = sessionConfig?.cutoffTime ?: when (sessionHeadcount.session) {
                        MealSession.BREAKFAST -> "07:00"
                        MealSession.LUNCH -> "12:00"
                        MealSession.DINNER -> "19:00"
                    }
                    val isCutoffPassed = DhakaDateUtils.isMealCutoffPassed(cutoffTime, state.selectedDate)

                    KitchenSessionCard(
                        headcount = sessionHeadcount,
                        cutoffTime = cutoffTime,
                        isCutoffPassed = isCutoffPassed,
                        isDayPaused = state.headcountData?.isPaused == true
                    )
                }

                // 5. Kitchen Policy Notice Footer
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Kitchen Privacy Policy: Anonymized headcount only. No student names or financial records are ever displayed on this terminal. Headcounts update automatically as cutoff times close.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Date Picker Input Dialog
    if (showDatePickerDialog) {
        var inputDateText by remember { mutableStateOf(state.selectedDate) }
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = { Text("Select Plan Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter date in ISO format (YYYY-MM-DD):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = inputDateText,
                        onValueChange = { inputDateText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectDate(inputDateText.trim())
                        showDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeadcountStatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tint
            )
        }
    }
}

@Composable
private fun KitchenSessionCard(
    headcount: ChefSessionHeadcount,
    cutoffTime: String,
    isCutoffPassed: Boolean,
    isDayPaused: Boolean
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Session Name, Icon, Serving Window
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Terracotta.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (headcount.session) {
                                    MealSession.BREAKFAST -> Icons.Rounded.FreeBreakfast
                                    MealSession.LUNCH -> Icons.Rounded.WbSunny
                                    MealSession.DINNER -> Icons.Rounded.NightsStay
                                },
                                contentDescription = null,
                                tint = Terracotta,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = headcount.session.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val window = if (!headcount.servingStartTime.isNullOrBlank() && !headcount.servingEndTime.isNullOrBlank()) {
                            "${headcount.servingStartTime} - ${headcount.servingEndTime}"
                        } else "Standard Serving Window"
                        Text(
                            text = window,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cutoff time badge
                Text(
                    text = "Cutoff: $cutoffTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Portions to Cook Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PORTIONS TO COOK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${headcount.totalHeadcount}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (headcount.totalHeadcount > 0) Terracotta else MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "servings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Breakdown chips
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(
                            text = "${headcount.studentOnCount} Regular",
                            color = SageGreen
                        )
                        if (headcount.guestMealCount > 0) {
                            StatusBadge(
                                text = "+${headcount.guestMealCount} Guests",
                                color = WarningAmber
                            )
                        }
                    }
                }
            }

            // Scheduled Dish
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scheduled Dish",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = headcount.menuItemName ?: "No specific dish entered for this session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (headcount.menuItemName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }

            // Special Dietary & Prep Requirements (Anonymized)
            val hasDietary = headcount.dietaryTags.isNotEmpty() || headcount.notes.isNotEmpty()
            if (hasDietary) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Special Dietary & Prep Requirements",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        headcount.dietaryTags.forEach { tag ->
                            StatusBadge(text = tag, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    if (headcount.notes.isNotEmpty()) {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                headcount.notes.forEach { note ->
                                    Text(
                                        text = "• $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Footer Status: Distinguish 3 distinct states
            when {
                isDayPaused || !headcount.isEnabled -> {
                    // State C: Mess Paused / Holiday — No Service
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Mess Paused / Holiday — No Service",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                isCutoffPassed && headcount.totalHeadcount > 0 -> {
                    // State B: Portions Locked for Prep
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Portions Locked for Prep (Cutoff passed)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                }
                headcount.totalHeadcount == 0 -> {
                    // State A: Kitchen Off / No Orders
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PowerOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Kitchen Off / No Orders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    // Active & Open for changes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = SageGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Orders Active (Cutoff: $cutoffTime)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SageGreen
                        )
                    }
                }
            }
        }
    }
}

private fun sharePrepSheet(
    context: Context,
    headcount: ChefDailyHeadcount,
    sessions: List<ChefSessionHeadcount>
) {
    val totalServings = sessions.sumOf { it.totalHeadcount }
    val regularServings = sessions.sumOf { it.studentOnCount }
    val guestServings = sessions.sumOf { it.guestMealCount }

    val sb = StringBuilder()
    sb.appendLine("=========================================")
    sb.appendLine("MESSMATE KITCHEN PREP STATION")
    sb.appendLine("Mess: ${headcount.messName}")
    sb.appendLine("Plan Date: ${DhakaDateUtils.formatDhakaDateHeader(headcount.date)}")
    sb.appendLine("Generated: ${DhakaDateUtils.formatTimeNow()} (Asia/Dhaka)")
    sb.appendLine("=========================================")
    sb.appendLine()
    sb.appendLine("DAILY SUMMARY:")
    sb.appendLine("• Total Servings: $totalServings")
    sb.appendLine("• Regular Members: $regularServings")
    sb.appendLine("• Guest Meals: $guestServings")
    sb.appendLine()

    if (headcount.isPaused) {
        sb.appendLine("HOLIDAY NOTICE: Mess is registered as PAUSED / HOLIDAY.")
        sb.appendLine("Reason: ${headcount.pauseReason ?: "Scheduled holiday"}")
        sb.appendLine()
    }

    sb.appendLine("SESSION PORTIONS & MENU:")
    sessions.forEach { s ->
        sb.appendLine("-----------------------------------------")
        sb.appendLine("[${s.session.displayName.uppercase()}]")
        sb.appendLine("Portions to Cook: ${s.totalHeadcount} (Regular: ${s.studentOnCount}, Guests: ${s.guestMealCount})")
        sb.appendLine("Serving Time: ${s.servingStartTime ?: "12:00"} - ${s.servingEndTime ?: "14:00"}")
        sb.appendLine("Scheduled Dish: ${s.menuItemName ?: "No specific dish scheduled"}")
        if (s.dietaryTags.isNotEmpty()) {
            sb.appendLine("Dietary Tags: ${s.dietaryTags.joinToString(", ")}")
        }
        if (s.notes.isNotEmpty()) {
            sb.appendLine("Prep Notes:")
            s.notes.forEach { n -> sb.appendLine("  - $n") }
        }
    }

    sb.appendLine("=========================================")
    sb.appendLine("KITCHEN PRIVACY POLICY:")
    sb.appendLine("Anonymized kitchen headcount only.")
    sb.appendLine("No student identities or financial data.")
    sb.appendLine("=========================================")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "MessMate Kitchen Prep Sheet - ${headcount.date}")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Kitchen Prep Sheet"))
}
