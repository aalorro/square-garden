package com.squaregarden.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.squaregarden.SquareGardenApp
import com.squaregarden.model.Difficulty
import com.squaregarden.model.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LeaderboardRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance(SquareGardenApp.DB_URL).reference

    private var cachedEntries: List<LeaderboardEntry>? = null
    private var cachedDifficulty: String? = null
    private var cacheTimestamp: Long = 0L
    private var lastSubmittedScore: Int = -1
    private var cachedMasterEntries: List<LeaderboardEntry>? = null
    private var cachedMasterTimestamp: Long = 0L
    private var lastSubmittedMasterScore: Int = -1

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun ensureAuthenticated(): String? = withContext(Dispatchers.IO) {
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            auth.currentUser?.uid
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous auth failed", e)
            null
        }
    }

    suspend fun submitTotalStars(
        name: String,
        emoji: String,
        difficulty: Difficulty,
        totalStars: Int,
        highestLevel: Int
    ) = withContext(Dispatchers.IO) {
        if (totalStars <= lastSubmittedScore) return@withContext
        val uid = ensureAuthenticated() ?: return@withContext
        try {
            val entry = mapOf(
                "name" to name.take(20),
                "emoji" to emoji,
                "score" to totalStars,
                "level" to highestLevel,
                "ts" to ServerValue.TIMESTAMP
            )
            db.child("leaderboards/total_stars/${difficulty.id}/$uid")
                .setValue(entry)
                .await()
            lastSubmittedScore = totalStars
            // Invalidate cache since our entry changed
            cachedEntries = null
        } catch (e: Exception) {
            Log.w(TAG, "Leaderboard submit failed (will retry when online)", e)
        }
    }

    suspend fun fetchLeaderboard(
        difficulty: Difficulty,
        limit: Int = 50
    ): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        // Return cache if fresh and same difficulty
        val now = System.currentTimeMillis()
        if (cachedEntries != null && cachedDifficulty == difficulty.id &&
            now - cacheTimestamp < CACHE_TTL
        ) {
            return@withContext cachedEntries!!
        }

        try {
            val snapshot = db.child("leaderboards/total_stars/${difficulty.id}")
                .orderByChild("score")
                .limitToLast(limit)
                .get()
                .await()

            val entries = mutableListOf<LeaderboardEntry>()
            for (child in snapshot.children) {
                val uid = child.key ?: continue
                val name = child.child("name").getValue(String::class.java) ?: ""
                val emoji = child.child("emoji").getValue(String::class.java) ?: ""
                val score = child.child("score").getValue(Int::class.java) ?: 0
                val level = child.child("level").getValue(Int::class.java) ?: 0
                val ts = child.child("ts").getValue(Long::class.java) ?: 0L
                entries.add(LeaderboardEntry(uid, name, emoji, score, level, ts))
            }

            // RTDB returns ascending by score; reverse for descending rank
            entries.sortByDescending { it.score }
            val ranked = entries.mapIndexed { i, e -> e.copy(rank = i + 1) }

            cachedEntries = ranked
            cachedDifficulty = difficulty.id
            cacheTimestamp = now
            ranked
        } catch (e: Exception) {
            Log.w(TAG, "Fetch leaderboard failed", e)
            cachedEntries ?: emptyList()
        }
    }

    fun findPlayerRank(entries: List<LeaderboardEntry>): LeaderboardEntry? {
        val uid = currentUid ?: return null
        return entries.find { it.uid == uid }
    }

    suspend fun submitMasterModeStars(
        name: String,
        emoji: String,
        totalStars: Int,
        bestStreak: Int
    ) = withContext(Dispatchers.IO) {
        if (totalStars <= lastSubmittedMasterScore) return@withContext
        val uid = ensureAuthenticated() ?: return@withContext
        try {
            val entry = mapOf(
                "name" to name.take(20),
                "emoji" to emoji,
                "score" to totalStars,
                "streak" to bestStreak,
                "ts" to ServerValue.TIMESTAMP
            )
            db.child("leaderboards/master_mode/$uid")
                .setValue(entry)
                .await()
            lastSubmittedMasterScore = totalStars
            cachedMasterEntries = null
        } catch (e: Exception) {
            Log.w(TAG, "Master leaderboard submit failed", e)
        }
    }

    suspend fun fetchMasterLeaderboard(
        limit: Int = 50
    ): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedMasterEntries != null && now - cachedMasterTimestamp < CACHE_TTL) {
            return@withContext cachedMasterEntries!!
        }

        try {
            val snapshot = db.child("leaderboards/master_mode")
                .orderByChild("score")
                .limitToLast(limit)
                .get()
                .await()

            val entries = mutableListOf<LeaderboardEntry>()
            for (child in snapshot.children) {
                val uid = child.key ?: continue
                val name = child.child("name").getValue(String::class.java) ?: ""
                val emoji = child.child("emoji").getValue(String::class.java) ?: ""
                val score = child.child("score").getValue(Int::class.java) ?: 0
                val streak = child.child("streak").getValue(Int::class.java) ?: 0
                val ts = child.child("ts").getValue(Long::class.java) ?: 0L
                entries.add(LeaderboardEntry(uid, name, emoji, score, streak, ts))
            }

            entries.sortByDescending { it.score }
            val ranked = entries.mapIndexed { i, e -> e.copy(rank = i + 1) }

            cachedMasterEntries = ranked
            cachedMasterTimestamp = now
            ranked
        } catch (e: Exception) {
            Log.w(TAG, "Fetch master leaderboard failed", e)
            cachedMasterEntries ?: emptyList()
        }
    }

    fun invalidateCache() {
        cachedEntries = null
        cachedMasterEntries = null
    }

    companion object {
        private const val TAG = "LeaderboardRepo"
        private const val CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    }
}
