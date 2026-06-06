package com.squaregarden.logic

import com.squaregarden.model.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Stateless generator for Master Mode levels. Produces [Level] instances with
 * tier-based parameters for infinite procedural puzzle generation.
 */
object MasterLevelGenerator {

    /** Master Mode levels use world = -1 as sentinel. */
    const val MASTER_WORLD = -1

    /** Level IDs for master mode: -100 - gamesPlayed */
    fun masterLevelId(gamesPlayed: Int): Int = -100 - gamesPlayed

    /**
     * Generate a Master Mode level.
     * @param gamesPlayed total master games played (used for tier weighting and level ID)
     * @param currentStreak current win streak (influences challenge probability)
     * @param skill player's skill level (only affects star earnings, NOT move budget)
     * @return the generated Level paired with the selected MasterTier
     */
    fun generateLevel(
        gamesPlayed: Int,
        currentStreak: Int,
        skill: Difficulty
    ): Pair<Level, MasterTier> {
        val tier = pickTier(gamesPlayed)
        val w = (tier.minWidth..tier.maxWidth).random()
        val h = (tier.minHeight..tier.maxHeight).random()
        val numColors = (tier.minColors..tier.maxColors).random()
        val goalCount = (tier.minGoals..tier.maxGoals).random()
        val frozenCount = (tier.minFrozen..tier.maxFrozen).random()

        val colors = TileColor.entries.toList().shuffled().take(numColors)
        val goals = pickMixedGoals(colors, goalCount)
        val tiles = generateRandomTiles(w, h, colors)
        val frozen = pickFrozenCells(w, h, frozenCount)

        // Estimate base moves from goal complexity, apply tier multiplier only
        val baseMoves = estimateBaseMoves(goals, w, h)
        val maxMoves = max(goalCount + 1, (baseMoves * tier.moveMultiplier).roundToInt())

        val level = Level(
            id = masterLevelId(gamesPlayed),
            world = MASTER_WORLD,
            name = tier.label,
            boardWidth = w,
            boardHeight = h,
            maxMoves = maxMoves,
            initialTiles = tiles,
            goals = goals,
            starThresholds = StarThresholds(
                twoStar = max(1, maxMoves / 2),
                threeStar = max(1, maxMoves / 4)
            ),
            frozenCells = frozen
        )
        return level to tier
    }

    /**
     * Determine if a challenge round should trigger.
     * ~10-25% chance after 5+ games, biased by streak. Weighted toward Blitz and Shifting.
     */
    fun shouldTriggerChallenge(gamesPlayed: Int, currentStreak: Int): ChallengeType? {
        if (gamesPlayed < 5) return null
        val baseChance = 0.10 + (currentStreak * 0.02).coerceAtMost(0.15)
        if (Math.random() > baseChance) return null
        // Weighted selection: Blitz 35%, Shifting 35%, Memory 15%, Overgrown 15%
        return when {
            Math.random() < 0.35 -> ChallengeType.BLITZ
            Math.random() < 0.54 -> ChallengeType.SHIFTING // 0.35 / 0.65 ≈ 0.54
            Math.random() < 0.50 -> ChallengeType.MEMORY
            else -> ChallengeType.OVERGROWN
        }
    }

    // ── Tier selection ──

    /**
     * Pick tier via weighted random. Early games favor easier tiers;
     * later games mix all tiers but never exclusively hard.
     */
    private fun pickTier(gamesPlayed: Int): MasterTier {
        val weights = when {
            gamesPlayed < 3 -> floatArrayOf(0.60f, 0.30f, 0.10f, 0.00f, 0.00f)
            gamesPlayed < 8 -> floatArrayOf(0.25f, 0.35f, 0.25f, 0.10f, 0.05f)
            gamesPlayed < 15 -> floatArrayOf(0.10f, 0.20f, 0.30f, 0.25f, 0.15f)
            else -> floatArrayOf(0.05f, 0.15f, 0.25f, 0.30f, 0.25f)
        }
        val roll = Math.random().toFloat()
        var cumulative = 0f
        for ((i, w) in weights.withIndex()) {
            cumulative += w
            if (roll < cumulative) return MasterTier.entries[i]
        }
        return MasterTier.entries.last()
    }

    // ── Goal generation (reuses ChallengeGenerator patterns) ──

    private fun pickMixedGoals(colors: List<TileColor>, count: Int): List<Goal> {
        val simpleShapes = listOf(ShapeType.L_SHAPE, ShapeType.T_SHAPE)
        val hardShapes = listOf(ShapeType.CROSS, ShapeType.Z_SHAPE, ShapeType.U_SHAPE)
        val picked = mutableListOf<Goal>()
        var colorIdx = 0
        var retries = 0
        while (picked.size < count && retries < count * 10) {
            val color = colors[colorIdx % colors.size]
            colorIdx++
            val roll = Math.random()
            // Master Mode: favor harder goals
            val candidate = when {
                roll < 0.15 -> Goal.Line(color, if (Math.random() < 0.5) 4 else 3)
                roll < 0.30 -> Goal.Square(color)
                roll < 0.60 -> Goal.Shape(color, simpleShapes.random())
                else -> Goal.Shape(color, hardShapes.random())
            }
            if (candidate !in picked) picked.add(candidate) else retries++
        }
        return picked
    }

    /** Estimate base moves needed from goal complexity + board size. */
    private fun estimateBaseMoves(goals: List<Goal>, w: Int, h: Int): Int {
        var moves = 0
        for (goal in goals) {
            moves += when (goal) {
                is Goal.Line -> goal.length
                is Goal.Square -> 3
                is Goal.Shape -> goal.shapeType.offsets.size
            }
        }
        // Scale by board size — larger boards need more moves to rearrange
        val sizeBonus = ((w + h) / 2) - 4
        return moves + sizeBonus
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
