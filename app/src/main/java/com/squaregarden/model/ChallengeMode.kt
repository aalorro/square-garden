package com.squaregarden.model

enum class ChallengeType(val id: Int, val title: String, val description: String, val starMultiplier: Int = 1) {
    BLITZ(-1, "Blitz Garden", "60-second time attack! Clear as many goals as possible."),
    OVERGROWN(-2, "Overgrown Garden", "Conquer a massive 9\u00D79 board with 16 moves and 3 tries."),
    SHIFTING(-3, "Shifting Sands", "Every 3 moves, uncompleted tiles scramble!", starMultiplier = 2),
    MEMORY(-4, "Memory Garden", "Tiles are hidden! Reveal them by swapping nearby.", starMultiplier = 3),
    FROZEN_WAVE(-5, "Frozen Wave", "A cold front sweeps in! Every 2 moves, a tile freezes solid.", starMultiplier = 2),
    ROTATION(-6, "Rotation Garden", "Every 3 moves the board rotates 90\u00B0. Stay oriented!", starMultiplier = 3),
    MIRROR(-7, "Mirror Garden", "Every swap is mirrored across the board center.", starMultiplier = 3),
    DECAY(-8, "Decay Garden", "Completed goals expire after 5 moves. Be quick!", starMultiplier = 3);

    companion object {
        fun fromId(id: Int): ChallengeType? = entries.firstOrNull { it.id == id }
    }
}

data class ChallengeState(
    val type: ChallengeType,
    // Blitz Garden
    val timerMillisRemaining: Long = 0L,
    val goalsCleared: Int = 0,
    val comboCount: Int = 0,
    val comboMultiplier: Int = 1,
    val blitzStarScore: Int = 0,
    // Overgrown Garden
    val triesRemaining: Int = 3,
    val overgrownStarScore: Int = 0,
    val overgrownTryMultiplier: Int = 1,
    // Shifting Sands
    val movesSinceLastScramble: Int = 0,
    // Memory Garden
    val revealedCells: Set<CellPos> = emptySet(),
    val initialRevealDone: Boolean = false,
    // Frozen Wave
    val freezeMoveCounter: Int = 0,
    // Rotation Garden
    val rotationMoveCounter: Int = 0,
    // Decay Garden
    val decayMoveCounter: Int = 0,
    val goalCompletionMoves: Map<String, Int> = emptyMap()
)
