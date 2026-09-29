package com.messmate.android.ui.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.repository.ApiResult
import com.messmate.android.data.repository.AuthRepository
import com.messmate.android.data.repository.ChefRepository
import com.messmate.android.data.repository.MessRepository
import com.messmate.android.domain.model.ChefDailyHeadcount
import com.messmate.android.domain.model.DiningConfig
import com.messmate.android.domain.model.UserRole
import com.messmate.android.util.DhakaDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChefUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val messId: String = "",
    val messName: String = "",
    val chefName: String = "",
    val userRole: UserRole = UserRole.CHEF,
    val selectedDate: String = DhakaDateUtils.todayDateString(),
    val headcountData: ChefDailyHeadcount? = null,
    val diningConfig: DiningConfig? = null,
    val lastUpdatedTime: String = DhakaDateUtils.formatTimeNow(),
    val isOffline: Boolean = false,
    val cachedAt: String? = null
)

@HiltViewModel
class ChefViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val chefRepository: ChefRepository,
    private val tokenDataStore: TokenDataStore,
    private val networkMonitor: com.messmate.android.util.NetworkMonitor,
    private val offlineCacheManager: com.messmate.android.data.local.OfflineCacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChefUiState())
    val uiState: StateFlow<ChefUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
        loadInitialData()
        startPolling()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val messId = authRepository.getSavedMessId()
            val userName = tokenDataStore.userName.first() ?: "Chef"
            val roleStr = tokenDataStore.userRole.first() ?: "CHEF"
            val role = UserRole.values().firstOrNull { it.name == roleStr } ?: UserRole.CHEF

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
                                chefName = me.data.user.fullName,
                                userRole = me.data.membership.role
                            )
                        }
                        fetchDiningConfigAndHeadcount(activeMessId, _uiState.value.selectedDate)
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = me.message) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        messId = messId,
                        chefName = userName,
                        userRole = role
                    )
                }
                fetchDiningConfigAndHeadcount(messId, _uiState.value.selectedDate)
            }
        }
    }

    private suspend fun fetchDiningConfigAndHeadcount(messId: String, date: String) {
        if (messId.isBlank()) return

        // 1. Fetch dining config (for cutoff times)
        var config: DiningConfig? = null
        when (val cfgRes = messRepository.getDiningConfig(messId)) {
            is ApiResult.Success -> config = cfgRes.data
            is ApiResult.Error -> {}
        }

        // 2. Fetch headcount
        when (val headcountRes = chefRepository.getDailyHeadcount(messId, date)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        headcountData = headcountRes.data,
                        messName = headcountRes.data.messName.ifBlank { it.messName },
                        diningConfig = config,
                        lastUpdatedTime = DhakaDateUtils.formatTimeNow(),
                        errorMessage = null
                    )
                }
            }
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        diningConfig = config,
                        errorMessage = headcountRes.message
                    )
                }
            }
        }
    }

    fun selectDate(date: String) {
        if (_uiState.value.selectedDate == date) return
        _uiState.update { it.copy(selectedDate = date, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            fetchDiningConfigAndHeadcount(_uiState.value.messId, date)
        }
    }

    fun nextDay() {
        val next = DhakaDateUtils.addDays(_uiState.value.selectedDate, 1)
        selectDate(next)
    }

    fun prevDay() {
        val prev = DhakaDateUtils.addDays(_uiState.value.selectedDate, -1)
        selectDate(prev)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            fetchDiningConfigAndHeadcount(_uiState.value.messId, _uiState.value.selectedDate)
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // Poll every 60 seconds
                val state = _uiState.value
                if (state.messId.isNotBlank() && !state.isLoading) {
                    when (val res = chefRepository.getDailyHeadcount(state.messId, state.selectedDate)) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    headcountData = res.data,
                                    lastUpdatedTime = DhakaDateUtils.formatTimeNow()
                                )
                            }
                        }
                        is ApiResult.Error -> {
                            // Don't show disruptive error on background polling failure, just preserve current
                        }
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
