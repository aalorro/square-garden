package com.squaregarden.logic

import com.squaregarden.model.*
import kotlin.math.abs

object PatternMatcher {

    fun isGoalMet(board: Board, goal: Goal, excludedCells: Set<CellPos> = emptySet()): Boolean = when (goal) {
        is Goal.Line -> hasLine(board, goal.color, goal.length, excludedCells)
        is Goal.Square -> hasSquare(board, goal.color, excludedCells)
        is Goal.Shape -> hasShape(board, goal.color, goal.shapeType, excludedCells)
    }

    fun hasLine(board: Board, color: TileColor, length: Int, excludedCells: Set<CellPos> = emptySet()): Boolean {
        // Horizontal scan
        for (r in 0 until board.height) {
            var run = 0
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color && CellPos(r, c) !in excludedCells) {
                    run++
                    if (run >= length) return true
                } else run = 0
            }
        }
        // Vertical scan
        for (c in 0 until board.width) {
            var run = 0
            for (r in 0 until board.height) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color && CellPos(r, c) !in excludedCells) {
                    run++
                    if (run >= length) return true
                } else run = 0
            }
        }
        return false
    }

    fun hasSquare(board: Board, color: TileColor, excludedCells: Set<CellPos> = emptySet()): Boolean {
        for (r in 0 until board.height - 1) {
            for (c in 0 until board.width - 1) {
                if (board.isVoid(r, c) || board.isVoid(r, c + 1) ||
                    board.isVoid(r + 1, c) || board.isVoid(r + 1, c + 1)) continue
                val cells = listOf(CellPos(r, c), CellPos(r, c + 1), CellPos(r + 1, c), CellPos(r + 1, c + 1))
                if (cells.any { it in excludedCells }) continue
                if (board.tileAt(r, c).color == color &&
                    board.tileAt(r, c + 1).color == color &&
                    board.tileAt(r + 1, c).color == color &&
                    board.tileAt(r + 1, c + 1).color == color
                ) return true
            }
        }
        return false
    }

    fun hasShape(board: Board, color: TileColor, shapeType: ShapeType, excludedCells: Set<CellPos> = emptySet()): Boolean {
        val rotations = generateRotations(shapeType.offsets)
        for (rotation in rotations) {
            for (r in 0 until board.height) {
                for (c in 0 until board.width) {
                    if (rotation.all { offset ->
                            val tr = r + offset.row
                            val tc = c + offset.col
                            tr in 0 until board.height &&
                                    tc in 0 until board.width &&
                                    !board.isVoid(tr, tc) &&
                                    board.tileAt(tr, tc).color == color &&
                                    CellPos(tr, tc) !in excludedCells
                        }) return true
                }
            }
        }
        return false
    }

    // ── Position-returning variants ──

    fun findGoalPositions(board: Board, goal: Goal, excludedCells: Set<CellPos> = emptySet()): Set<CellPos>? = when (goal) {
        is Goal.Line -> findLinePositions(board, goal.color, goal.length, excludedCells)
        is Goal.Square -> findSquarePositions(board, goal.color, excludedCells)
        is Goal.Shape -> findShapePositions(board, goal.color, goal.shapeType, excludedCells)
    }

    fun findLinePositions(board: Board, color: TileColor, length: Int, excludedCells: Set<CellPos> = emptySet()): Set<CellPos>? {
        // Horizontal scan
        for (r in 0 until board.height) {
            var run = 0
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color && CellPos(r, c) !in excludedCells) {
                    run++
                    if (run >= length) {
                        return (c - length + 1..c).map { CellPos(r, it) }.toSet()
                    }
                } else run = 0
            }
        }
        // Vertical scan
        for (c in 0 until board.width) {
            var run = 0
            for (r in 0 until board.height) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color && CellPos(r, c) !in excludedCells) {
                    run++
                    if (run >= length) {
                        return (r - length + 1..r).map { CellPos(it, c) }.toSet()
                    }
                } else run = 0
            }
        }
        return null
    }

    fun findSquarePositions(board: Board, color: TileColor, excludedCells: Set<CellPos> = emptySet()): Set<CellPos>? {
        for (r in 0 until board.height - 1) {
            for (c in 0 until board.width - 1) {
                if (board.isVoid(r, c) || board.isVoid(r, c + 1) ||
                    board.isVoid(r + 1, c) || board.isVoid(r + 1, c + 1)) continue
                val cells = listOf(CellPos(r, c), CellPos(r, c + 1), CellPos(r + 1, c), CellPos(r + 1, c + 1))
                if (cells.any { it in excludedCells }) continue
                if (board.tileAt(r, c).color == color &&
                    board.tileAt(r, c + 1).color == color &&
                    board.tileAt(r + 1, c).color == color &&
                    board.tileAt(r + 1, c + 1).color == color
                ) {
                    return cells.toSet()
                }
            }
        }
        return null
    }

    fun findShapePositions(board: Board, color: TileColor, shapeType: ShapeType, excludedCells: Set<CellPos> = emptySet()): Set<CellPos>? {
        val rotations = generateRotations(shapeType.offsets)
        for (rotation in rotations) {
            for (r in 0 until board.height) {
                for (c in 0 until board.width) {
                    if (rotation.all { offset ->
                            val tr = r + offset.row
                            val tc = c + offset.col
                            tr in 0 until board.height &&
                                    tc in 0 until board.width &&
                                    !board.isVoid(tr, tc) &&
                                    board.tileAt(tr, tc).color == color &&
                                    CellPos(tr, tc) !in excludedCells
                        }) {
                        return rotation.map { CellPos(r + it.row, c + it.col) }.toSet()
                    }
                }
            }
        }
        return null
    }

    private fun generateRotations(offsets: List<CellPos>): List<List<CellPos>> {
        val all = mutableListOf<List<CellPos>>()
        // Generate 4 rotations of original + 4 rotations of mirror (horizontal flip)
        for (base in listOf(offsets, offsets.map { CellPos(it.row, -it.col) })) {
            var current = normalize(base)
            all.add(current)
            repeat(3) {
                current = normalize(current.map { CellPos(it.col, -it.row) })
                all.add(current)
            }
        }
        return all.distinctBy { it.sortedBy { p -> p.row * 100 + p.col } }
    }

    private fun normalize(offsets: List<CellPos>): List<CellPos> {
        val minR = offsets.minOf { it.row }
        val minC = offsets.minOf { it.col }
        return offsets.map { CellPos(it.row - minR, it.col - minC) }
    }
}
