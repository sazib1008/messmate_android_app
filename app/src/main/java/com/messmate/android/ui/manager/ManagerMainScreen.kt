package com.messmate.android.ui.manager

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.messmate.android.domain.model.DepositStatus
import com.messmate.android.ui.common.LoadingScreen
import com.messmate.android.ui.theme.Terracotta
import kotlinx.coroutines.launch

enum class ManagerTab(
    val title: String,
    val icon: ImageVector
) {
    OVERVIEW("Overview", Icons.Rounded.Dashboard),
    EXPENSES("Expenses", Icons.AutoMirrored.Rounded.ReceiptLong),
    DEPOSITS("Deposits", Icons.Rounded.Savings),
    MEMBERS("Members", Icons.Rounded.People),
    JOIN_REQUESTS("Join Requests", Icons.Rounded.PersonAdd),
    MENU("Menu Planner", Icons.Rounded.RestaurantMenu),
    SETTINGS("Cycle & Settings", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerMainScreen(
    onLogout: () -> Unit,
    onSwitchToStudentView: () -> Unit,
    onNavigateToChefTerminal: (() -> Unit)? = null,
    viewModel: ManagerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(ManagerTab.OVERVIEW) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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

    val pendingDepositsCount = remember(state.deposits) {
        state.deposits.count { it.status == DepositStatus.PENDING }
    }
    val pendingJoinRequestsCount = remember(state.joinRequests) {
        state.joinRequests.size
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "MessMate",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Terracotta
                    )
                    Text(
                        text = state.messName.ifBlank { "Manager Portal" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${state.userName} • ${state.userRole.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                ManagerTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val badgeCount = when (tab) {
                        ManagerTab.DEPOSITS -> pendingDepositsCount
                        ManagerTab.JOIN_REQUESTS -> pendingJoinRequestsCount
                        else -> 0
                    }

                    NavigationDrawerItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) },
                        selected = isSelected,
                        badge = if (badgeCount > 0) {
                            {
                                Badge(containerColor = Terracotta) {
                                    Text("$badgeCount", color = Color.White)
                                }
                            }
                        } else null,
                        onClick = {
                            currentTab = tab
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Kitchen Prep Station (Chef preview)
                if (onNavigateToChefTerminal != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = Terracotta) },
                        label = { Text("Kitchen Prep Station", color = Terracotta, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToChefTerminal()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                // Switch to Student View item
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = Terracotta) },
                    label = { Text("Switch to Student View", color = Terracotta, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSwitchToStudentView()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout(onLogout)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open navigation drawer")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = currentTab.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
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
                        // Student View button
                        FilledTonalButton(
                            onClick = onSwitchToStudentView,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Student View", style = MaterialTheme.typography.labelSmall)
                        }

                        if (onNavigateToChefTerminal != null) {
                            IconButton(onClick = onNavigateToChefTerminal) {
                                Icon(
                                    imageVector = Icons.Rounded.Restaurant,
                                    contentDescription = "Kitchen Prep Station",
                                    tint = Terracotta
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.loadInitialData() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh data")
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
                    val bottomTabs = listOf(
                        ManagerTab.OVERVIEW,
                        ManagerTab.EXPENSES,
                        ManagerTab.DEPOSITS,
                        ManagerTab.MEMBERS
                    )
                    bottomTabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        val badgeCount = when (tab) {
                            ManagerTab.DEPOSITS -> pendingDepositsCount
                            else -> 0
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                if (badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = Terracotta) {
                                                Text("$badgeCount", color = Color.White)
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Terracotta,
                                selectedTextColor = Terracotta,
                                indicatorColor = Terracotta.copy(alpha = 0.12f)
                            )
                        )
                    }

                    // 5th bottom tab: "More" button to open full drawer
                    val isMoreSelected = currentTab !in bottomTabs
                    NavigationBarItem(
                        selected = isMoreSelected,
                        onClick = { scope.launch { drawerState.open() } },
                        icon = {
                            if (pendingJoinRequestsCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Terracotta) {
                                            Text("$pendingJoinRequestsCount", color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
                                }
                            } else {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
                            }
                        },
                        label = { Text("More") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Terracotta,
                            selectedTextColor = Terracotta,
                            indicatorColor = Terracotta.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        ) { paddingValues ->
            if (state.isLoading && state.expenses.isEmpty() && state.members.isEmpty()) {
                LoadingScreen(message = "Loading manager portal...", modifier = Modifier.padding(paddingValues))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    com.messmate.android.ui.common.OfflineStatusBar(isOnline = !state.isOffline)
                    com.messmate.android.ui.common.CachedDataBanner(cachedAt = state.cachedAt)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (currentTab) {
                            ManagerTab.OVERVIEW -> {
                            ManagerOverviewScreen(
                                state = state,
                                onNavigateToExpenses = { currentTab = ManagerTab.EXPENSES },
                                onNavigateToDeposits = { currentTab = ManagerTab.DEPOSITS },
                                onNavigateToMembers = { currentTab = ManagerTab.MEMBERS },
                                onNavigateToMenu = { currentTab = ManagerTab.MENU },
                                onNavigateToSettings = { currentTab = ManagerTab.SETTINGS },
                                onPauseDay = { date, session, reason ->
                                    viewModel.pauseDay(date, session, reason) {}
                                },
                                onFinalizeCycle = { confirmForfeit ->
                                    viewModel.finalizeCycle(
                                        confirmForfeitSurplus = confirmForfeit,
                                        onSurplusWarning = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                        onSuccess = {}
                                    )
                                }
                            )
                        }
                        ManagerTab.EXPENSES -> {
                            ManagerExpensesScreen(
                                state = state,
                                onCategoryFilterChange = viewModel::setExpenseCategoryFilter,
                                onCreateExpense = { cat, title, amt, dt, rec, notes, target, parts, onDone ->
                                    viewModel.createExpense(cat, title, amt, dt, rec, notes, target, parts, onDone)
                                },
                                onUpdateExpense = { id, cat, title, amt, dt, rec, notes, target, parts, onDone ->
                                    viewModel.updateExpense(id, cat, title, amt, dt, rec, notes, target, parts, onDone)
                                },
                                onDeleteExpense = viewModel::deleteExpense
                            )
                        }
                        ManagerTab.DEPOSITS -> {
                            ManagerDepositsScreen(
                                state = state,
                                onTabSelected = viewModel::setSelectedDepositTab,
                                onApproveDeposit = viewModel::approveDeposit,
                                onRejectDeposit = viewModel::rejectDeposit
                            )
                        }
                        ManagerTab.MEMBERS -> {
                            ManagerMembersScreen(
                                state = state,
                                onSearchQueryChange = viewModel::setMemberSearchQuery,
                                onSortOptionChange = viewModel::setMemberSortOption,
                                onUpdateRoom = viewModel::updateMemberRoom
                            )
                        }
                        ManagerTab.JOIN_REQUESTS -> {
                            ManagerJoinRequestsScreen(
                                state = state,
                                onReviewRequest = viewModel::reviewJoinRequest
                            )
                        }
                        ManagerTab.MENU -> {
                            ManagerMenuPlannerScreen(
                                state = state,
                                onSelectDay = viewModel::setSelectedMenuDay,
                                onCreateDish = { day, session, name, desc, cat, tags, onDone ->
                                    viewModel.createMenuItem(day, session, name, desc, cat, tags, onDone)
                                },
                                onDeleteDish = viewModel::deleteMenuItem
                            )
                        }
                        ManagerTab.SETTINGS -> {
                            ManagerCycleSettingsScreen(
                                state = state,
                                onPauseDay = { date, session, reason ->
                                    viewModel.pauseDay(date, session, reason) {}
                                },
                                onFinalizeCycle = { confirmForfeit ->
                                    viewModel.finalizeCycle(
                                        confirmForfeitSurplus = confirmForfeit,
                                        onSurplusWarning = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                        onSuccess = {}
                                    )
                                },
                                onStartNewCycle = { targetDays, carryForward ->
                                    viewModel.startNewCycle(targetDays, carryForward) {}
                                },
                                onUpdateDiningConfig = { config ->
                                    viewModel.updateDiningConfig(config) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

