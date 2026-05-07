package com.squaregarden.model

data class PlayerProgress(
    val levelStars: Map<Int, Int> = emptyMap(),
    val favoriteLevels: Set<Int> = emptySet(),
    val hintsRemaining: Int = 5
) {
    fun highestUnlockedLevel(startingLevel: Int = 1): Int {
        val earned = if (levelStars.isEmpty()) 1 else (levelStars.keys.maxOrNull() ?: 0) + 1
        return maxOf(earned, startingLevel)
    }

    fun totalStars(): Int = levelStars.values.sum()
}
