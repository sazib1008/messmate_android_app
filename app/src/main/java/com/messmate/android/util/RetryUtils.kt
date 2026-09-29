package com.messmate.android.util

import com.messmate.android.data.repository.ApiResult
import kotlinx.coroutines.delay

/**
 * Sensible retry with exponential backoff for transient failures on idempotent GET queries.
 * IMPORTANT: NEVER use on financial or mutating write actions (e.g. Approve/Reject/Deposit/Toggle).
 */
suspend fun <T> retryGetWithBackoff(
    maxRetries: Int = 2,
    initialDelayMs: Long = 800,
    factor: Double = 2.0,
    block: suspend () -> ApiResult<T>
): ApiResult<T> {
    var currentDelay = initialDelayMs
    var lastResult: ApiResult<T> = block()

    for (attempt in 1..maxRetries) {
        if (lastResult is ApiResult.Success) {
            return lastResult
        }
        val code = (lastResult as? ApiResult.Error)?.code
        // Do not retry 4xx client errors (401, 403, 404, 409) — only network or 5xx server issues
        if (code != null && code in 400..499) {
            return lastResult
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
        lastResult = block()
    }
    return lastResult
}
