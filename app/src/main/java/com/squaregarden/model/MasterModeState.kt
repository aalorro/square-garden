package com.squaregarden.model

/**
 * Tier controlling board generation parameters for Master Mode.
 * Move multiplier is the sole move budget factor — skill-level multiplier does NOT stack.
 */
enum class MasterTier(
    val label: String,
    val minWidth: Int, val maxWidth: Int,
    val minHeight: Int, val maxHeight: Int,
    val minGoals: Int, val maxGoals: Int,
    val minColors: Int, val maxColors: Int,
    val minFrozen: Int, val maxFrozen: Int,
    val moveMultiplier: Float,
    val baseStars: Int
) {
    WARMING_UP(
        "Warming Up",
        minWidth = 5, maxWidth = 5, minHeight = 5, maxHeight = 6,
        minGoals = 3, maxGoals = 4, minColors = 3, maxColors = 4,
        minFrozen = 0, maxFrozen = 2,
        moveMultiplier = 1.0f, baseStars = 2
    ),
    STEADY(
        "Steady",
        minWidth = 5, maxWidth = 6, minHeight = 5, maxHeight = 6,
        minGoals = 3, maxGoals = 5, minColors = 3, maxColors = 5,
        minFrozen = 1, maxFrozen = 4,
        moveMultiplier = 0.9f, baseStars = 3
    ),
    HEATING_UP(
        "Heating Up",
        minWidth = 6, maxWidth = 7, minHeight = 6, maxHeight = 7,
        minGoals = 4, maxGoals = 6, minColors = 4, maxColors = 5,
        minFrozen = 2, maxFrozen = 6,
        moveMultiplier = 0.8f, baseStars = 5
    ),
    INTENSE(
        "Intense",
        minWidth = 7, maxWidth = 8, minHeight = 7, maxHeight = 8,
        minGoals = 5, maxGoals = 8, minColors = 4, maxColors = 6,
        minFrozen = 4, maxFrozen = 10,
        moveMultiplier = 0.7f, baseStars = 8
    ),
    BRUTAL(
        "Brutal",
        minWidth = 8, maxWidth = 9, minHeight = 8, maxHeight = 9,
        minGoals = 6, maxGoals = 10, minColors = 5, maxColors = 6,
        minFrozen = 6, maxFrozen = 14,
        moveMultiplier = 0.6f, baseStars = 12
    )
}

/**
 * Master Mode session/persistent state.
 */
data class MasterModeState(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalMasterStars: Int = 0,
    val sessionStars: Int = 0,
    val currentTier: MasterTier = MasterTier.WARMING_UP,
    val isChallengeRound: Boolean = false
) {
    /** Streak multiplier: 1.0 + floor(streak/3) * 0.5, capped at 5.0 */
    val streakMultiplier: Float
        get() = (1.0f + (currentStreak / 3) * 0.5f).coerceAtMost(5.0f)
}
