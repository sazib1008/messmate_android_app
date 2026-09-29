package com.messmate.android.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor(
    private val tokenDataStore: TokenDataStore
) {
    private val _sessionExpiredEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    fun notifySessionExpired(message: String = "Your session has expired. Please log in again.") {
        CoroutineScope(Dispatchers.IO).launch {
            tokenDataStore.clear()
            _sessionExpiredEvent.tryEmit(message)
        }
    }
}
