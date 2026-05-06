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
    val unlockedWorldName: String? = null,
    val shuffleTokens: Int = 0,
    val shuffleTokenAwarded: Boolean = false,
    val passthroughTokens: Int = 0,
    val passthroughTokenAwarded: Boolean = false,
    val passthroughActive: Boolean = false,
    val unfreezeTokens: Int = 0,
    val unfreezeTokenAwarded: Boolean = false,
    val unfreezeMode: Boolean = false
)

enum class GameDifficulty(val label: String, val starMultiplier: Float) {
    EASY("Easy", 0.75f),
    MEDIUM("Medium", 1.0f),
    HARD("Hard", 1.25f),
    VERY_HARD("Very Hard", 1.5f),
    EXTREMELY_HARD("Extremely Hard", 2.0f);

    companion object {
        fun calculate(
            board: Board,
            maxMoves: Int,
            goals: List<Goal>,
            frozenCount: Int,
            voidCount: Int,
            skill: Difficulty = Difficulty.MEDIUM
        ): GameDifficulty {
            var points = 0f

            // ── Tile scatter (heaviest weight: 0-8 pts) ──
            // For each goal, find the tightest cluster of needed tiles
            // and measure how spread out they are relative to board size
            val maxDist = (board.width + board.height - 2).toFloat()
            var scatterTotal = 0f
            for (goal in goals) {
                val positions = mutableListOf<CellPos>()
                for (r in 0 until board.height) {
                    for (c in 0 until board.width) {
                        if (!board.isVoid(r, c) && board.tileAt(r, c).color == goal.color)
                            positions.add(CellPos(r, c))
                    }
                }
                val needed = when (goal) {
                    is Goal.Line -> goal.length
                    is Goal.Square -> 4
                    is Goal.Shape -> goal.shapeType.offsets.size
                }
                if (positions.size >= needed) {
                    // Try each tile as anchor, find tightest cluster of N nearest
                    var minSpread = Float.MAX_VALUE
                    for (anchor in positions) {
                        val closest = positions
                            .sortedBy { Math.abs(it.row - anchor.row) + Math.abs(it.col - anchor.col) }
                            .take(needed)
                        val spanR = closest.maxOf { it.row } - closest.minOf { it.row }
                        val spanC = closest.maxOf { it.col } - closest.minOf { it.col }
                        val spread = (spanR + spanC).toFloat() / maxDist
                        if (spread < minSpread) minSpread = spread
                    }
                    scatterTotal += minSpread
                }
            }
            val avgScatter = if (goals.isNotEmpty()) scatterTotal / goals.size else 0f
            points += avgScatter * 8f

            // ── Move pressure (0-7 pts) ──
            val movesPerGoal = if (goals.isNotEmpty()) maxMoves.toFloat() / goals.size else maxMoves.toFloat()
            points += when {
                movesPerGoal >= 5f -> 0f
                movesPerGoal >= 4f -> 1.5f
                movesPerGoal >= 3f -> 3f
                movesPerGoal >= 2f -> 5f
                else -> 7f
            }

            // ── Goal type complexity (0-6 pts) ──
            for (goal in goals) {
                points += when (goal) {
                    is Goal.Line -> 0f
                    is Goal.Square -> 0.5f
                    is Goal.Shape -> when (goal.shapeType) {
                        ShapeType.L_SHAPE, ShapeType.T_SHAPE -> 1f
                        ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE -> 1.5f
                    }
                }
            }

            // ── Board constraints (variable) ──
            points += frozenCount * 0.3f + voidCount * 0.2f

            // Skill-relative thresholds: what's Easy for Pro is harder for Casual
            return when (skill) {
                Difficulty.EASY -> when {   // Casual — strict (less skilled, things feel harder)
                    points < 2f -> EASY
                    points < 4.5f -> MEDIUM
                    points < 7f -> HARD
                    points < 11f -> VERY_HARD
                    else -> EXTREMELY_HARD
                }
                Difficulty.MEDIUM -> when { // Standard — baseline
                    points < 3f -> EASY
                    points < 6f -> MEDIUM
                    points < 9f -> HARD
                    points < 14f -> VERY_HARD
                    else -> EXTREMELY_HARD
                }
                Difficulty.HARD -> when {   // Pro — generous (more skilled, things feel easier)
                    points < 4f -> EASY
                    points < 7f -> MEDIUM
                    points < 10f -> HARD
                    points < 15f -> VERY_HARD
                    else -> EXTREMELY_HARD
                }
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
