package com.messmate.android.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.remote.dto.ActiveCycleDtoRemote
import com.messmate.android.data.remote.dto.CycleCalculationResultDto
import com.messmate.android.data.remote.dto.MemberSettlementSummaryDto
import com.messmate.android.data.repository.*
import com.messmate.android.domain.model.*
import com.messmate.android.util.DhakaDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SessionToggleState(
    val session: MealSession,
    val isEnabled: Boolean,
    val cutoffTime: String,
    val status: MealStatus,
    val isCutoffPassed: Boolean,
    val isToggling: Boolean = false
)

data class DaySessionState(
    val session: MealSession,
    val status: MealStatus,
    val isCutoffPassed: Boolean,
    val isEditable: Boolean,
    val isToggling: Boolean = false
)

data class CalendarDayState(
    val date: String, // "YYYY-MM-DD"
    val dayOfWeek: String, // "Mon", "Tue"
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isPast: Boolean,
    val sessions: List<DaySessionState>
)

data class StudentUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val messId: String = "",
    val messName: String = "",
    val userName: String = "",
    val userRole: UserRole = UserRole.MEMBER,
    val diningConfig: DiningConfig? = null,
    val todayDate: String = "",
    val todaySessions: List<SessionToggleState> = emptyList(),
    val todayDishes: List<MenuItem> = emptyList(),
    // Calendar
    val calendarDays: List<CalendarDayState> = emptyList(),
    val isLoadingCalendar: Boolean = false,
    // Wallet
    val activeCycle: ActiveCycleDtoRemote? = null,
    val calculationPreview: CycleCalculationResultDto? = null,
    val mySettlement: MemberSettlementSummaryDto? = null,
    val deposits: List<Deposit> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val isSubmittingDeposit: Boolean = false,
    val depositSuccess: Boolean = false,
    // Menu
    val weeklyMenu: WeeklyMenu? = null,
    val selectedDayOfWeek: Int = 1,
    val isLoadingMenu: Boolean = false,
    // Offline / Cache resilience
    val isOffline: Boolean = false,
    val cachedAt: String? = null
)

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val mealRepository: MealRepository,
    private val depositRepository: DepositRepository,
    private val calculationRepository: CalculationRepository,
    private val expenseRepository: ExpenseRepository,
    private val menuRepository: MenuRepository,
    private val tokenDataStore: TokenDataStore,
    private val networkMonitor: com.messmate.android.util.NetworkMonitor,
    private val offlineCacheManager: com.messmate.android.data.local.OfflineCacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val messId = authRepository.getSavedMessId()
            val userId = authRepository.getSavedUserId()
            val userName = tokenDataStore.userName.first() ?: "Student"
            val todayStr = DhakaDateUtils.todayDateString()

            if (messId.isNullOrBlank()) {
                // Try fetching /api/auth/me to refresh state
                when (val meResult = authRepository.getMe()) {
                    is ApiResult.Success -> {
                        val newMessId = meResult.data.membership?.messId
                        if (newMessId.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "No active mess membership found."
                                )
                            }
                            return@launch
                        }
                        loadDataForMess(newMessId, meResult.data.user.id, meResult.data.user.fullName, todayStr)
                    }
                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = meResult.message
                            )
                        }
                    }
                }
            } else {
                loadDataForMess(messId, userId ?: "", userName, todayStr)
            }
        }
    }

    private suspend fun loadDataForMess(
        messId: String,
        userId: String,
        userName: String,
        todayStr: String
    ) {
        // Fetch Dining Config first to know active sessions
        var config: DiningConfig? = null
        when (val cfgRes = messRepository.getDiningConfig(messId)) {
            is ApiResult.Success -> config = cfgRes.data
            is ApiResult.Error -> {
                // If config fails, continue with default sessions
            }
        }

        val activeSessions = config?.getActiveSessionList() ?: listOf(
            MealSession.BREAKFAST,
            MealSession.LUNCH,
            MealSession.DINNER
        )

        _uiState.update {
            it.copy(
                messId = messId,
                userName = userName,
                todayDate = todayStr,
                diningConfig = config
            )
        }

        // Fetch Dashboard, Calendar, Wallet, Menu concurrently
        loadDashboard(messId, todayStr, activeSessions, config)
        loadCalendar(messId, todayStr, activeSessions, config)
        loadWallet(messId, userId)
        loadMenu(messId)

        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun loadDashboard(
        messId: String,
        todayStr: String,
        activeSessions: List<MealSession>,
        config: DiningConfig?
    ) {
        // Fetch today's meal status
        var todayStatuses = emptyList<DailyMealStatus>()
        when (val statusRes = mealRepository.getMyMealStatuses(todayStr, todayStr)) {
            is ApiResult.Success -> todayStatuses = statusRes.data
            is ApiResult.Error -> {}
        }

        // Build today's session cards
        val sessionStates = activeSessions.map { session ->
            val sessionCfg = config?.sessions?.firstOrNull { it.session == session }
            val cutoff = sessionCfg?.cutoffTime ?: when (session) {
                MealSession.BREAKFAST -> "07:00"
                MealSession.LUNCH -> "12:00"
                MealSession.DINNER -> "19:00"
            }
            val isCutoff = DhakaDateUtils.isMealCutoffPassed(cutoff, todayStr)
            val currentStatus = todayStatuses.firstOrNull { it.session == session }?.status ?: MealStatus.ON
            SessionToggleState(
                session = session,
                isEnabled = sessionCfg?.isEnabled ?: true,
                cutoffTime = cutoff,
                status = currentStatus,
                isCutoffPassed = isCutoff,
                isToggling = false
            )
        }

        // Fetch today's dishes
        var todayDishes = emptyList<MenuItem>()
        when (val dishRes = menuRepository.getTodayMenu(messId)) {
            is ApiResult.Success -> todayDishes = dishRes.data
            is ApiResult.Error -> {}
        }

        _uiState.update {
            it.copy(
                todaySessions = sessionStates,
                todayDishes = todayDishes
            )
        }
    }

    private suspend fun loadCalendar(
        messId: String,
        todayStr: String,
        activeSessions: List<MealSession>,
        config: DiningConfig?
    ) {
        _uiState.update { it.copy(isLoadingCalendar = true) }
        val startDate = DhakaDateUtils.dateWithOffset(-3)
        val endDate = DhakaDateUtils.dateWithOffset(10)

        var statuses = emptyList<DailyMealStatus>()
        when (val res = mealRepository.getMyMealStatuses(startDate, endDate)) {
            is ApiResult.Success -> statuses = res.data
            is ApiResult.Error -> {}
        }

        val dates = DhakaDateUtils.generateDateRange(startDate, endDate)
        val calendarDays = dates.map { dateStr ->
            val isToday = DhakaDateUtils.isToday(dateStr)
            val isPast = DhakaDateUtils.isPastDate(dateStr)
            val localDate = LocalDate.parse(dateStr)
            val dayOfWeekStr = localDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val dayOfMonth = localDate.dayOfMonth

            val daySessions = activeSessions.map { session ->
                val sessionCfg = config?.sessions?.firstOrNull { it.session == session }
                val cutoff = sessionCfg?.cutoffTime ?: "12:00"
                val cutoffPassed = if (isToday) DhakaDateUtils.isMealCutoffPassed(cutoff, dateStr) else isPast
                val status = statuses.firstOrNull { it.date == dateStr && it.session == session }?.status ?: MealStatus.ON
                val isEditable = !isPast && !cutoffPassed

                DaySessionState(
                    session = session,
                    status = status,
                    isCutoffPassed = cutoffPassed,
                    isEditable = isEditable
                )
            }

            CalendarDayState(
                date = dateStr,
                dayOfWeek = dayOfWeekStr,
                dayOfMonth = dayOfMonth,
                isToday = isToday,
                isPast = isPast,
                sessions = daySessions
            )
        }

        _uiState.update {
            it.copy(
                calendarDays = calendarDays,
                isLoadingCalendar = false
            )
        }
    }

    private suspend fun loadWallet(messId: String, userId: String) {
        // Active cycle info
        var cycle: ActiveCycleDtoRemote? = null
        when (val cRes = calculationRepository.getActiveCycle(messId)) {
            is ApiResult.Success -> cycle = cRes.data
            is ApiResult.Error -> {}
        }

        // Calculation preview (with member breakdown)
        var preview: CycleCalculationResultDto? = null
        var mySummary: MemberSettlementSummaryDto? = null
        when (val calcRes = calculationRepository.previewCalculation(messId)) {
            is ApiResult.Success -> {
                preview = calcRes.data
                mySummary = calcRes.data.memberSummaries.firstOrNull { it.userId == userId }
            }
            is ApiResult.Error -> {}
        }

        // Deposits
        var deposits = emptyList<Deposit>()
        when (val depRes = depositRepository.listDeposits(messId, userId.takeIf { it.isNotBlank() })) {
            is ApiResult.Success -> deposits = depRes.data.sortedByDescending { it.createdAt }
            is ApiResult.Error -> {}
        }

        // Expenses (read-only)
        var expenses = emptyList<Expense>()
        when (val expRes = expenseRepository.listExpenses(messId)) {
            is ApiResult.Success -> expenses = expRes.data.sortedByDescending { it.expenseDate }
            is ApiResult.Error -> {}
        }

        _uiState.update {
            it.copy(
                activeCycle = cycle,
                calculationPreview = preview,
                mySettlement = mySummary,
                deposits = deposits,
                expenses = expenses
            )
        }
    }

    private suspend fun loadMenu(messId: String) {
        _uiState.update { it.copy(isLoadingMenu = true) }
        when (val menuRes = menuRepository.getWeeklyMenu(messId)) {
            is ApiResult.Success -> {
                val currentDayOfWeek = DhakaDateUtils.todayDayOfWeekNumber()
                _uiState.update {
                    it.copy(
                        weeklyMenu = menuRes.data,
                        selectedDayOfWeek = currentDayOfWeek,
                        isLoadingMenu = false
                    )
                }
            }
            is ApiResult.Error -> {
                _uiState.update { it.copy(isLoadingMenu = false) }
            }
        }
    }

    // ======================== ACTIONS ========================

    fun toggleMeal(date: String, session: MealSession, newStatus: MealStatus) {
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(errorMessage = "You're offline — meal changes require a connection") }
            return
        }

        viewModelScope.launch {
            // Indicate toggling in progress WITHOUT optimistic update of status
            _uiState.update { state ->
                val updatedToday = state.todaySessions.map {
                    if (it.session == session && date == state.todayDate) it.copy(isToggling = true) else it
                }
                val updatedCalendar = state.calendarDays.map { day ->
                    if (day.date == date) {
                        val newSessions = day.sessions.map { s ->
                            if (s.session == session) s.copy(isToggling = true) else s
                        }
                        day.copy(sessions = newSessions)
                    } else day
                }
                state.copy(todaySessions = updatedToday, calendarDays = updatedCalendar)
            }

            // Call API
            when (val res = mealRepository.toggleMeal(date, session, newStatus)) {
                is ApiResult.Success -> {
                    // Update state with confirmed status from server
                    _uiState.update { state ->
                        val updatedToday = state.todaySessions.map {
                            if (it.session == session && date == state.todayDate) it.copy(status = res.data.status, isToggling = false) else it
                        }
                        val updatedCalendar = state.calendarDays.map { day ->
                            if (day.date == date) {
                                val newSessions = day.sessions.map { s ->
                                    if (s.session == session) s.copy(status = res.data.status, isToggling = false) else s
                                }
                                day.copy(sessions = newSessions)
                            } else day
                        }
                        state.copy(todaySessions = updatedToday, calendarDays = updatedCalendar)
                    }
                }
                is ApiResult.Error -> {
                    // Reset toggling spinner, keep original status, show clear server error
                    _uiState.update { state ->
                        val updatedToday = state.todaySessions.map {
                            if (it.session == session && date == state.todayDate) it.copy(isToggling = false) else it
                        }
                        val updatedCalendar = state.calendarDays.map { day ->
                            if (day.date == date) {
                                val newSessions = day.sessions.map { s ->
                                    if (s.session == session) s.copy(isToggling = false) else s
                                }
                                day.copy(sessions = newSessions)
                            } else day
                        }
                        state.copy(
                            todaySessions = updatedToday,
                            calendarDays = updatedCalendar,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun submitDeposit(
        amount: Double,
        paymentMethod: PaymentMethod,
        transactionRef: String?,
        notes: String?,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingDeposit = true, errorMessage = null) }
            when (val res = depositRepository.submitDeposit(amount, paymentMethod, transactionRef, notes)) {
                is ApiResult.Success -> {
                    // Prepend new deposit to the list
                    _uiState.update { state ->
                        state.copy(
                            deposits = listOf(res.data) + state.deposits,
                            isSubmittingDeposit = false,
                            successMessage = "Deposit request submitted successfully!"
                        )
                    }
                    onComplete(true)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingDeposit = false,
                            errorMessage = res.message
                        )
                    }
                    onComplete(false)
                }
            }
        }
    }

    fun selectMenuDay(day: Int) {
        _uiState.update { it.copy(selectedDayOfWeek = day) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
