package com.squaregarden.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.squaregarden.model.Difficulty
import com.squaregarden.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

class ProfileRepository(private val context: Context) {

    companion object {
        private val USERNAME = stringPreferencesKey("username")
        private val AVATAR_ID = intPreferencesKey("avatar_id")
        private val YEAR_OF_BIRTH = intPreferencesKey("year_of_birth")
        private val GENDER = stringPreferencesKey("gender")
        private val THEME_ID = stringPreferencesKey("theme_id")
        private val DIFFICULTY = stringPreferencesKey("difficulty")
        private val PLAYER_LEVEL = intPreferencesKey("player_level")
        private val LEADERBOARD_OPT_IN = booleanPreferencesKey("leaderboard_opt_in")
        private val CUSTOM_AVATAR_PATH = stringPreferencesKey("custom_avatar_path")
        private val OVERRIDE_STARTING_LEVEL = intPreferencesKey("override_starting_level")
        private val PERFECT_GAME_SPLASH_SHOWN = booleanPreferencesKey("perfect_game_splash_shown")
        private val MASTERY_BADGE_EARNED = booleanPreferencesKey("mastery_badge_earned")
        private val PRO_UPGRADE_DECLINED = booleanPreferencesKey("pro_upgrade_declined")
    }

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            username = prefs[USERNAME] ?: "",
            avatarId = prefs[AVATAR_ID] ?: 0,
            customAvatarPath = prefs[CUSTOM_AVATAR_PATH] ?: "",
            yearOfBirth = prefs[YEAR_OF_BIRTH] ?: 2000,
            gender = prefs[GENDER] ?: "prefer_not_to_say",
            themeId = prefs[THEME_ID] ?: "light",
            difficulty = prefs[DIFFICULTY] ?: "medium",
            playerLevel = prefs[PLAYER_LEVEL] ?: 0,
            leaderboardOptIn = prefs[LEADERBOARD_OPT_IN] ?: false,
            overrideStartingLevel = prefs[OVERRIDE_STARTING_LEVEL] ?: 0,
            perfectGameSplashShown = prefs[PERFECT_GAME_SPLASH_SHOWN] ?: false,
            masteryBadgeEarned = prefs[MASTERY_BADGE_EARNED] ?: false,
            proUpgradeDeclined = prefs[PRO_UPGRADE_DECLINED] ?: false
        )
    }

    suspend fun loadProfile(): UserProfile {
        return profileFlow.first()
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[USERNAME] = profile.username
            prefs[AVATAR_ID] = profile.avatarId
            prefs[YEAR_OF_BIRTH] = profile.yearOfBirth
            prefs[GENDER] = profile.gender
            prefs[THEME_ID] = profile.themeId
            prefs[DIFFICULTY] = profile.difficulty
            prefs[LEADERBOARD_OPT_IN] = profile.leaderboardOptIn
            prefs[CUSTOM_AVATAR_PATH] = profile.customAvatarPath
            // playerLevel is NOT overwritten here — it's incremented separately
        }
    }

    suspend fun incrementPlayerLevel() {
        context.profileDataStore.edit { prefs ->
            val current = prefs[PLAYER_LEVEL] ?: 0
            prefs[PLAYER_LEVEL] = current + 1
        }
    }

    suspend fun resetPlayerLevel() {
        context.profileDataStore.edit { prefs ->
            prefs[PLAYER_LEVEL] = 0
        }
    }

    suspend fun markPerfectGameSplashShown() {
        context.profileDataStore.edit { prefs ->
            prefs[PERFECT_GAME_SPLASH_SHOWN] = true
        }
    }

    suspend fun upgradeSkill(newDifficulty: Difficulty, overrideStartingLevel: Int) {
        context.profileDataStore.edit { prefs ->
            prefs[DIFFICULTY] = newDifficulty.id
            prefs[OVERRIDE_STARTING_LEVEL] = overrideStartingLevel
            // Clear declined flag when upgrading (e.g. to Pro+)
            prefs[PRO_UPGRADE_DECLINED] = false
        }
    }

    suspend fun markProUpgradeDeclined() {
        context.profileDataStore.edit { prefs ->
            prefs[PRO_UPGRADE_DECLINED] = true
        }
    }

    suspend fun markMasteryBadgeEarned() {
        context.profileDataStore.edit { prefs ->
            prefs[MASTERY_BADGE_EARNED] = true
        }
    }

    suspend fun clearAll() {
        context.profileDataStore.edit { it.clear() }
    }
}
