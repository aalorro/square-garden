package com.squaregarden.logic

import com.squaregarden.model.*

object ChallengeGenerator {

    fun generateLevel(type: ChallengeType, skill: Difficulty = Difficulty.MEDIUM): Level {
        return when (type) {
            ChallengeType.BLITZ -> generateBlitz(skill)
            ChallengeType.OVERGROWN -> generateOvergrown(skill)
            ChallengeType.SHIFTING -> generateShifting(skill)
            ChallengeType.MEMORY -> generateMemory(skill)
            ChallengeType.FROZEN_WAVE -> generateFrozenWave(skill)
            ChallengeType.ROTATION -> generateRotation(skill)
            ChallengeType.MIRROR -> generateMirror(skill)
            ChallengeType.DECAY -> generateDecay(skill)
        }
    }

    /**
     * Generate a fresh set of goals achievable on the current board (for Blitz replenish).
     * Each goal is individually verified NOT to be already met on the board.
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
        val colors = colorCounts.keys.toList()
        if (colors.isEmpty()) return listOf(Goal.Line(TileColor.RED, 3))

        val goalCount = when (skill) {
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 4
            Difficulty.PRO_PLUS -> 5
        }

        val simpleShapes = listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        val hardShapes = listOf(ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE)

        // Build candidate goals per color, ordered hardest-first so we prefer unmet ones
        fun candidatesForColor(color: TileColor): List<Goal> {
            val count = colorCounts[color] ?: 0
            val candidates = mutableListOf<Goal>()
            // Shapes are hardest to accidentally form
            hardShapes.forEach { candidates.add(Goal.Shape(color, it)) }
            simpleShapes.forEach { candidates.add(Goal.Shape(color, it)) }
            if (count >= 4) candidates.add(Goal.Square(color))
            candidates.add(Goal.Line(color, 4))
            candidates.add(Goal.Line(color, 3))
            return candidates
        }

        // Pick goals one at a time, each verified unmet
        val picked = mutableListOf<Goal>()
        val usedColors = mutableSetOf<TileColor>()
        val shuffledColors = colors.shuffled()

        repeat(goalCount) {
            // Try each color, preferring unused ones
            val colorOrder = shuffledColors.sortedBy { if (it in usedColors) 1 else 0 }
            var found = false
            for (color in colorOrder) {
                for (candidate in candidatesForColor(color).shuffled()) {
                    // Skip duplicates
                    if (candidate in picked) continue
                    // Verify this goal is NOT already met on the board
                    val met = BoardEngine.evaluateGoals(board, listOf(candidate))
                    if (met.isEmpty()) {
                        picked.add(candidate)
                        usedColors.add(color)
                        found = true
                        break
                    }
                }
                if (found) break
            }
            // If no unmet goal found for any color, skip this slot
        }

        // Must return at least 1 goal — if nothing was unmet, use a shape (least likely met)
        if (picked.isEmpty()) {
            val color = shuffledColors.first()
            picked.add(Goal.Shape(color, hardShapes.random()))
        }

        return picked
    }

    // ── Blitz: timer-based, skill-scaled ──

    private fun generateBlitz(skill: Difficulty): Level {
        val w = when (skill) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 6
            Difficulty.HARD -> 7
            Difficulty.PRO_PLUS -> 8
        }
        val h = w
        val numColors = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 6
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

    // ── Overgrown: 9×9, 8 mixed goals, 16 moves, 10-14 frozen ──

    private fun generateOvergrown(skill: Difficulty): Level {
        val w = 9; val h = 9
        val colors = TileColor.entries.toList()
        val goalCount = when (skill) {
            Difficulty.EASY -> 6
            Difficulty.MEDIUM -> 8
            Difficulty.HARD -> 10
            Difficulty.PRO_PLUS -> 12
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        val frozenCount = when (skill) {
            Difficulty.EASY -> 8
            Difficulty.MEDIUM -> 10
            Difficulty.HARD -> 12
            Difficulty.PRO_PLUS -> 14
        }
        val frozen = pickFrozenCells(w, h, frozenCount)
        return Level(
            id = ChallengeType.OVERGROWN.id,
            world = 0,
            name = ChallengeType.OVERGROWN.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 16,
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
            Difficulty.PRO_PLUS -> 6
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 6
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
            Difficulty.PRO_PLUS -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
            Difficulty.PRO_PLUS -> 3
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

    // ── Frozen Wave: 6×6, no initial frozen, 14 moves ──

    private fun generateFrozenWave(skill: Difficulty): Level {
        val w = 6; val h = 6
        val numColors = when (skill) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 5
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        return Level(
            id = ChallengeType.FROZEN_WAVE.id,
            world = 0,
            name = ChallengeType.FROZEN_WAVE.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 14,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 4, threeStar = 8)
        )
    }

    // ── Rotation Garden: 6×6 square, 14 moves ──

    private fun generateRotation(skill: Difficulty): Level {
        val w = 6; val h = 6
        val numColors = when (skill) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 5
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        val frozenCount = when (skill) {
            Difficulty.EASY -> 0
            Difficulty.MEDIUM -> 1
            Difficulty.HARD -> 2
            Difficulty.PRO_PLUS -> 2
        }
        val frozen = if (frozenCount > 0) pickFrozenCells(w, h, frozenCount) else emptySet()
        return Level(
            id = ChallengeType.ROTATION.id,
            world = 0,
            name = ChallengeType.ROTATION.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 14,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 4, threeStar = 8),
            frozenCells = frozen
        )
    }

    // ── Mirror Garden: 6×6 even-width, 10 moves ──

    private fun generateMirror(skill: Difficulty): Level {
        val w = 6; val h = 6
        val numColors = when (skill) {
            Difficulty.EASY -> 4
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.PRO_PLUS -> 5
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = when (skill) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 5
            Difficulty.HARD -> 6
            Difficulty.PRO_PLUS -> 6
        }
        val goals = pickMixedGoals(colors, goalCount, skill)
        val tiles = generateRandomTiles(w, h, colors)
        return Level(
            id = ChallengeType.MIRROR.id,
            world = 0,
            name = ChallengeType.MIRROR.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 10,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 3, threeStar = 6)
        )
    }

    // ── Decay Garden: 6×6, simpler goals, 16 moves ──

    private fun generateDecay(skill: Difficulty): Level {
        val w = 6; val h = 6
        val numColors = when (skill) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 4
            Difficulty.PRO_PLUS -> 4
        }
        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goalCount = 4
        val goals = pickDecayGoals(colors, goalCount)
        val tiles = generateRandomTiles(w, h, colors)
        return Level(
            id = ChallengeType.DECAY.id,
            world = 0,
            name = ChallengeType.DECAY.title,
            boardWidth = w,
            boardHeight = h,
            maxMoves = 16,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(twoStar = 5, threeStar = 10)
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

    private fun pickDecayGoals(colors: List<TileColor>, count: Int): List<Goal> {
        val shuffled = colors.shuffled()
        val simpleShapes = listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        val picked = mutableListOf<Goal>()
        var colorIdx = 0
        var retries = 0
        while (picked.size < count && retries < count * 10) {
            val color = shuffled[colorIdx % shuffled.size]
            colorIdx++
            val roll = Math.random()
            val candidate = when {
                roll < 0.45 -> Goal.Line(color, 3)
                roll < 0.75 -> Goal.Square(color)
                else -> Goal.Shape(color, simpleShapes.random())
            }
            if (candidate !in picked) picked.add(candidate) else retries++
        }
        return picked
    }

    private fun pickMixedGoals(colors: List<TileColor>, count: Int, skill: Difficulty): List<Goal> {
        val shuffled = colors.shuffled()
        val simpleShapes = listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        val hardShapes = listOf(ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE)
        val picked = mutableListOf<Goal>()
        var colorIdx = 0
        var retries = 0
        while (picked.size < count && retries < count * 10) {
            val color = shuffled[colorIdx % shuffled.size]
            colorIdx++
            val roll = Math.random()
            val candidate = when (skill) {
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
                Difficulty.PRO_PLUS -> when {
                    roll < 0.15 -> Goal.Line(color, if (Math.random() < 0.5) 4 else 3)
                    roll < 0.3 -> Goal.Square(color)
                    roll < 0.6 -> Goal.Shape(color, simpleShapes.random())
                    else -> Goal.Shape(color, hardShapes.random())
                }
            }
            if (candidate !in picked) {
                picked.add(candidate)
            } else {
                retries++
            }
        }
        return picked
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
