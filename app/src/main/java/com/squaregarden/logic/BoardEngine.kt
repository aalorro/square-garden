package com.squaregarden.logic

import com.squaregarden.model.*
import kotlin.math.abs

object BoardEngine {

    fun isAdjacentSwap(a: CellPos, b: CellPos): Boolean {
        return (abs(a.row - b.row) + abs(a.col - b.col)) == 1
    }

    /** Diagonal-or-orthogonal adjacency: Chebyshev distance == 1 (8 neighbors). */
    fun isDiagonalAdjacent(a: CellPos, b: CellPos): Boolean {
        val dr = abs(a.row - b.row)
        val dc = abs(a.col - b.col)
        return (dr <= 1 && dc <= 1) && (dr != 0 || dc != 0)
    }

    fun canSwap(board: Board, a: CellPos, b: CellPos, allowDiagonal: Boolean = false): Boolean {
        val adjacent = if (allowDiagonal) isDiagonalAdjacent(a, b) else isAdjacentSwap(a, b)
        if (!adjacent) return false
        if (!board.isValidCell(a.row, a.col) || !board.isValidCell(b.row, b.col)) return false
        if (board.tileAt(a.row, a.col).frozen || board.tileAt(b.row, b.col).frozen) return false
        return true
    }

    fun executeSwap(board: Board, a: CellPos, b: CellPos): Board {
        return board.withSwap(a.row, a.col, b.row, b.col)
    }

    fun evaluateGoals(board: Board, goals: List<Goal>, excludedCells: Set<CellPos> = emptySet()): Set<String> {
        return goals.filter { PatternMatcher.isGoalMet(board, it, excludedCells) }
            .map { it.id }
            .toSet()
    }

    fun checkWin(completedGoals: Set<String>, allGoals: List<Goal>): Boolean {
        return allGoals.all { it.id in completedGoals }
    }

    fun checkLose(movesRemaining: Int, won: Boolean): Boolean {
        return movesRemaining <= 0 && !won
    }

    fun calculateStars(movesRemaining: Int, thresholds: StarThresholds): Int {
        return when {
            movesRemaining >= thresholds.threeStar -> 3
            movesRemaining >= thresholds.twoStar -> 2
            else -> 1
        }
    }
}
