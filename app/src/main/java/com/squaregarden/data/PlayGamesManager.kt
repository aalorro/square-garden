package com.squaregarden.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.tasks.Tasks
import com.squaregarden.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /** Submit scores immediately to server (blocks until complete) then open leaderboards. */
    suspend fun submitAndShowLeaderboards(
        activity: Activity,
        difficulty: Difficulty,
        totalStars: Int,
        highestLevel: Int
    ) {
        val client = PlayGames.getLeaderboardsClient(activity)
        // Submit scores with immediate server sync
        withContext(Dispatchers.IO) {
            try {
                val starsId = totalStarsLeaderboardId(activity, difficulty)
                val levelId = highestLevelLeaderboardId(activity, difficulty)
                if (starsId != null) {
                    Tasks.await(client.submitScoreImmediate(starsId, totalStars.toLong()))
                    Log.d(TAG, "Immediate submit totalStars=$totalStars for ${difficulty.name}")
                }
                if (levelId != null) {
                    Tasks.await(client.submitScoreImmediate(levelId, highestLevel.toLong()))
                    Log.d(TAG, "Immediate submit highestLevel=$highestLevel for ${difficulty.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Immediate submit failed, falling back to cached submit", e)
                submitTotalStars(activity, difficulty, totalStars)
                submitHighestLevel(activity, difficulty, highestLevel)
            }
        }
        showAllLeaderboards(activity)
    }

    private const val RC_LEADERBOARD_UI = 9004

    fun showAllLeaderboards(activity: Activity) {
        Log.d(TAG, "Requesting leaderboards intent...")
        PlayGames.getLeaderboardsClient(activity)
            .allLeaderboardsIntent
            .addOnSuccessListener { intent ->
                Log.d(TAG, "Leaderboards intent received: action=${intent.action}, component=${intent.component}, flags=${intent.flags}")
                @Suppress("DEPRECATION")
                activity.startActivityForResult(intent, RC_LEADERBOARD_UI)
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
        // Pro+ leaderboard string resources may not exist yet in Google Play Console;
        // getStringRes() returns null for missing/PLACEHOLDER entries and the
        // submit calls early-return safely.
        Difficulty.PRO_PLUS -> "pro_plus"
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
