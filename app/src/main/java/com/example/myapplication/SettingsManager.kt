package com.example.myapplication

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {
    private val dataStore = context.dataStore

    val darkModeFlow: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: false }
    val notificationsFlow: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val soundEffectsFlow: Flow<Boolean> = dataStore.data.map { it[SOUND_EFFECTS] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun setSoundEffects(enabled: Boolean) {
        dataStore.edit { it[SOUND_EFFECTS] = enabled }
    }

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
    }
}
