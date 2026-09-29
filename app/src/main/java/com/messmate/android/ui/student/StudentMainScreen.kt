package com.messmate.android.ui.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messmate.android.ui.common.LoadingScreen
import com.messmate.android.ui.theme.Terracotta

sealed class StudentTab(val index: Int, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : StudentTab(0, "Today", Icons.Rounded.Home)
    object Calendar : StudentTab(1, "Calendar", Icons.Rounded.CalendarMonth)
    object Wallet : StudentTab(2, "Wallet", Icons.Rounded.AccountBalanceWallet)
    object Menu : StudentTab(3, "Menu", Icons.Rounded.RestaurantMenu)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(
    onLogout: () -> Unit,
    onSwitchToManager: (() -> Unit)? = null,
    viewModel: StudentViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf<StudentTab>(StudentTab.Dashboard) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MessMate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Terracotta
                        )
                        if (state.messName.isNotBlank()) {
                            Text(
                                text = state.messName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (state.userRole.isManager && onSwitchToManager != null) {
                        FilledTonalButton(
                            onClick = onSwitchToManager,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manager View", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.loadInitialData() }
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = {
                            viewModel.logout { onLogout() }
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    StudentTab.Dashboard,
                    StudentTab.Calendar,
                    StudentTab.Wallet,
                    StudentTab.Menu
                )
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Terracotta,
                            selectedTextColor = Terracotta,
                            indicatorColor = Terracotta.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading && state.todaySessions.isEmpty() && state.calendarDays.isEmpty()) {
            LoadingScreen(
                message = "Loading student portal...",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                com.messmate.android.ui.common.OfflineStatusBar(isOnline = !state.isOffline)
                com.messmate.android.ui.common.CachedDataBanner(cachedAt = state.cachedAt)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        StudentTab.Dashboard -> DashboardScreen(
                            state = state,
                            onToggleMeal = { date, session, newStatus ->
                                viewModel.toggleMeal(date, session, newStatus)
                            }
                        )
                        StudentTab.Calendar -> CalendarScreen(
                            state = state,
                            onToggleMeal = { date, session, newStatus ->
                                viewModel.toggleMeal(date, session, newStatus)
                            }
                        )
                        StudentTab.Wallet -> WalletScreen(
                            state = state,
                            onSubmitDeposit = { amount, method, ref, notes, callback ->
                                viewModel.submitDeposit(amount, method, ref, notes, callback)
                            }
                        )
                        StudentTab.Menu -> WeeklyMenuScreen(
                            state = state,
                            onSelectDay = { dayNum ->
                                viewModel.selectMenuDay(dayNum)
                            }
                        )
                    }
                }
            }
        }
    }
}
