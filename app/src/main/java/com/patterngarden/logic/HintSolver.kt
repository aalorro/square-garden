package com.patterngarden.logic

import com.patterngarden.model.*
import kotlin.math.max

object HintSolver {

    fun findBestSwap(
        board: Board,
        goals: List<Goal>,
        completedGoalIds: Set<String>
    ): Pair<CellPos, CellPos>? {
        val remainingGoals = goals.filter { it.id !in completedGoalIds }
        if (remainingGoals.isEmpty()) return null

        var bestSwap: Pair<CellPos, CellPos>? = null
        var bestScore = Int.MIN_VALUE

        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (board.isVoid(r, c) || board.tileAt(r, c).frozen) continue
                val from = CellPos(r, c)
                val neighbors = listOf(CellPos(r, c + 1), CellPos(r + 1, c))
                for (neighbor in neighbors) {
                    if (!BoardEngine.canSwap(board, from, neighbor)) continue

                    val swapped = BoardEngine.executeSwap(board, from, neighbor)
                    val newlyCompleted = BoardEngine.evaluateGoals(swapped, remainingGoals)
                    // Score: big bonus per completed goal + partial progress
                    val score = newlyCompleted.size * 1000 +
                            scorePartialProgress(swapped, remainingGoals, newlyCompleted)

                    if (score > bestScore) {
                        bestScore = score
                        bestSwap = Pair(from, neighbor)
                    }
                }
            }
        }

        return bestSwap
    }

    /**
     * Beam search solver: explores multiple paths in parallel to find a solution
     * that completes all goals within maxMoves.
     */
    fun findSolution(
        board: Board,
        goals: List<Goal>,
        maxMoves: Int
    ): List<Pair<CellPos, CellPos>> {
        val beamWidth = 25

        // Each search state tracks the board, cumulatively completed goals, and the swap sequence
        data class State(
            val board: Board,
            val completedIds: Set<String>,
            val steps: List<Pair<CellPos, CellPos>>
        )

        var beam = listOf(State(board, emptySet(), emptyList()))

        for (move in 0 until maxMoves) {
            val candidates = mutableListOf<State>()

            for (state in beam) {
                // Already solved
                if (state.completedIds.size == goals.size) return state.steps

                // Generate all valid swaps from this state
                for (r in 0 until state.board.height) {
                    for (c in 0 until state.board.width) {
                        if (state.board.isVoid(r, c) || state.board.tileAt(r, c).frozen) continue
                        val from = CellPos(r, c)
                        for (neighbor in listOf(CellPos(r, c + 1), CellPos(r + 1, c))) {
                            if (!BoardEngine.canSwap(state.board, from, neighbor)) continue

                            val newBoard = BoardEngine.executeSwap(state.board, from, neighbor)
                            // Cumulative goal tracking (matches actual game behavior)
                            val metNow = BoardEngine.evaluateGoals(newBoard, goals)
                            val newCompleted = state.completedIds + metNow

                            candidates.add(
                                State(
                                    newBoard,
                                    newCompleted,
                                    state.steps + Pair(from, neighbor)
                                )
                            )
                        }
                    }
                }
            }

            if (candidates.isEmpty()) break

            // Check for a full solution among candidates
            val solution = candidates.find { it.completedIds.size == goals.size }
            if (solution != null) return solution.steps

            // Score, deduplicate by completed goals count + progress, and keep top beamWidth
            beam = candidates
                .sortedByDescending { state ->
                    state.completedIds.size * 1000 +
                            scorePartialProgress(state.board, goals, state.completedIds)
                }
                .take(beamWidth)
        }

        // Return the best partial solution found
        return beam
            .maxByOrNull { state ->
                state.completedIds.size * 1000 +
                        scorePartialProgress(state.board, goals, state.completedIds)
            }
            ?.steps ?: emptyList()
    }

    /**
     * Scores how close the board is to completing remaining goals.
     * Higher score = closer to completion.
     */
    private fun scorePartialProgress(
        board: Board,
        goals: List<Goal>,
        completedIds: Set<String>
    ): Int {
        var score = 0
        for (goal in goals) {
            if (goal.id in completedIds) continue
            score += when (goal) {
                is Goal.Line -> scoreLineProgress(board, goal.color, goal.length)
                is Goal.Square -> scoreSquareProgress(board, goal.color)
                is Goal.Shape -> scoreShapeProgress(board, goal.color, goal.shapeType)
            }
        }
        return score
    }

    /** Best consecutive run of the target color in any row or column. */
    private fun scoreLineProgress(board: Board, color: TileColor, length: Int): Int {
        var maxRun = 0
        // Horizontal
        for (r in 0 until board.height) {
            var run = 0
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color) {
                    run++
                    maxRun = max(maxRun, run)
                } else run = 0
            }
        }
        // Vertical
        for (c in 0 until board.width) {
            var run = 0
            for (r in 0 until board.height) {
                if (!board.isVoid(r, c) && board.tileAt(r, c).color == color) {
                    run++
                    maxRun = max(maxRun, run)
                } else run = 0
            }
        }
        // Scale: e.g. 3/4 of a line-of-4 scores 75
        return if (length > 0) (maxRun * 100) / length else 0
    }

    /** Best 2x2 square overlap with the target color (0-4 matching cells). */
    private fun scoreSquareProgress(board: Board, color: TileColor): Int {
        var bestMatch = 0
        for (r in 0 until board.height - 1) {
            for (c in 0 until board.width - 1) {
                var match = 0
                for (dr in 0..1) for (dc in 0..1) {
                    val nr = r + dr; val nc = c + dc
                    if (!board.isVoid(nr, nc) && board.tileAt(nr, nc).color == color) match++
                }
                bestMatch = max(bestMatch, match)
            }
        }
        // Scale: 3/4 cells matching → 75
        return bestMatch * 25
    }

    /** Best shape placement overlap with the target color. */
    private fun scoreShapeProgress(board: Board, color: TileColor, shapeType: ShapeType): Int {
        val rotations = generateRotations(shapeType.offsets)
        var bestMatch = 0
        val totalCells = shapeType.offsets.size

        for (rotation in rotations) {
            for (r in 0 until board.height) {
                for (c in 0 until board.width) {
                    var match = 0
                    var valid = true
                    for (offset in rotation) {
                        val tr = r + offset.row; val tc = c + offset.col
                        if (tr !in 0 until board.height || tc !in 0 until board.width || board.isVoid(tr, tc)) {
                            valid = false; break
                        }
                        if (board.tileAt(tr, tc).color == color) match++
                    }
                    if (valid) bestMatch = max(bestMatch, match)
                }
            }
        }
        // Scale: 4/5 cells matching → 80
        return if (totalCells > 0) (bestMatch * 100) / totalCells else 0
    }

    /** Generate all rotations + mirror of shape offsets. */
    private fun generateRotations(offsets: List<CellPos>): List<List<CellPos>> {
        val all = mutableListOf<List<CellPos>>()
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
