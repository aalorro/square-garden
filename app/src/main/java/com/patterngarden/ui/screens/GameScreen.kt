package com.patterngarden.ui.screens

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.patterngarden.model.GamePhase
import com.patterngarden.ui.components.*
import com.patterngarden.ui.navigation.Screen
import com.patterngarden.ui.theme.*
import com.patterngarden.viewmodel.GameViewModel
import com.patterngarden.viewmodel.GameViewModelFactory
import com.patterngarden.logic.BoardEngine

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasNotMoved = state.movesRemaining == state.level.maxMoves
            val canGoBack = hasNotMoved || state.phase == GamePhase.WON || state.phase == GamePhase.LOST
            OutlinedButton(
                onClick = { navController.popBackStack() },
                enabled = canGoBack,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.5.dp,
                    if (canGoBack) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            ) {
                Text("Back", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = state.level.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            // Placeholder for symmetry
            Spacer(modifier = Modifier.width(60.dp))
        }

        // Goal panel
        GoalPanel(
            goals = state.level.goals,
            completedIds = state.completedGoalIds
        )

        // Move counter + difficulty
        MoveCounter(
            remaining = state.movesRemaining,
            max = state.level.maxMoves,
            difficultyLabel = "Difficulty: ${state.difficulty.label}"
        )

        // Game board — drag to swap in one seamless action
        GameBoardCanvas(
            board = state.board,
            selectedCell = state.selectedCell,
            hintCells = state.hintCells,
            swapAnim = state.swapAnim,
            completedGoalCells = state.completedGoalCells.values.flatten().toSet(),
            onDragSwap = { from, to -> viewModel.onDragSwap(from, to) },
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        )

        // Bottom bar with hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { viewModel.requestHint() },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Hint",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedButton(
                onClick = { viewModel.resetLevel() },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Restart")
            }
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
            onStarLanded = { viewModel.playStarCollect() },
            onNext = if (state.level.id < 49) {
                {
                    val nextId = state.level.id + 1
                    navController.navigate(Screen.Game.create(nextId)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            } else null,
            onMenu = { navController.popBackStack() }
        )
    }

    // Life restore notification
    if (state.phase == GamePhase.WON && state.winsToRestoreLife in 1..2) {
        var showNotif by remember(state.winsToRestoreLife) { mutableStateOf(true) }
        if (showNotif) {
            LaunchedEffect(state.winsToRestoreLife) {
                delay(10_000)
                showNotif = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitPointerEvent()
                            showNotif = false
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    val diffLabel = state.difficulty.label
                    Text(
                        text = "${state.winsToRestoreLife} more ${if (state.winsToRestoreLife == 1) "win" else "wins"} in $diffLabel or harder to restore a life!",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    // Lose dialog
    if (state.phase == GamePhase.LOST) {
        LoseDialog(
            onRetry = { viewModel.resetLevel() },
            onMenu = { navController.popBackStack() }
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
private fun WinOverlay(stars: Int, levelName: String, onStarLanded: () -> Unit = {}, onNext: (() -> Unit)?, onMenu: () -> Unit) {
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
                Text(
                    text = "Congratulations!",
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

                Text(
                    text = when (stars) {
                        3 -> "Perfect! Flawless victory!"
                        2 -> "Great job! Almost perfect!"
                        else -> "Well done! Try again for more stars!"
                    },
                    fontSize = 14.sp,
                    color = WarmBrown,
                    textAlign = TextAlign.Center
                )

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
            onComplete = {},
            onStarLanded = onStarLanded
        )
    }
}

@Composable
private fun LoseDialog(onRetry: () -> Unit, onMenu: () -> Unit) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onMenu, shape = RoundedCornerShape(20.dp)) {
                        Text("Menu")
                    }
                    Button(onClick = onRetry, shape = RoundedCornerShape(20.dp)) {
                        Text("Try Again")
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
