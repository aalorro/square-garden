package com.squaregarden.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SHAPES_EXPLAINER_DISMISSED = booleanPreferencesKey("shapes_explainer_dismissed")
        private val DISMISSED_UPDATE_VERSION = intPreferencesKey("dismissed_update_version")
    }

    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[SOUND_ENABLED] ?: true }
    val musicEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[MUSIC_ENABLED] ?: true }
    val shapesExplainerDismissed: Flow<Boolean> = context.settingsDataStore.data.map { it[SHAPES_EXPLAINER_DISMISSED] ?: false }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[MUSIC_ENABLED] = enabled }
    }

    suspend fun setShapesExplainerDismissed(dismissed: Boolean) {
        context.settingsDataStore.edit { it[SHAPES_EXPLAINER_DISMISSED] = dismissed }
    }

    val dismissedUpdateVersion: Flow<Int> = context.settingsDataStore.data.map { it[DISMISSED_UPDATE_VERSION] ?: 0 }

    suspend fun setDismissedUpdateVersion(versionCode: Int) {
        context.settingsDataStore.edit { it[DISMISSED_UPDATE_VERSION] = versionCode }
    }
}
