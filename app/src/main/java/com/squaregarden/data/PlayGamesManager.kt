package com.squaregarden.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.squaregarden.model.Difficulty

object PlayGamesManager {

    private const val TAG = "PlayGamesManager"

    fun checkSignIn(activity: Activity, onResult: (Boolean) -> Unit) {
        Log.d(TAG, "Checking sign-in status...")
        PlayGames.getGamesSignInClient(activity).isAuthenticated
            .addOnCompleteListener { task ->
                val isAuth = task.isSuccessful && task.result.isAuthenticated
                Log.d(TAG, "checkSignIn result: success=${task.isSuccessful}, authenticated=$isAuth")
                if (!task.isSuccessful) {
                    Log.e(TAG, "checkSignIn failed", task.exception)
                }
                onResult(isAuth)
            }
    }

    fun signIn(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        Log.d(TAG, "Initiating sign-in...")
        PlayGames.getGamesSignInClient(activity).signIn()
            .addOnCompleteListener { task ->
                val success = task.isSuccessful
                Log.d(TAG, "Sign-in complete: success=$success")
                if (!success) {
                    Log.e(TAG, "Sign-in failed", task.exception)
                }
                onComplete?.invoke(success)
            }
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

    fun showAllLeaderboards(activity: Activity) {
        Log.d(TAG, "Requesting leaderboards intent...")
        PlayGames.getLeaderboardsClient(activity)
            .allLeaderboardsIntent
            .addOnSuccessListener { intent ->
                Log.d(TAG, "Leaderboards intent received, launching...")
                activity.startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to open leaderboards: ${e.message}", e)
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
}
