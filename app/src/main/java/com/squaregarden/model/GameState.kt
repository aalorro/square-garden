package com.squaregarden.model

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
    val gameDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val winsToRestoreLife: Int = 0,
    val lifeRestored: Boolean = false,
    val initialBoard: Board? = null,
    val solutionSteps: List<Pair<CellPos, CellPos>>? = null,
    val hasSolution: Boolean = false,
    val unlockedWorldName: String? = null
)

enum class GameDifficulty(val label: String, val starMultiplier: Float) {
    EASY("Easy", 0.75f),
    MEDIUM("Medium", 1.0f),
    HARD("Hard", 1.25f),
    VERY_HARD("Very Hard", 1.5f),
    EXTREMELY_HARD("Extremely Hard", 2.0f);

    companion object {
        fun calculate(
            solutionMoves: Int,
            maxMoves: Int,
            goals: List<Goal>,
            frozenCount: Int,
            voidCount: Int,
            boardArea: Int,
            colorCount: Int
        ): GameDifficulty {
            val tightness = if (maxMoves > 0) solutionMoves.toFloat() / maxMoves else 0f

            val goalComplexity = goals.sumOf { goal: Goal ->
                val w: Int = when (goal) {
                    is Goal.Line -> 1
                    is Goal.Square -> 2
                    is Goal.Shape -> when (goal.shapeType) {
                        ShapeType.L_SHAPE, ShapeType.T_SHAPE -> 3
                        ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE -> 4
                    }
                }
                w
            }
            val maxComplexity = goals.size * 4
            val complexityRatio = if (maxComplexity > 0) goalComplexity.toFloat() / maxComplexity else 0f

            val goalCountRatio = (goals.size.toFloat() / 4f).coerceAtMost(1f)

            val constraintRatio = if (boardArea > 0) (frozenCount + voidCount).toFloat() / boardArea else 0f

            val colorRatio = colorCount.toFloat() / 5f

            val score = tightness * 0.4f +
                    complexityRatio * 0.25f +
                    goalCountRatio * 0.15f +
                    constraintRatio * 0.1f +
                    colorRatio * 0.1f

            return when {
                score < 0.25f -> EASY
                score < 0.40f -> MEDIUM
                score < 0.55f -> HARD
                score < 0.70f -> VERY_HARD
                else -> EXTREMELY_HARD
            }
        }
    }
}

data class SwapAnimation(
    val from: CellPos,
    val to: CellPos,
    val progress: Float // 0f..1f
)

enum class GamePhase {
    PLAYING, ANIMATING, WON, LOST, TUTORIAL_PAUSE, SHOWING_SOLUTION
}
