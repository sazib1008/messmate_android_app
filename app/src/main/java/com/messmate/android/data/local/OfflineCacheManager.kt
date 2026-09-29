package com.messmate.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.messmate.android.util.DhakaDateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "messmate_offline_cache")

data class CachedEntry(
    val data: String,
    val cachedAtTime: String
)

@Singleton
class OfflineCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val KEY_STUDENT_DASHBOARD = "cache_student_dashboard"
        const val KEY_STUDENT_WALLET = "cache_student_wallet"
        const val KEY_WEEKLY_MENU = "cache_weekly_menu"
        const val KEY_MANAGER_OVERVIEW = "cache_manager_overview"
        const val KEY_CHEF_HEADCOUNT = "cache_chef_headcount"
    }

    suspend fun saveCache(key: String, jsonData: String) {
        val dataKey = stringPreferencesKey("${key}_data")
        val timeKey = stringPreferencesKey("${key}_time")
        val timestamp = DhakaDateUtils.formatTimeNow()

        context.cacheDataStore.edit { prefs ->
            prefs[dataKey] = jsonData
            prefs[timeKey] = timestamp
        }
    }

    suspend fun getCache(key: String): CachedEntry? {
        val dataKey = stringPreferencesKey("${key}_data")
        val timeKey = stringPreferencesKey("${key}_time")
        val prefs = context.cacheDataStore.data.first()

        val data = prefs[dataKey] ?: return null
        val time = prefs[timeKey] ?: "Recently"
        return CachedEntry(data = data, cachedAtTime = time)
    }

    suspend fun clearCache() {
        context.cacheDataStore.edit { it.clear() }
    }
}
