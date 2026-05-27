package com.squaregarden.logic

import com.squaregarden.model.*
import kotlin.random.Random

object GoalSetGenerator {

    private enum class GoalType { LINE, SQUARE, SHAPE }

    private data class WorldConstraints(
        val colors: List<TileColor>,
        val allowedTypes: Set<GoalType>,
        val lineLengths: IntRange,
        val allowedShapes: List<ShapeType>
    )

    private val FOUR_COLORS = listOf(TileColor.RED, TileColor.BLUE, TileColor.GREEN, TileColor.YELLOW)
    private val FIVE_COLORS = TileColor.entries.toList()

    private val worldConstraints = mapOf(
        1 to WorldConstraints(FOUR_COLORS, setOf(GoalType.LINE), 3..5, emptyList()),
        2 to WorldConstraints(FOUR_COLORS, setOf(GoalType.LINE, GoalType.SQUARE), 4..5, emptyList()),
        3 to WorldConstraints(
            FOUR_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 3..5,
            listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        ),
        4 to WorldConstraints(
            FOUR_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 3..5,
            listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE, ShapeType.CROSS, ShapeType.Z_SHAPE)
        ),
        5 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 3..5,
            ShapeType.entries.toList()
        ),
        6 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 3..5,
            ShapeType.entries.toList()
        ),
        7 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 5..6,
            ShapeType.entries.toList()
        ),
        8 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 5..6,
            ShapeType.entries.toList()
        ),
        9 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 5..7,
            ShapeType.entries.toList()
        ),
        10 to WorldConstraints(
            FIVE_COLORS, setOf(GoalType.LINE, GoalType.SQUARE, GoalType.SHAPE), 6..7,
            ShapeType.entries.toList()
        )
    )

    /**
     * Generate 4 goal sets for a level: the original + 3 alternatives.
     * Tutorial levels return only the original set.
     * Casual players get a minimum of 3 goals per level after tutorials.
     * All alternate sets are validated to ensure the board has enough tiles of
     * each color to make the set solvable (sum-of-requirements per color).
     */
    fun generateGoalSets(level: Level, difficulty: Difficulty = Difficulty.MEDIUM): List<List<Goal>> {
        if (level.tutorialSteps != null) return listOf(level.goals)
        val minGoals = if (difficulty == Difficulty.EASY) 3 else 0
        val padded = if (minGoals > level.goals.size) padGoals(level, minGoals) else level.goals
        val paddedLevel = if (padded !== level.goals) level.copy(goals = padded) else level
        val available = availableTilesByColor(paddedLevel)
        return listOf(
            padded,
            generateAlternateSet(paddedLevel, 1, minGoals, available),
            generateAlternateSet(paddedLevel, 2, minGoals, available),
            generateAlternateSet(paddedLevel, 3, minGoals, available)
        )
    }

    /** Count playable tiles by color on the board (excludes void cells; includes frozen tiles). */
    private fun availableTilesByColor(level: Level): Map<TileColor, Int> {
        val counts = mutableMapOf<TileColor, Int>()
        for (r in level.initialTiles.indices) {
            for (c in level.initialTiles[r].indices) {
                if (CellPos(r, c) in level.voidCells) continue
                val color = level.initialTiles[r][c]
                counts[color] = (counts[color] ?: 0) + 1
            }
        }
        return counts
    }

    /** Number of tiles required to complete a goal. */
    private fun goalTileCount(goal: Goal): Int = when (goal) {
        is Goal.Line -> goal.length
        is Goal.Square -> 4
        is Goal.Shape -> goal.shapeType.offsets.size
    }

    /** True if the board has enough tiles of every color used by [goals] (sum-per-color). */
    private fun isFeasible(goals: List<Goal>, available: Map<TileColor, Int>): Boolean {
        val required = mutableMapOf<TileColor, Int>()
        for (g in goals) {
            required[g.color] = (required[g.color] ?: 0) + goalTileCount(g)
        }
        for ((color, need) in required) {
            if ((available[color] ?: 0) < need) return false
        }
        return true
    }

    /** Pad a goal list to the minimum count by generating extra goals. */
    private fun padGoals(level: Level, minGoals: Int): List<Goal> {
        val rng = Random(level.id.toLong() * 97)
        val constraints = worldConstraints[level.world] ?: worldConstraints[10]!!
        val available = availableTilesByColor(level)
        val goals = level.goals.toMutableList()
        val usedIds = goals.map { it.id }.toMutableSet()
        var retries = 0
        while (goals.size < minGoals && retries < 200) {
            val goal = generateSingleGoal(rng, constraints, 0.6f, 0.2f)
            if (goal.id !in usedIds && isFeasible(goals + goal, available)) {
                goals.add(goal)
                usedIds.add(goal.id)
            } else {
                retries++
            }
        }
        return goals
    }

    private fun generateAlternateSet(
        level: Level,
        setIndex: Int,
        minGoals: Int = 0,
        available: Map<TileColor, Int> = availableTilesByColor(level)
    ): List<Goal> {
        val rng = Random(level.id.toLong() * 31 + setIndex.toLong())
        val constraints = worldConstraints[level.world] ?: worldConstraints[10]!!
        val goalCount = maxOf(level.goals.size, minGoals)

        // Analyze original type distribution for weighted selection
        val lineCount = level.goals.count { it is Goal.Line }
        val squareCount = level.goals.count { it is Goal.Square }
        val shapeCount = level.goals.count { it is Goal.Shape }
        val total = (lineCount + squareCount + shapeCount).toFloat()

        val linePct = if (total > 0) lineCount / total else 0.5f
        val squarePct = if (total > 0) squareCount / total else 0.2f

        val goals = mutableListOf<Goal>()
        val usedIds = mutableSetOf<String>()
        var retries = 0

        while (goals.size < goalCount && retries < goalCount * 60) {
            val goal = generateSingleGoal(rng, constraints, linePct, squarePct)
            if (goal.id !in usedIds && isFeasible(goals + goal, available)) {
                goals.add(goal)
                usedIds.add(goal.id)
            } else {
                retries++
            }
        }

        // Fallback: mutate original goals' colors to fill remaining slots
        var fallbackIdx = 0
        while (goals.size < goalCount && fallbackIdx < level.goals.size) {
            val mutated = mutateGoalColor(level.goals[fallbackIdx], rng, constraints.colors, usedIds, goals, available)
            if (mutated.id !in usedIds && isFeasible(goals + mutated, available)) {
                goals.add(mutated)
                usedIds.add(mutated.id)
            }
            fallbackIdx++
        }

        // Last-resort fallback: if we still couldn't fill the set, copy from the original
        // (which is guaranteed solvable by the level designer). Avoids returning a short set.
        var origIdx = 0
        while (goals.size < goalCount && origIdx < level.goals.size) {
            val orig = level.goals[origIdx]
            if (orig.id !in usedIds) {
                goals.add(orig)
                usedIds.add(orig.id)
            }
            origIdx++
        }

        return goals
    }

    private fun generateSingleGoal(
        rng: Random,
        constraints: WorldConstraints,
        linePct: Float,
        squarePct: Float
    ): Goal {
        val color = constraints.colors[rng.nextInt(constraints.colors.size)]

        // Weighted type selection with noise for variety
        val roll = rng.nextFloat()
        val adjLinePct = (linePct + rng.nextFloat() * 0.3f - 0.15f).coerceIn(0f, 1f)
        val adjSquarePct = (squarePct + rng.nextFloat() * 0.3f - 0.15f).coerceIn(0f, 1f)

        return when {
            roll < adjLinePct && GoalType.LINE in constraints.allowedTypes -> {
                val length = rng.nextInt(constraints.lineLengths.first, constraints.lineLengths.last + 1)
                Goal.Line(color, length)
            }
            roll < adjLinePct + adjSquarePct && GoalType.SQUARE in constraints.allowedTypes -> {
                Goal.Square(color)
            }
            GoalType.SHAPE in constraints.allowedTypes && constraints.allowedShapes.isNotEmpty() -> {
                val shape = constraints.allowedShapes[rng.nextInt(constraints.allowedShapes.size)]
                Goal.Shape(color, shape)
            }
            GoalType.LINE in constraints.allowedTypes -> {
                val length = rng.nextInt(constraints.lineLengths.first, constraints.lineLengths.last + 1)
                Goal.Line(color, length)
            }
            else -> Goal.Line(color, 3)
        }
    }

    private fun mutateGoalColor(
        original: Goal,
        rng: Random,
        colors: List<TileColor>,
        usedIds: Set<String>,
        currentGoals: List<Goal> = emptyList(),
        available: Map<TileColor, Int> = emptyMap()
    ): Goal {
        for (color in colors.shuffled(rng)) {
            val mutated = when (original) {
                is Goal.Line -> Goal.Line(color, original.length)
                is Goal.Square -> Goal.Square(color)
                is Goal.Shape -> Goal.Shape(color, original.shapeType)
            }
            if (mutated.id !in usedIds && (available.isEmpty() || isFeasible(currentGoals + mutated, available))) {
                return mutated
            }
        }
        return original
    }
}
