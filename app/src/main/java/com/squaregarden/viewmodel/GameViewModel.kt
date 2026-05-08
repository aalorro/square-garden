package com.squaregarden.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.squaregarden.audio.AudioManager
import com.squaregarden.data.PlayGamesManager
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.logic.BoardEngine
import com.squaregarden.logic.HintSolver
import com.squaregarden.logic.LevelLoader
import com.squaregarden.logic.PatternMatcher
import com.squaregarden.model.*
import kotlinx.coroutines.Dispatchers
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

            val levels = LevelLoader.loadAllLevels(context)
            level = levels.first { it.id == levelId }
            adjustedMaxMoves = max(1, (level.maxMoves * difficulty.moveMultiplier).roundToInt())
            initLevel()
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
                HintSolver.findSolution(board, level.goals, adjustedMaxMoves)
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

    private fun initLevel() {
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
            val (board, solution) = generateBoardWithSolution(adjustedMaxMoves)
            precomputedSolution = solution
            val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
            _state.value = GameState(
                level = adjustedLevel, board = board,
                movesRemaining = adjustedMaxMoves, difficulty = difficulty,
                gameDifficulty = computeGameDifficulty(board),
                initialBoard = board, hasSolution = solution != null,
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
                phase = GamePhase.PLAYING
            )
            // If reverse-construction failed, try solver in background
            if (solution == null) computeSolutionAsync(board)
        }
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
        hasMovedSinceReset = false
        val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
        _state.value = GameState(
            level = adjustedLevel, board = board,
            movesRemaining = moves, difficulty = difficulty,
            gameDifficulty = computeGameDifficulty(board),
            initialBoard = board, hasSolution = solution != null,
            shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
            phase = if (hasTutorial) GamePhase.TUTORIAL_PAUSE else GamePhase.PLAYING
        )
        if (solution == null) computeSolutionAsync(board)
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
            val newTiles = current.board.tiles.mapIndexed { r, rowTiles ->
                rowTiles.mapIndexed { c, t ->
                    if (r == row && c == col) t.copy(frozen = false) else t
                }
            }
            val newBoard = current.board.copy(tiles = newTiles)
            audioManager.playMatch()
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

            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompleted.isNotEmpty()) audioManager.playMatch()

            var starsAwarded = 0; var winsNeeded = 0; var unlockedWorld: String? = null
            var isPerfect = false
            val phase = when {
                won -> {
                    val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                    val gameDiff = _state.value.gameDifficulty
                    val movesUsed = adjustedMaxMoves - newMoves
                    isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                    val perfectMultiplier = if (isPerfect) 2f else 1f
                    starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                    audioManager.playWin(baseStars)
                    val oldTotal = progressRepo.totalStarsFlow.first()
                    unlockedWorld = detectNewWorldUnlock(oldTotal, oldTotal + starsAwarded)
                    winResultCommitted = false
                    pendingWinLevelId = current.level.id
                    pendingWinStars = starsAwarded
                    GamePhase.WON
                }
                lost -> {
                    audioManager.playLose()
                    progressRepo.loseLife(difficulty.ordinal)
                    GamePhase.LOST
                }
                else -> GamePhase.PLAYING
            }

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
                perfectGame = isPerfect
            )
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
            audioManager.playSwap()
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

            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompletedPt.isNotEmpty()) audioManager.playMatch()

            var starsAwarded = 0; var winsNeeded = 0; var unlockedWorld: String? = null
            var isPerfect = false
            val phase = when {
                won -> {
                    val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                    val gameDiff = _state.value.gameDifficulty
                    val movesUsed = adjustedMaxMoves - newMoves
                    isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                    val perfectMultiplier = if (isPerfect) 2f else 1f
                    starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                    audioManager.playWin(baseStars)
                    val oldTotal = progressRepo.totalStarsFlow.first()
                    unlockedWorld = detectNewWorldUnlock(oldTotal, oldTotal + starsAwarded)
                    winResultCommitted = false
                    pendingWinLevelId = current.level.id
                    pendingWinStars = starsAwarded
                    GamePhase.WON
                }
                lost -> {
                    audioManager.playLose()
                    progressRepo.loseLife(difficulty.ordinal)
                    GamePhase.LOST
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
        if (redoTokens <= 0) return
        viewModelScope.launch {
            val success = progressRepo.useRedoToken()
            if (!success) return@launch
            redoTokens--
            redoFullReset = true
            resetLevel()
            redoFullReset = false
        }
    }

    fun togglePassthrough() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
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
        }
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

    override fun onCleared() { audioManager.release() }
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
