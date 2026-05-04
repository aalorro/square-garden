package com.patterngarden.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.patterngarden.audio.AudioManager
import com.patterngarden.data.ProfileRepository
import com.patterngarden.data.ProgressRepository
import com.patterngarden.logic.BoardEngine
import com.patterngarden.logic.HintSolver
import com.patterngarden.logic.LevelLoader
import com.patterngarden.logic.PatternMatcher
import com.patterngarden.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val progressRepo = ProgressRepository(context)
    private val profileRepo = ProfileRepository(context)
    private val audioManager = AudioManager(context)

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

            val levels = LevelLoader.loadAllLevels(context)
            level = levels.first { it.id == levelId }
            // Adjust moves based on difficulty
            adjustedMaxMoves = max(1, (level.maxMoves * difficulty.moveMultiplier).roundToInt())
            initLevel()
        }
    }

    private fun generateValidBoard(): Board {
        val colors = TileColor.entries.toTypedArray()
        // Calculate minimum tiles needed per color from goals
        val minRequired = mutableMapOf<TileColor, Int>()
        for (goal in level.goals) {
            val needed = when (goal) {
                is Goal.Line -> goal.length
                is Goal.Square -> 4
                is Goal.Shape -> goal.shapeType.offsets.size
            }
            minRequired[goal.color] = max(minRequired[goal.color] ?: 0, needed)
        }

        val totalCells = level.boardWidth * level.boardHeight
        val voids = level.voidCells
        val frozenPositions = level.frozenCells
        val playableCells = totalCells - voids.size

        repeat(200) {
            // Seed required tiles first, then fill rest randomly
            val tileList = mutableListOf<Tile>()
            for ((color, count) in minRequired) {
                repeat(count) { tileList.add(Tile(color)) }
            }
            while (tileList.size < playableCells) {
                tileList.add(Tile(colors.random()))
            }
            tileList.shuffle()

            // Reshape into 2D grid, placing voids as placeholder tiles
            var idx = 0
            val tiles = (0 until level.boardHeight).map { r ->
                (0 until level.boardWidth).map { c ->
                    val pos = CellPos(r, c)
                    if (pos in voids) {
                        Tile(TileColor.RED) // placeholder, never rendered
                    } else {
                        val tile = tileList[idx++]
                        if (pos in frozenPositions) tile.copy(frozen = true) else tile
                    }
                }
            }
            val board = Board(level.boardWidth, level.boardHeight, tiles, voids)
            val alreadyMet = BoardEngine.evaluateGoals(board, level.goals)
            if (alreadyMet.isEmpty()) return board
        }
        // Fallback
        val tileList = mutableListOf<Tile>()
        for ((color, count) in minRequired) {
            repeat(count) { tileList.add(Tile(color)) }
        }
        while (tileList.size < playableCells) {
            tileList.add(Tile(colors.random()))
        }
        tileList.shuffle()
        var idx = 0
        val tiles = (0 until level.boardHeight).map { r ->
            (0 until level.boardWidth).map { c ->
                val pos = CellPos(r, c)
                if (pos in voids) {
                    Tile(TileColor.RED)
                } else {
                    val tile = tileList[idx++]
                    if (pos in frozenPositions) tile.copy(frozen = true) else tile
                }
            }
        }
        return Board(level.boardWidth, level.boardHeight, tiles, voids)
    }

    private fun initLevel() {
        val hasTutorial = level.tutorialSteps != null
        val board = if (hasTutorial) {
            Board(
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
        } else {
            generateValidBoard()
        }
        // Create a modified level with difficulty-adjusted moves
        val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
        _state.value = GameState(
            level = adjustedLevel,
            board = board,
            movesRemaining = adjustedMaxMoves,
            difficulty = difficulty,
            initialBoard = board,
            phase = if (level.tutorialSteps != null) GamePhase.TUTORIAL_PAUSE
            else GamePhase.PLAYING
        )
    }

    fun onDragSwap(from: CellPos, to: CellPos) {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (!BoardEngine.canSwap(current.board, from, to)) return
        executeSwap(from, to)
    }

    fun onCellTapped(row: Int, col: Int) {
        val current = _state.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.board.isVoid(row, col)) return

        val tapped = CellPos(row, col)

        when {
            current.selectedCell == null -> {
                _state.value = current.copy(selectedCell = tapped, hintCells = emptySet())
                audioManager.playTap()
            }
            current.selectedCell == tapped -> {
                _state.value = current.copy(selectedCell = null)
            }
            BoardEngine.canSwap(current.board, current.selectedCell, tapped) -> {
                executeSwap(current.selectedCell, tapped)
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

        // Hard mode: block swaps that touch bordered cells
        if (crossesBorder && difficulty == Difficulty.HARD) {
            return
        }

        hasMovedSinceReset = true

        _state.value = current.copy(
            phase = GamePhase.ANIMATING,
            selectedCell = null,
            hintCells = emptySet(),
            swapAnim = SwapAnimation(from, to, 0f)
        )

        viewModelScope.launch {
            audioManager.playSwap()

            val steps = 15
            val stepDelay = 17L
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                _state.value = _state.value.copy(
                    swapAnim = SwapAnimation(from, to, eased)
                )
                delay(stepDelay)
            }

            val newBoard = BoardEngine.executeSwap(current.board, from, to)
            val newMoves = current.movesRemaining - 1

            // Medium mode: if swap crossed a border, invalidate affected goals
            var baseGoalIds = current.completedGoalIds
            var baseGoalCells = current.completedGoalCells
            var invalidatedGoals = emptySet<String>()
            if (crossesBorder && difficulty == Difficulty.MEDIUM) {
                invalidatedGoals = current.completedGoalCells.filter { (_, cells) ->
                    from in cells || to in cells
                }.keys
                baseGoalIds = baseGoalIds - invalidatedGoals
                baseGoalCells = baseGoalCells - invalidatedGoals
            }

            // Evaluate goals on new board (exclude goals just invalidated by medium border cross)
            val goalsToCheck = current.level.goals.filter { it.id !in invalidatedGoals }
            val metGoalIds = BoardEngine.evaluateGoals(newBoard, goalsToCheck)
            val newCompleted = baseGoalIds + metGoalIds

            // Build goal cell map for all currently completed goals
            val newGoalCells = baseGoalCells.toMutableMap()
            for (goal in current.level.goals) {
                if (goal.id in newCompleted) {
                    val cells = PatternMatcher.findGoalPositions(newBoard, goal)
                    if (cells != null) {
                        newGoalCells[goal.id] = cells
                    }
                } else {
                    newGoalCells.remove(goal.id)
                }
            }

            val won = BoardEngine.checkWin(newCompleted, current.level.goals)
            val lost = BoardEngine.checkLose(newMoves, won)

            val newlyCompletedGoals = newCompleted - current.completedGoalIds
            if (newlyCompletedGoals.isNotEmpty()) {
                audioManager.playMatch()
            }

            var starsAwarded = 0
            var winsNeeded = 0
            val phase = when {
                won -> {
                    val baseStars = BoardEngine.calculateStars(newMoves, current.level.starThresholds)
                    starsAwarded = baseStars * difficulty.starMultiplier
                    audioManager.playWin(baseStars)
                    progressRepo.saveLevelResult(current.level.id, starsAwarded)
                    winsNeeded = progressRepo.recordWin(difficulty.ordinal)
                    profileRepo.incrementPlayerLevel()
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
                board = newBoard,
                movesRemaining = newMoves,
                completedGoalIds = newCompleted,
                completedGoalCells = newGoalCells,
                selectedCell = null,
                hintCells = emptySet(),
                swapAnim = null,
                phase = phase,
                starsAwarded = starsAwarded,
                winsToRestoreLife = winsNeeded
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

            // Highlight the quadrant containing the best swap, not the exact cells
            val midRow = current.board.height / 2
            val midCol = current.board.width / 2
            val hintRow = (hint.first.row + hint.second.row) / 2
            val hintCol = (hint.first.col + hint.second.col) / 2
            val rowRange = if (hintRow < midRow) 0 until midRow else midRow until current.board.height
            val colRange = if (hintCol < midCol) 0 until midCol else midCol until current.board.width
            val quadrantCells = mutableSetOf<CellPos>()
            for (r in rowRange) {
                for (c in colRange) {
                    quadrantCells.add(CellPos(r, c))
                }
            }

            _state.value = current.copy(hintCells = quadrantCells)

            delay(2000)
            val latest = _state.value
            if (latest.hintCells.isNotEmpty()) {
                _state.value = latest.copy(hintCells = emptySet())
            }
        }
    }

    fun playStarCollect() {
        audioManager.playStarCollect()
    }

    fun advanceTutorial() {
        val current = _state.value
        val nextIndex = current.tutorialStepIndex + 1
        val steps = current.level.tutorialSteps ?: return
        if (nextIndex >= steps.size) {
            _state.value = current.copy(phase = GamePhase.PLAYING, tutorialStepIndex = nextIndex)
        } else {
            _state.value = current.copy(tutorialStepIndex = nextIndex)
        }
    }

    fun showSolution() {
        val current = _state.value
        val initBoard = current.initialBoard ?: return
        viewModelScope.launch {
            val steps = withContext(Dispatchers.Default) {
                HintSolver.findSolution(initBoard, current.level.goals, current.level.maxMoves)
            }
            if (steps.isNotEmpty()) {
                _state.value = _state.value.copy(
                    solutionSteps = steps,
                    phase = GamePhase.SHOWING_SOLUTION
                )
            }
        }
    }

    fun dismissSolution() {
        _state.value = _state.value.copy(
            phase = GamePhase.LOST,
            solutionSteps = null
        )
    }

    fun resetLevel() {
        val current = _state.value
        val hasTutorial = level.tutorialSteps != null
        // Only randomize if the player has made at least one move since last reset
        val board = if (hasTutorial) {
            Board(
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
        } else if (hasMovedSinceReset) {
            generateValidBoard()
        } else {
            current.board
        }
        val moves = when (difficulty) {
            Difficulty.EASY -> adjustedMaxMoves
            Difficulty.MEDIUM -> max(1, adjustedMaxMoves - 2)
            Difficulty.HARD -> if (current.movesRemaining > 0) current.movesRemaining else adjustedMaxMoves
        }
        hasMovedSinceReset = false
        val adjustedLevel = level.copy(maxMoves = adjustedMaxMoves)
        _state.value = GameState(
            level = adjustedLevel,
            board = board,
            movesRemaining = moves,
            difficulty = difficulty,
            initialBoard = board,
            phase = if (hasTutorial) GamePhase.TUTORIAL_PAUSE else GamePhase.PLAYING
        )
    }

    override fun onCleared() {
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
