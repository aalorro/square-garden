package com.squaregarden.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.savedGameDataStore: DataStore<Preferences> by preferencesDataStore(name = "saved_game")

class SavedGameRepository(private val context: Context) {

    companion object {
        private val SAVED_GAME_JSON = stringPreferencesKey("saved_game_json")
    }

    val hasSavedGameFlow: Flow<Boolean> = context.savedGameDataStore.data.map {
        it[SAVED_GAME_JSON] != null
    }

    suspend fun hasSavedGame(): Boolean = hasSavedGameFlow.first()

    suspend fun saveGame(json: String) {
        context.savedGameDataStore.edit { it[SAVED_GAME_JSON] = json }
    }

    suspend fun loadGame(): String? {
        return context.savedGameDataStore.data.first()[SAVED_GAME_JSON]
    }

    suspend fun clearSavedGame() {
        context.savedGameDataStore.edit { it.remove(SAVED_GAME_JSON) }
    }
}
