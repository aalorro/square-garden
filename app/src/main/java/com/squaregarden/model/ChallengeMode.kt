package com.squaregarden.model

enum class ChallengeType(val id: Int, val title: String, val description: String) {
    BLITZ(-1, "Blitz Garden", "60-second time attack! Clear as many goals as possible."),
    OVERGROWN(-2, "Overgrown Garden", "Conquer a massive 9\u00D79 board with 16 moves and 3 tries."),
    SHIFTING(-3, "Shifting Sands", "Every 3 moves, uncompleted tiles scramble!"),
    MEMORY(-4, "Memory Garden", "Tiles are hidden! Reveal them by swapping nearby.");

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
    // Overgrown Garden
    val triesRemaining: Int = 3,
    // Shifting Sands
    val movesSinceLastScramble: Int = 0,
    // Memory Garden
    val revealedCells: Set<CellPos> = emptySet(),
    val initialRevealDone: Boolean = false
)
