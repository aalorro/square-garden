package com.squaregarden.viewmodel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.squaregarden.audio.AudioManager
import com.squaregarden.audio.MusicManager
import com.squaregarden.data.LeaderboardRepository
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.MasterModeRepository
import com.squaregarden.logic.BoardEngine
import com.squaregarden.logic.ChallengeGenerator
import com.squaregarden.logic.GoalSetGenerator
import com.squaregarden.logic.HintSolver
import com.squaregarden.logic.LevelLoader
import com.squaregarden.logic.MasterLevelGenerator
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

    companion object {
        const val MASTER_MODE_SIGNAL = -999
        const val ENDGAME_SIM_SIGNAL = -998
    }

    private lateinit var level: Level
    private lateinit var baseLevel: Level
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
    private var diagonalTokens: Int = 0
    private val progressRepo = ProgressRepository(context)
    private val profileRepo = ProfileRepository(context)
    private val audioManager = AudioManager(context)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var usedPowerUpThisGame: Boolean = false
    private var effectiveStartingLevel: Int = 1
    private var masterModeRepo: MasterModeRepository? = null
    private var masterModeState: MasterModeState? = null
    private var masterTier: MasterTier? = null
    private var solverJob: Job? = null
    private var resetJob: Job? = null
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
            effectiveStartingLevel = if (profile.overrideStartingLevel > 0)
                profile.overrideStartingLevel else difficulty.startingLevel

            shuffleTokens = progressRepo.shuffleTokensFlow.first()
            passthroughTokens = progressRepo.passthroughTokensFlow.first()
            unfreezeTokens = progressRepo.unfreezeTokensFlow.first()
            redoTokens = progressRepo.redoTokensFlow.first()
            diagonalTokens = progressRepo.diagonalTokensFlow.first()

            if (levelId == ENDGAME_SIM_SIGNAL) {
                // Dev: simulate a nearly-solved level 126 (1 swap to win, 4 moves left).
                // Strategy: build a fully solved board, then make ONE swap to break
                // exactly one goal. All other goals stay completed. Player reverses
                // that swap to win.
                val levels = LevelLoader.loadAllLevels(context)
                baseLevel = levels.first { it.id == 126 }
                level = baseLevel.copy(tutorialSteps = null)

                _state.value = _state.value.copy(boardGenerating = true)
                val simBoard = withContext(Dispatchers.Default) {
                    val deadline = System.currentTimeMillis() + 5000L
                    var solved: Board? = null
                    repeat(300) {
                        if (solved != null || System.currentTimeMillis() >= deadline) return@repeat
                        solved = buildSolvedBoard(deadline)
                        // Verify all goals met
                        if (solved != null && BoardEngine.evaluateGoals(solved!!, level.goals).size != level.goals.size) {
                            solved = null
                        }
                    }
                    solved
                }

                if (simBoard != null) {
                    // Find all goal cell positions on the solved board
                    val allGoalCells = mutableMapOf<String, Set<CellPos>>()
                    for (goal in level.goals) {
                        val cells = PatternMatcher.findGoalPositions(simBoard, goal)
                        if (cells != null) allGoalCells[goal.id] = cells
                    }
                    val occupiedCells = allGoalCells.values.flatten().toSet()

                    // Find a swap that breaks exactly one goal: swap a goal-edge tile
                    // with an adjacent non-goal tile of a different color.
                    var breakSwap: Pair<CellPos, CellPos>? = null
                    var brokenGoalId: String? = null
                    for ((goalId, cells) in allGoalCells) {
                        if (breakSwap != null) break
                        for (cell in cells) {
                            if (breakSwap != null) break
                            if (simBoard.tileAt(cell.row, cell.col).frozen) continue
                            // Check 4 orthogonal neighbors
                            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                                val nr = cell.row + dr; val nc = cell.col + dc
                                val neighbor = CellPos(nr, nc)
                                if (!simBoard.isValidCell(nr, nc)) continue
                                if (neighbor in occupiedCells) continue // don't swap two goal tiles
                                if (simBoard.tileAt(nr, nc).frozen) continue
                                if (simBoard.tileAt(nr, nc).color == simBoard.tileAt(cell.row, cell.col).color) continue
                                // This swap should break the goal — verify
                                val testBoard = BoardEngine.executeSwap(simBoard, cell, neighbor)
                                val stillMet = BoardEngine.evaluateGoals(testBoard, level.goals)
                                if (goalId !in stillMet && stillMet.size == level.goals.size - 1) {
                                    breakSwap = cell to neighbor
                                    brokenGoalId = goalId
                                    break
                                }
                            }
                        }
                    }

                    if (breakSwap != null && brokenGoalId != null) {
                        val brokenBoard = BoardEngine.executeSwap(simBoard, breakSwap.first, breakSwap.second)
                        val completedIds = allGoalCells.keys - brokenGoalId
                        val completedCells = allGoalCells.filterKeys { it != brokenGoalId }
                        val simLevel = level.copy(maxMoves = 4)
                        _state.value = GameState(
                            level = simLevel, board = brokenBoard,
                            movesRemaining = 4, difficulty = difficulty,
                            gameDifficulty = GameDifficulty.EXTREMELY_HARD,
                            initialBoard = brokenBoard, hasSolution = true,
                            completedGoalIds = completedIds,
                            completedGoalCells = completedCells,
                            shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens,
                            unfreezeTokens = unfreezeTokens, redoTokens = redoTokens,
                            diagonalTokens = diagonalTokens,
                            phase = GamePhase.PLAYING
                        )
                        adjustedMaxMoves = 4
                    } else {
                        // Fallback: load level 126 normally
                        adjustedMaxMoves = max(1, (level.maxMoves * difficulty.moveMultiplier).roundToInt())
                        initLevel()
                    }
                } else {
                    // Fallback: load level 126 normally
                    adjustedMaxMoves = max(1, (level.maxMoves * difficulty.moveMultiplier).roundToInt())
                    initLevel()
                }
                return@launch
            }

            if (levelId == MASTER_MODE_SIGNAL) {
                // Master Mode: generate level via MasterLevelGenerator
                val repo = MasterModeRepository(context)
                masterModeRepo = repo
                val mState = repo.loadState()
                masterModeState = mState

                // Check for challenge round
                val challengeType = MasterLevelGenerator.shouldTriggerChallenge(
                    mState.gamesPlayed, mState.currentStreak
                )
                if (challengeType != null) {
                    level = ChallengeGenerator.generateLevel(challengeType, difficulty)
                    adjustedMaxMoves = level.maxMoves
                    val updatedMState = mState.copy(isChallengeRound = true)
                    masterModeState = updatedMState
                    masterTier = null
                    initLevel(challengeType)
                    _state.value = _state.value.copy(
                        masterModeState = updatedMState
                    )
                } else {
                    val (genLevel, tier) = MasterLevelGenerator.generateLevel(
                        mState.gamesPlayed, mState.currentStreak, difficulty
                    )
                    level = genLevel
                    masterTier = tier
                    // Tier moveMultiplier is sole factor — no skill stacking
                    adjustedMaxMoves = level.maxMoves
                    val updatedMState = mState.copy(currentTier = tier)
                    masterModeState = updatedMState
                    initLevel()
                    _state.value = _state.value.copy(
                        masterModeState = updatedMState,
                        masterTier = tier
                    )
                }
                return@launch
            }

            val challengeType = ChallengeType.fromId(levelId)
            if (challengeType != null) {
                level = ChallengeGenerator.generateLevel(challengeType, difficulty)
                adjustedMaxMoves = level.maxMoves // No difficulty adjustment for challenges
                initLevel(challengeType)
            } else {
                val levels = LevelLoader.loadAllLevels(context)
                baseLevel = levels.first { it.id == levelId }
                // Tutorials only play on the first visit to a level. Track this
                // separately from completion so replays / retries don't re-show
                // the tutorial (e.g. the Pro+ welcome on level 91) even if the
                // player hasn't finished the level yet.
                val savedProgress = progressRepo.loadProgress()
                val tutorialAlreadySeen = baseLevel.id in savedProgress.tutorialsSeen
                if (tutorialAlreadySeen) {
                    baseLevel = baseLevel.copy(tutorialSteps = null)
                }
                val goalSets = GoalSetGenerator.generateGoalSets(baseLevel, difficulty)
                // When the tutorial is still active, use the original goal set so
                // the tutorial messages line up with what's on the board.
                val chosenGoals = if (baseLevel.tutorialSteps != null) goalSets.first() else goalSets.random()
                level = baseLevel.copy(goals = chosenGoals)
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
    private fun buildSolvedBoard(deadline: Long = Long.MAX_VALUE): Board? {
        val w = level.boardWidth
        val h = level.boardHeight
        val voids = level.voidCells
        val frozenPositions = level.frozenCells

        val grid = Array(h) { arrayOfNulls<TileColor>(w) }

        // Place each goal's pattern (shuffled order for variety)
        // Sort larger/harder goals first — they need more space
        val sortedGoals = level.goals.shuffled().sortedByDescending { goal ->
            when (goal) {
                is Goal.Line -> goal.length
                is Goal.Shape -> goal.shapeType.offsets.size + 1
                is Goal.Square -> 4
            }
        }
        if (!placeGoalsBacktracking(grid, w, h, sortedGoals, 0, voids, deadline)) return null

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

    /**
     * Find all valid placement candidates for a goal on the current grid.
     * For Pro/Pro+ difficulty, only empty (null) cells are eligible so that
     * each goal gets distinct tile positions — completed-goal cells are excluded
     * from new goal matching in those modes.
     */
    private fun findGoalCandidates(
        grid: Array<Array<TileColor?>>, w: Int, h: Int,
        goal: Goal, voids: Set<CellPos>
    ): List<List<CellPos>> {
        val candidates = mutableListOf<List<CellPos>>()
        // Pro/Pro+ exclude completed-goal cells from new goal matching,
        // so each goal must occupy distinct cells on the solved board.
        val exclusiveMode = difficulty == Difficulty.HARD || difficulty == Difficulty.PRO_PLUS

        fun cellOk(pos: CellPos): Boolean =
            pos !in voids && if (exclusiveMode) grid[pos.row][pos.col] == null
            else (grid[pos.row][pos.col] == null || grid[pos.row][pos.col] == goal.color)

        when (goal) {
            is Goal.Line -> {
                for (r in 0 until h) {
                    for (c in 0..w - goal.length) {
                        val cells = (c until c + goal.length).map { CellPos(r, it) }
                        if (cells.all { cellOk(it) }) candidates.add(cells)
                    }
                }
                for (c in 0 until w) {
                    for (r in 0..h - goal.length) {
                        val cells = (r until r + goal.length).map { CellPos(it, c) }
                        if (cells.all { cellOk(it) }) candidates.add(cells)
                    }
                }
            }
            is Goal.Square -> {
                for (r in 0 until h - 1) {
                    for (c in 0 until w - 1) {
                        val cells = listOf(CellPos(r, c), CellPos(r, c + 1), CellPos(r + 1, c), CellPos(r + 1, c + 1))
                        if (cells.all { cellOk(it) }) candidates.add(cells)
                    }
                }
            }
            is Goal.Shape -> {
                for (rotation in shapeRotations(goal.shapeType.offsets)) {
                    for (r in 0 until h) {
                        for (c in 0 until w) {
                            val cells = rotation.map { CellPos(r + it.row, c + it.col) }
                            if (cells.all {
                                    it.row in 0 until h && it.col in 0 until w && cellOk(it)
                                }) candidates.add(cells)
                        }
                    }
                }
            }
        }
        return candidates
    }

    /**
     * Recursively place goals with backtracking. When a goal can't be placed,
     * backtracks to try alternative placements for previous goals instead of
     * failing the entire attempt.
     */
    private fun placeGoalsBacktracking(
        grid: Array<Array<TileColor?>>, w: Int, h: Int,
        goals: List<Goal>, index: Int, voids: Set<CellPos>,
        deadline: Long = Long.MAX_VALUE
    ): Boolean {
        if (index == goals.size) return true
        if (System.currentTimeMillis() >= deadline) return false
        val goal = goals[index]
        // Shuffle for variety but cap attempts to avoid exponential blowup
        val candidates = findGoalCandidates(grid, w, h, goal, voids).shuffled().take(20)
        for (placement in candidates) {
            // Save cells that will be overwritten
            val saved = placement.map { it to grid[it.row][it.col] }
            for (cell in placement) grid[cell.row][cell.col] = goal.color
            if (placeGoalsBacktracking(grid, w, h, goals, index + 1, voids, deadline)) return true
            // Undo placement
            for ((cell, prev) in saved) grid[cell.row][cell.col] = prev
        }
        return false
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
    private fun generateBoardWithSolution(
        moves: Int, deadline: Long = Long.MAX_VALUE
    ): Pair<Board, List<Pair<CellPos, CellPos>>?> {
        // More attempts for complex levels (many goals or large boards)
        val maxAttempts = if (level.goals.size >= 5 || level.boardWidth >= 8) 300 else 100
        repeat(maxAttempts) {
            if (Thread.interrupted() || System.currentTimeMillis() >= deadline) return Pair(placeTokenTiles(generateValidBoard()), null)
            val solved = buildSolvedBoard(deadline) ?: return@repeat
            // Verify all goals actually met
            if (BoardEngine.evaluateGoals(solved, level.goals).size != level.goals.size) return@repeat

            val (scrambled, swaps) = scrambleBoard(solved, moves)
            if (swaps.size < moves) return@repeat // not enough valid swaps

            // Reject if any goal is already met on the scrambled board
            if (BoardEngine.evaluateGoals(scrambled, level.goals).isNotEmpty()) return@repeat

            val solution = swaps.reversed()
            // Casual: swaps through completed goals are allowed, so the reversed
            // scramble always reaches the solved state — skip expensive verification.
            // Standard/Pro/Pro+: verify the path respects blocked-swap rules.
            if (difficulty != Difficulty.EASY &&
                !HintSolver.verifySolution(scrambled, level.goals, solution, difficulty)) {
                return@repeat
            }
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
        val exclusiveMode = difficulty == Difficulty.HARD || difficulty == Difficulty.PRO_PLUS
        val minRequired = mutableMapOf<TileColor, Int>()
        for (goal in level.goals) {
            val needed = when (goal) {
                is Goal.Line -> goal.length
                is Goal.Square -> 4
                is Goal.Shape -> goal.shapeType.offsets.size
            }
            // Pro/Pro+ exclude completed-goal cells from new goal matching,
            // so each goal needs its own distinct tiles (sum, not max).
            if (exclusiveMode) {
                minRequired[goal.color] = (minRequired[goal.color] ?: 0) + needed
            } else {
                minRequired[goal.color] = max(minRequired[goal.color] ?: 0, needed)
            }
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

    /** Place token tiles on random non-frozen, non-void cells (World 4+ or Master Mode, ~25% chance each). */
    private fun placeTokenTiles(board: Board): Board {
        if (level.world < 4 && level.world != MasterLevelGenerator.MASTER_WORLD) return board
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
        // Diagonal token: spawns on World 11+ or Master Mode
        val diagPos = if ((level.world >= 11 || level.world == MasterLevelGenerator.MASTER_WORLD) && (1..4).random() == 1) candidates.random() else null

        val newTiles = board.tiles.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                val pos = CellPos(r, c)
                tile.copy(
                    redo = tile.redo || pos == redoPos,
                    shuffleToken = tile.shuffleToken || pos == shufflePos,
                    passthroughToken = tile.passthroughToken || pos == ptPos,
                    unfreezeToken = tile.unfreezeToken || pos == ufPos,
                    diagonalToken = tile.diagonalToken || pos == diagPos
                )
            }
        }
        return board.copy(tiles = newTiles)
    }

    // ── Async fallback solver (for boards not built via reverse-construction) ──

    private fun computeSolutionAsync(board: Board) {
        solverJob?.cancel()
        solverJob = viewModelScope.launch {
            val goals = level.goals
            val maxMoves = adjustedMaxMoves
            val solution = withContext(Dispatchers.Default) {
                // Quick probe: try short solutions with wider beam (catches easy/medium boards fast)
                val quickLimit = goals.size + 3
                val quick = HintSolver.findSolution(board, goals, quickLimit, difficulty, beamWidth = 500)
                // If quick probe fails, do full solve with standard beam
                quick ?: HintSolver.findSolution(board, goals, maxMoves, difficulty, beamWidth = 200)
            }
            val current = _state.value
            if (current.initialBoard != board) return@launch
            if (solution != null) {
                precomputedSolution = solution
                val solverDifficulty = GameDifficulty.fromSolverResult(
                    solutionLength = solution.size,
                    maxMoves = maxMoves
                )
                _state.value = current.copy(
                    hasSolution = true,
                    gameDifficulty = solverDifficulty
                )
            }
            // If solver fails entirely, keep heuristic difficulty as-is
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

    private suspend fun initLevel(challengeType: ChallengeType? = null) {
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
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens, diagonalTokens = diagonalTokens,
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
            } else if (challengeType == ChallengeType.OVERGROWN) {
                // Show loading indicator while generating solvable board
                _state.value = _state.value.copy(boardGenerating = true)
                val genResult = withContext(Dispatchers.Default) {
                    val deadline = System.currentTimeMillis() + 3000L
                    var curLevel = level
                    var result: Pair<Board, List<Pair<CellPos, CellPos>>?>
                    var attempts = 0
                    do {
                        if (attempts > 0) {
                            curLevel = ChallengeGenerator.generateLevel(ChallengeType.OVERGROWN, difficulty)
                            level = curLevel // generateBoardWithSolution reads level.goals
                        }
                        result = generateBoardWithSolution(curLevel.maxMoves, deadline)
                        attempts++
                    } while (result.second == null && attempts < 20 && System.currentTimeMillis() < deadline)
                    Triple(curLevel, result.first, result.second)
                }
                level = genResult.first
                adjustedMaxMoves = level.maxMoves
                board = genResult.second
                solution = genResult.third
            } else {
                _state.value = _state.value.copy(boardGenerating = true)
                val result = withContext(Dispatchers.Default) {
                    // Master Mode boards (especially Intense/Brutal tiers) need more
                    // time for reverse-construction on large boards with many goals.
                    val timeoutMs = if (level.world == -1) 5000L else 3000L
                    val deadline = System.currentTimeMillis() + timeoutMs
                    var best: Pair<Board, List<Pair<CellPos, CellPos>>?> =
                        generateBoardWithSolution(adjustedMaxMoves, deadline)
                    // Retry reverse-construction a few times if first attempt
                    // didn't find a guaranteed solution. Don't call the expensive
                    // beam-search solver here — it runs async after board is shown.
                    var i = 1
                    while (i < 8 && best.second == null && System.currentTimeMillis() < deadline) {
                        val attempt = generateBoardWithSolution(adjustedMaxMoves, deadline)
                        best = attempt
                        i++
                    }
                    best
                }
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
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens, diagonalTokens = diagonalTokens,
                phase = GamePhase.SCRAMBLING,
                challengeState = chalState
            )
            // Always run solver async to find optimal solution and calibrate difficulty
            if (challengeType != ChallengeType.BLITZ) computeSolutionAsync(board)
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

        // Check if any goals are already met on the initial board
        val current = _state.value
        val metGoalIds = BoardEngine.evaluateGoals(finalBoard, current.level.goals)
        if (metGoalIds.isNotEmpty()) {
            val goalCells = mutableMapOf<String, Set<CellPos>>()
            for (goal in current.level.goals) {
                if (goal.id in metGoalIds) {
                    val cells = PatternMatcher.findGoalPositions(finalBoard, goal)
                    if (cells != null) goalCells[goal.id] = cells
                }
            }
            // Update challenge state for Overgrown star tracking
            val updatedChal = if (current.challengeState?.type == ChallengeType.OVERGROWN && metGoalIds.isNotEmpty()) {
                val cs = current.challengeState
                cs.copy(
                    goalsCleared = cs.goalsCleared + metGoalIds.size,
                    overgrownStarScore = cs.overgrownStarScore + metGoalIds.size * cs.overgrownTryMultiplier
                )
            } else current.challengeState
            audioManager.playMatch()
            vibrator.vibrate(VibrationEffect.createOneShot(100, 255))
            _state.value = current.copy(
                completedGoalIds = metGoalIds,
                completedGoalCells = goalCells,
                challengeState = updatedChal
            )
        }
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
        resetJob?.cancel()
        solverJob?.cancel()
        val current = _state.value
        val hasTutorial = level.tutorialSteps != null

        // Re-randomize goals when generating a new board
        if (!hasTutorial && hasMovedSinceReset && ::baseLevel.isInitialized) {
            val goalSets = GoalSetGenerator.generateGoalSets(baseLevel, difficulty)
            level = baseLevel.copy(goals = goalSets.random())
        }

        if (!hasTutorial && hasMovedSinceReset) {
            // New board generation — may be slow, run async with loading indicator
            val genMoves = when (difficulty) {
                Difficulty.EASY -> adjustedMaxMoves
                Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
                Difficulty.HARD -> adjustedMaxMoves
                Difficulty.PRO_PLUS -> adjustedMaxMoves
            }
            val capturedMovesRemaining = current.movesRemaining
            val capturedRedoFullReset = redoFullReset
            _state.value = current.copy(boardGenerating = true)
            resetJob = viewModelScope.launch {
                val result = withContext(Dispatchers.Default) {
                    generateBoardWithSolution(genMoves, System.currentTimeMillis() + 3000L)
                }
                val board = result.first
                val solution = result.second
                precomputedSolution = solution
                val moves = if (capturedRedoFullReset) adjustedMaxMoves else when (difficulty) {
                    Difficulty.EASY -> adjustedMaxMoves
                    Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
                    Difficulty.HARD -> if (capturedMovesRemaining > 0) capturedMovesRemaining else adjustedMaxMoves
                    Difficulty.PRO_PLUS -> if (capturedMovesRemaining > 0) capturedMovesRemaining else adjustedMaxMoves
                }
                hasMovedSinceReset = false
                val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
                _state.value = GameState(
                    level = adjustedLevel, board = board,
                    movesRemaining = moves, difficulty = difficulty,
                    gameDifficulty = computeGameDifficulty(board),
                    initialBoard = board, hasSolution = solution != null,
                    shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens, diagonalTokens = diagonalTokens,
                    phase = GamePhase.SCRAMBLING
                )
                computeSolutionAsync(board)
                animateScramble(board)
            }
            return
        }

        // Fast path: tutorial or same board (no generation needed)
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
        } else {
            board = current.board
            solution = precomputedSolution
        }

        precomputedSolution = solution
        val moves = if (redoFullReset) adjustedMaxMoves else when (difficulty) {
            Difficulty.EASY -> adjustedMaxMoves
            Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
            Difficulty.HARD -> if (current.movesRemaining > 0) current.movesRemaining else adjustedMaxMoves
            Difficulty.PRO_PLUS -> if (current.movesRemaining > 0) current.movesRemaining else adjustedMaxMoves
        }
        hasMovedSinceReset = false
        val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
        _state.value = GameState(
            level = adjustedLevel, board = board,
            movesRemaining = moves, difficulty = difficulty,
            gameDifficulty = computeGameDifficulty(board),
            initialBoard = board, hasSolution = solution != null,
            shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens, diagonalTokens = diagonalTokens,
            phase = if (hasTutorial) GamePhase.TUTORIAL_PAUSE else GamePhase.PLAYING
        )
        computeSolutionAsync(board)
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

        if (!BoardEngine.canSwap(current.board, from, to, allowDiagonal = current.diagonalMode)) return
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
            progressRepo.recordTokenUsed()
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
            BoardEngine.canSwap(current.board, current.selectedCell, tapped, allowDiagonal = current.diagonalMode) -> {
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

        // Standard and Pro block swaps touching goal cells (passthrough skip handled before reaching here)
        // Casual can pass through but the touched goal breaks
        if (crossesBorder && difficulty != Difficulty.EASY) return

        // Detect one-shot diagonal swap (Manhattan > 1 means it's a diagonal move).
        val isDiagonalSwap = current.diagonalMode &&
            (kotlin.math.abs(from.row - to.row) + kotlin.math.abs(from.col - to.col) > 1)

        // Start Blitz timer on first swap
        if (!blitzTimerStarted && current.challengeState?.type == ChallengeType.BLITZ) {
            blitzTimerStarted = true
            startBlitzTimer()
        }

        hasMovedSinceReset = true
        _state.value = current.copy(
            phase = GamePhase.ANIMATING, selectedCell = null,
            hintCells = emptySet(), swapAnim = SwapAnimation(from, to, 0f),
            diagonalMode = if (isDiagonalSwap) false else current.diagonalMode
        )

        viewModelScope.launch {
            progressRepo.recordSwap()
            if (isDiagonalSwap) {
                progressRepo.useDiagonalToken()
                progressRepo.recordTokenUsed()
                diagonalTokens--
                usedPowerUpThisGame = true
            }
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
            if (crossesBorder && difficulty == Difficulty.EASY) {
                invalidatedGoals = current.completedGoalCells.filter { (_, cells) ->
                    from in cells || to in cells
                }.keys
                baseGoalIds = baseGoalIds - invalidatedGoals
                baseGoalCells = baseGoalCells - invalidatedGoals
            }

            val goalsToCheck = current.level.goals.filter { it.id !in invalidatedGoals }
            // Pro: previously completed goal cells can't count toward new goals (one-move sharing only)
            val metGoalIds: Set<String>
            val excludedCells: Set<CellPos>
            if ((difficulty == Difficulty.HARD || difficulty == Difficulty.PRO_PLUS) && baseGoalCells.isNotEmpty()) {
                excludedCells = baseGoalCells.values.flatten().toSet()
                val alreadyCompleted = goalsToCheck.filter { it.id in baseGoalIds }
                val uncompleted = goalsToCheck.filter { it.id !in baseGoalIds }
                val stillMet = BoardEngine.evaluateGoals(newBoard, alreadyCompleted)
                val newlyMet = BoardEngine.evaluateGoals(newBoard, uncompleted, excludedCells)
                metGoalIds = stillMet + newlyMet
            } else {
                excludedCells = emptySet()
                metGoalIds = BoardEngine.evaluateGoals(newBoard, goalsToCheck)
            }
            val newCompleted = baseGoalIds + metGoalIds

            val newGoalCells = baseGoalCells.toMutableMap()
            for (goal in current.level.goals) {
                if (goal.id in newCompleted) {
                    // Keep already-completed goals locked in their original cells —
                    // re-finding them can shift the highlight away from where the
                    // player set it, which is jarring. Only locate cells for newly
                    // completed goals.
                    if (goal.id in baseGoalIds) continue
                    val cells = PatternMatcher.findGoalPositions(newBoard, goal, excludedCells)
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
            var diagCaptured = false
            // Snapshot counts before incrementing — UI shows old count until trail lands
            val preShuffleTokens = shuffleTokens
            val prePassthroughTokens = passthroughTokens
            val preUnfreezeTokens = unfreezeTokens
            val preRedoTokens = redoTokens
            val preDiagonalTokens = diagonalTokens
            val newlyCompleted = newCompleted - current.completedGoalIds
            if (newlyCompleted.isNotEmpty()) {
                val newCells = newlyCompleted.flatMap { id -> newGoalCells[id] ?: emptySet() }
                for (cell in newCells) {
                    val t = boardAfterCapture.tileAt(cell.row, cell.col)
                    if (t.redo) { progressRepo.addRedoToken(); redoTokens++; redoCaptured = true }
                    if (t.shuffleToken) { progressRepo.addShuffleToken(); shuffleTokens++; shuffleCaptured = true }
                    if (t.passthroughToken) { progressRepo.addPassthroughToken(); passthroughTokens++; ptCaptured = true }
                    if (t.unfreezeToken) { progressRepo.addUnfreezeToken(); unfreezeTokens++; ufCaptured = true }
                    if (t.diagonalToken) { progressRepo.addDiagonalToken(); diagonalTokens++; diagCaptured = true }
                }
                if (redoCaptured || shuffleCaptured || ptCaptured || ufCaptured || diagCaptured) {
                    val updatedTiles = boardAfterCapture.tiles.mapIndexed { r, row ->
                        row.mapIndexed { c, tile ->
                            if (CellPos(r, c) in newCells) tile.copy(
                                redo = false, shuffleToken = false,
                                passthroughToken = false, unfreezeToken = false,
                                diagonalToken = false
                            ) else tile
                        }
                    }
                    boardAfterCapture = boardAfterCapture.copy(tiles = updatedTiles)
                    audioManager.playTokenCapture()
                    vibrator.vibrate(VibrationEffect.createOneShot(80, 200))
                }
            }

            val isChallenge = current.isChallenge
            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompleted.isNotEmpty()) {
                audioManager.playMatch()
                vibrator.vibrate(VibrationEffect.createOneShot(100, 255))
            }

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
                        } else if (cs.type == ChallengeType.OVERGROWN) {
                            // Overgrown win: accumulated stars + last goals, with 2x win bonus
                            val completedGoalStars = newlyCompleted.size * cs.overgrownTryMultiplier
                            val finalStars = ((cs.overgrownStarScore + completedGoalStars) * 2).coerceAtLeast(1)
                            starsAwarded = finalStars
                            winResultCommitted = false
                            pendingWinLevelId = current.level.id
                            pendingWinStars = finalStars
                            GamePhase.WON
                        } else {
                            // Memory: 3x multiplier, Shifting: 2x multiplier
                            val challengeMultiplier = when (cs.type) {
                                ChallengeType.MEMORY -> 3
                                ChallengeType.SHIFTING -> 2
                                else -> 1
                            }
                            starsAwarded = (BoardEngine.calculateStars(newMoves, current.level.starThresholds) * challengeMultiplier).coerceAtLeast(1)
                            winResultCommitted = false
                            pendingWinLevelId = current.level.id
                            pendingWinStars = starsAwarded
                            // Memory: delay WON to show revealed board first
                            if (cs.type == ChallengeType.MEMORY) GamePhase.PLAYING else GamePhase.WON
                        }
                    } else if (current.isMasterMode) {
                        // Master Mode win (executeSwap path)
                        val mState = current.masterModeState!!
                        val tier = current.masterTier ?: MasterTier.WARMING_UP
                        val gameDiff = _state.value.gameDifficulty
                        val newStreak = mState.currentStreak + 1
                        val newMState = mState.copy(
                            currentStreak = newStreak,
                            bestStreak = maxOf(mState.bestStreak, newStreak),
                            gamesWon = mState.gamesWon + 1,
                            gamesPlayed = mState.gamesPlayed + 1
                        )
                        starsAwarded = (tier.baseStars * gameDiff.starMultiplier * newMState.streakMultiplier * difficulty.starMultiplier).roundToInt().coerceAtLeast(1)
                        masterModeState = newMState.copy(
                            sessionStars = mState.sessionStars + starsAwarded,
                            totalMasterStars = mState.totalMasterStars + starsAwarded
                        )
                        MusicManager.startWinMusic(context, perfectGame = false, loop = false)
                        audioManager.playWinClap(perfectGame = false)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
                        winResultCommitted = false
                        pendingWinLevelId = current.level.id
                        pendingWinStars = starsAwarded
                        GamePhase.WON
                    } else {
                        val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                        val gameDiff = _state.value.gameDifficulty
                        val movesUsed = adjustedMaxMoves - newMoves
                        isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                        val perfectMultiplier = if (isPerfect) 2f else 1f
                        starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                        val isGameComplete = current.level.id == 126
                        MusicManager.startWinMusic(context, perfectGame = isPerfect || isGameComplete, loop = isGameComplete)
                        audioManager.playWinClap(perfectGame = isPerfect || isGameComplete)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
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
                    if (cs?.type == ChallengeType.OVERGROWN) {
                        // Overgrown: always LOST — dialog will offer retry or take score
                        audioManager.playLose()
                        GamePhase.LOST
                    } else {
                        audioManager.playLose()
                        if (!isChallenge && !current.isMasterMode) progressRepo.loseLife(difficulty.ordinal)
                        blitzTimerJob?.cancel()
                        GamePhase.LOST
                    }
                }
                else -> GamePhase.PLAYING
            }

            // Update master mode state on win/loss
            if (current.isMasterMode) {
                if (phase == GamePhase.LOST) {
                    val mState = current.masterModeState!!
                    masterModeState = mState.copy(
                        currentStreak = 0,
                        gamesPlayed = mState.gamesPlayed + 1
                    )
                    masterModeRepo?.recordLoss()
                    progressRepo.loseLife(difficulty.ordinal)
                }
                if (phase == GamePhase.WON || phase == GamePhase.LOST) {
                    _state.value = _state.value.copy(masterModeState = masterModeState)
                }
            }

            // Update challenge state after swap (include Overgrown LOST to track final goals)
            val updatedChalState = if (isChallenge && (phase == GamePhase.PLAYING || phase == GamePhase.WON
                        || (phase == GamePhase.LOST && current.challengeState?.type == ChallengeType.OVERGROWN))) {
                val cs = current.challengeState!!
                when (cs.type) {
                    ChallengeType.BLITZ -> {
                        if (newlyCompleted.isNotEmpty()) cs.copy(
                            goalsCleared = cs.goalsCleared + newlyCompleted.size,
                            blitzStarScore = cs.blitzStarScore + newlyCompleted.size * cs.comboMultiplier
                        )
                        else cs
                    }
                    ChallengeType.OVERGROWN -> {
                        if (newlyCompleted.isNotEmpty()) cs.copy(
                            goalsCleared = cs.goalsCleared + newlyCompleted.size,
                            overgrownStarScore = cs.overgrownStarScore + newlyCompleted.size * cs.overgrownTryMultiplier
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
                shuffleTokens = if (shuffleCaptured) preShuffleTokens else shuffleTokens,
                shuffleTokenAwarded = shuffleCaptured,
                passthroughTokens = if (ptCaptured) prePassthroughTokens else passthroughTokens,
                passthroughTokenAwarded = ptCaptured,
                unfreezeTokens = if (ufCaptured) preUnfreezeTokens else unfreezeTokens,
                unfreezeTokenAwarded = ufCaptured,
                redoTokens = if (redoCaptured) preRedoTokens else redoTokens,
                redoTokenAwarded = redoCaptured,
                diagonalTokens = if (diagCaptured) preDiagonalTokens else diagonalTokens,
                diagonalTokenAwarded = diagCaptured,
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
                        if (won) {
                            // Reveal entire board before celebration
                            revealAllCells()
                            delay(3000)
                            _state.value = _state.value.copy(phase = GamePhase.WON)
                        } else {
                            revealAroundSwap(from, to)
                        }
                    }
                    else -> {}
                }
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
            progressRepo.recordSwap()
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
            progressRepo.recordTokenUsed()
            passthroughTokens--
            usedPowerUpThisGame = true
            audioManager.playMatch()

            // Swap from <-> landing directly; goal cells in between are untouched
            val newBoard = current.board.withSwap(from.row, from.col, landing.row, landing.col)
            val newMoves = current.movesRemaining - 1

            // Re-evaluate goals (no invalidation — goal cells stayed in place)
            // Pro: previously completed goal cells can't count toward new goals
            val metGoalIds: Set<String>
            val ptExcludedCells: Set<CellPos>
            if ((difficulty == Difficulty.HARD || difficulty == Difficulty.PRO_PLUS) && current.completedGoalCells.isNotEmpty()) {
                ptExcludedCells = current.completedGoalCells.values.flatten().toSet()
                val alreadyCompleted = current.level.goals.filter { it.id in current.completedGoalIds }
                val uncompleted = current.level.goals.filter { it.id !in current.completedGoalIds }
                val stillMet = BoardEngine.evaluateGoals(newBoard, alreadyCompleted)
                val newlyMet = BoardEngine.evaluateGoals(newBoard, uncompleted, ptExcludedCells)
                metGoalIds = stillMet + newlyMet
            } else {
                ptExcludedCells = emptySet()
                metGoalIds = BoardEngine.evaluateGoals(newBoard, current.level.goals)
            }
            val newCompleted = current.completedGoalIds + metGoalIds

            val newGoalCells = current.completedGoalCells.toMutableMap()
            for (goal in current.level.goals) {
                if (goal.id in newCompleted) {
                    // Already-completed goals stay locked to their original cells so
                    // a same-color match elsewhere doesn't visually shift the highlight.
                    if (goal.id in current.completedGoalIds) continue
                    val cells = PatternMatcher.findGoalPositions(newBoard, goal, ptExcludedCells)
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
            var diagCaptured = false
            val preShuffleTokensPt = shuffleTokens
            val prePassthroughTokensPt = passthroughTokens
            val preUnfreezeTokensPt = unfreezeTokens
            val preRedoTokensPt = redoTokens
            val preDiagonalTokensPt = diagonalTokens
            val newlyCompletedPt = newCompleted - current.completedGoalIds
            if (newlyCompletedPt.isNotEmpty()) {
                val newCells = newlyCompletedPt.flatMap { id -> newGoalCells[id] ?: emptySet() }
                for (cell in newCells) {
                    val t = boardAfterCapture.tileAt(cell.row, cell.col)
                    if (t.redo) { progressRepo.addRedoToken(); redoTokens++; redoCaptured = true }
                    if (t.shuffleToken) { progressRepo.addShuffleToken(); shuffleTokens++; shuffleCaptured = true }
                    if (t.passthroughToken) { progressRepo.addPassthroughToken(); passthroughTokens++; ptCaptured = true }
                    if (t.unfreezeToken) { progressRepo.addUnfreezeToken(); unfreezeTokens++; ufCaptured = true }
                    if (t.diagonalToken) { progressRepo.addDiagonalToken(); diagonalTokens++; diagCaptured = true }
                }
                if (redoCaptured || shuffleCaptured || ptCaptured || ufCaptured || diagCaptured) {
                    val updatedTiles = boardAfterCapture.tiles.mapIndexed { r, row ->
                        row.mapIndexed { c, tile ->
                            if (CellPos(r, c) in newCells) tile.copy(
                                redo = false, shuffleToken = false,
                                passthroughToken = false, unfreezeToken = false,
                                diagonalToken = false
                            ) else tile
                        }
                    }
                    boardAfterCapture = boardAfterCapture.copy(tiles = updatedTiles)
                    audioManager.playTokenCapture()
                    vibrator.vibrate(VibrationEffect.createOneShot(80, 200))
                }
            }

            val isChallenge = current.isChallenge
            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            if (newlyCompletedPt.isNotEmpty()) {
                audioManager.playMatch()
                vibrator.vibrate(VibrationEffect.createOneShot(100, 255))
            }

            var starsAwarded = 0; var winsNeeded = 0; var unlockedWorld: String? = null
            var isPerfect = false
            var blitzReplenishPt = false
            val phase = when {
                won -> {
                    if (isChallenge) {
                        val cs = current.challengeState!!
                        if (cs.type == ChallengeType.BLITZ) {
                            blitzReplenishPt = true
                            GamePhase.PLAYING
                        } else if (cs.type == ChallengeType.OVERGROWN) {
                            // Overgrown win: accumulated stars + last goals, with 2x win bonus
                            val completedGoalStars = newlyCompletedPt.size * cs.overgrownTryMultiplier
                            val finalStars = ((cs.overgrownStarScore + completedGoalStars) * 2).coerceAtLeast(1)
                            starsAwarded = finalStars
                            winResultCommitted = false
                            pendingWinLevelId = current.level.id
                            pendingWinStars = finalStars
                            GamePhase.WON
                        } else {
                            // Memory: 3x multiplier, Shifting: 2x multiplier
                            val challengeMultiplier = when (cs.type) {
                                ChallengeType.MEMORY -> 3
                                ChallengeType.SHIFTING -> 2
                                else -> 1
                            }
                            starsAwarded = (BoardEngine.calculateStars(newMoves, current.level.starThresholds) * challengeMultiplier).coerceAtLeast(1)
                            winResultCommitted = false
                            pendingWinLevelId = current.level.id
                            pendingWinStars = starsAwarded
                            // Memory: delay WON to show revealed board first
                            if (cs.type == ChallengeType.MEMORY) GamePhase.PLAYING else GamePhase.WON
                        }
                    } else if (current.isMasterMode) {
                        // Master Mode win (passthrough path)
                        val mState = current.masterModeState!!
                        val tier = current.masterTier ?: MasterTier.WARMING_UP
                        val gameDiff = _state.value.gameDifficulty
                        val newStreak = mState.currentStreak + 1
                        val newMState = mState.copy(
                            currentStreak = newStreak,
                            bestStreak = maxOf(mState.bestStreak, newStreak),
                            gamesWon = mState.gamesWon + 1,
                            gamesPlayed = mState.gamesPlayed + 1
                        )
                        starsAwarded = (tier.baseStars * gameDiff.starMultiplier * newMState.streakMultiplier * difficulty.starMultiplier).roundToInt().coerceAtLeast(1)
                        masterModeState = newMState.copy(
                            sessionStars = mState.sessionStars + starsAwarded,
                            totalMasterStars = mState.totalMasterStars + starsAwarded
                        )
                        MusicManager.startWinMusic(context, perfectGame = false, loop = false)
                        audioManager.playWinClap(perfectGame = false)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
                        winResultCommitted = false
                        pendingWinLevelId = current.level.id
                        pendingWinStars = starsAwarded
                        GamePhase.WON
                    } else {
                        val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                        val gameDiff = _state.value.gameDifficulty
                        val movesUsed = adjustedMaxMoves - newMoves
                        isPerfect = movesUsed <= current.level.goals.size && level.world >= 5
                        val perfectMultiplier = if (isPerfect) 2f else 1f
                        starsAwarded = (baseStars * difficulty.starMultiplier * gameDiff.starMultiplier * perfectMultiplier).roundToInt()
                        val isGameComplete = current.level.id == 126
                        MusicManager.startWinMusic(context, perfectGame = isPerfect || isGameComplete, loop = isGameComplete)
                        audioManager.playWinClap(perfectGame = isPerfect || isGameComplete)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
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
                    if (cs?.type == ChallengeType.OVERGROWN) {
                        audioManager.playLose()
                        GamePhase.LOST
                    } else {
                        audioManager.playLose()
                        if (!isChallenge && !current.isMasterMode) progressRepo.loseLife(difficulty.ordinal)
                        blitzTimerJob?.cancel()
                        GamePhase.LOST
                    }
                }
                else -> GamePhase.PLAYING
            }

            // Update master mode state on win/loss (passthrough path)
            if (current.isMasterMode) {
                if (phase == GamePhase.LOST) {
                    val mState = current.masterModeState!!
                    masterModeState = mState.copy(
                        currentStreak = 0,
                        gamesPlayed = mState.gamesPlayed + 1
                    )
                    masterModeRepo?.recordLoss()
                    progressRepo.loseLife(difficulty.ordinal)
                }
                if (phase == GamePhase.WON || phase == GamePhase.LOST) {
                    _state.value = _state.value.copy(masterModeState = masterModeState)
                }
            }

            // Update challenge state after passthrough swap (include Overgrown LOST)
            val updatedChalStatePt = if (isChallenge && (phase == GamePhase.PLAYING || phase == GamePhase.WON
                        || (phase == GamePhase.LOST && current.challengeState?.type == ChallengeType.OVERGROWN))) {
                val cs = current.challengeState!!
                when (cs.type) {
                    ChallengeType.BLITZ -> {
                        if (newlyCompletedPt.isNotEmpty()) cs.copy(
                            goalsCleared = cs.goalsCleared + newlyCompletedPt.size,
                            blitzStarScore = cs.blitzStarScore + newlyCompletedPt.size * cs.comboMultiplier
                        )
                        else cs
                    }
                    ChallengeType.OVERGROWN -> {
                        if (newlyCompletedPt.isNotEmpty()) cs.copy(
                            goalsCleared = cs.goalsCleared + newlyCompletedPt.size,
                            overgrownStarScore = cs.overgrownStarScore + newlyCompletedPt.size * cs.overgrownTryMultiplier
                        )
                        else cs
                    }
                    ChallengeType.SHIFTING -> cs.copy(movesSinceLastScramble = cs.movesSinceLastScramble + 1)
                    ChallengeType.MEMORY -> cs
                    else -> cs
                }
            } else current.challengeState

            _state.value = _state.value.copy(
                board = boardAfterCapture, movesRemaining = newMoves,
                completedGoalIds = newCompleted, completedGoalCells = newGoalCells,
                selectedCell = null, hintCells = emptySet(), swapAnim = null,
                passthroughActive = false,
                passthroughTokens = if (ptCaptured) prePassthroughTokensPt else passthroughTokens,
                shuffleTokens = if (shuffleCaptured) preShuffleTokensPt else shuffleTokens,
                shuffleTokenAwarded = shuffleCaptured,
                passthroughTokenAwarded = ptCaptured,
                unfreezeTokens = if (ufCaptured) preUnfreezeTokensPt else unfreezeTokens,
                unfreezeTokenAwarded = ufCaptured,
                phase = phase, starsAwarded = starsAwarded, winsToRestoreLife = winsNeeded,
                unlockedWorldName = unlockedWorld,
                redoTokens = if (redoCaptured) preRedoTokensPt else redoTokens,
                redoTokenAwarded = redoCaptured,
                diagonalTokens = if (diagCaptured) preDiagonalTokensPt else diagonalTokens,
                diagonalTokenAwarded = diagCaptured,
                perfectGame = isPerfect,
                challengeState = updatedChalStatePt
            )

            // Blitz goal replenish (passthrough)
            if (blitzReplenishPt) {
                blitzReplenishGoals()
            }

            // Challenge post-swap logic (passthrough)
            if (isChallenge && phase == GamePhase.PLAYING) {
                val cs = _state.value.challengeState ?: updatedChalStatePt!!
                when (cs.type) {
                    ChallengeType.SHIFTING -> {
                        if (cs.movesSinceLastScramble >= 3) {
                            _state.value = _state.value.copy(
                                challengeState = cs.copy(movesSinceLastScramble = 0)
                            )
                            animateScramble(_state.value.board)
                        }
                    }
                    ChallengeType.MEMORY -> {
                        if (won) {
                            // Reveal entire board before celebration
                            revealAllCells()
                            delay(3000)
                            _state.value = _state.value.copy(phase = GamePhase.WON)
                        } else {
                            revealAroundSwap(from, landing)
                        }
                    }
                    else -> {}
                }
            }

        }
    }

    fun requestHint() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        viewModelScope.launch {
            val hint = HintSolver.findBestSwap(
                current.board, current.level.goals, current.completedGoalIds,
                current.completedGoalCells.values.flatten().toSet(), difficulty
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
            progressRepo.recordTokenUsed()
            shuffleTokens--
            usedPowerUpThisGame = true
            val goalCells = current.completedGoalCells.values.flatten().toSet()
            val shuffled = smartShuffle(current.board, goalCells, current.completedGoalIds)
            audioManager.playShuffle()
            _state.value = current.copy(
                board = shuffled, shuffleReady = false,
                shuffleTokens = shuffleTokens, passthroughTokens = passthroughTokens, unfreezeTokens = unfreezeTokens, redoTokens = redoTokens, diagonalTokens = diagonalTokens,
                gameDifficulty = computeGameDifficulty(shuffled),
                hintCells = emptySet(),
                selectedCell = null
            )
        }
    }

    /**
     * Biased shuffle: randomly rearranges movable tiles with a mild clustering
     * bias for a subset of remaining goals. NOT a guaranteed win — gives the
     * player a slight edge (~20% win rate) rather than a solved board.
     *
     * Rules:
     * - No gimmes: no remaining goal may be already formed after shuffle
     * - Only ~1/3 of remaining goals get a mild clustering boost
     * - Clustering moves just 1-2 same-colored tiles closer, not all of them
     * - Early-game shuffle (many goals left) is barely better than random
     */
    private fun smartShuffle(
        board: Board,
        lockedCells: Set<CellPos>,
        completedGoalIds: Set<String>
    ): Board {
        val w = board.width
        val h = board.height

        // Identify movable cells (not frozen, not void, not locked by completed goals)
        val movable = mutableListOf<CellPos>()
        for (r in 0 until h) {
            for (c in 0 until w) {
                if (board.isVoid(r, c)) continue
                if (board.tileAt(r, c).frozen) continue
                val pos = CellPos(r, c)
                if (pos in lockedCells) continue
                movable.add(pos)
            }
        }

        val remainingGoals = level.goals.filter { it.id !in completedGoalIds }

        // Collect tiles at movable positions (preserving token flags)
        val movableTiles = movable.map { board.tileAt(it.row, it.col) }

        // Try up to 10 times to get a no-gimme result
        for (attempt in 0 until 10) {
            val shuffled = movableTiles.toMutableList().apply { shuffle() }

            // Mild clustering bias: for ~1/3 of remaining goals, nudge 1-2
            // same-colored tiles toward a random anchor of that color.
            val goalsToHelp = remainingGoals.shuffled()
                .take(max(1, remainingGoals.size / 3))
            for (goal in goalsToHelp) {
                val colorIndices = shuffled.indices.filter { shuffled[it].color == goal.color }
                if (colorIndices.size < 2) continue

                // Pick a random anchor tile of this color
                val anchor = colorIndices.random()
                val anchorPos = movable[anchor]

                // Find nearby movable slots with a DIFFERENT color
                val nearby = movable.indices.filter { idx ->
                    idx != anchor && shuffled[idx].color != goal.color &&
                        Math.abs(movable[idx].row - anchorPos.row) +
                        Math.abs(movable[idx].col - anchorPos.col) <= 2
                }
                // Find distant same-colored tiles (Manhattan > 3 from anchor)
                val distant = colorIndices.filter { idx ->
                    idx != anchor &&
                        Math.abs(movable[idx].row - anchorPos.row) +
                        Math.abs(movable[idx].col - anchorPos.col) > 3
                }

                // Swap at most 1-2 distant same-colored tiles with nearby slots
                val swapCount = minOf(nearby.size, distant.size, 2)
                for (s in 0 until swapCount) {
                    val temp = shuffled[nearby[s]]
                    shuffled[nearby[s]] = shuffled[distant[s]]
                    shuffled[distant[s]] = temp
                }
            }

            // Build candidate board
            val mutableTiles = board.tiles.map { it.toMutableList() }
            for (i in movable.indices) {
                mutableTiles[movable[i].row][movable[i].col] = shuffled[i]
            }
            val candidate = board.copy(tiles = mutableTiles.map { it.toList() })

            // No gimmes: reject if any remaining goal is already met
            val preMet = BoardEngine.evaluateGoals(candidate, remainingGoals)
            if (preMet.isEmpty()) return candidate
        }

        // Fallback: pure random (still check no gimmes but accept after 10 tries)
        val fallbackTiles = movableTiles.shuffled()
        val mutableTiles = board.tiles.map { it.toMutableList() }
        for (i in movable.indices) {
            mutableTiles[movable[i].row][movable[i].col] = fallbackTiles[i]
        }
        return board.copy(tiles = mutableTiles.map { it.toList() })
    }

    fun executeRedo() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (redoTokens <= 0) return
        viewModelScope.launch {
            val success = progressRepo.useRedoToken()
            if (!success) return@launch
            progressRepo.recordTokenUsed()
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
                diagonalMode = false,
                selectedCell = null, hintCells = emptySet()
            )
        }
    }

    fun toggleDiagonal() {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.isChallenge) return
        if (current.diagonalMode) {
            _state.value = current.copy(diagonalMode = false)
        } else if (current.diagonalTokens > 0) {
            _state.value = current.copy(
                diagonalMode = true, shuffleReady = false, unfreezeMode = false,
                passthroughActive = false,
                selectedCell = null, hintCells = emptySet()
            )
        }
    }

    /** Called by UI after the token trail animation lands — bumps the displayed count. */
    fun commitTokenCapture(icon: String) {
        val current = _state.value
        _state.value = when (icon) {
            "\uD83D\uDD00" -> current.copy(shuffleTokens = shuffleTokens, shuffleTokenAwarded = false)
            "\uD83D\uDEE1\uFE0F" -> current.copy(passthroughTokens = passthroughTokens, passthroughTokenAwarded = false)
            "\u2744\uFE0F" -> current.copy(unfreezeTokens = unfreezeTokens, unfreezeTokenAwarded = false)
            "\u21BB" -> current.copy(redoTokens = redoTokens, redoTokenAwarded = false)
            "\u2197\uFE0F" -> current.copy(diagonalTokens = diagonalTokens, diagonalTokenAwarded = false)
            else -> current
        }
    }

    fun playStarCollect() { audioManager.playStarCollect() }
    fun playTokenCapture() { audioManager.playTokenCapture() }
    fun playSwapSound() { audioManager.playSwap() }
    fun playMatchSound() { audioManager.playMatch() }
    fun playBeepSound() { audioManager.playBeep() }
    fun playClapSound() { audioManager.playWinClap() }
    fun playWinSound(stars: Int = 1) { audioManager.playWin(stars) }
    fun playPerfectGameSound() { audioManager.playPerfectGame() }
    fun playWorldUnlockSound() { audioManager.playWorldUnlock() }
    fun playChallengeMusic() {
        MusicManager.startWinMusic(context, perfectGame = true)
        audioManager.playWinClap(perfectGame = true)
    }

    fun advanceTutorial() {
        val current = _state.value
        val nextIndex = current.tutorialStepIndex + 1
        val steps = current.level.tutorialSteps ?: return
        if (nextIndex >= steps.size) {
            // Tutorial finished — persist so it never replays, and strip
            // tutorialSteps from both baseLevel and state.level so a retry
            // (resetLevel) regenerates a fresh randomized goal set.
            val levelId = current.level.id
            viewModelScope.launch { progressRepo.markTutorialSeen(levelId) }
            baseLevel = baseLevel.copy(tutorialSteps = null)
            _state.value = current.copy(
                level = current.level.copy(tutorialSteps = null),
                phase = GamePhase.PLAYING,
                tutorialStepIndex = nextIndex
            )
        } else {
            _state.value = current.copy(tutorialStepIndex = nextIndex)
        }
    }

    fun showSolution() {
        val steps = precomputedSolution
        if (steps != null) {
            _state.value = _state.value.copy(
                solutionSteps = steps, phase = GamePhase.SHOWING_SOLUTION
            )
            return
        }
        // Solution was lost (e.g. cleared by async recompute) — try to find it now
        val initialBoard = _state.value.initialBoard ?: return
        val goals = level.goals
        val moves = adjustedMaxMoves
        val diff = difficulty
        _state.value = _state.value.copy(boardGenerating = true)
        viewModelScope.launch {
            val solution = withContext(Dispatchers.Default) {
                HintSolver.findSolution(initialBoard, goals, moves, diff, beamWidth = 200)
            }
            if (solution != null) {
                precomputedSolution = solution
                _state.value = _state.value.copy(
                    solutionSteps = solution, phase = GamePhase.SHOWING_SOLUTION,
                    boardGenerating = false, hasSolution = true
                )
            } else {
                _state.value = _state.value.copy(boardGenerating = false, hasSolution = false)
            }
        }
    }

    fun dismissSolution() {
        _state.value = _state.value.copy(phase = GamePhase.LOST, solutionSteps = null)
    }

    fun commitWinResult() {
        if (winResultCommitted) return
        winResultCommitted = true
        viewModelScope.launch {
            val state = _state.value
            if (state.isMasterMode) {
                // Master Mode win: persist to MasterModeRepository
                val mState = masterModeState ?: return@launch
                masterModeRepo?.recordWin(pendingWinStars)
                if (state.isChallenge) {
                    masterModeRepo?.recordChallengeCompletion()
                    // Challenge rounds still award bonus tokens
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
                // Submit to Firebase leaderboard
                val profile = profileRepo.loadProfile()
                if (profile.leaderboardOptIn) {
                    try {
                        val leaderboardRepo = LeaderboardRepository()
                        val emoji = com.squaregarden.ui.components.getAvatar(profile.avatarId).emoji
                        leaderboardRepo.submitMasterModeStars(
                            profile.username, emoji, mState.totalMasterStars, mState.bestStreak
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("GameVM", "Master leaderboard submit failed", e)
                    }
                }
                return@launch
            }
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

            // Check if world was already complete BEFORE saving this win
            val worldForTrigger = (pendingWinLevelId - 1) / 9 + 1
            val worldWasAlreadyComplete = progressRepo.checkWorldComplete(worldForTrigger)

            progressRepo.saveLevelResult(pendingWinLevelId, pendingWinStars)

            // ── Game Complete detection (level 126 is the final level) ──
            if (pendingWinLevelId == 126 && !progressRepo.gameCompletedFlow.first()) {
                progressRepo.markGameCompleted()
                progressRepo.markCompletionOnDifficulty(difficulty.id)
                profileRepo.markMasteryBadgeEarned()
                _state.value = _state.value.copy(gameCompleted = true)
            }

            // ── Pro+ upgrade prompt (Pro player just completed level 90) ──
            if (pendingWinLevelId == 90 && difficulty == Difficulty.HARD) {
                _state.value = _state.value.copy(proUpgradePrompt = true)
            }

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
                // Diagonal token only on Pro+ worlds (11+)
                if (_state.value.level.world >= 11) {
                    progressRepo.addDiagonalToken(); diagonalTokens++
                    _state.value = _state.value.copy(
                        diagonalTokens = diagonalTokens,
                        diagonalTokenAwarded = true
                    )
                }
            }
            if (result == -1) {
                audioManager.playLifeRestored()
                _state.value = _state.value.copy(lifeRestored = true)
            }

            // Submit scores to Firebase leaderboard
            val profile = profileRepo.loadProfile()
            if (profile.leaderboardOptIn) {
                try {
                    val leaderboardRepo = LeaderboardRepository()
                    val totalStars = progressRepo.totalStarsFlow.first()
                    val progress = progressRepo.loadProgress()
                    val highestLevel = progress.highestUnlockedLevel(effectiveStartingLevel)
                    val emoji = com.squaregarden.ui.components.getAvatar(profile.avatarId).emoji
                    leaderboardRepo.submitTotalStars(
                        profile.username, emoji, difficulty, totalStars, highestLevel
                    )
                } catch (e: Exception) {
                    android.util.Log.w("GameVM", "Leaderboard submit failed", e)
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
                        // Only trigger if THIS win is what completed the world
                        !worldWasAlreadyComplete &&
                            progressRepo.checkWorldComplete(world) &&
                            !progressRepo.hasOvergrownTriggered(world)
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
        val effectiveStartWorld = (effectiveStartingLevel - 1) / 9 + 1
        val startingWorld = effectiveStartWorld
        // Casual/Pro+ use new thresholds; Standard/Pro use legacy base × starMultiplier
        data class WT(val id: Int, val casualBase: Int, val legacyBase: Int, val name: String)
        val worldThresholds = listOf(
            WT(2, 14, 7, "Blooming Meadow"),
            WT(3, 32, 14, "Ancient Grove"),
            WT(4, 55, 18, "Crystal Cavern"),
            WT(5, 80, 42, "Shattered Isles"),
            WT(6, 108, 68, "Void Fortress"),
            WT(7, 140, 98, "Molten Core"),
            WT(8, 176, 132, "Starfall Summit"),
            WT(9, 218, 172, "Abyssal Depths"),
            WT(10, 270, 240, "Prism Citadel")
        )
        for (wt in worldThresholds) {
            if (wt.id <= startingWorld) continue
            val threshold = when (difficulty) {
                Difficulty.MEDIUM, Difficulty.HARD -> wt.legacyBase * difficulty.starMultiplier
                else -> wt.casualBase
            }
            if (oldStars < threshold && newStars >= threshold) return wt.name
        }
        return null
    }

    // ── Challenge-specific methods ──

    fun overgrownAcceptRetry() {
        val cs = _state.value.challengeState ?: return
        if (cs.type != ChallengeType.OVERGROWN || cs.triesRemaining <= 1) return
        // Show loading state while generating board off main thread
        _state.value = _state.value.copy(phase = GamePhase.SCRAMBLING, boardGenerating = true)
        viewModelScope.launch {
            overgrownRetry(cs.triesRemaining - 1)
        }
    }

    fun overgrownDeclineRetry() {
        val cs = _state.value.challengeState ?: return
        if (cs.type != ChallengeType.OVERGROWN) return
        val finalStars = cs.overgrownStarScore.coerceAtLeast(1)
        winResultCommitted = false
        pendingWinLevelId = _state.value.level.id
        pendingWinStars = finalStars
        _state.value = _state.value.copy(
            phase = GamePhase.WON,
            starsAwarded = finalStars
        )
    }

    private suspend fun overgrownRetry(triesLeft: Int) {
        // Stars reset per try — player forfeits this try's stars by choosing retry
        val prevChalState = _state.value.challengeState
        val nextMultiplier = (prevChalState?.overgrownTryMultiplier ?: 1) + 1

        // Generate solvable board off main thread
        val genResult = withContext(Dispatchers.Default) {
            val deadline = System.currentTimeMillis() + 3000L
            var curLevel = level
            var curMoves = adjustedMaxMoves
            var result: Pair<Board, List<Pair<CellPos, CellPos>>?>
            var attempts = 0
            do {
                if (attempts > 0) {
                    curLevel = ChallengeGenerator.generateLevel(ChallengeType.OVERGROWN, difficulty)
                    curMoves = curLevel.maxMoves
                    level = curLevel // generateBoardWithSolution reads level.goals
                }
                result = generateBoardWithSolution(curMoves, deadline)
                attempts++
            } while (result.second == null && attempts < 20 && System.currentTimeMillis() < deadline)
            Triple(curLevel, curMoves, result)
        }
        level = genResult.first
        adjustedMaxMoves = genResult.second
        val (board, solution) = genResult.third
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
            diagonalTokens = diagonalTokens,
            phase = GamePhase.SCRAMBLING,
            challengeState = ChallengeState(
                type = ChallengeType.OVERGROWN,
                triesRemaining = triesLeft,
                overgrownStarScore = 0,
                goalsCleared = 0,
                overgrownTryMultiplier = nextMultiplier
            )
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

    private fun revealAllCells() {
        val current = _state.value
        val cs = current.challengeState ?: return
        val all = mutableSetOf<CellPos>()
        for (r in 0 until current.board.height) {
            for (c in 0 until current.board.width) {
                if (!current.board.isVoid(r, c)) all.add(CellPos(r, c))
            }
        }
        _state.value = current.copy(
            challengeState = cs.copy(revealedCells = all)
        )
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
