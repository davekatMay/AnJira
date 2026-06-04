package com.anjira.taskplanner.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskplanner_prefs")

private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USERNAME_KEY = stringPreferencesKey("username")
private val EMAIL_KEY = stringPreferencesKey("email")
private val THEME_KEY = stringPreferencesKey("theme_mode")
private val LAST_SYNC_KEY = stringPreferencesKey("last_sync")

class DataStoreManager(private val context: Context) {

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    suspend fun saveUserInfo(userId: Int, username: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId.toString()
            preferences[USERNAME_KEY] = username
            preferences[EMAIL_KEY] = email
        }
    }

    suspend fun getUserId(): Int? {
        return context.dataStore.data.first()[USER_ID_KEY]?.toIntOrNull()
    }

    suspend fun getUsername(): String? {
        return context.dataStore.data.first()[USERNAME_KEY]
    }

    suspend fun getEmail(): String? {
        return context.dataStore.data.first()[EMAIL_KEY]
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            val currentTheme = preferences[THEME_KEY]
            val currentLastSync = preferences[LAST_SYNC_KEY]
            preferences.clear()
            currentTheme?.let { preferences[THEME_KEY] = it }
            currentLastSync?.let { preferences[LAST_SYNC_KEY] = it }
        }
    }

    fun observeAccessToken(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    fun observeUserId(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]?.toIntOrNull() ?: -1
    }

    fun observeThemeMode(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[THEME_KEY] = mode }
    }

    suspend fun getLastSyncTime(): String? {
        return context.dataStore.data.first()[LAST_SYNC_KEY]
    }

    suspend fun setLastSyncTime(time: String) {
        context.dataStore.edit { preferences -> preferences[LAST_SYNC_KEY] = time }
    }

    fun observeLastSyncTime(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_KEY]
    }
}
