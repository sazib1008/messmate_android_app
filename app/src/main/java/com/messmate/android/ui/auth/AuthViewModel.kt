package com.messmate.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.android.data.repository.ApiResult
import com.messmate.android.data.repository.AuthRepository
import com.messmate.android.data.repository.MessRepository
import com.messmate.android.domain.model.AuthState
import com.messmate.android.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val authState: AuthState) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val pushNotificationManager: com.messmate.android.notification.PushNotificationManager,
    private val authSessionManager: com.messmate.android.data.local.AuthSessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _joinState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val joinState: StateFlow<AuthUiState> = _joinState.asStateFlow()

    init {
        viewModelScope.launch {
            authSessionManager.sessionExpiredEvent.collect { message ->
                _authState.value = AuthUiState.Error(message)
            }
        }
    }

    /** Called at app startup to check if user is already logged in */
    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val token = authRepository.getSavedToken()
            if (!token.isNullOrBlank()) {
                when (val result = authRepository.getMe()) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(result.data)
                        pushNotificationManager.syncDeviceToken()
                    }
                    is ApiResult.Error -> _authState.value = AuthUiState.Idle // token expired → go to login
                }
            } else {
                _authState.value = AuthUiState.Idle
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(result.data)
                    pushNotificationManager.syncDeviceToken()
                }
                is ApiResult.Error -> _authState.value = AuthUiState.Error(result.message.parseError())
            }
        }
    }

    fun register(
        email: String, password: String, fullName: String, phone: String?,
        studentId: String?, department: String?, university: String?, roomNumber: String?
    ) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = authRepository.register(
                email, password, fullName, phone, studentId, department, university, roomNumber
            )) {
                is ApiResult.Success -> _authState.value = AuthUiState.Success(result.data)
                is ApiResult.Error -> _authState.value = AuthUiState.Error(result.message.parseError())
            }
        }
    }

    fun submitJoinRequest(code: String, notes: String?, roomNumber: String?) {
        viewModelScope.launch {
            _joinState.value = AuthUiState.Loading
            when (val result = messRepository.submitJoinRequest(code, notes, roomNumber)) {
                is ApiResult.Success -> _joinState.value = AuthUiState.Success(
                    // reuse Success with current auth
                    (_authState.value as? AuthUiState.Success)?.authState
                        ?: AuthState("", com.messmate.android.domain.model.User("","","",null,null,null,null,null,null,false), null, null)
                )
                is ApiResult.Error -> _joinState.value = AuthUiState.Error(result.message.parseError())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            pushNotificationManager.unregisterDeviceToken()
            authRepository.logout()
            _authState.value = AuthUiState.Idle
        }
    }

    fun resetJoinState() { _joinState.value = AuthUiState.Idle }
    fun resetAuthState() { _authState.value = AuthUiState.Idle }

    val currentRole: UserRole?
        get() = (_authState.value as? AuthUiState.Success)?.authState?.membership?.role

    val currentMessId: String?
        get() = (_authState.value as? AuthUiState.Success)?.authState?.membership?.messId

    val currentUserId: String?
        get() = (_authState.value as? AuthUiState.Success)?.authState?.user?.id

    val currentUserName: String?
        get() = (_authState.value as? AuthUiState.Success)?.authState?.user?.fullName
}

private fun String.parseError(): String {
    return try {
        // Try to extract message from JSON error body like {"message": "..."}
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(this)
        match?.groupValues?.get(1) ?: this
    } catch (e: Exception) { this }
}
