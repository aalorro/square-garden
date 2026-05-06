package com.squaregarden.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.squaregarden.model.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_progress")

class ProgressRepository(private val context: Context) {

    companion object {
        private const val LEVEL_STARS_PREFIX = "level_stars_"
        private val HINTS_KEY = intPreferencesKey("hints_remaining")
        private val LIVES_KEY = intPreferencesKey("lives")
        private val COOLDOWN_UNTIL_KEY = longPreferencesKey("cooldown_until")
        private val WIN_STREAK_KEY = intPreferencesKey("win_streak")
        private val TOTAL_STARS_KEY = intPreferencesKey("total_stars")
        private val GAMES_PLAYED_KEY = intPreferencesKey("games_played")
        private val LIFE_LOST_DIFFICULTY_KEY = intPreferencesKey("life_lost_difficulty")
        private val LAST_WON_LEVEL_KEY = intPreferencesKey("last_won_level")
        private val STARS_MIGRATED_KEY = booleanPreferencesKey("stars_migrated")
    }

    /** One-time migration: seed TOTAL_STARS_KEY from sum of per-level bests. */
    suspend fun migrateStarsIfNeeded() {
        context.dataStore.edit { prefs ->
            if (prefs[STARS_MIGRATED_KEY] == true) return@edit
            val sumOfBests = prefs.asMap().entries
                .filter { it.key.name.startsWith(LEVEL_STARS_PREFIX) && it.value is Int }
                .sumOf { it.value as Int }
            if (sumOfBests > 0 && (prefs[TOTAL_STARS_KEY] ?: 0) == 0) {
                prefs[TOTAL_STARS_KEY] = sumOfBests
            }
            prefs[STARS_MIGRATED_KEY] = true
        }
    }

    val totalStarsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOTAL_STARS_KEY] ?: 0
    }

    val gamesPlayedFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[GAMES_PLAYED_KEY] ?: 0
    }

    val lastWonLevelFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_WON_LEVEL_KEY] ?: -1
    }

    val livesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LIVES_KEY] ?: 3
    }

    val cooldownUntilFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[COOLDOWN_UNTIL_KEY] ?: 0L
    }

    suspend fun loadProgress(): PlayerProgress {
        val prefs = context.dataStore.data.first()
        val stars = mutableMapOf<Int, Int>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith(LEVEL_STARS_PREFIX) && value is Int) {
                val levelId = key.name.removePrefix(LEVEL_STARS_PREFIX).toIntOrNull()
                if (levelId != null) stars[levelId] = value
            }
        }
        val hints = prefs[HINTS_KEY] ?: 5
        return PlayerProgress(levelStars = stars, hintsRemaining = hints)
    }

    suspend fun saveLevelResult(levelId: Int, stars: Int) {
        context.dataStore.edit { prefs ->
            // Track per-level best (for level select display)
            val key = intPreferencesKey("$LEVEL_STARS_PREFIX$levelId")
            val existing = prefs[key] ?: 0
            if (stars > existing) {
                prefs[key] = stars
            }
            // Add full stars to cumulative running total
            val currentTotal = prefs[TOTAL_STARS_KEY] ?: 0
            prefs[TOTAL_STARS_KEY] = currentTotal + stars
            // Increment games played
            prefs[GAMES_PLAYED_KEY] = (prefs[GAMES_PLAYED_KEY] ?: 0) + 1
            // Track last won level
            prefs[LAST_WON_LEVEL_KEY] = levelId
        }
    }

    suspend fun useHint() {
        context.dataStore.edit { prefs ->
            val current = prefs[HINTS_KEY] ?: 5
            if (current > 0) prefs[HINTS_KEY] = current - 1
        }
    }

    /**
     * @param difficultyOrdinal ordinal of the Difficulty enum (EASY=0, MEDIUM=1, HARD=2)
     */
    suspend fun loseLife(difficultyOrdinal: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[LIVES_KEY] ?: 3
            val newLives = current - 1
            prefs[LIVES_KEY] = newLives
            prefs[WIN_STREAK_KEY] = 0
            // Record the difficulty at which the life was lost
            prefs[LIFE_LOST_DIFFICULTY_KEY] = difficultyOrdinal
            // Increment games played
            prefs[GAMES_PLAYED_KEY] = (prefs[GAMES_PLAYED_KEY] ?: 0) + 1
            if (newLives <= 0) {
                prefs[COOLDOWN_UNTIL_KEY] = System.currentTimeMillis() + 300_000L // 5 minutes
            }
        }
    }

    /**
     * @param difficultyOrdinal ordinal of the Difficulty enum (EASY=0, MEDIUM=1, HARD=2)
     * @param levelId the level that was just won
     * @return -1 if a life was just restored, 0 otherwise
     */
    suspend fun recordWin(difficultyOrdinal: Int, levelId: Int): Int {
        var result = 0
        context.dataStore.edit { prefs ->
            val currentLives = prefs[LIVES_KEY] ?: 3
            if (currentLives >= 3) return@edit

            val lostAt = prefs[LIFE_LOST_DIFFICULTY_KEY] ?: 0
            // Only count toward streak if playing at the loss difficulty or harder
            if (difficultyOrdinal < lostAt) return@edit

            // Only count wins within 5 levels of highest completed level
            val highestLevel = prefs.asMap().entries
                .filter { it.key.name.startsWith(LEVEL_STARS_PREFIX) && it.value is Int && (it.value as Int) > 0 }
                .mapNotNull { it.key.name.removePrefix(LEVEL_STARS_PREFIX).toIntOrNull() }
                .maxOrNull() ?: 0
            if (levelId < highestLevel - 4) return@edit

            val streak = (prefs[WIN_STREAK_KEY] ?: 0) + 1
            prefs[WIN_STREAK_KEY] = streak
            if (streak >= 3) {
                prefs[LIVES_KEY] = currentLives + 1
                prefs[WIN_STREAK_KEY] = 0
                result = -1 // life restored
            }
        }
        return result
    }

    suspend fun checkAndRestoreLives() {
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val cooldownUntil = prefs[COOLDOWN_UNTIL_KEY] ?: 0L
            if (cooldownUntil > 0L) {
                // Cap any existing cooldown to 5 minutes from now
                val maxCooldown = now + 300_000L
                if (cooldownUntil > maxCooldown) {
                    prefs[COOLDOWN_UNTIL_KEY] = maxCooldown
                }
                if (now >= (prefs[COOLDOWN_UNTIL_KEY] ?: 0L)) {
                    prefs[LIVES_KEY] = 3
                    prefs[COOLDOWN_UNTIL_KEY] = 0L
                }
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
