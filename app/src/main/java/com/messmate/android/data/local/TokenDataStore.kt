package com.messmate.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "messmate_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_MESS_ID = stringPreferencesKey("mess_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_JWT_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val messId: Flow<String?> = context.dataStore.data.map { it[KEY_MESS_ID] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ROLE] }
    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val fcmToken: Flow<String?> = context.dataStore.data.map { it[KEY_FCM_TOKEN] }

    suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { it[KEY_FCM_TOKEN] = fcmToken }
    }

    suspend fun saveAuth(token: String, userId: String, messId: String?, role: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_JWT_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_MESS_ID] = messId ?: ""
            prefs[KEY_USER_ROLE] = role
            prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun updateMessId(messId: String) {
        context.dataStore.edit { it[KEY_MESS_ID] = messId }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
