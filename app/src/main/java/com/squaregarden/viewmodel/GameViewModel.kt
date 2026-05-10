package com.squaregarden.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.squaregarden.audio.AudioManager
import com.squaregarden.audio.MusicManager
import com.squaregarden.data.PlayGamesManager
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.logic.BoardEngine
import com.squaregarden.logic.ChallengeGenerator
import com.squaregarden.logic.HintSolver
import com.squaregarden.logic.LevelLoader
import com.squaregarden.logic.PatternMatcher
import com.squaregarden.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class GameViewModel(
    private val context: Context,
    private val levelId: Int
) : ViewModel() {

    private lateinit var level: Level
    private var difficulty: Difficulty = Difficulty.MEDIUM
    private var adjustedMaxMoves: Int = 0
    private var hasMovedSinceReset: Boolean = false
    private var winResultCommitted: Boolean = false
    private var pendingWinLevelId: Int = 0
    private var pendingWinStars: Int = 0
    private var precomputedSolution: List<Pair<CellPos, CellPos>>? = null
    private var redoFullReset: Boolean = false
    private var shuffleTokens: Int = 0
    private var passthroughTokens: Int = 0
    private var unfreezeTokens: Int = 0
    private var redoTokens: Int = 0
    private val progressRepo = ProgressRepository(context)
    private val profileRepo = ProfileRepository(context)
    private val audioManager = AudioManager(context)
    private var usedPowerUpThisGame: Boolean = false
    private var blitzTimerJob: Job? = null
    private var blitzTimerStarted: Boolean = false
    var activity: Activity? = null

    private val _state = MutableStateFlow(
        GameState(
            level = Level(0, 0, "", 5, 5, 0, emptyList(), emptyList(), StarThresholds(0, 0)),
            board = Board(5, 5, List(5) { List(5) { Tile(TileColor.RED) } }),
            movesRemaining = 0
        )
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        audioManager.observeSettings(viewModelScope)
        viewModelScope.launch {
            val profile = profileRepo.loadProfile()
            difficulty = Difficulty.fromId(profile.difficulty)

            shuffleTokens = progressRepo.shuffleTokensFlow.first()
            passthroughTokens = progressRepo.passthroughTokensFlow.first()
            unfreezeTokens = progressRepo.unfreezeTokensFlow.first()
            redoTokens = progressRepo.redoTokensFlow.first()

            val challengeType = ChallengeType.fromId(levelId)
            if (challengeType != null) {
                level = ChallengeGenerator.generateLevel(challengeType, difficulty)
                adjustedMaxMoves = level.maxMoves // No difficulty adjustment for challenges
                initLevel(challengeType)
            } else {
                val levels = LevelLoader.loadAllLevels(context)
                level = levels.first { it.id == levelId }
                adjustedMaxMoves = max(1, (level.maxMoves * difficulty.moveMultiplier).roundToInt())
                initLevel()
            }
        }
    }

    // ── Board generation: reverse-construction approach ──

    /**
     * Build a board where all goals are simultaneously met by placing goal
     * patterns explicitly, then filling the rest with safe random colors.
     */
    private fun buildSolvedBoard(): Board? {
        val w = level.boardWidth
        val h = level.boardHeight
        val voids = level.voidCells
        val frozenPositions = level.frozenCells

        val grid = Array(h) { arrayOfNulls<TileColor>(w) }

        // Place each goal's pattern (shuffled order for variety)
        for (goal in level.goals.shuffled()) {
            if (!placeGoalOnGrid(grid, w, h, goal, voids)) return null
        }

        // Fill remaining cells, avoiding accidental runs of 3+
        val allColors = levelColors()
        for (r in 0 until h) {
            for (c in 0 until w) {
                if (grid[r][c] != null || CellPos(r, c) in voids) continue
                val forbidden = mutableSetOf<TileColor>()
                // Don't extend horizontal run to 3
                if (c >= 2 && grid[r][c - 1] != null && grid[r][c - 1] == grid[r][c - 2])
                    forbidden.add(grid[r][c - 1]!!)
                // Don't extend vertical run to 3
                if (r >= 2 && grid[r - 1][c] != null && grid[r - 1][c] == grid[r - 2][c])
                    forbidden.add(grid[r - 1][c]!!)
                val available = allColors.filter { it !in forbidden }
                grid[r][c] = if (available.isNotEmpty()) available.random() else allColors.random()
            }
        }

        val tiles = (0 until h).map { r ->
            (0 until w).map { c ->
                val pos = CellPos(r, c)
                if (pos in voids) Tile(TileColor.RED)
                else {
                    val tile = Tile(grid[r][c]!!)
                    if (pos in frozenPositions) tile.copy(frozen = true) else tile
                }
            }
        }
        return Board(w, h, tiles, voids)
    }

    private fun placeGoalOnGrid(
        grid: Array<Array<TileColor?>>, w: Int, h: Int,
        goal: Goal, voids: Set<CellPos>
    ): Boolean {
        val candidates = mutableListOf<List<CellPos>>()

        when (goal) {
            is Goal.Line -> {
                // Horizontal
                for (r in 0 until h) {
                    for (c in 0..w - goal.length) {
                        val cells = (c until c + goal.length).map { CellPos(r, it) }
                        if (cells.all { it !in voids && (grid[it.row][it.col] == null || grid[it.row][it.col] == goal.color) })
                            candidates.add(cells)
                    }
                }
                // Vertical
                for (c in 0 until w) {
                    for (r in 0..h - goal.length) {
                        val cells = (r until r + goal.length).map { CellPos(it, c) }
                        if (cells.all { it !in voids && (grid[it.row][it.col] == null || grid[it.row][it.col] == goal.color) })
                            candidates.add(cells)
                    }
                }
            }
            is Goal.Square -> {
                for (r in 0 until h - 1) {
                    for (c in 0 until w - 1) {
                        val cells = listOf(CellPos(r, c), CellPos(r, c + 1), CellPos(r + 1, c), CellPos(r + 1, c + 1))
                        if (cells.all { it !in voids && (grid[it.row][it.col] == null || grid[it.row][it.col] == goal.color) })
                            candidates.add(cells)
                    }
                }
            }
            is Goal.Shape -> {
                for (rotation in shapeRotations(goal.shapeType.offsets)) {
                    for (r in 0 until h) {
                        for (c in 0 until w) {
                            val cells = rotation.map { CellPos(r + it.row, c + it.col) }
                            if (cells.all {
                                    it.row in 0 until h && it.col in 0 until w &&
                                            it !in voids &&
                                            (grid[it.row][it.col] == null || grid[it.row][it.col] == goal.color)
                                }) candidates.add(cells)
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return false
        val chosen = candidates.random()
        for (cell in chosen) grid[cell.row][cell.col] = goal.color
        return true
    }

    /**
     * Scramble a solved board with exactly [numSwaps] random valid swaps.
     * Returns the scrambled board and the list of swaps performed.
     * Avoids immediately undoing the previous swap.
     */
    private fun scrambleBoard(
        board: Board, numSwaps: Int, protectedCells: Set<CellPos> = emptySet()
    ): Pair<Board, List<Pair<CellPos, CellPos>>> {
        var current = board
        val swaps = mutableListOf<Pair<CellPos, CellPos>>()

        for (i in 0 until numSwaps) {
            val valid = mutableListOf<Pair<CellPos, CellPos>>()
            for (r in 0 until current.height) {
                for (c in 0 until current.width) {
                    if (current.isVoid(r, c) || current.tileAt(r, c).frozen) continue
                    val from = CellPos(r, c)
                    if (from in protectedCells) continue
                    for (nb in listOf(CellPos(r, c + 1), CellPos(r + 1, c))) {
                        if (!BoardEngine.canSwap(current, from, nb)) continue
                        if (nb in protectedCells) continue
                        // Skip if it would just undo the last swap
                        if (swaps.isNotEmpty()) {
                            val last = swaps.last()
                            if (from == last.first && nb == last.second) continue
                            if (from == last.second && nb == last.first) continue
                        }
                        valid.add(Pair(from, nb))
                    }
                }
            }
            if (valid.isEmpty()) break
            val swap = valid.random()
            current = BoardEngine.executeSwap(current, swap.first, swap.second)
            swaps.add(swap)
        }
        return Pair(current, swaps)
    }

    /**
     * Generate a board with a guaranteed solution using reverse-construction:
     * 1. Build a board where all goals are met
     * 2. Scramble it with [moves] random swaps
     * 3. The reversed swaps = the solution
     *
     * Falls back to random board + async solver if construction fails.
     */
    private fun generateBoardWithSolution(moves: Int): Pair<Board, List<Pair<CellPos, CellPos>>?> {
        repeat(50) {
            val solved = buildSolvedBoard() ?: return@repeat
            // Verify all goals actually met
            if (BoardEngine.evaluateGoals(solved, level.goals).size != level.goals.size) return@repeat

            val (scrambled, swaps) = scrambleBoard(solved, moves)
            if (swaps.size < moves) return@repeat // not enough valid swaps

            // Reject if any goal is already met on the scrambled board
            if (BoardEngine.evaluateGoals(scrambled, level.goals).isNotEmpty()) return@repeat

            val solution = swaps.reversed()
            return Pair(placeTokenTiles(scrambled), solution)
        }
        // Fallback: random board, solver will try in background
        return Pair(placeTokenTiles(generateValidBoard()), null)
    }

    // Shape rotation helpers (mirrors PatternMatcher logic)
    private fun shapeRotations(offsets: List<CellPos>): List<List<CellPos>> {
        val all = mutableListOf<List<CellPos>>()
        for (base in listOf(offsets, offsets.map { CellPos(it.row, -it.col) })) {
            var cur = normalize(base)
            all.add(cur)
            repeat(3) {
                cur = normalize(cur.map { CellPos(it.col, -it.row) })
                all.add(cur)
            }
        }
        return all.distinctBy { it.sortedBy { p -> p.row * 100 + p.col } }
    }

    private fun normalize(offsets: List<CellPos>): List<CellPos> {
        val minR = offsets.minOf { it.row }
        val minC = offsets.minOf { it.col }
        return offsets.map { CellPos(it.row - minR, it.col - minC) }
    }

    // ── Fallback: old random board (no guaranteed solution) ──

    /** Colors actually used in this level (from initial tiles + goals). */
    private fun levelColors(): Array<TileColor> {
        val colors = mutableSetOf<TileColor>()
        for (row in level.initialTiles) for (c in row) colors.add(c)
        for (goal in level.goals) colors.add(goal.color)
        return if (colors.isNotEmpty()) colors.toTypedArray() else TileColor.entries.toTypedArray()
    }

    private fun generateValidBoard(): Board {
        val colors = levelColors()
        val minRequired = mutableMapOf<TileColor, Int>()
        for (goal in level.goals) {
            val needed = when (goal) {
                is Goal.Line -> goal.length
                is Goal.Square -> 4
                is Goal.Shape -> goal.shapeType.offsets.size
            }
            minRequired[goal.color] = max(minRequired[goal.color] ?: 0, needed)
        }
        val voids = level.voidCells
        val frozenPositions = level.frozenCells
        val playableCells = level.boardWidth * level.boardHeight - voids.size

        repeat(200) {
            val tileList = mutableListOf<Tile>()
            for ((color, count) in minRequired) repeat(count) { tileList.add(Tile(color)) }
            while (tileList.size < playableCells) tileList.add(Tile(colors.random()))
            tileList.shuffle()
            var idx = 0
            val tiles = (0 until level.boardHeight).map { r ->
                (0 until level.boardWidth).map { c ->
                    val pos = CellPos(r, c)
                    if (pos in voids) Tile(TileColor.RED)
                    else {
                        val tile = tileList[idx++]
                        if (pos in frozenPositions) tile.copy(frozen = true) else tile
                    }
                }
            }
            val board = Board(level.boardWidth, level.boardHeight, tiles, voids)
            if (BoardEngine.evaluateGoals(board, level.goals).isEmpty()) return board
        }
        // Last resort fallback
        val tileList = mutableListOf<Tile>()
        for ((color, count) in minRequired) repeat(count) { tileList.add(Tile(color)) }
        while (tileList.size < playableCells) tileList.add(Tile(colors.random()))
        tileList.shuffle()
        var idx = 0
        val tiles = (0 until level.boardHeight).map { r ->
            (0 until level.boardWidth).map { c ->
                val pos = CellPos(r, c)
                if (pos in voids) Tile(TileColor.RED)
                else {
                    val tile = tileList[idx++]
                    if (pos in frozenPositions) tile.copy(frozen = true) else tile
                }
            }
        }
        return Board(level.boardWidth, level.boardHeight, tiles, voids)
    }

    /** Place token tiles on random non-frozen, non-void cells (World 4+, ~25% chance each). */
    private fun placeTokenTiles(board: Board): Board {
        if (level.world < 4) return board
        val candidates = mutableListOf<CellPos>()
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (board.isVoid(r, c)) continue
                if (board.tileAt(r, c).frozen) continue
                candidates.add(CellPos(r, c))
            }
        }
        if (candidates.isEmpty()) return board

        // Each token type rolls independently (~25% chance each)
        val redoPos = if ((1..4).random() == 1) candidates.random() else null
        val shufflePos = if ((1..4).random() == 1) candidates.random() else null
        val ptPos = if ((1..4).random() == 1) candidates.random() else null
        val ufPos = if ((1..4).random() == 1) candidates.random() else null

        val newTiles = board.tiles.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                val pos = CellPos(r, c)
                tile.copy(
                    redo = tile.redo || pos == redoPos,
                    shuffleToken = tile.shuffleToken || pos == shufflePos,
                    passthroughToken = tile.passthroughToken || pos == ptPos,
                    unfreezeToken = tile.unfreezeToken || pos == ufPos
                )
            }
        }
        return board.copy(tiles = newTiles)
    }

    // ── Async fallback solver (for boards not built via reverse-construction) ──

    private fun computeSolutionAsync(board: Board) {
        precomputedSolution = null
        viewModelScope.launch {
            val solution = withContext(Dispatchers.Default) {
                HintSolver.findSolution(board, level.goals, level.maxMoves)
            }
            if (solution.isNotEmpty()) {
                precomputedSolution = solution
                val current = _state.value
                if (current.initialBoard == board) {
                    _state.value = current.copy(hasSolution = true)
                }
            }
        }
    }

    // ── Level lifecycle ──

    private fun computeGameDifficulty(board: Board): GameDifficulty {
        return GameDifficulty.calculate(
            board = board,
            maxMoves = adjustedMaxMoves,
            goals = level.goals,
            frozenCount = level.frozenCells.size,
            voidCount = level.voidCells.size,
            skill = difficulty
        )
    }

    private fun initLevel(challengeType: ChallengeType? = null) {
        usedPowerUpThisGame = false
        val hasTutorial = level.tutorialSteps != null

        if (hasTutorial) {
            val board = Board(
                width = level.boardWidth,
                height = level.boardHeight,
                tiles = level.initialTiles.mapIndexed { r, row ->
                    row.mapIndexed { c, color ->
                        val frozen = CellPos(r, c) in level.frozenCells
                        Tile(color, frozen)
                    }
                },
                voids = level.voidCells
            )
            val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
            _state.value = GameState(
                level = adjustedLevel, board = board,
                movesRemaining = adjustedMaxMoves, difficulty = difficulty,
                gameDifficulty = computeGameDifficulty(board),
                initialBoard = board,
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
                phase = GamePhase.TUTORIAL_PAUSE
            )
            computeSolutionAsync(board)
        } else {
            val chalState = when (challengeType) {
                ChallengeType.BLITZ -> ChallengeState(type = ChallengeType.BLITZ, timerMillisRemaining = 60_000L)
                ChallengeType.OVERGROWN -> ChallengeState(type = ChallengeType.OVERGROWN)
                ChallengeType.SHIFTING -> ChallengeState(type = ChallengeType.SHIFTING)
                ChallengeType.MEMORY -> ChallengeState(type = ChallengeType.MEMORY)
                null -> null
            }

            // Blitz uses the board directly from ChallengeGenerator (goals verified not pre-met)
            val board: Board
            val solution: List<Pair<CellPos, CellPos>>?
            if (challengeType == ChallengeType.BLITZ) {
                board = Board(
                    width = level.boardWidth, height = level.boardHeight,
                    tiles = level.initialTiles.map { row -> row.map { Tile(it) } }
                )
                solution = null
            } else {
                val result = generateBoardWithSolution(adjustedMaxMoves)
                board = result.first
                solution = result.second
            }
            precomputedSolution = solution
            val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)

            _state.value = GameState(
                level = adjustedLevel, board = board,
                movesRemaining = adjustedMaxMoves, difficulty = difficulty,
                gameDifficulty = computeGameDifficulty(board),
                initialBoard = board, hasSolution = solution != null,
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
                phase = GamePhase.SCRAMBLING,
                challengeState = chalState
            )
            // If reverse-construction failed, try solver in background
            if (solution == null && challengeType != ChallengeType.BLITZ) computeSolutionAsync(board)
            viewModelScope.launch {
                animateScramble(board)
                // Start challenge-specific setup after scramble
                if (challengeType == ChallengeType.MEMORY) startMemoryReveal()
            }
        }
    }

    private suspend fun animateScramble(finalBoard: Board) {
        // Total animation = 4000ms to match scramble sound duration
        audioManager.playScramble()
        var displayBoard = randomizeBoard(finalBoard)
        _state.value = _state.value.copy(board = displayBoard, phase = GamePhase.SCRAMBLING)

        // Fast phase: 25 swaps over 2000ms
        repeat(25) {
            displayBoard = randomSwap(displayBoard)
            _state.value = _state.value.copy(board = displayBoard)
            delay(80)
        }
        // Slow phase: 8 swaps over 1000ms
        repeat(8) {
            displayBoard = randomSwap(displayBoard)
            _state.value = _state.value.copy(board = displayBoard)
            delay(125)
        }
        // Settle phase: 1000ms — progressively place tiles into final positions
        val misplaced = findMisplacedTiles(displayBoard, finalBoard).toMutableList()
        misplaced.shuffle()
        val settleCount = misplaced.size.coerceAtMost(10)
        val settleInterval = if (settleCount > 0) 1000L / settleCount else 0L
        for (idx in 0 until settleCount) {
            displayBoard = placeCorrectTile(displayBoard, finalBoard, misplaced[idx])
            _state.value = _state.value.copy(board = displayBoard)
            delay(settleInterval)
        }
        _state.value = _state.value.copy(board = finalBoard, phase = GamePhase.PLAYING)
    }

    /** Find positions where displayBoard differs from finalBoard. */
    private fun findMisplacedTiles(display: Board, target: Board): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until display.height) {
            for (c in 0 until display.width) {
                if (display.isVoid(r, c)) continue
                if (display.tileAt(r, c).color != target.tileAt(r, c).color) {
                    result.add(r to c)
                }
            }
        }
        return result
    }

    /** Swap the tile at pos with whatever tile currently holds the correct color for that pos. */
    private fun placeCorrectTile(display: Board, target: Board, pos: Pair<Int, Int>): Board {
        val (tr, tc) = pos
        val targetColor = target.tileAt(tr, tc).color
        if (display.tileAt(tr, tc).color == targetColor) return display
        // Find a tile that has the target color but is in the wrong place
        for (r in 0 until display.height) {
            for (c in 0 until display.width) {
                if (display.isVoid(r, c)) continue
                if (r == tr && c == tc) continue
                if (display.tileAt(r, c).color == targetColor && display.tileAt(r, c).color != target.tileAt(r, c).color) {
                    return display.withSwap(tr, tc, r, c)
                }
            }
        }
        // Fallback: just swap with any tile that has the right color
        for (r in 0 until display.height) {
            for (c in 0 until display.width) {
                if (display.isVoid(r, c)) continue
                if (r == tr && c == tc) continue
                if (display.tileAt(r, c).color == targetColor) {
                    return display.withSwap(tr, tc, r, c)
                }
            }
        }
        return display
    }

    private fun randomizeBoard(board: Board): Board {
        val playable = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c)) playable.add(r to c)
            }
        }
        val tiles = playable.map { (r, c) -> board.tileAt(r, c) }.toMutableList()
        // Fisher-Yates shuffle
        for (i in tiles.lastIndex downTo 1) {
            val j = (0..i).random()
            val tmp = tiles[i]; tiles[i] = tiles[j]; tiles[j] = tmp
        }
        val mutable = board.tiles.map { it.toMutableList() }
        playable.forEachIndexed { idx, (r, c) ->
            val original = board.tileAt(r, c)
            mutable[r][c] = tiles[idx].copy(frozen = original.frozen)
        }
        return board.copy(tiles = mutable.map { it.toList() })
    }

    private fun randomSwap(board: Board): Board {
        val playable = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (!board.isVoid(r, c)) playable.add(r to c)
            }
        }
        if (playable.size < 2) return board
        val (r1, c1) = playable.random()
        var r2: Int; var c2: Int
        do {
            val p = playable.random(); r2 = p.first; c2 = p.second
        } while (r1 == r2 && c1 == c2)
        return board.withSwap(r1, c1, r2, c2)
    }

    fun resetLevel() {
        val current = _state.value
        val hasTutorial = level.tutorialSteps != null

        val board: Board
        var solution: List<Pair<CellPos, CellPos>>? = null

        if (hasTutorial) {
            board = Board(
                width = level.boardWidth, height = level.boardHeight,
                tiles = level.initialTiles.mapIndexed { r, row ->
                    row.mapIndexed { c, color ->
                        val frozen = CellPos(r, c) in level.frozenCells
                        Tile(color, frozen)
                    }
                },
                voids = level.voidCells
            )
        } else if (hasMovedSinceReset) {
            val moves = when (difficulty) {
                Difficulty.EASY -> adjustedMaxMoves
                Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
                Difficulty.HARD -> adjustedMaxMoves
            }
            val result = generateBoardWithSolution(moves)
            board = result.first
            solution = result.second
        } else {
            board = current.board
            solution = precomputedSolution
        }

        precomputedSolution = solution
        val moves = if (redoFullReset) adjustedMaxMoves else when (difficulty) {
            Difficulty.EASY -> adjustedMaxMoves
            Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
            Difficulty.HARD -> if (current.movesRemaining > 0) current.movesRemaining else adjustedMaxMoves
        }
        val shouldScramble = !hasTutorial && hasMovedSinceReset
        hasMovedSinceReset = false
        val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
        _state.value = GameState(
            level = adjustedLevel, board = board,
            movesRemaining = moves, difficulty = difficulty,
            gameDifficulty = computeGameDifficulty(board),
            initialBoard = board, hasSolution = solution != null,
            shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
            phase = if (hasTutorial) GamePhase.TUTORIAL_PAUSE else if (shouldScramble) GamePhase.SCRAMBLING else GamePhase.PLAYING
        )
        if (solution == null) computeSolutionAsync(board)
        if (shouldScramble) viewModelScope.launch { animateScramble(board) }
    }

    // ── Gameplay ──

    fun onDragSwap(from: CellPos, to: CellPos) {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return

        // Passthrough skip: jump over completed goal cells and frozen tiles
        val borderedCells = allGoalCells()
        val toFrozen = current.board.tileAt(to.row, to.col).frozen
        if (current.passthroughActive && (to in borderedCells || toFrozen) && from !in borderedCells) {
            val dr = to.row - from.row
            val dc = to.col - from.col
            var r = to.row; var c = to.col
            while (current.board.isValidCell(r, c) &&
                (CellPos(r, c) in borderedCells || current.board.tileAt(r, c).frozen)) {
                r += dr; c += dc
            }
            if (!current.board.isValidCell(r, c)) return
            executePassthroughSwap(from, CellPos(r, c))
            return
        }

        if (!BoardEngine.canSwap(current.board, from, to)) return
        executeSwap(from, to)
    }

    fun toggleUnfreeze() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (current.unfreezeMode) {
            _state.value = current.copy(unfreezeMode = false)
        } else if (current.unfreezeTokens > 0) {
            _state.value = current.copy(
                unfreezeMode = true, shuffleReady = false, passthroughActive = false,
                selectedCell = null, hintCells = emptySet()
            )
        }
    }

    private fun unfreezeCell(row: Int, col: Int) {
        val current = _state.value
        val tile = current.board.tileAt(row, col)
        if (!tile.frozen) return
        viewModelScope.launch {
            val success = progressRepo.useUnfreezeToken()
            if (!success) return@launch
            unfreezeTokens--
            usedPowerUpThisGame = true
            val newTiles = current.board.tiles.mapIndexed { r, rowTiles ->
                rowTiles.mapIndexed { c, t ->
                    if (r == row && c == col) t.copy(frozen = false) else t
                }
            }
            val newBoard = current.board.copy(tiles = newTiles)
            audioManager.playUnfreeze()
            _state.value = current.copy(
                board = newBoard, unfreezeTokens = unfreezeTokens, unfreezeMode = false
            )
        }
    }

    fun onCellTapped(row: Int, col: Int) {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.board.isVoid(row, col)) return

        // Shuffle mode: tap the board to execute shuffle
        if (current.shuffleReady) {
            executeShuffle()
            return
        }

        // Unfreeze mode: tap a frozen cell to unfreeze it
        if (current.unfreezeMode) {
            if (current.board.tileAt(row, col).frozen) {
                unfreezeCell(row, col)
            }
            return
        }

        val tapped = CellPos(row, col)
        when {
            current.selectedCell == null -> {
                _state.value = current.copy(selectedCell = tapped, hintCells = emptySet())
                audioManager.playTap()
            }
            current.selectedCell == tapped ->
                _state.value = current.copy(selectedCell = null)
            BoardEngine.canSwap(current.board, current.selectedCell, tapped) -> {
                // Passthrough skip for tap-swap
                val borderedCells = allGoalCells()
                val sel = current.selectedCell
                val tappedFrozen = current.board.tileAt(tapped.row, tapped.col).frozen
                if (current.passthroughActive && (tapped in borderedCells || tappedFrozen) && sel !in borderedCells) {
                    val dr = tapped.row - sel.row
                    val dc = tapped.col - sel.col
                    var r = tapped.row; var c = tapped.col
                    while (current.board.isValidCell(r, c) &&
                        (CellPos(r, c) in borderedCells || current.board.tileAt(r, c).frozen)) {
                        r += dr; c += dc
                    }
                    if (current.board.isValidCell(r, c)) {
                        executePassthroughSwap(sel, CellPos(r, c))
                    }
                } else {
                    executeSwap(sel, tapped)
                }
            }
            else -> {
                _state.value = current.copy(selectedCell = tapped, hintCells = emptySet())
                audioManager.playTap()
            }
        }
    }

    private fun allGoalCells(): Set<CellPos> =
        _state.value.completedGoalCells.values.flatten().toSet()

    private fun executeSwap(from: CellPos, to: CellPos) {
        val current = _state.value
        val borderedCells = allGoalCells()
        val crossesBorder = from in borderedCells || to in borderedCells

        // Pro blocks swaps touching goal cells (passthrough skip handled before reaching here)
        if (crossesBorder && difficulty == Difficulty.HARD) return

        // Start Blitz timer on first swap
        if (!blitzTimerStarted && current.challengeState?.type == ChallengeType.BLITZ) {
            blitzTimerStarted = true
            startBlitzTimer()
        }

        hasMovedSinceReset = true
        _state.value = current.copy(
            phase = GamePhase.ANIMATING, selectedCell = null,
            hintCells = emptySet(), swapAnim = SwapAnimation(from, to, 0f)
        )

        viewModelScope.launch {
            audioManager.playSwap()
            val steps = 15; val stepDelay = 17L
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                _state.value = _state.value.copy(swapAnim = SwapAnimation(from, to, eased))
                delay(stepDelay)
            }

            val newBoard = BoardEngine.executeSwap(current.board, from, to)
            val newMoves = current.movesRemaining - 1

            var baseGoalIds = current.completedGoalIds
            var baseGoalCells = current.completedGoalCells
            var invalidatedGoals = emptySet<String>()
            if (crossesBorder && (difficulty == Difficulty.EASY || difficulty == Difficulty.MEDIUM)) {
                invalidatedGoals = current.completedGoalCells.filter { (_, cells) ->
                    from in cells || to in cells
                }.keys
                baseGoalIds = baseGoalIds - invalidatedGoals
                baseGoalCells = baseGoalCells - invalidatedGoals
            }

            val goalsToCheck = current.level.goals.filter { it.id !in invalidatedGoals }
            val metGoalIds = BoardEngine.evaluateGoals(newBoard, goalsToCheck)
            val newCompleted = baseGoalIds + metGoalIds

            val newGoalCells = baseGoalCells.toMutableMap()
            for (goal in current.level.goals) {
                if (goal.id in newCompleted) {
                    val cells = PatternMatcher.findGoalPositions(newBoard, goal)
                    if (cells != null) newGoalCells[goal.id] = cells
                } else {
                    newGoalCells.remove(goal.id)
                }
            }

            // Check for token tile captures in newly completed goals
            var boardAfterCapture = newBoard
            var redoCaptured = false
            var shuffleCaptured = false
            var ptCaptured = false
            var ufCaptured = false
            val newlyCompleted = newCompleted - current.completedGoalIds
            if (newlyCompleted.isNotEmpty()) {
                val newCells = newlyCompleted.flatMap { id -> newGoalCells[id] ?: emptySet() }
                for (cell in newCells) {
                    val t = boardAfterCapture.tileAt(cell.row, cell.col)
                    if (t.redo) { progressRepo.addRedoToken(); redoTokens++; redoCaptured = true }
                    if (t.shuffleToken) { progressRepo.addShuffleToken(); shuffleTokens++; shuffleCaptured = true }
                    if (t.passthroughToken) { progressRepo.addPassthroughToken(); passthroughTokens++; ptCaptured = true }
                    if (t.unfreezeToken) { progressRepo.addUnfreezeToken(); unfreezeTokens++; ufCaptured = true }
                }
                if (redoCaptured || shuffleCaptured || ptCaptured || ufCaptured) {
                    val updatedTiles = boardAfterCapture.tiles.mapIndexed { r, row ->
                        row.mapIndexed { c, tile ->
                            if (CellPos(r, c) in newCells) tile.copy(
                                redo = false, shuffleToken = false,
                                passthroughToken = false, unfreezeToken = false
                            ) else tile
                        }
                    }
                    boardAfterCapture = boardAfterCapture.copy(tiles = updatedTiles)
                }
            }

            val isChallenge = current.isChallenge
            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompleted.isNotEmpty()) audioManager.playMatch()

            var starsAwarded = 0; var winsNeeded = 0; var unlockedWorld: String? = null
            var isPerfect = false
            var blitzReplenish = false
            val phase = when {
                won -> {
                    if (isChallenge) {
                        val cs = current.challengeState!!
                        if (cs.type == ChallengeType.BLITZ) {
                            // Blitz: replenish goals instead of winning
                            blitzReplenish = true
                            GamePhase.PLAYING
                        } else {
                            starsAwarded = BoardEngine.calculateStars(newMoves, current.level.starThresholds).coerceAtLeast(1)
                            // Music delayed — triggered after count-up animation in UI
                            winResultCommitted = false
                            pendingWinLevelId = current.level.id
                            pendingWinStars = starsAwarded
                            GamePhase.WON
                        }
                    } else {
                        val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                        val gameDiff = _state.value.gameDifficulty
                        val movesUsed = adjustedMaxMoves - newMoves
                        isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                        val perfectMultiplier = if (isPerfect) 2f else 1f
                        starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                        MusicManager.startWinMusic(context, perfectGame = isPerfect)
                        val oldTotal = progressRepo.totalStarsFlow.first()
                        unlockedWorld = detectNewWorldUnlock(oldTotal, oldTotal + starsAwarded)
                        winResultCommitted = false
                        pendingWinLevelId = current.level.id
                        pendingWinStars = starsAwarded
                        GamePhase.WON
                    }
                }
                lost -> {
                    val cs = current.challengeState
                    if (cs?.type == ChallengeType.OVERGROWN && cs.triesRemaining > 1) {
                        // Overgrown retry — will reset after state update
                        audioManager.playLose()
                        GamePhase.PLAYING // temporary, reset below
                    } else {
                        audioManager.playLose()
                        if (!isChallenge) progressRepo.loseLife(difficulty.ordinal)
                        blitzTimerJob?.cancel()
                        GamePhase.LOST
                    }
                }
                else -> GamePhase.PLAYING
            }

            // Update challenge state after swap
            val updatedChalState = if (isChallenge && phase == GamePhase.PLAYING) {
                val cs = current.challengeState!!
                when (cs.type) {
                    ChallengeType.BLITZ -> {
                        if (newlyCompleted.isNotEmpty()) cs.copy(
                            goalsCleared = cs.goalsCleared + newlyCompleted.size,
                            blitzStarScore = cs.blitzStarScore + newlyCompleted.size * cs.comboMultiplier
                        )
                        else cs
                    }
                    ChallengeType.SHIFTING -> cs.copy(movesSinceLastScramble = cs.movesSinceLastScramble + 1)
                    ChallengeType.MEMORY -> cs // reveal handled below
                    else -> cs
                }
            } else current.challengeState

            _state.value = _state.value.copy(
                board = boardAfterCapture, movesRemaining = newMoves,
                completedGoalIds = newCompleted, completedGoalCells = newGoalCells,
                selectedCell = null, hintCells = emptySet(), swapAnim = null,
                phase = phase, starsAwarded = starsAwarded, winsToRestoreLife = winsNeeded,
                unlockedWorldName = unlockedWorld,
                shuffleTokens = shuffleTokens, shuffleTokenAwarded = shuffleCaptured,
                passthroughTokens = passthroughTokens, passthroughTokenAwarded = ptCaptured,
                unfreezeTokens = unfreezeTokens, unfreezeTokenAwarded = ufCaptured,
                redoTokens = redoTokens, redoTokenAwarded = redoCaptured,
                perfectGame = isPerfect,
                challengeState = updatedChalState
            )

            // Blitz goal replenish
            if (blitzReplenish) {
                blitzReplenishGoals()
            }

            // Challenge post-swap logic
            if (isChallenge && phase == GamePhase.PLAYING) {
                val cs = _state.value.challengeState ?: updatedChalState!!
                when (cs.type) {
                    ChallengeType.SHIFTING -> {
                        if (cs.movesSinceLastScramble >= 3) {
                            _state.value = _state.value.copy(
                                challengeState = cs.copy(movesSinceLastScramble = 0)
                            )
                            val goalCells = allGoalCells()
                            val numSwaps = boardAfterCapture.width * boardAfterCapture.height
                            val (scrambled, _) = scrambleBoard(boardAfterCapture, numSwaps, protectedCells = goalCells)
                            animateScramble(scrambled)
                        }
                    }
                    ChallengeType.MEMORY -> {
                        revealAroundSwap(from, to)
                    }
                    else -> {}
                }
            }

            // Overgrown retry: reset board with decremented tries
            if (lost && current.challengeState?.type == ChallengeType.OVERGROWN
                && current.challengeState.triesRemaining > 1) {
                delay(800) // brief pause so player sees they lost
                overgrownRetry(current.challengeState.triesRemaining - 1)
            }
        }
    }

    /** Passthrough swap: tile jumps over completed goal cells and lands on the other side. */
    private fun executePassthroughSwap(from: CellPos, landing: CellPos) {
        val current = _state.value

        hasMovedSinceReset = true
        _state.value = current.copy(
            phase = GamePhase.ANIMATING, selectedCell = null,
            hintCells = emptySet(), swapAnim = SwapAnimation(from, landing, 0f)
        )

        viewModelScope.launch {
            audioManager.playPassthrough()
            val steps = 15; val stepDelay = 17L
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                _state.value = _state.value.copy(swapAnim = SwapAnimation(from, landing, eased))
                delay(stepDelay)
            }

            // Consume passthrough token
            progressRepo.usePassthroughToken()
            passthroughTokens--
            usedPowerUpThisGame = true
            audioManager.playMatch()

            // Swap from <-> landing directly; goal cells in between are untouched
            val newBoard = current.board.withSwap(from.row, from.col, landing.row, landing.col)
            val newMoves = current.movesRemaining - 1

            // Re-evaluate goals (no invalidation — goal cells stayed in place)
            val metGoalIds = BoardEngine.evaluateGoals(newBoard, current.level.goals)
            val newCompleted = current.completedGoalIds + metGoalIds

            val newGoalCells = current.completedGoalCells.toMutableMap()
            for (goal in current.level.goals) {
                if (goal.id in newCompleted) {
                    val cells = PatternMatcher.findGoalPositions(newBoard, goal)
                    if (cells != null) newGoalCells[goal.id] = cells
                } else {
                    newGoalCells.remove(goal.id)
                }
            }

            // Check for token tile captures in newly completed goals
            var boardAfterCapture = newBoard
            var redoCaptured = false
            var shuffleCaptured = false
            var ptCaptured = false
            var ufCaptured = false
            val newlyCompletedPt = newCompleted - current.completedGoalIds
            if (newlyCompletedPt.isNotEmpty()) {
                val newCells = newlyCompletedPt.flatMap { id -> newGoalCells[id] ?: emptySet() }
                for (cell in newCells) {
                    val t = boardAfterCapture.tileAt(cell.row, cell.col)
                    if (t.redo) { progressRepo.addRedoToken(); redoTokens++; redoCaptured = true }
                    if (t.shuffleToken) { progressRepo.addShuffleToken(); shuffleTokens++; shuffleCaptured = true }
                    if (t.passthroughToken) { progressRepo.addPassthroughToken(); passthroughTokens++; ptCaptured = true }
                    if (t.unfreezeToken) { progressRepo.addUnfreezeToken(); unfreezeTokens++; ufCaptured = true }
                }
                if (redoCaptured || shuffleCaptured || ptCaptured || ufCaptured) {
                    val updatedTiles = boardAfterCapture.tiles.mapIndexed { r, row ->
                        row.mapIndexed { c, tile ->
                            if (CellPos(r, c) in newCells) tile.copy(
                                redo = false, shuffleToken = false,
                                passthroughToken = false, unfreezeToken = false
                            ) else tile
                        }
                    }
                    boardAfterCapture = boardAfterCapture.copy(tiles = updatedTiles)
                }
            }

            val isChallenge = current.isChallenge
            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompletedPt.isNotEmpty()) audioManager.playMatch()

            var starsAwarded = 0; var winsNeeded = 0; var unlockedWorld: String? = null
            var isPerfect = false
            val phase = when {
                won -> {
                    if (isChallenge) {
                        blitzTimerJob?.cancel()
                        starsAwarded = BoardEngine.calculateStars(newMoves, current.level.starThresholds).coerceAtLeast(1)
                        // Music delayed — triggered after count-up animation in UI
                    } else {
                        val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                        val gameDiff = _state.value.gameDifficulty
                        val movesUsed = adjustedMaxMoves - newMoves
                        isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                        val perfectMultiplier = if (isPerfect) 2f else 1f
                        starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                        MusicManager.startWinMusic(context, perfectGame = isPerfect)
                        val oldTotal = progressRepo.totalStarsFlow.first()
                        unlockedWorld = detectNewWorldUnlock(oldTotal, oldTotal + starsAwarded)
                    }
                    winResultCommitted = false
                    pendingWinLevelId = current.level.id
                    pendingWinStars = starsAwarded
                    GamePhase.WON
                }
                lost -> {
                    val cs = current.challengeState
                    if (cs?.type == ChallengeType.OVERGROWN && cs.triesRemaining > 1) {
                        // Overgrown retry — will reset after state update
                        audioManager.playLose()
                        GamePhase.PLAYING // temporary, reset below
                    } else {
                        audioManager.playLose()
                        if (!isChallenge) progressRepo.loseLife(difficulty.ordinal)
                        blitzTimerJob?.cancel()
                        GamePhase.LOST
                    }
                }
                else -> GamePhase.PLAYING
            }

            _state.value = _state.value.copy(
                board = boardAfterCapture, movesRemaining = newMoves,
                completedGoalIds = newCompleted, completedGoalCells = newGoalCells,
                selectedCell = null, hintCells = emptySet(), swapAnim = null,
                passthroughActive = false, passthroughTokens = passthroughTokens,
                shuffleTokens = shuffleTokens, shuffleTokenAwarded = shuffleCaptured,
                passthroughTokenAwarded = ptCaptured,
                unfreezeTokens = unfreezeTokens, unfreezeTokenAwarded = ufCaptured,
                phase = phase, starsAwarded = starsAwarded, winsToRestoreLife = winsNeeded,
                unlockedWorldName = unlockedWorld,
                redoTokens = redoTokens, redoTokenAwarded = redoCaptured,
                perfectGame = isPerfect
            )
        }
    }

    fun requestHint() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        viewModelScope.launch {
            val hint = HintSolver.findBestSwap(
                current.board, current.level.goals, current.completedGoalIds
            ) ?: return@launch

            val midRow = current.board.height / 2
            val midCol = current.board.width / 2
            val hintRow = (hint.first.row + hint.second.row) / 2
            val hintCol = (hint.first.col + hint.second.col) / 2
            val rowRange = if (hintRow < midRow) 0 until midRow else midRow until current.board.height
            val colRange = if (hintCol < midCol) 0 until midCol else midCol until current.board.width
            val quadrantCells = mutableSetOf<CellPos>()
            for (r in rowRange) for (c in colRange) quadrantCells.add(CellPos(r, c))

            _state.value = current.copy(hintCells = quadrantCells)
            delay(2000)
            val latest = _state.value
            if (latest.hintCells.isNotEmpty()) _state.value = latest.copy(hintCells = emptySet())
        }
    }

    fun toggleShuffle() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (current.shuffleReady) {
            _state.value = current.copy(shuffleReady = false)
        } else if (current.shuffleTokens > 0) {
            _state.value = current.copy(
                shuffleReady = true, passthroughActive = false, unfreezeMode = false,
                selectedCell = null, hintCells = emptySet()
            )
        }
    }

    private fun executeShuffle() {
        val current = _state.value
        if (!current.shuffleReady || current.shuffleTokens <= 0) return
        viewModelScope.launch {
            val success = progressRepo.useShuffleToken()
            if (!success) return@launch
            shuffleTokens--
            usedPowerUpThisGame = true
            val numSwaps = current.board.width * current.board.height
            val goalCells = current.completedGoalCells.values.flatten().toSet()
            val (shuffled, _) = scrambleBoard(current.board, numSwaps, protectedCells = goalCells)
            audioManager.playShuffle()
            _state.value = current.copy(
                board = shuffled, shuffleReady = false,
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
                gameDifficulty = computeGameDifficulty(shuffled),
                hintCells = emptySet(),
                selectedCell = null
            )
            computeSolutionAsync(shuffled)
        }
    }

    fun executeRedo() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (redoTokens <= 0) return
        viewModelScope.launch {
            val success = progressRepo.useRedoToken()
            if (!success) return@launch
            redoTokens--
            usedPowerUpThisGame = true
            redoFullReset = true
            resetLevel()
            redoFullReset = false
        }
    }

    fun togglePassthrough() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (current.passthroughActive) {
            _state.value = current.copy(passthroughActive = false)
        } else if (current.passthroughTokens > 0 &&
            (current.completedGoalIds.isNotEmpty() || current.board.tiles.any { row -> row.any { it.frozen } })) {
            _state.value = current.copy(
                passthroughActive = true, shuffleReady = false, unfreezeMode = false,
                selectedCell = null, hintCells = emptySet()
            )
        }
    }

    fun playStarCollect() { audioManager.playStarCollect() }
    fun playSwapSound() { audioManager.playSwap() }
    fun playMatchSound() { audioManager.playMatch() }
    fun playWinSound(stars: Int = 1) { audioManager.playWin(stars) }
    fun playPerfectGameSound() { audioManager.playPerfectGame() }
    fun playWorldUnlockSound() { audioManager.playWorldUnlock() }
    fun playChallengeMusic() { MusicManager.startWinMusic(context, perfectGame = true) }

    fun advanceTutorial() {
        val current = _state.value
        val nextIndex = current.tutorialStepIndex + 1
        val steps = current.level.tutorialSteps ?: return
        if (nextIndex >= steps.size)
            _state.value = current.copy(phase = GamePhase.PLAYING, tutorialStepIndex = nextIndex)
        else
            _state.value = current.copy(tutorialStepIndex = nextIndex)
    }

    fun showSolution() {
        val steps = precomputedSolution ?: return
        _state.value = _state.value.copy(
            solutionSteps = steps, phase = GamePhase.SHOWING_SOLUTION
        )
    }

    fun dismissSolution() {
        _state.value = _state.value.copy(phase = GamePhase.LOST, solutionSteps = null)
    }

    fun commitWinResult() {
        if (winResultCommitted) return
        winResultCommitted = true
        viewModelScope.launch {
            val state = _state.value
            if (state.isChallenge) {
                // Challenge win: bonus stars + 1 of each token, no per-level save
                progressRepo.saveChallengeStars(pendingWinStars)
                progressRepo.recordChallengeCompletion(pendingWinLevelId)
                progressRepo.addShuffleToken(); shuffleTokens++
                progressRepo.addPassthroughToken(); passthroughTokens++
                progressRepo.addUnfreezeToken(); unfreezeTokens++
                progressRepo.addRedoToken(); redoTokens++
                _state.value = _state.value.copy(
                    shuffleTokenAwarded = true,
                    passthroughTokenAwarded = true,
                    unfreezeTokenAwarded = true,
                    redoTokenAwarded = true
                )
                return@launch
            }

            progressRepo.saveLevelResult(pendingWinLevelId, pendingWinStars)
            val result = progressRepo.recordWin(difficulty.ordinal, pendingWinLevelId)
            profileRepo.incrementPlayerLevel()
            val playerLevel = profileRepo.loadProfile().playerLevel
            if (_state.value.unlockedWorldName != null || (playerLevel > 0 && playerLevel % 7 == 0)) {
                progressRepo.addShuffleToken()
                shuffleTokens++
                _state.value = _state.value.copy(shuffleTokenAwarded = true)
            }
            if (playerLevel > 0 && playerLevel % 7 == 0) {
                progressRepo.addPassthroughToken()
                passthroughTokens++
                _state.value = _state.value.copy(passthroughTokenAwarded = true)
            }
            val unfreezeAwarded = progressRepo.recordUnfreezeStreak(pendingWinLevelId)
            if (unfreezeAwarded) {
                unfreezeTokens++
                _state.value = _state.value.copy(unfreezeTokenAwarded = true)
            }
            // Perfect game: award +1 of every token + record count
            if (_state.value.perfectGame) {
                progressRepo.incrementPerfectGames()
                progressRepo.addShuffleToken(); shuffleTokens++
                progressRepo.addPassthroughToken(); passthroughTokens++
                progressRepo.addUnfreezeToken(); unfreezeTokens++
                progressRepo.addRedoToken(); redoTokens++
                _state.value = _state.value.copy(
                    shuffleTokenAwarded = true,
                    passthroughTokenAwarded = true,
                    unfreezeTokenAwarded = true,
                    redoTokenAwarded = true
                )
            }
            if (result == -1) {
                audioManager.playLifeRestored()
                _state.value = _state.value.copy(lifeRestored = true)
            }

            // Submit scores to Google Play Games leaderboards (only if opted in)
            val profile = profileRepo.loadProfile()
            if (profile.leaderboardOptIn) {
                activity?.let { act ->
                    val totalStars = progressRepo.totalStarsFlow.first()
                    val progress = progressRepo.loadProgress()
                    val highestLevel = progress.highestUnlockedLevel(difficulty.startingLevel)
                    PlayGamesManager.submitTotalStars(act, difficulty, totalStars)
                    PlayGamesManager.submitHighestLevel(act, difficulty, highestLevel)
                }
            }

            // ── Challenge trigger detection (normal games only) ──
            val world = (pendingWinLevelId - 1) / 9 + 1
            if (world >= 5) {
                // Priority: Memory > Blitz > Overgrown > Shifting
                val triggered: ChallengeType? = when {
                    _state.value.perfectGame -> ChallengeType.MEMORY
                    progressRepo.recordProgressiveWin(pendingWinLevelId) -> ChallengeType.BLITZ
                    run {
                        val worldComplete = progressRepo.checkWorldComplete(world)
                        worldComplete && !progressRepo.hasOvergrownTriggered(world)
                    } -> {
                        progressRepo.markOvergrownTriggered(world)
                        ChallengeType.OVERGROWN
                    }
                    !usedPowerUpThisGame && progressRepo.recordNoPowerupWin() -> ChallengeType.SHIFTING
                    else -> null
                }
                if (triggered != null) {
                    _state.value = _state.value.copy(pendingChallenge = triggered)
                }
            }
            // Reset no-powerup streak if a power-up was used
            if (usedPowerUpThisGame) progressRepo.resetNoPowerupStreak()
        }
    }

    fun dismissChallenge() {
        _state.value = _state.value.copy(pendingChallenge = null)
    }

    private fun detectNewWorldUnlock(oldStars: Int, newStars: Int): String? {
        val startingWorld = difficulty.startingWorld
        val skillMultiplier = difficulty.starMultiplier
        val worldThresholds = listOf(
            Triple(2, 8, "Blooming Meadow"),
            Triple(3, 20, "Ancient Grove"),
            Triple(4, 35, "Crystal Cavern"),
            Triple(5, 55, "Shattered Isles"),
            Triple(6, 80, "Void Fortress"),
            Triple(7, 110, "Molten Core"),
            Triple(8, 145, "Starfall Summit"),
            Triple(9, 185, "Abyssal Depths"),
            Triple(10, 230, "Prism Citadel")
        )
        for ((worldId, baseThreshold, name) in worldThresholds) {
            if (worldId <= startingWorld) continue
            val threshold = baseThreshold * skillMultiplier
            if (oldStars < threshold && newStars >= threshold) return name
        }
        return null
    }

    // ── Challenge-specific methods ──

    private fun overgrownRetry(triesLeft: Int) {
        val (board, solution) = generateBoardWithSolution(adjustedMaxMoves)
        precomputedSolution = solution
        _state.value = GameState(
            level = level.copy(maxMoves = adjustedMaxMoves),
            board = board,
            movesRemaining = adjustedMaxMoves,
            difficulty = difficulty,
            gameDifficulty = computeGameDifficulty(board),
            initialBoard = board,
            hasSolution = solution != null,
            shuffleTokens = shuffleTokens,
            passthroughTokens = passthroughTokens,
            unfreezeTokens = unfreezeTokens,
            redoTokens = redoTokens,
            phase = GamePhase.SCRAMBLING,
            challengeState = ChallengeState(type = ChallengeType.OVERGROWN, triesRemaining = triesLeft)
        )
        if (solution == null) computeSolutionAsync(board)
        viewModelScope.launch { animateScramble(board) }
    }

    private fun startBlitzTimer() {
        blitzTimerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val s = _state.value
                if (s.phase != GamePhase.PLAYING) continue
                val cs = s.challengeState ?: break
                if (cs.type != ChallengeType.BLITZ) break
                val remaining = cs.timerMillisRemaining - 100
                if (remaining <= 0) {
                    // Time's up — Blitz always wins, stars = accumulated score
                    pendingWinStars = cs.blitzStarScore.coerceAtLeast(1)
                    pendingWinLevelId = ChallengeType.BLITZ.id
                    winResultCommitted = false
                    // Music delayed — triggered after count-up animation in UI
                    _state.value = s.copy(
                        challengeState = cs.copy(timerMillisRemaining = 0),
                        phase = GamePhase.WON,
                        starsAwarded = pendingWinStars
                    )
                    break
                }
                _state.value = s.copy(
                    challengeState = cs.copy(timerMillisRemaining = remaining)
                )
            }
        }
    }

    /** Called when all goals are completed in Blitz — replenish with new goals. */
    fun blitzReplenishGoals() {
        val current = _state.value
        val cs = current.challengeState ?: return
        if (cs.type != ChallengeType.BLITZ) return
        val newGoals = ChallengeGenerator.generateBlitzGoalSet(current.board, difficulty)
        val newCombo = cs.comboCount + 1
        // Each completed round increases multiplier by 1 (1x, 2x, 3x, 4x...)
        val newMultiplier = newCombo + 1
        val newLevel = current.level.copy(goals = newGoals)
        _state.value = current.copy(
            level = newLevel,
            completedGoalIds = emptySet(),
            completedGoalCells = emptyMap(),
            challengeState = cs.copy(
                comboCount = newCombo,
                comboMultiplier = newMultiplier
            )
        )
    }

    private fun startMemoryReveal() {
        val current = _state.value
        val cs = current.challengeState ?: return
        // Reveal all cells for 3 seconds
        val allCells = mutableSetOf<CellPos>()
        for (r in 0 until current.board.height) {
            for (c in 0 until current.board.width) {
                if (!current.board.isVoid(r, c)) allCells.add(CellPos(r, c))
            }
        }
        _state.value = current.copy(
            challengeState = cs.copy(revealedCells = allCells)
        )
        viewModelScope.launch {
            delay(3000)
            val s = _state.value
            val cState = s.challengeState ?: return@launch
            _state.value = s.copy(
                challengeState = cState.copy(revealedCells = emptySet(), initialRevealDone = true)
            )
        }
    }

    private fun revealAroundSwap(from: CellPos, to: CellPos) {
        val current = _state.value
        val cs = current.challengeState ?: return
        if (!cs.initialRevealDone) return
        // Reveal 1-cell radius around both swapped positions
        val revealed = cs.revealedCells.toMutableSet()
        for (pos in listOf(from, to)) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = pos.row + dr; val c = pos.col + dc
                    if (current.board.isValidCell(r, c) && !current.board.isVoid(r, c)) {
                        revealed.add(CellPos(r, c))
                    }
                }
            }
        }
        _state.value = current.copy(
            challengeState = cs.copy(revealedCells = revealed)
        )
        // Hide after 1.5 seconds
        viewModelScope.launch {
            delay(1500)
            val s = _state.value
            val cState = s.challengeState ?: return@launch
            _state.value = s.copy(
                challengeState = cState.copy(revealedCells = emptySet())
            )
        }
    }

    override fun onCleared() {
        blitzTimerJob?.cancel()
        audioManager.release()
    }
}

class GameViewModelFactory(
    private val context: Context,
    private val levelId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(context, levelId) as T
    }
}
