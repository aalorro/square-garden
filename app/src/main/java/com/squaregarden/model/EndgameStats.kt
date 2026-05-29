package com.squaregarden.model

data class EndgameStats(
    val totalStars: Int = 0,
    val gamesPlayed: Int = 0,
    val perfectGames: Int = 0,
    val totalSwaps: Int = 0,
    val tokensUsed: Int = 0,
    val levelsThreeStarred: Int = 0,
    val completedDifficulties: Set<String> = emptySet(),
    val challengeCompletions: Map<String, Int> = emptyMap(),
    val completionTimestamp: Long = 0L
)
