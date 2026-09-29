package com.messmate.android.ui.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.remote.dto.ActiveCycleDtoRemote
import com.messmate.android.data.remote.dto.CycleCalculationResultDto
import com.messmate.android.data.remote.dto.StartNewCycleRequestDto
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
import javax.inject.Inject

enum class MemberSortOption { NAME, ROOM, BALANCE }

data class ManagerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val messId: String = "",
    val messName: String = "",
    val userName: String = "",
    val userRole: UserRole = UserRole.MANAGER,

    // Overview
    val activeCycle: ActiveCycleDtoRemote? = null,
    val calculationPreview: CycleCalculationResultDto? = null,
    val isRateVolatile: Boolean = false,
    val volatilityReason: String? = null,
    val mealRate: Double = 0.0,
    val totalExpensesAmount: Double = 0.0,
    val treasuryBalance: Double = 0.0,
    val recentExpenses: List<Expense> = emptyList(),

    // Expenses
    val expenses: List<Expense> = emptyList(),
    val selectedExpenseCategory: ExpenseCategory? = null,
    val isCreatingExpense: Boolean = false,
    val deletingExpenseIds: Set<String> = emptySet(),

    // Deposits
    val selectedDepositTab: Int = 0, // 0 = Requests, 1 = History
    val deposits: List<Deposit> = emptyList(),
    val depositActionLoadingIds: Set<String> = emptySet(),

    // Members
    val members: List<MemberInfo> = emptyList(),
    val memberSearchQuery: String = "",
    val memberSortOption: MemberSortOption = MemberSortOption.NAME,
    val updatingRoomMembershipIds: Set<String> = emptySet(),

    // Join Requests
    val joinRequests: List<ManagerJoinRequest> = emptyList(),
    val reviewingJoinRequestIds: Set<String> = emptySet(),

    // Menu Planner
    val weeklyMenu: WeeklyMenu? = null,
    val diningConfig: DiningConfig? = null,
    val selectedMenuDay: Int = 7, // 7 = Sun (or 1 = Mon). We'll handle 1..7
    val isAddingDish: Boolean = false,
    val deletingDishIds: Set<String> = emptySet(),

    // Cycle & Settings
    val isPausingDay: Boolean = false,
    val isFinalizingCycle: Boolean = false,
    val isStartingNewCycle: Boolean = false,
    val isUpdatingConfig: Boolean = false,
    val cycleHistory: List<PastCycleSummary> = emptyList(),
    // Offline / Cache resilience
    val isOffline: Boolean = false,
    val cachedAt: String? = null
)

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val depositRepository: DepositRepository,
    private val calculationRepository: CalculationRepository,
    private val expenseRepository: ExpenseRepository,
    private val menuRepository: MenuRepository,
    private val tokenDataStore: TokenDataStore,
    private val networkMonitor: com.messmate.android.util.NetworkMonitor,
    private val offlineCacheManager: com.messmate.android.data.local.OfflineCacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

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
            val userName = tokenDataStore.userName.first() ?: "Manager"
            val roleStr = tokenDataStore.userRole.first() ?: "MANAGER"
            val role = UserRole.values().firstOrNull { it.name == roleStr } ?: UserRole.MANAGER

            if (messId.isNullOrBlank()) {
                when (val me = authRepository.getMe()) {
                    is ApiResult.Success -> {
                        val activeMessId = me.data.membership?.messId
                        if (activeMessId.isNullOrBlank()) {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "No active mess found") }
                            return@launch
                        }
                        _uiState.update {
                            it.copy(
                                messId = activeMessId,
                                messName = me.data.membership.messName,
                                userName = me.data.user.fullName,
                                userRole = me.data.membership.role
                            )
                        }
                        refreshAllData(activeMessId)
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = me.message) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        messId = messId,
                        userName = userName,
                        userRole = role
                    )
                }
                refreshAllData(messId)
            }
        }
    }

    suspend fun refreshAllData(messId: String = _uiState.value.messId) {
        if (messId.isBlank()) return

        // 1. Dining Config
        var config: DiningConfig? = null
        when (val cfgRes = messRepository.getDiningConfig(messId)) {
            is ApiResult.Success -> config = cfgRes.data
            is ApiResult.Error -> {}
        }

        // 2. Active Cycle
        var cycle: ActiveCycleDtoRemote? = null
        when (val cRes = calculationRepository.getActiveCycle(messId)) {
            is ApiResult.Success -> cycle = cRes.data
            is ApiResult.Error -> {}
        }

        // 3. Calculation preview
        var calcPreview: CycleCalculationResultDto? = null
        when (val calcRes = calculationRepository.previewCalculation(messId)) {
            is ApiResult.Success -> calcPreview = calcRes.data
            is ApiResult.Error -> {}
        }

        // 4. Expenses
        var expenses = emptyList<Expense>()
        when (val expRes = expenseRepository.listExpenses(messId)) {
            is ApiResult.Success -> expenses = expRes.data.sortedByDescending { it.expenseDate }
            is ApiResult.Error -> {}
        }

        // 5. Deposits
        var deposits = emptyList<Deposit>()
        when (val depRes = depositRepository.listDeposits(messId = messId, userId = null)) {
            is ApiResult.Success -> deposits = depRes.data.sortedByDescending { it.createdAt }
            is ApiResult.Error -> {}
        }

        // 6. Members
        var members = emptyList<MemberInfo>()
        when (val memRes = messRepository.getMembers(messId)) {
            is ApiResult.Success -> {
                // Attach net balances from calcPreview if available
                val balanceMap = calcPreview?.memberSummaries?.associate { it.userId to it.netBalance } ?: emptyMap()
                members = memRes.data.map { m ->
                    m.copy(netBalance = balanceMap[m.userId] ?: 0.0)
                }
            }
            is ApiResult.Error -> {}
        }

        // 7. Join requests
        var joinRequests = emptyList<ManagerJoinRequest>()
        when (val jrRes = messRepository.listJoinRequests(messId)) {
            is ApiResult.Success -> joinRequests = jrRes.data.filter { it.status == JoinRequestStatus.PENDING }
            is ApiResult.Error -> {}
        }

        // 8. Weekly menu
        var menu: WeeklyMenu? = null
        when (val menuRes = menuRepository.getWeeklyMenu(messId)) {
            is ApiResult.Success -> menu = menuRes.data
            is ApiResult.Error -> {}
        }

        // 9. Cycle History
        var history = emptyList<PastCycleSummary>()
        when (val histRes = calculationRepository.getCycleHistory(messId)) {
            is ApiResult.Success -> history = histRes.data.sortedByDescending { it.cycleNumber }
            is ApiResult.Error -> {}
        }

        val approvedDepositsTotal = deposits.filter { it.status == DepositStatus.APPROVED }.sumOf { it.amount }
        val totalExpenses = calcPreview?.totalExpenses ?: expenses.sumOf { it.amount }
        val treasury = approvedDepositsTotal - totalExpenses
        val pendingCount = deposits.count { it.status == DepositStatus.PENDING }

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                diningConfig = config,
                activeCycle = cycle,
                calculationPreview = calcPreview,
                mealRate = calcPreview?.mealRate ?: 0.0,
                isRateVolatile = calcPreview?.isRateVolatile ?: false,
                volatilityReason = calcPreview?.volatilityReason,
                totalExpensesAmount = totalExpenses,
                treasuryBalance = treasury,
                recentExpenses = expenses.take(5),
                expenses = expenses,
                deposits = deposits,
                // Default to Requests tab if pending deposits exist
                selectedDepositTab = if (pendingCount > 0) 0 else state.selectedDepositTab,
                members = members,
                joinRequests = joinRequests,
                weeklyMenu = menu,
                cycleHistory = history
            )
        }
    }

    // ======================== EXPENSES ACTIONS ========================

    fun setExpenseCategoryFilter(category: ExpenseCategory?) {
        _uiState.update { it.copy(selectedExpenseCategory = category) }
    }

    fun createExpense(
        category: ExpenseCategory,
        title: String,
        amount: Double,
        expenseDate: String,
        receiptUrl: String?,
        notes: String?,
        targetMemberId: String?,
        participantIds: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingExpense = true, errorMessage = null) }
            val messId = _uiState.value.messId
            when (val res = expenseRepository.createExpense(
                category = category,
                title = title,
                amount = amount,
                expenseDate = expenseDate,
                receiptUrl = receiptUrl,
                notes = notes,
                targetMemberId = targetMemberId,
                participantIds = participantIds
            )) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isCreatingExpense = false,
                            expenses = listOf(res.data) + state.expenses,
                            successMessage = "Expense added successfully!"
                        )
                    }
                    onSuccess()
                    refreshAllData(messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isCreatingExpense = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun updateExpense(
        id: String,
        category: ExpenseCategory,
        title: String,
        amount: Double,
        expenseDate: String,
        receiptUrl: String?,
        notes: String?,
        targetMemberId: String?,
        participantIds: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            when (val res = expenseRepository.updateExpense(
                id = id,
                category = category,
                title = title,
                amount = amount,
                expenseDate = expenseDate,
                receiptUrl = receiptUrl,
                notes = notes,
                targetMemberId = targetMemberId,
                participantIds = participantIds
            )) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.expenses.map { if (it.id == id) res.data else it }
                        state.copy(expenses = updated, successMessage = "Expense updated successfully!")
                    }
                    onSuccess()
                    refreshAllData(_uiState.value.messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(deletingExpenseIds = state.deletingExpenseIds + id, errorMessage = null)
            }
            when (val res = expenseRepository.deleteExpense(id)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingExpenseIds = state.deletingExpenseIds - id,
                            expenses = state.expenses.filterNot { it.id == id },
                            successMessage = "Expense deleted successfully!"
                        )
                    }
                    refreshAllData(_uiState.value.messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingExpenseIds = state.deletingExpenseIds - id,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    // ======================== DEPOSITS ACTIONS ========================

    fun setSelectedDepositTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedDepositTab = tabIndex) }
    }

    fun approveDeposit(depositId: String) {
        viewModelScope.launch {
            // Scope loading state strictly to this depositId
            _uiState.update { state ->
                state.copy(depositActionLoadingIds = state.depositActionLoadingIds + depositId, errorMessage = null)
            }
            when (val res = depositRepository.approveDeposit(depositId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.deposits.map { if (it.id == depositId) res.data else it }
                        state.copy(
                            depositActionLoadingIds = state.depositActionLoadingIds - depositId,
                            deposits = updatedList,
                            successMessage = "Deposit approved for ${res.data.userFullName}"
                        )
                    }
                    refreshAllData(_uiState.value.messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            depositActionLoadingIds = state.depositActionLoadingIds - depositId,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun rejectDeposit(depositId: String, reason: String? = null) {
        viewModelScope.launch {
            // Scope loading state strictly to this depositId
            _uiState.update { state ->
                state.copy(depositActionLoadingIds = state.depositActionLoadingIds + depositId, errorMessage = null)
            }
            when (val res = depositRepository.rejectDeposit(depositId, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.deposits.map { if (it.id == depositId) res.data else it }
                        state.copy(
                            depositActionLoadingIds = state.depositActionLoadingIds - depositId,
                            deposits = updatedList,
                            successMessage = "Deposit rejected"
                        )
                    }
                    refreshAllData(_uiState.value.messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            depositActionLoadingIds = state.depositActionLoadingIds - depositId,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    // ======================== MEMBERS ACTIONS ========================

    fun setMemberSearchQuery(query: String) {
        _uiState.update { it.copy(memberSearchQuery = query) }
    }

    fun setMemberSortOption(option: MemberSortOption) {
        _uiState.update { it.copy(memberSortOption = option) }
    }

    fun updateMemberRoom(membershipId: String, roomNumber: String?) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(updatingRoomMembershipIds = state.updatingRoomMembershipIds + membershipId, errorMessage = null)
            }
            val messId = _uiState.value.messId
            when (val res = messRepository.updateMemberRoom(messId, membershipId, roomNumber)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.members.map {
                            if (it.id == membershipId) it.copy(roomNumber = roomNumber) else it
                        }
                        state.copy(
                            updatingRoomMembershipIds = state.updatingRoomMembershipIds - membershipId,
                            members = updated,
                            successMessage = "Room updated successfully!"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            updatingRoomMembershipIds = state.updatingRoomMembershipIds - membershipId,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    // ======================== JOIN REQUESTS ACTIONS ========================

    fun reviewJoinRequest(requestId: String, approved: Boolean, notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(reviewingJoinRequestIds = state.reviewingJoinRequestIds + requestId, errorMessage = null)
            }
            val messId = _uiState.value.messId
            when (val res = messRepository.reviewJoinRequest(messId, requestId, approved, notes)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            reviewingJoinRequestIds = state.reviewingJoinRequestIds - requestId,
                            joinRequests = state.joinRequests.filterNot { it.id == requestId },
                            successMessage = if (approved) "Join request approved!" else "Join request rejected."
                        )
                    }
                    refreshAllData(messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            reviewingJoinRequestIds = state.reviewingJoinRequestIds - requestId,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    // ======================== MENU PLANNER ACTIONS ========================

    fun setSelectedMenuDay(dayOfWeek: Int) {
        _uiState.update { it.copy(selectedMenuDay = dayOfWeek) }
    }

    fun createMenuItem(
        dayOfWeek: Int,
        session: MealSession,
        itemName: String,
        description: String?,
        category: String?,
        dietaryTags: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingDish = true, errorMessage = null) }
            val messId = _uiState.value.messId
            when (val res = menuRepository.createMenuItem(
                messId = messId,
                dayOfWeek = dayOfWeek,
                session = session,
                itemName = itemName,
                description = description,
                category = category,
                dietaryTags = dietaryTags
            )) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val oldItems = state.weeklyMenu?.items ?: emptyList()
                        val updatedMenu = state.weeklyMenu?.copy(items = oldItems + res.data)
                        state.copy(
                            isAddingDish = false,
                            weeklyMenu = updatedMenu,
                            successMessage = "Dish added to menu!"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isAddingDish = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun deleteMenuItem(itemId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(deletingDishIds = state.deletingDishIds + itemId, errorMessage = null)
            }
            when (val res = menuRepository.deleteMenuItem(itemId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val oldItems = state.weeklyMenu?.items ?: emptyList()
                        val updatedMenu = state.weeklyMenu?.copy(items = oldItems.filterNot { it.id == itemId })
                        state.copy(
                            deletingDishIds = state.deletingDishIds - itemId,
                            weeklyMenu = updatedMenu,
                            successMessage = "Dish removed from menu."
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(deletingDishIds = state.deletingDishIds - itemId, errorMessage = res.message)
                    }
                }
            }
        }
    }

    // ======================== CYCLE & SETTINGS ACTIONS ========================

    fun pauseDay(pausedDate: String, session: MealSession?, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPausingDay = true, errorMessage = null) }
            val messId = _uiState.value.messId
            when (val res = calculationRepository.pauseDay(messId, pausedDate, session, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isPausingDay = false,
                            activeCycle = res.data,
                            successMessage = "Holiday / paused day recorded for $pausedDate"
                        )
                    }
                    onSuccess()
                    refreshAllData(messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isPausingDay = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun finalizeCycle(
        confirmForfeitSurplus: Boolean = false,
        onSurplusWarning: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFinalizingCycle = true, errorMessage = null) }
            val messId = _uiState.value.messId
            when (val res = calculationRepository.finalizeCalculation(messId, confirmForfeitSurplus)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isFinalizingCycle = false,
                            calculationPreview = res.data,
                            successMessage = "Cycle #${state.activeCycle?.cycleNumber} concluded successfully!"
                        )
                    }
                    onSuccess()
                    refreshAllData(messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isFinalizingCycle = false) }
                    // Check if error is due to surplus members needing confirmation
                    if (res.message.contains("positive balance") || res.message.contains("surplus")) {
                        onSurplusWarning(res.message)
                    } else {
                        _uiState.update { it.copy(errorMessage = res.message) }
                    }
                }
            }
        }
    }

    fun startNewCycle(
        targetDays: Int,
        carryForward: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingNewCycle = true, errorMessage = null) }
            val messId = _uiState.value.messId
            val req = StartNewCycleRequestDto(
                targetActiveDays = targetDays,
                defaultCarryForward = carryForward
            )
            when (val res = calculationRepository.startNewCycle(messId, req)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isStartingNewCycle = false,
                            activeCycle = res.data,
                            successMessage = "New Cycle #${res.data.cycleNumber} started!"
                        )
                    }
                    onSuccess()
                    refreshAllData(messId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isStartingNewCycle = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun updateDiningConfig(config: DiningConfig, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingConfig = true, errorMessage = null) }
            val messId = _uiState.value.messId
            when (val res = messRepository.updateDiningConfig(messId, config)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingConfig = false,
                            diningConfig = res.data,
                            successMessage = "Dining settings updated successfully!"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingConfig = false, errorMessage = res.message) }
                }
            }
        }
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
