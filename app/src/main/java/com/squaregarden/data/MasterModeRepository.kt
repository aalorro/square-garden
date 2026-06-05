package com.squaregarden.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.squaregarden.model.MasterModeState
import com.squaregarden.model.MasterTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.masterDataStore: DataStore<Preferences> by preferencesDataStore(name = "master_mode")

class MasterModeRepository(private val context: Context) {

    companion object {
        // All-time stats
        private val TOTAL_MASTER_STARS = intPreferencesKey("total_master_stars")
        private val TOTAL_MASTER_GAMES = intPreferencesKey("total_master_games")
        private val TOTAL_MASTER_WINS = intPreferencesKey("total_master_wins")
        private val BEST_STREAK = intPreferencesKey("best_streak")
        private val MASTER_CHALLENGES_COMPLETED = intPreferencesKey("master_challenges_completed")
        // Session stats
        private val SESSION_GAMES_PLAYED = intPreferencesKey("session_games_played")
        private val SESSION_GAMES_WON = intPreferencesKey("session_games_won")
        private val SESSION_STARS = intPreferencesKey("session_stars")
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
        private val SESSION_START = longPreferencesKey("session_start")
    }

    val totalMasterStarsFlow: Flow<Int> = context.masterDataStore.data.map { it[TOTAL_MASTER_STARS] ?: 0 }
    val bestStreakFlow: Flow<Int> = context.masterDataStore.data.map { it[BEST_STREAK] ?: 0 }
    val totalMasterGamesFlow: Flow<Int> = context.masterDataStore.data.map { it[TOTAL_MASTER_GAMES] ?: 0 }
    val totalMasterWinsFlow: Flow<Int> = context.masterDataStore.data.map { it[TOTAL_MASTER_WINS] ?: 0 }

    suspend fun loadState(): MasterModeState {
        val prefs = context.masterDataStore.data.first()
        return MasterModeState(
            gamesPlayed = prefs[SESSION_GAMES_PLAYED] ?: 0,
            gamesWon = prefs[SESSION_GAMES_WON] ?: 0,
            currentStreak = prefs[CURRENT_STREAK] ?: 0,
            bestStreak = prefs[BEST_STREAK] ?: 0,
            totalMasterStars = prefs[TOTAL_MASTER_STARS] ?: 0,
            sessionStars = prefs[SESSION_STARS] ?: 0
        )
    }

    suspend fun recordWin(stars: Int) {
        context.masterDataStore.edit { prefs ->
            val newStreak = (prefs[CURRENT_STREAK] ?: 0) + 1
            prefs[CURRENT_STREAK] = newStreak
            prefs[BEST_STREAK] = maxOf(prefs[BEST_STREAK] ?: 0, newStreak)
            prefs[TOTAL_MASTER_STARS] = (prefs[TOTAL_MASTER_STARS] ?: 0) + stars
            prefs[SESSION_STARS] = (prefs[SESSION_STARS] ?: 0) + stars
            prefs[TOTAL_MASTER_GAMES] = (prefs[TOTAL_MASTER_GAMES] ?: 0) + 1
            prefs[TOTAL_MASTER_WINS] = (prefs[TOTAL_MASTER_WINS] ?: 0) + 1
            prefs[SESSION_GAMES_PLAYED] = (prefs[SESSION_GAMES_PLAYED] ?: 0) + 1
            prefs[SESSION_GAMES_WON] = (prefs[SESSION_GAMES_WON] ?: 0) + 1
        }
    }

    suspend fun recordLoss() {
        context.masterDataStore.edit { prefs ->
            prefs[CURRENT_STREAK] = 0
            prefs[TOTAL_MASTER_GAMES] = (prefs[TOTAL_MASTER_GAMES] ?: 0) + 1
            prefs[SESSION_GAMES_PLAYED] = (prefs[SESSION_GAMES_PLAYED] ?: 0) + 1
        }
    }

    suspend fun recordChallengeCompletion() {
        context.masterDataStore.edit { prefs ->
            prefs[MASTER_CHALLENGES_COMPLETED] = (prefs[MASTER_CHALLENGES_COMPLETED] ?: 0) + 1
        }
    }

    suspend fun startNewSession() {
        context.masterDataStore.edit { prefs ->
            prefs[SESSION_GAMES_PLAYED] = 0
            prefs[SESSION_GAMES_WON] = 0
            prefs[SESSION_STARS] = 0
            prefs[SESSION_START] = System.currentTimeMillis()
            // Streak persists across sessions
        }
    }

    suspend fun getMasterChallengesCompleted(): Int {
        return context.masterDataStore.data.first()[MASTER_CHALLENGES_COMPLETED] ?: 0
    }

    suspend fun clearAll() {
        context.masterDataStore.edit { it.clear() }
    }
}
