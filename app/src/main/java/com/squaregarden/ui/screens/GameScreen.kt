package com.squaregarden.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.squaregarden.data.ProgressRepository
import com.squaregarden.logic.BoardEngine
import com.squaregarden.logic.PatternMatcher
import com.squaregarden.model.*
import com.squaregarden.ui.components.*
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import com.squaregarden.viewmodel.GameViewModel
import com.squaregarden.viewmodel.GameViewModelFactory

@Composable
fun GameScreen(
    levelId: Int,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(context.applicationContext, levelId)
    )
    val state by viewModel.state.collectAsState()
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    viewModel.activity = context as? android.app.Activity
    val progressRepo = remember { ProgressRepository(context) }
    val scope = rememberCoroutineScope()
    var isFavorite by remember { mutableStateOf(false) }
    LaunchedEffect(levelId) {
        val progress = progressRepo.loadProgress()
        isFavorite = levelId in progress.favoriteLevels
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = if (isCompact) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasNotMoved = state.movesRemaining == state.level.maxMoves
            val canGoBack = hasNotMoved || state.phase == GamePhase.WON || state.phase == GamePhase.LOST
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = canGoBack
            ) {
                Text(
                    text = "\u2190",
                    fontSize = if (isCompact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canGoBack) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            Text(
                text = state.level.name,
                fontFamily = com.squaregarden.ui.theme.DisplayFontFamily,
                fontSize = if (isCompact) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            TextButton(
                onClick = {
                    scope.launch {
                        isFavorite = progressRepo.toggleFavorite(levelId)
                    }
                },
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = if (isFavorite) "\u2605" else "\u2606",
                    fontSize = if (isCompact) 18.sp else 22.sp,
                    color = if (isFavorite) TileYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Goals + Moves panel (combined, stacked vertically)
        GoalPanel(
            goals = state.level.goals,
            completedIds = state.completedGoalIds,
            movesRemaining = state.movesRemaining,
            movesMax = state.level.maxMoves,
            gameDifficulty = state.gameDifficulty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 10.dp else 16.dp,
                    vertical = if (isCompact) 3.dp else 6.dp
                )
        )

        // Game board — full width on phones & 7" tablets, padded on 10"+
        val boardHPad = if (LocalConfiguration.current.screenWidthDp < 800) 0.dp else 12.dp
        GameBoardCanvas(
            board = state.board,
            selectedCell = state.selectedCell,
            hintCells = state.hintCells,
            swapAnim = state.swapAnim,
            completedGoalCells = state.completedGoalCells.values.flatten().toSet(),
            passthroughActive = state.passthroughActive,
            onDragSwap = { from, to -> viewModel.onDragSwap(from, to) },
            onCellTapped = { row, col -> viewModel.onCellTapped(row, col) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = boardHPad, vertical = 4.dp)
        )

        // Bottom bar — circular action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            ActionCircle(
                icon = "\uD83D\uDCA1",
                label = "Hint",
                onClick = { viewModel.requestHint() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            ActionCircle(
                icon = "\uD83D\uDD00",
                label = if (state.shuffleReady) "Tap" else "\u00D7${state.shuffleTokens}",
                onClick = { viewModel.toggleShuffle() },
                enabled = state.shuffleReady || (state.shuffleTokens > 0 && state.phase == GamePhase.PLAYING),
                containerColor = if (state.shuffleReady) TileYellow else null,
                contentColor = if (state.shuffleReady) DarkSage else null
            )
            ActionCircle(
                icon = "\uD83D\uDEE1\uFE0F",
                label = if (state.passthroughActive) "On" else "\u00D7${state.passthroughTokens}",
                onClick = { viewModel.togglePassthrough() },
                enabled = state.passthroughActive || (state.passthroughTokens > 0 && state.phase == GamePhase.PLAYING && state.completedGoalIds.isNotEmpty()),
                containerColor = if (state.passthroughActive) Sage else null,
                contentColor = if (state.passthroughActive) SoftWhite else null
            )
            ActionCircle(
                icon = "\u2744\uFE0F",
                label = if (state.unfreezeMode) "Tap" else "\u00D7${state.unfreezeTokens}",
                onClick = { viewModel.toggleUnfreeze() },
                enabled = state.unfreezeMode || (state.unfreezeTokens > 0 && state.phase == GamePhase.PLAYING),
                containerColor = if (state.unfreezeMode) TileBlue else null,
                contentColor = if (state.unfreezeMode) SoftWhite else null
            )
            ActionCircle(
                icon = "\u21BB",
                label = "Redo",
                onClick = { viewModel.resetLevel() }
            )
        }
    }

    // Win overlay with confetti + star trail + dialog (all in same window layer)
    if (state.phase == GamePhase.WON) {
        val stars = state.starsAwarded

        // Confetti behind everything
        ConfettiOverlay(stars = stars)

        // Fullscreen overlay instead of Dialog so star trail renders on top
        WinOverlay(
            stars = stars,
            levelName = state.level.name,
            unlockedWorldName = state.unlockedWorldName,
            shuffleTokenAwarded = state.shuffleTokenAwarded,
            passthroughTokenAwarded = state.passthroughTokenAwarded,
            unfreezeTokenAwarded = state.unfreezeTokenAwarded,
            onStarLanded = { viewModel.playStarCollect() },
            onAllStarsLanded = { viewModel.commitWinResult() },
            onNext = if (state.level.id < 90) {
                {
                    viewModel.commitWinResult()
                    val nextId = state.level.id + 1
                    navController.navigate(Screen.Game.create(nextId)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            } else null,
            onMenu = {
                viewModel.commitWinResult()
                navController.popBackStack()
            }
        )
    }

    // Life restored celebration splash
    if (state.phase == GamePhase.WON && state.lifeRestored) {
        ConfettiOverlay(stars = 3)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SoftWhite
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "\u2764\uFE0F",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Life Restored!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSage
                    )
                    Text(
                        text = "Great streak! You earned a life back.",
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.commitWinResult()
                                navController.popBackStack()
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Menu", fontSize = 13.sp)
                        }
                        if (state.level.id < 90) {
                            Button(
                                onClick = {
                                    viewModel.commitWinResult()
                                    val nextId = state.level.id + 1
                                    navController.navigate(Screen.Game.create(nextId)) {
                                        popUpTo(Screen.Game.route) { inclusive = true }
                                    }
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Next Level", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Lose dialog
    if (state.phase == GamePhase.LOST) {
        LoseDialog(
            onRetry = { viewModel.resetLevel() },
            onMenu = { navController.popBackStack() },
            onShowSolution = if (state.hasSolution) {{ viewModel.showSolution() }} else null
        )
    }

    // Solution replay overlay
    val solutionSteps = state.solutionSteps
    val initialBoard = state.initialBoard
    if (state.phase == GamePhase.SHOWING_SOLUTION && solutionSteps != null && initialBoard != null) {
        SolutionReplayOverlay(
            initialBoard = initialBoard,
            steps = solutionSteps,
            goals = state.level.goals,
            onReplay = {},
            onClose = { viewModel.dismissSolution() },
            onSwapSound = { viewModel.playSwapSound() },
            onMatchSound = { viewModel.playMatchSound() },
            onWinSound = { viewModel.playWinSound() }
        )
    }

    // Tutorial overlay
    state.level.tutorialSteps?.getOrNull(state.tutorialStepIndex)?.let { step ->
        if (state.phase == GamePhase.TUTORIAL_PAUSE) {
            TutorialOverlay(
                message = step.message,
                onDismiss = { viewModel.advanceTutorial() }
            )
        }
    }
}

@Composable
private fun WinOverlay(stars: Int, levelName: String, unlockedWorldName: String? = null, shuffleTokenAwarded: Boolean = false, passthroughTokenAwarded: Boolean = false, unfreezeTokenAwarded: Boolean = false, onStarLanded: () -> Unit = {}, onAllStarsLanded: () -> Unit = {}, onNext: (() -> Unit)?, onMenu: () -> Unit) {
    // Pulsing scale animation for the star display
    val infiniteTransition = rememberInfiniteTransition(label = "starPulse")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // World unlock banner bounce animation
    val unlockScale by animateFloatAsState(
        targetValue = if (unlockedWorldName != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "unlockScale"
    )

    // Fullscreen overlay (same window layer — not a Dialog)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Win card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SoftWhite)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val headline = remember {
                    listOf(
                        "Congratulations!",
                        "You did it!",
                        "Brilliant!",
                        "Amazing!",
                        "Well played!",
                        "Superb!",
                        "Fantastic!",
                        "Wonderful!",
                        "Bravo!",
                        "Nicely done!",
                        "Spectacular!",
                        "Outstanding!"
                    ).random()
                }
                Text(
                    text = headline,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSage
                )

                Text(
                    text = "You've won $stars ${if (stars == 1) "star" else "stars"}!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TileYellow
                )

                Spacer(modifier = Modifier.height(4.dp))

                StarDisplay(
                    stars = stars,
                    fontSize = 40.sp,
                    modifier = Modifier.scale(starScale)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = remember(stars) {
                    when (stars) {
                        3 -> listOf(
                            "Perfect! Flawless victory!",
                            "Three stars! You're a natural!",
                            "Maximum stars! Incredible!",
                            "Perfection! Not a move wasted!",
                            "Masterful! A perfect score!",
                            "Stunning! All three stars!"
                        ).random()
                        2 -> listOf(
                            "Great job! Almost perfect!",
                            "So close to perfection!",
                            "Two stars! Impressive work!",
                            "Nearly flawless! Great effort!",
                            "Solid win! One more star awaits!",
                            "Strong finish! Can you get three?"
                        ).random()
                        else -> listOf(
                            "Well done! Try again for more stars!",
                            "A win is a win! Keep going!",
                            "Good start! Room to grow!",
                            "You cleared it! Aim higher next time!",
                            "Nice work! More stars await!",
                            "Victory! Replay for a better score!"
                        ).random()
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = WarmBrown,
                    textAlign = TextAlign.Center
                )

                // World unlock celebration
                if (unlockedWorldName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Sage.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.scale(unlockScale)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "New World Unlocked!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Sage
                            )
                            Text(
                                text = unlockedWorldName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkSage
                            )
                        }
                    }
                }

                // Shuffle token reward
                if (shuffleTokenAwarded) {
                    val tokenScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(800) // wait for world unlock card to appear
                        tokenScale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TileYellow.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.scale(tokenScale.value)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Shuffle Token Earned!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmBrown
                            )
                            Text(
                                text = "+1 \uD83D\uDD00",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkSage
                            )
                        }
                    }
                }

                // Passthrough token reward
                if (passthroughTokenAwarded) {
                    val ptScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(if (shuffleTokenAwarded) 1600L else 800L)
                        ptScale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Sage.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.scale(ptScale.value)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Passthrough Token Earned!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Sage
                            )
                            Text(
                                text = "+1 \uD83D\uDEE1\uFE0F",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkSage
                            )
                        }
                    }
                }

                // Unfreeze token reward
                if (unfreezeTokenAwarded) {
                    val ufScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(when {
                            shuffleTokenAwarded && passthroughTokenAwarded -> 2400L
                            shuffleTokenAwarded || passthroughTokenAwarded -> 1600L
                            else -> 800L
                        })
                        ufScale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TileBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.scale(ufScale.value)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Unfreeze Token Earned!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TileBlue
                            )
                            Text(
                                text = "+1 \u2744\uFE0F",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkSage
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onMenu, shape = RoundedCornerShape(20.dp)) {
                        Text("Menu")
                    }
                    if (onNext != null) {
                        Button(onClick = onNext, shape = RoundedCornerShape(20.dp)) {
                            Text("Next Level")
                        }
                    }
                }
            }
        }

        // Star trail flies from card center to top-right (badge area)
        StarTrailOverlay(
            starCount = stars,
            targetOffset = Offset.Zero,
            onComplete = onAllStarsLanded,
            onStarLanded = onStarLanded
        )
    }
}

@Composable
private fun LoseDialog(onRetry: () -> Unit, onMenu: () -> Unit, onShowSolution: (() -> Unit)?) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SoftWhite)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Out of Moves",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TileRed
                )

                Text(
                    text = "Don't give up!\nTry a different approach.",
                    fontSize = 14.sp,
                    color = WarmBrown,
                    textAlign = TextAlign.Center
                )

                if (onShowSolution != null) {
                    Button(
                        onClick = onShowSolution,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Show Solution", color = MaterialTheme.colorScheme.onTertiary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onMenu,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Menu", maxLines = 1, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try Again", maxLines = 1, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SolutionReplayOverlay(
    initialBoard: Board,
    steps: List<Pair<CellPos, CellPos>>,
    goals: List<Goal>,
    onReplay: () -> Unit,
    onClose: () -> Unit,
    onSwapSound: () -> Unit = {},
    onMatchSound: () -> Unit = {},
    onWinSound: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(-1) }
    var replayBoard by remember { mutableStateOf(initialBoard) }
    var completedGoalIds by remember { mutableStateOf(emptySet<String>()) }
    var completedGoalCells by remember { mutableStateOf<Map<String, Set<CellPos>>>(emptyMap()) }
    var swapAnim by remember { mutableStateOf<SwapAnimation?>(null) }
    var replayKey by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    // Reset state when replaying
    LaunchedEffect(replayKey) {
        currentStep = -1
        replayBoard = initialBoard
        completedGoalIds = emptySet()
        completedGoalCells = emptyMap()
        swapAnim = null
        finished = false

        delay(600)

        for (i in steps.indices) {
            currentStep = i
            val from = steps[i].first
            val to = steps[i].second

            // Animate swap
            val animSteps = 15
            val stepDelay = 17L
            for (frame in 1..animSteps) {
                val t = frame.toFloat() / animSteps
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                swapAnim = SwapAnimation(from, to, eased)
                delay(stepDelay)
            }

            // Apply swap
            replayBoard = BoardEngine.executeSwap(replayBoard, from, to)
            swapAnim = null
            onSwapSound()

            // Evaluate goals cumulatively (once met, stays met — matches game behavior)
            val prevCompleted = completedGoalIds
            val metNow = BoardEngine.evaluateGoals(replayBoard, goals)
            completedGoalIds = completedGoalIds + metNow
            if ((completedGoalIds - prevCompleted).isNotEmpty()) {
                onMatchSound()
            }
            val updatedGoalCells = completedGoalCells.toMutableMap()
            for (goal in goals) {
                if (goal.id in completedGoalIds) {
                    val cells = PatternMatcher.findGoalPositions(replayBoard, goal)
                    if (cells != null) updatedGoalCells[goal.id] = cells
                }
            }
            completedGoalCells = updatedGoalCells

            // Check if all goals met
            if (BoardEngine.checkWin(completedGoalIds, goals)) {
                finished = true
                onWinSound()
                break
            }

            delay(500)
        }
        if (!finished) finished = true
    }

    // Fullscreen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SoftWhite),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Solution",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSage
                )

                val totalGoals = goals.size
                val doneGoals = completedGoalIds.size
                val allDone = BoardEngine.checkWin(completedGoalIds, goals)
                Text(
                    text = if (allDone)
                        "Solved in ${currentStep + 1} moves!"
                    else
                        "Move ${(currentStep + 1).coerceAtLeast(0)} of ${steps.size}  |  Goals: $doneGoals/$totalGoals",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (allDone) Sage else WarmBrown,
                    textAlign = TextAlign.Center
                )

                // Board replay
                GameBoardCanvas(
                    board = replayBoard,
                    selectedCell = null,
                    hintCells = emptySet(),
                    swapAnim = swapAnim,
                    completedGoalCells = completedGoalCells.values.flatten().toSet(),
                    onDragSwap = { _, _ -> },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = { replayKey++ },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Replay")
                    }
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SoftWhite)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = DeepForest,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
private fun ActionCircle(
    icon: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color? = null,
    contentColor: androidx.compose.ui.graphics.Color? = null
) {
    val bgColor = containerColor ?: MaterialTheme.colorScheme.surface
    val fgColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val alpha = if (enabled) 1f else 0.4f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = { if (enabled) onClick() },
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = bgColor.copy(alpha = alpha),
            border = if (containerColor == null) BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = alpha)
            ) else null,
            tonalElevation = if (containerColor != null) 2.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(icon, fontSize = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fgColor.copy(alpha = alpha),
            maxLines = 1
        )
    }
}
