package com.squaregarden.logic

import com.squaregarden.model.*

object ChallengeGenerator {

    fun generateLevel(type: ChallengeType, skill: Difficulty = Difficulty.MEDIUM): Level {
        return when (type) {
            ChallengeType.BLITZ -> generateBlitz(skill)
            ChallengeType.OVERGROWN -> generateOvergrown(skill)
            ChallengeType.SHIFTING -> generateShifting(skill)
            ChallengeType.MEMORY -> generateMemory(skill)
        }
    }

    /**
     * Generate a fresh set of goals achievable on the current board (for Blitz replenish).
     * Goals are guaranteed NOT to be already met on the board.
     */
    fun generateBlitzGoalSet(board: Board, skill: Difficulty = Difficulty.MEDIUM): List<Goal> {
        val colorCounts = mutableMapOf<TileColor, Int>()
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c)) {
                    val color = board.tileAt(r, c).color
                    colorCounts[color] = (colorCounts[color] ?: 0) + 1
                }
            }
        }
        val abundant = colorCounts.entries
            .filter { it.value >= 3 }
            .sortedByDescending { it.value }
            .map { it.key }
            .shuffled()

        if (abundant.isEmpty()) {
            val fallback = colorCounts.keys.toList().shuffled().first()
            return listOf(Goal.Line(fallback, 3))
        }

        val goalCount = when (skill) {
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 4
        }.coerceAtMost(abundant.size)

        // Try multiple times to find goals not already met
        repeat(30) {
            val goals = mutableListOf<Goal>()
            val shuffledColors = abundant.shuffled()
            for (i in 0 until goalCount) {
                val color = shuffledColors[i]
                val count = colorCounts[color] ?: 0
                val useSquare = skill != Difficulty.EASY && count >= 4 && Math.random() < 0.3
                goals.add(if (useSquare) Goal.Square(color) else Goal.Line(color, 3))
            }
            // Verify none are already met
            val met = BoardEngine.evaluateGoals(board, goals)
            if (met.isEmpty()) return goals
        }

        // Last resort: just pick goals (shouldn't normally reach here)
        return (0 until goalCount).map { i ->
            Goal.Line(abundant[i % abundant.size], 3)
        }
    }

    // ── Blitz: timer-based, skill-scaled ──

    private fun generateBlitz(skill: Difficulty): Level {
        val w = when (skill) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 6
            Difficulty.HARD -> 7
        }
        val h = w
        val numColors = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val tiles = generateRandomTiles(w, h, colors)
        // Build a temp board to pick goals that aren't pre-met
        val tempBoard = Board(w, h, tiles.map { row -> row.map { Tile(it) } })
        val goals = generateBlitzGoalSet(tempBoard, skill)
        return Level(
            id = ChallengeType.BLITZ.id,
            world = 0,
            name = ChallengeType.BLITZ.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 999,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 0, threeStar = 0)
        )
    }

    // ── Overgrown: 9×9, 8 mixed goals, 12 moves, ~15 frozen ──

    private fun generateOvergrown(skill: Difficulty): Level {
        val w = 9; val h = 9
        val colors = TileColor.entries.toList()
        val goalCount = when (skill) {
            Difficulty.EASY -> 6
            Difficulty.MEDIUM -> 8
            Difficulty.HARD -> 10
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        val frozenCount = when (skill) {
            Difficulty.EASY -> 10
            Difficulty.MEDIUM -> 15
            Difficulty.HARD -> 20
        }
        val frozen = pickFrozenCells(w, h, frozenCount)
        return Level(
            id = ChallengeType.OVERGROWN.id,
            world = 0,
            name = ChallengeType.OVERGROWN.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 12,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 3, threeStar = 6),
            frozenCells = frozen
        )
    }

    // ── Shifting Sands: 7×7, 4 mixed goals, 20 moves ──

    private fun generateShifting(skill: Difficulty): Level {
        val w = 7; val h = 7
        val numColors = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        return Level(
            id = ChallengeType.SHIFTING.id,
            world = 0,
            name = ChallengeType.SHIFTING.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 20,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 6, threeStar = 12)
        )
    }

    // ── Memory Garden: 5×5, 2 simple goals, 8 moves ──

    private fun generateMemory(skill: Difficulty): Level {
        val w = 5; val h = 5
        val numColors = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 4
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
        }
        val goals = pickSimpleGoals(colors, goalCount)
        val tiles = generateRandomTiles(w, h, colors)
        return Level(
            id = ChallengeType.MEMORY.id,
            world = 0,
            name = ChallengeType.MEMORY.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 8,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 2, threeStar = 4)
        )
    }

    // ── Helpers ──

    private fun pickSimpleGoals(colors: List<TileColor>, count: Int): List<Goal> {
        val shuffled = colors.shuffled()
        return (0 until count).map { i ->
            val color = shuffled[i % shuffled.size]
            if (Math.random() < 0.25) Goal.Square(color) else Goal.Line(color, 3)
        }
    }

    private fun pickMixedGoals(colors: List<TileColor>, count: Int, skill: Difficulty): List<Goal> {
        val shuffled = colors.shuffled()
        val simpleShapes = listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        val hardShapes = listOf(ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE)
        return (0 until count).map { i ->
            val color = shuffled[i % shuffled.size]
            val roll = Math.random()
            when (skill) {
                Difficulty.EASY -> when {
                    roll < 0.6 -> Goal.Line(color, 3)
                    roll < 0.85 -> Goal.Square(color)
                    else -> Goal.Shape(color, simpleShapes.random())
                }
                Difficulty.MEDIUM -> when {
                    roll < 0.35 -> Goal.Line(color, if (Math.random() < 0.3) 4 else 3)
                    roll < 0.55 -> Goal.Square(color)
                    roll < 0.80 -> Goal.Shape(color, simpleShapes.random())
                    else -> Goal.Shape(color, hardShapes.random())
                }
                Difficulty.HARD -> when {
                    roll < 0.2 -> Goal.Line(color, if (Math.random() < 0.5) 4 else 3)
                    roll < 0.4 -> Goal.Square(color)
                    roll < 0.7 -> Goal.Shape(color, simpleShapes.random())
                    else -> Goal.Shape(color, hardShapes.random())
                }
            }
        }
    }

    private fun generateRandomTiles(w: Int, h: Int, colors: List<TileColor>): List<List<TileColor>> {
        return List(h) { List(w) { colors.random() } }
    }

    private fun pickFrozenCells(w: Int, h: Int, count: Int): Set<CellPos> {
        val all = mutableListOf<CellPos>()
        for (r in 1 until h - 1) {
            for (c in 1 until w - 1) {
                all.add(CellPos(r, c))
            }
        }
        return all.shuffled().take(count.coerceAtMost(all.size)).toSet()
    }
}
