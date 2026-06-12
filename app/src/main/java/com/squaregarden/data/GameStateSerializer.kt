package com.squaregarden.data

import com.squaregarden.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Minimal snapshot of an in-progress game, sufficient to fully restore it. */
data class SavedGameData(
    val levelId: Int,
    val level: Level,
    val board: Board,
    val movesRemaining: Int,
    val adjustedMaxMoves: Int,
    val completedGoalIds: Set<String>,
    val completedGoalCells: Map<String, Set<CellPos>>,
    val difficulty: Difficulty,
    val gameDifficulty: GameDifficulty,
    val shuffleTokens: Int,
    val passthroughTokens: Int,
    val unfreezeTokens: Int,
    val redoTokens: Int,
    val diagonalTokens: Int,
    val undoTokens: Int,
    val passthroughActive: Boolean,
    val unfreezeMode: Boolean,
    val diagonalMode: Boolean,
    val challengeState: ChallengeState?,
    val masterModeState: MasterModeState?,
    val masterTier: MasterTier?,
    val usedPowerUpThisGame: Boolean,
    val hasMovedSinceReset: Boolean
)

/** JSON serialization/deserialization for in-progress game state using org.json. */
object GameStateSerializer {

    private const val VERSION = 1

    // ── Top-level ──

    fun serialize(
        levelId: Int,
        level: Level,
        board: Board,
        movesRemaining: Int,
        adjustedMaxMoves: Int,
        completedGoalIds: Set<String>,
        completedGoalCells: Map<String, Set<CellPos>>,
        difficulty: Difficulty,
        gameDifficulty: GameDifficulty,
        shuffleTokens: Int,
        passthroughTokens: Int,
        unfreezeTokens: Int,
        redoTokens: Int,
        diagonalTokens: Int,
        undoTokens: Int,
        passthroughActive: Boolean,
        unfreezeMode: Boolean,
        diagonalMode: Boolean,
        challengeState: ChallengeState?,
        masterModeState: MasterModeState?,
        masterTier: MasterTier?,
        usedPowerUpThisGame: Boolean,
        hasMovedSinceReset: Boolean
    ): String {
        val json = JSONObject()
        json.put("version", VERSION)
        json.put("levelId", levelId)
        json.put("savedAt", System.currentTimeMillis())
        json.put("level", serializeLevel(level))
        json.put("board", serializeBoard(board))
        json.put("movesRemaining", movesRemaining)
        json.put("adjustedMaxMoves", adjustedMaxMoves)
        json.put("completedGoalIds", JSONArray(completedGoalIds.toList()))
        json.put("completedGoalCells", serializeGoalCellsMap(completedGoalCells))
        json.put("difficulty", difficulty.id)
        json.put("gameDifficulty", gameDifficulty.name)
        json.put("shuffleTokens", shuffleTokens)
        json.put("passthroughTokens", passthroughTokens)
        json.put("unfreezeTokens", unfreezeTokens)
        json.put("redoTokens", redoTokens)
        json.put("diagonalTokens", diagonalTokens)
        json.put("undoTokens", undoTokens)
        json.put("passthroughActive", passthroughActive)
        json.put("unfreezeMode", unfreezeMode)
        json.put("diagonalMode", diagonalMode)
        json.put("usedPowerUpThisGame", usedPowerUpThisGame)
        json.put("hasMovedSinceReset", hasMovedSinceReset)
        if (challengeState != null) json.put("challengeState", serializeChallengeState(challengeState))
        if (masterModeState != null) json.put("masterModeState", serializeMasterModeState(masterModeState))
        if (masterTier != null) json.put("masterTier", masterTier.name)
        return json.toString()
    }

    fun deserialize(jsonStr: String): SavedGameData {
        val json = JSONObject(jsonStr)
        return SavedGameData(
            levelId = json.getInt("levelId"),
            level = deserializeLevel(json.getJSONObject("level")),
            board = deserializeBoard(json.getJSONObject("board")),
            movesRemaining = json.getInt("movesRemaining"),
            adjustedMaxMoves = json.getInt("adjustedMaxMoves"),
            completedGoalIds = deserializeStringSet(json.getJSONArray("completedGoalIds")),
            completedGoalCells = deserializeGoalCellsMap(json.getJSONObject("completedGoalCells")),
            difficulty = Difficulty.fromId(json.getString("difficulty")),
            gameDifficulty = GameDifficulty.valueOf(json.getString("gameDifficulty")),
            shuffleTokens = json.getInt("shuffleTokens"),
            passthroughTokens = json.getInt("passthroughTokens"),
            unfreezeTokens = json.getInt("unfreezeTokens"),
            redoTokens = json.getInt("redoTokens"),
            diagonalTokens = json.getInt("diagonalTokens"),
            undoTokens = json.optInt("undoTokens", 0),
            passthroughActive = json.optBoolean("passthroughActive", false),
            unfreezeMode = json.optBoolean("unfreezeMode", false),
            diagonalMode = json.optBoolean("diagonalMode", false),
            challengeState = if (json.has("challengeState")) deserializeChallengeState(json.getJSONObject("challengeState")) else null,
            masterModeState = if (json.has("masterModeState")) deserializeMasterModeState(json.getJSONObject("masterModeState")) else null,
            masterTier = if (json.has("masterTier")) MasterTier.valueOf(json.getString("masterTier")) else null,
            usedPowerUpThisGame = json.optBoolean("usedPowerUpThisGame", false),
            hasMovedSinceReset = json.optBoolean("hasMovedSinceReset", false)
        )
    }

    /** Quick extraction of levelId without full deserialization. */
    fun extractLevelId(jsonStr: String): Int = JSONObject(jsonStr).getInt("levelId")

    // ── Board ──

    private fun serializeBoard(board: Board): JSONObject {
        val json = JSONObject()
        json.put("width", board.width)
        json.put("height", board.height)
        val tilesArr = JSONArray()
        for (row in board.tiles) {
            val rowArr = JSONArray()
            for (tile in row) rowArr.put(serializeTile(tile))
            tilesArr.put(rowArr)
        }
        json.put("tiles", tilesArr)
        json.put("voids", serializeCellPosSet(board.voids))
        return json
    }

    private fun deserializeBoard(json: JSONObject): Board {
        val tilesArr = json.getJSONArray("tiles")
        val tiles = (0 until tilesArr.length()).map { r ->
            val rowArr = tilesArr.getJSONArray(r)
            (0 until rowArr.length()).map { c -> deserializeTile(rowArr.getJSONObject(c)) }
        }
        return Board(
            width = json.getInt("width"),
            height = json.getInt("height"),
            tiles = tiles,
            voids = if (json.has("voids")) deserializeCellPosSet(json.getJSONArray("voids")) else emptySet()
        )
    }

    // ── Tile ──

    private fun serializeTile(tile: Tile): JSONObject {
        val json = JSONObject()
        json.put("c", tile.color.name)
        if (tile.frozen) json.put("f", true)
        if (tile.redo) json.put("r", true)
        if (tile.shuffleToken) json.put("st", true)
        if (tile.passthroughToken) json.put("pt", true)
        if (tile.unfreezeToken) json.put("ut", true)
        if (tile.diagonalToken) json.put("dt", true)
        if (tile.undoToken) json.put("nt", true)
        return json
    }

    private fun deserializeTile(json: JSONObject): Tile {
        return Tile(
            color = TileColor.valueOf(json.getString("c")),
            frozen = json.optBoolean("f", false),
            redo = json.optBoolean("r", false),
            shuffleToken = json.optBoolean("st", false),
            passthroughToken = json.optBoolean("pt", false),
            unfreezeToken = json.optBoolean("ut", false),
            diagonalToken = json.optBoolean("dt", false),
            undoToken = json.optBoolean("nt", false)
        )
    }

    // ── Level ──

    private fun serializeLevel(level: Level): JSONObject {
        val json = JSONObject()
        json.put("id", level.id)
        json.put("world", level.world)
        json.put("name", level.name)
        json.put("boardWidth", level.boardWidth)
        json.put("boardHeight", level.boardHeight)
        json.put("maxMoves", level.maxMoves)
        json.put("goals", serializeGoals(level.goals))
        val st = JSONObject()
        st.put("twoStar", level.starThresholds.twoStar)
        st.put("threeStar", level.starThresholds.threeStar)
        json.put("starThresholds", st)
        json.put("frozenCells", serializeCellPosSet(level.frozenCells))
        json.put("voidCells", serializeCellPosSet(level.voidCells))
        // initialTiles and tutorialSteps omitted — not needed for restore
        return json
    }

    private fun deserializeLevel(json: JSONObject): Level {
        val st = json.getJSONObject("starThresholds")
        return Level(
            id = json.getInt("id"),
            world = json.getInt("world"),
            name = json.getString("name"),
            boardWidth = json.getInt("boardWidth"),
            boardHeight = json.getInt("boardHeight"),
            maxMoves = json.getInt("maxMoves"),
            initialTiles = emptyList(), // Not needed for restore
            goals = deserializeGoals(json.getJSONArray("goals")),
            starThresholds = StarThresholds(st.getInt("twoStar"), st.getInt("threeStar")),
            frozenCells = if (json.has("frozenCells")) deserializeCellPosSet(json.getJSONArray("frozenCells")) else emptySet(),
            voidCells = if (json.has("voidCells")) deserializeCellPosSet(json.getJSONArray("voidCells")) else emptySet()
        )
    }

    // ── Goals ──

    private fun serializeGoals(goals: List<Goal>): JSONArray {
        val arr = JSONArray()
        for (goal in goals) {
            val g = JSONObject()
            when (goal) {
                is Goal.Line -> {
                    g.put("type", "line")
                    g.put("color", goal.color.name)
                    g.put("length", goal.length)
                }
                is Goal.Square -> {
                    g.put("type", "square")
                    g.put("color", goal.color.name)
                }
                is Goal.Shape -> {
                    g.put("type", "shape")
                    g.put("color", goal.color.name)
                    g.put("shapeType", goal.shapeType.name)
                }
            }
            arr.put(g)
        }
        return arr
    }

    private fun deserializeGoals(arr: JSONArray): List<Goal> {
        return (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            val color = TileColor.valueOf(g.getString("color"))
            when (g.getString("type")) {
                "line" -> Goal.Line(color, g.getInt("length"))
                "square" -> Goal.Square(color)
                "shape" -> Goal.Shape(color, ShapeType.valueOf(g.getString("shapeType")))
                else -> throw IllegalArgumentException("Unknown goal type: ${g.getString("type")}")
            }
        }
    }

    // ── ChallengeState ──

    private fun serializeChallengeState(cs: ChallengeState): JSONObject {
        val json = JSONObject()
        json.put("type", cs.type.name)
        json.put("timerMillisRemaining", cs.timerMillisRemaining)
        json.put("goalsCleared", cs.goalsCleared)
        json.put("comboCount", cs.comboCount)
        json.put("comboMultiplier", cs.comboMultiplier)
        json.put("blitzStarScore", cs.blitzStarScore)
        json.put("triesRemaining", cs.triesRemaining)
        json.put("overgrownStarScore", cs.overgrownStarScore)
        json.put("overgrownTryMultiplier", cs.overgrownTryMultiplier)
        json.put("movesSinceLastScramble", cs.movesSinceLastScramble)
        json.put("revealedCells", serializeCellPosSet(cs.revealedCells))
        json.put("initialRevealDone", cs.initialRevealDone)
        json.put("freezeMoveCounter", cs.freezeMoveCounter)
        json.put("rotationMoveCounter", cs.rotationMoveCounter)
        json.put("decayMoveCounter", cs.decayMoveCounter)
        val gcm = JSONObject()
        for ((goalId, moveNum) in cs.goalCompletionMoves) gcm.put(goalId, moveNum)
        json.put("goalCompletionMoves", gcm)
        return json
    }

    private fun deserializeChallengeState(json: JSONObject): ChallengeState {
        return ChallengeState(
            type = ChallengeType.valueOf(json.getString("type")),
            timerMillisRemaining = json.getLong("timerMillisRemaining"),
            goalsCleared = json.optInt("goalsCleared", 0),
            comboCount = json.optInt("comboCount", 0),
            comboMultiplier = json.optInt("comboMultiplier", 1),
            blitzStarScore = json.optInt("blitzStarScore", 0),
            triesRemaining = json.optInt("triesRemaining", 3),
            overgrownStarScore = json.optInt("overgrownStarScore", 0),
            overgrownTryMultiplier = json.optInt("overgrownTryMultiplier", 1),
            movesSinceLastScramble = json.optInt("movesSinceLastScramble", 0),
            revealedCells = if (json.has("revealedCells")) deserializeCellPosSet(json.getJSONArray("revealedCells")) else emptySet(),
            initialRevealDone = json.optBoolean("initialRevealDone", false),
            freezeMoveCounter = json.optInt("freezeMoveCounter", 0),
            rotationMoveCounter = json.optInt("rotationMoveCounter", 0),
            decayMoveCounter = json.optInt("decayMoveCounter", 0),
            goalCompletionMoves = if (json.has("goalCompletionMoves")) {
                val gcm = json.getJSONObject("goalCompletionMoves")
                val map = mutableMapOf<String, Int>()
                for (key in gcm.keys()) map[key] = gcm.getInt(key)
                map
            } else emptyMap()
        )
    }

    // ── MasterModeState ──

    private fun serializeMasterModeState(ms: MasterModeState): JSONObject {
        val json = JSONObject()
        json.put("gamesPlayed", ms.gamesPlayed)
        json.put("gamesWon", ms.gamesWon)
        json.put("currentStreak", ms.currentStreak)
        json.put("bestStreak", ms.bestStreak)
        json.put("totalMasterStars", ms.totalMasterStars)
        json.put("sessionStars", ms.sessionStars)
        json.put("currentTier", ms.currentTier.name)
        json.put("isChallengeRound", ms.isChallengeRound)
        return json
    }

    private fun deserializeMasterModeState(json: JSONObject): MasterModeState {
        return MasterModeState(
            gamesPlayed = json.getInt("gamesPlayed"),
            gamesWon = json.getInt("gamesWon"),
            currentStreak = json.getInt("currentStreak"),
            bestStreak = json.getInt("bestStreak"),
            totalMasterStars = json.getInt("totalMasterStars"),
            sessionStars = json.optInt("sessionStars", 0),
            currentTier = MasterTier.valueOf(json.getString("currentTier")),
            isChallengeRound = json.optBoolean("isChallengeRound", false)
        )
    }

    // ── CellPos helpers ──

    private fun serializeCellPosSet(cells: Set<CellPos>): JSONArray {
        val arr = JSONArray()
        for (pos in cells) {
            val p = JSONObject()
            p.put("r", pos.row)
            p.put("c", pos.col)
            arr.put(p)
        }
        return arr
    }

    private fun deserializeCellPosSet(arr: JSONArray): Set<CellPos> {
        return (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            CellPos(p.getInt("r"), p.getInt("c"))
        }.toSet()
    }

    private fun serializeGoalCellsMap(map: Map<String, Set<CellPos>>): JSONObject {
        val json = JSONObject()
        for ((key, cells) in map) {
            json.put(key, serializeCellPosSet(cells))
        }
        return json
    }

    private fun deserializeGoalCellsMap(json: JSONObject): Map<String, Set<CellPos>> {
        val map = mutableMapOf<String, Set<CellPos>>()
        for (key in json.keys()) {
            map[key] = deserializeCellPosSet(json.getJSONArray(key))
        }
        return map
    }

    private fun deserializeStringSet(arr: JSONArray): Set<String> {
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }
}
