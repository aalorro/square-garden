package com.patterngarden.model

data class GameState(
    val level: Level,
    val board: Board,
    val movesRemaining: Int,
    val completedGoalIds: Set<String> = emptySet(),
    val completedGoalCells: Map<String, Set<CellPos>> = emptyMap(),
    val selectedCell: CellPos? = null,
    val hintCells: Set<CellPos> = emptySet(),
    val phase: GamePhase = GamePhase.PLAYING,
    val tutorialStepIndex: Int = 0,
    val swapAnim: SwapAnimation? = null,
    val starsAwarded: Int = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val winsToRestoreLife: Int = 0,
    val initialBoard: Board? = null,
    val solutionSteps: List<Pair<CellPos, CellPos>>? = null
)

data class SwapAnimation(
    val from: CellPos,
    val to: CellPos,
    val progress: Float // 0f..1f
)

enum class GamePhase {
    PLAYING, ANIMATING, WON, LOST, TUTORIAL_PAUSE, SHOWING_SOLUTION
}
