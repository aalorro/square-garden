package com.squaregarden.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.squaregarden.model.Difficulty

object PlayGamesManager {

    private const val TAG = "PlayGamesManager"

    fun checkSignIn(activity: Activity, onResult: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity).isAuthenticated
            .addOnCompleteListener { task ->
                val isAuth = task.isSuccessful && task.result.isAuthenticated
                onResult(isAuth)
            }
    }

    fun signIn(activity: Activity) {
        PlayGames.getGamesSignInClient(activity).signIn()
    }

    fun submitTotalStars(activity: Activity, difficulty: Difficulty, totalStars: Int) {
        val id = totalStarsLeaderboardId(activity, difficulty) ?: return
        PlayGames.getLeaderboardsClient(activity)
            .submitScore(id, totalStars.toLong())
        Log.d(TAG, "Submitted totalStars=$totalStars for ${difficulty.name}")
    }

    fun submitHighestLevel(activity: Activity, difficulty: Difficulty, level: Int) {
        val id = highestLevelLeaderboardId(activity, difficulty) ?: return
        PlayGames.getLeaderboardsClient(activity)
            .submitScore(id, level.toLong())
        Log.d(TAG, "Submitted highestLevel=$level for ${difficulty.name}")
    }

    fun submitWorldStars(activity: Activity, difficulty: Difficulty, worldId: Int, stars: Int) {
        val id = worldStarsLeaderboardId(activity, difficulty, worldId) ?: return
        PlayGames.getLeaderboardsClient(activity)
            .submitScore(id, stars.toLong())
        Log.d(TAG, "Submitted world${worldId}Stars=$stars for ${difficulty.name}")
    }

    fun showAllLeaderboards(activity: Activity) {
        PlayGames.getLeaderboardsClient(activity)
            .allLeaderboardsIntent
            .addOnSuccessListener { intent ->
                activity.startActivity(intent)
            }
    }

    fun showLeaderboard(activity: Activity, leaderboardId: String) {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent ->
                activity.startActivity(intent)
            }
    }

    // --- ID resolution helpers ---

    private fun skillSuffix(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EASY -> "casual"
        Difficulty.MEDIUM -> "standard"
        Difficulty.HARD -> "pro"
    }

    private fun getStringRes(context: Context, name: String): String? {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        if (resId == 0) return null
        val value = context.getString(resId)
        return if (value == "PLACEHOLDER") null else value
    }

    private fun totalStarsLeaderboardId(context: Context, difficulty: Difficulty): String? =
        getStringRes(context, "leaderboard_total_stars_${skillSuffix(difficulty)}")

    private fun highestLevelLeaderboardId(context: Context, difficulty: Difficulty): String? =
        getStringRes(context, "leaderboard_highest_level_${skillSuffix(difficulty)}")

    private fun worldStarsLeaderboardId(context: Context, difficulty: Difficulty, worldId: Int): String? =
        getStringRes(context, "leaderboard_world_${worldId}_${skillSuffix(difficulty)}")
}
