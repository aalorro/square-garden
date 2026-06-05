package com.squaregarden.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.squaregarden.audio.MusicManager
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.ProfileRepository
import kotlinx.coroutines.flow.first
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
    var showRedoConfirm by remember { mutableStateOf(false) }
    val lives by progressRepo.livesFlow.collectAsState(initial = 3)
    val cooldownUntil by progressRepo.cooldownUntilFlow.collectAsState(initial = 0L)

    // Default action for every "Back to Game" button: jump to the player's
    // current progression point — one level higher than their highest completed
    // level (floored at their skill's starting level, capped at the final level).
    // This way players resume their progress regardless of which level triggered
    // the challenge.
    val profileRepo = remember { ProfileRepository(context) }
    val backToGame: () -> Unit = {
        scope.launch {
            val progress = progressRepo.loadProgress()
            val profile = profileRepo.profileFlow.first()
            val difficulty = profile?.let { Difficulty.fromId(it.difficulty) } ?: Difficulty.MEDIUM
            val startingLevel = maxOf(
                difficulty.startingLevel,
                profile?.overrideStartingLevel ?: 0
            )
            val isPro_Plus = difficulty == Difficulty.PRO_PLUS
            val maxLevel = if (isPro_Plus) 126 else 90
            val nextLevel = if (profile?.proUpgradeDeclined == true && !isPro_Plus) {
                (37..90).random()
            } else {
                progress.highestUnlockedLevel(startingLevel).coerceAtMost(maxLevel)
            }
            // Pop current Game entry, then pop the underlying source Game WON entry if present.
            navController.popBackStack()
            if (navController.currentBackStackEntry?.destination?.route == Screen.Game.route) {
                navController.popBackStack()
            }
            navController.navigate(Screen.Game.create(nextLevel))
        }
    }

    // Block back button mid-game (prevent accidental exit during play)
    BackHandler(enabled = state.phase == GamePhase.PLAYING || state.phase == GamePhase.ANIMATING || state.phase == GamePhase.SCRAMBLING || state.phase == GamePhase.TUTORIAL_PAUSE) { }

    // Stop any music when GameScreen exits
    DisposableEffect(Unit) { onDispose { MusicManager.stopAll() } }

    // Redo confirmation dialog
    if (showRedoConfirm) {
        AlertDialog(
            onDismissRequest = { showRedoConfirm = false },
            title = { Text("Refresh Board") },
            text = { Text("This will generate a fresh board without losing a life. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showRedoConfirm = false
                    viewModel.executeRedo()
                }) { Text("Yes", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRedoConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Safety net: if cooldown is active, pop back immediately (skip for challenges)
    LaunchedEffect(lives, cooldownUntil) {
        if ((levelId > 0 || levelId == GameViewModel.MASTER_MODE_SIGNAL) && lives <= 0 && cooldownUntil > System.currentTimeMillis()) {
            navController.popBackStack()
        }
    }

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
            val challengeBlocked = state.isChallenge && state.phase != GamePhase.WON && state.phase != GamePhase.LOST
            val canGoBack = !challengeBlocked && (hasNotMoved || state.phase == GamePhase.WON || state.phase == GamePhase.LOST)
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
            if (state.isMasterMode) {
                // Master Mode: show tier label instead of level number
                val tierLabel = state.masterTier?.label ?: "Master Mode"
                Text(
                    text = tierLabel,
                    fontFamily = com.squaregarden.ui.theme.DisplayFontFamily,
                    fontSize = if (isCompact) 13.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB8860B),
                    maxLines = 1
                )
            } else if (isCompact) {
                var showNumber by remember(state.level.id) { mutableStateOf(true) }
                Text(
                    text = if (showNumber) "${state.level.name} (${state.level.id})"
                           else state.level.name,
                    fontFamily = com.squaregarden.ui.theme.DisplayFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    onTextLayout = { result ->
                        if (showNumber && result.hasVisualOverflow) showNumber = false
                    }
                )
            } else {
                Text(
                    text = "${state.level.name} (${state.level.id})",
                    fontFamily = com.squaregarden.ui.theme.DisplayFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
            if (levelId > 0 && !state.isMasterMode) {
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
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Goals + Moves panel (combined, stacked vertically)
        val isBlitz = state.challengeState?.type == ChallengeType.BLITZ
        GoalPanel(
            goals = state.level.goals,
            completedIds = state.completedGoalIds,
            movesRemaining = if (isBlitz) -1 else state.movesRemaining,
            movesMax = if (isBlitz) -1 else state.level.maxMoves,
            gameDifficulty = if (state.isChallenge && !state.isMasterMode) null else state.gameDifficulty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 10.dp else 16.dp,
                    vertical = if (isCompact) 3.dp else 6.dp
                )
        )

        // Overgrown tries remaining
        val overgrownTries = state.challengeState?.takeIf { it.type == ChallengeType.OVERGROWN }?.triesRemaining
        if (overgrownTries != null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tries:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                repeat(3) { i ->
                    Text(
                        text = "\u2764",
                        fontSize = 14.sp,
                        color = if (i < overgrownTries) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Challenge round label above board
        if (state.isChallenge) {
            val infiniteTransition = rememberInfiniteTransition(label = "challengePulse")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "challengeGlow"
            )
            val isTablet = LocalConfiguration.current.screenWidthDp >= 600
            Text(
                text = "CHALLENGE ROUND",
                fontSize = if (isTablet) 32.sp else 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = TileYellow.copy(alpha = glowAlpha),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Game board — full width on phones & 7" tablets, padded on 10"+
        val boardHPad = if (LocalConfiguration.current.screenWidthDp < 800) 0.dp else 12.dp
        // Memory Garden fog: all cells minus revealed cells (after initial reveal)
        val foggedCells = remember(state.challengeState) {
            val cs = state.challengeState
            if (cs != null && cs.type == ChallengeType.MEMORY && cs.initialRevealDone) {
                val all = mutableSetOf<CellPos>()
                for (r in 0 until state.board.height) {
                    for (c in 0 until state.board.width) {
                        if (!state.board.isVoid(r, c)) all.add(CellPos(r, c))
                    }
                }
                all - cs.revealedCells
            } else emptySet()
        }

        // While the level is still loading the ViewModel holds a sentinel
        // placeholder board (level.id == 0, a 5x5 red grid). Don't render it —
        // hold the layout slot with a Spacer until the real level arrives.
        if (state.level.id != 0) {
            GameBoardCanvas(
                board = state.board,
                selectedCell = state.selectedCell,
                hintCells = state.hintCells,
                swapAnim = state.swapAnim,
                completedGoalCells = state.completedGoalCells.values.flatten().toSet(),
                passthroughActive = state.passthroughActive,
                diagonalMode = state.diagonalMode,
                foggedCells = foggedCells,
                onDragSwap = { from, to -> viewModel.onDragSwap(from, to) },
                onCellTapped = { row, col -> viewModel.onCellTapped(row, col) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = boardHPad, vertical = 4.dp)
            )
        } else {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = boardHPad, vertical = 4.dp)
            )
        }

        // Blitz timer + stats bar
        val blitzState = state.challengeState?.takeIf { it.type == ChallengeType.BLITZ }
        if (blitzState != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val seconds = blitzState.timerMillisRemaining / 1000
                val tenths = (blitzState.timerMillisRemaining % 1000) / 100
                Text(
                    text = "\u23F1 ${seconds}.${tenths}s",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (seconds < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "\u2B50 ${blitzState.blitzStarScore}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TileYellow
                )
                Text(
                    text = "Goals: ${blitzState.goalsCleared}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${blitzState.comboMultiplier}x",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (blitzState.comboMultiplier > 1) TileYellow
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Bottom bar — circular action buttons (hidden during challenges)
        if (!state.isChallenge) {
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
                    label = "\u00D7${state.redoTokens}",
                    onClick = { showRedoConfirm = true },
                    enabled = state.redoTokens > 0 && state.phase == GamePhase.PLAYING,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                // Diagonal swap power-up: Pro+ only (World 11+)
                if (state.level.world >= 11) {
                    ActionCircle(
                        icon = "\u2197\uFE0F",
                        label = if (state.diagonalMode) "On" else "\u00D7${state.diagonalTokens}",
                        onClick = { viewModel.toggleDiagonal() },
                        enabled = state.diagonalMode || (state.diagonalTokens > 0 && state.phase == GamePhase.PLAYING),
                        containerColor = if (state.diagonalMode) TileViolet else null,
                        contentColor = if (state.diagonalMode) SoftWhite else null
                    )
                }
            }
        }

        // (challenge banner moved above game board)
    }

    // Token captured celebrations (mid-game) — splash icon then cascading trail
    if (state.phase == GamePhase.PLAYING) {
        // Compute target X fractions based on button count (SpaceEvenly positions)
        val hasDiagonal = state.level.world >= 11
        val buttonCount = if (hasDiagonal) 6 else 5
        fun buttonX(index: Int) = (2f * index + 1f) / (2f * buttonCount)
        // Button order: Hint(0), Shuffle(1), Passthrough(2), Unfreeze(3), Redo(4), Diagonal(5)
        val captured = listOfNotNull(
            if (state.shuffleTokenAwarded) AwardedToken("\uD83D\uDD00", "Shuffle Token Earned!", TileYellow, buttonX(1)) else null,
            if (state.passthroughTokenAwarded) AwardedToken("\uD83D\uDEE1\uFE0F", "Passthrough Token Earned!", Sage, buttonX(2)) else null,
            if (state.unfreezeTokenAwarded) AwardedToken("\u2744\uFE0F", "Unfreeze Token Earned!", TileBlue, buttonX(3)) else null,
            if (state.redoTokenAwarded) AwardedToken("\u21BB", "Redo Token Earned!", TileGreen, buttonX(4)) else null,
            if (state.diagonalTokenAwarded) AwardedToken("\u2197\uFE0F", "Diagonal Token Earned!", TileViolet, buttonX(5)) else null
        )
        captured.forEachIndexed { index, tok ->
            val splashScale = remember(tok.icon) { Animatable(0f) }
            var showTrail by remember(tok.icon) { mutableStateOf(false) }
            var dismissed by remember(tok.icon) { mutableStateOf(false) }
            LaunchedEffect(tok.icon) {
                delay(index * 1400L) // stagger multiple celebrations
                // Phase 1: splash in
                splashScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f))
                delay(1500) // hold splash
                // Phase 2: shrink splash, launch trail
                splashScale.animateTo(0f, animationSpec = tween(300))
                showTrail = true
            }
            // Splash icon popup
            if (!dismissed && splashScale.value > 0f) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = tok.accent.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(120.dp)
                                .scale(splashScale.value)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = tok.icon, fontSize = 56.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.scale(splashScale.value)
                        ) {
                            Text(
                                text = tok.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tok.accent,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            // Cascading trail to bottom bar icon
            if (showTrail && !dismissed) {
                TokenTrailOverlay(
                    icon = tok.icon,
                    accentColor = tok.accent,
                    targetXFraction = tok.targetX,
                    onComplete = {
                        viewModel.commitTokenCapture(tok.icon)
                        dismissed = true
                    },
                    onLanded = { viewModel.playTokenCapture() }
                )
            }
        }
    }

    // Win overlay with confetti + balloons + star burst + star trail + dialog
    if (state.phase == GamePhase.WON) {
        if (state.isMasterMode && !state.isChallenge) {
            // Master Mode win: use MasterModeSummaryOverlay
            val mState = state.masterModeState!!
            val tier = state.masterTier ?: MasterTier.WARMING_UP
            LaunchedEffect(Unit) { viewModel.commitWinResult() }
            MasterModeSummaryOverlay(
                starsEarned = state.starsAwarded,
                tierBaseStars = tier.baseStars,
                gameDiffMultiplier = state.gameDifficulty.starMultiplier,
                streakMultiplier = mState.streakMultiplier,
                skillMultiplier = state.difficulty.starMultiplier,
                currentStreak = mState.currentStreak,
                tier = tier,
                onNextGame = {
                    MusicManager.stopWinMusic()
                    navController.navigate(Screen.Game.create(GameViewModel.MASTER_MODE_SIGNAL)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onEndRun = {
                    MusicManager.stopWinMusic()
                    navController.popBackStack()
                }
            )
            ConfettiOverlay(stars = state.starsAwarded, perfectGame = false)
        } else {
        val stars = state.starsAwarded
        var challengeCelebrationReady by remember { mutableStateOf(!state.isChallenge) }

        // Fullscreen overlay card (renders first = behind celebration particles)
        WinOverlay(
            stars = stars,
            levelName = "${state.level.name} (${state.level.id})",
            unlockedWorldName = state.unlockedWorldName,
            shuffleTokenAwarded = state.shuffleTokenAwarded,
            passthroughTokenAwarded = state.passthroughTokenAwarded,
            unfreezeTokenAwarded = state.unfreezeTokenAwarded,
            redoTokenAwarded = state.redoTokenAwarded,
            diagonalTokenAwarded = state.diagonalTokenAwarded,
            perfectGame = state.perfectGame,
            onStarLanded = { viewModel.playStarCollect() },
            onAllStarsLanded = { viewModel.commitWinResult() },
            onPerfectGameSound = { viewModel.playPerfectGameSound() },
            onWorldUnlockSound = { viewModel.playWorldUnlockSound() },
            onChallengeCountUp = { viewModel.playMatchSound() },
            onChallengeMusic = {
                viewModel.playChallengeMusic()
                challengeCelebrationReady = true
            },
            isChallenge = state.isChallenge,
            challengeGoalsCleared = state.challengeState?.goalsCleared ?: 0,
            onNext = if (!state.isChallenge && state.level.id < 126 && !state.proUpgradePrompt) {
                {
                    MusicManager.stopWinMusic()
                    viewModel.commitWinResult()
                    scope.launch {
                        val profile = profileRepo.loadProfile()
                        val isPro_Plus = state.difficulty == Difficulty.PRO_PLUS
                        val maxLevel = if (isPro_Plus) 126 else 90
                        val nextId = if (profile.proUpgradeDeclined && !isPro_Plus) {
                            (37..90).random()
                        } else {
                            (state.level.id + 1).coerceAtMost(maxLevel)
                        }
                        navController.navigate(Screen.Game.create(nextId)) {
                            popUpTo(Screen.Game.route) { inclusive = true }
                        }
                    }
                }
            } else null,
            onMenu = {
                MusicManager.stopWinMusic()
                viewModel.commitWinResult()
                backToGame()
            }
        )

        // Celebration layers in the foreground, raining down over the card
        if (challengeCelebrationReady) {
            val maxCelebration = state.perfectGame || state.gameCompleted
            ConfettiOverlay(stars = stars, perfectGame = maxCelebration)
            BalloonOverlay(stars = stars, perfectGame = maxCelebration)
            StarBurstOverlay(stars = stars, perfectGame = maxCelebration)
        }
        }
    }

    // Pro+ upgrade prompt (after beating level 90 as Pro)
    if (state.phase == GamePhase.WON && state.proUpgradePrompt) {
        var showUpgrade by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(3000) // Let normal win celebration play first
            showUpgrade = true
        }
        if (showUpgrade) {
            val upgradeProfileRepo = remember { ProfileRepository(context) }
            val upgTotalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
            val upgPerfectGames by progressRepo.perfectGamesFlow.collectAsState(initial = 0)
            ProUpgradeOverlay(
                totalStars = upgTotalStars,
                perfectGames = upgPerfectGames,
                onUpgrade = {
                    scope.launch {
                        upgradeProfileRepo.upgradeSkill(Difficulty.PRO_PLUS, Difficulty.PRO_PLUS.startingLevel)
                    }
                    MusicManager.stopWinMusic()
                    viewModel.commitWinResult()
                    navController.navigate(Screen.Game.create(91)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onDecline = {
                    scope.launch { upgradeProfileRepo.markProUpgradeDeclined() }
                    MusicManager.stopWinMusic()
                    viewModel.commitWinResult()
                    val randomLevel = (37..90).random()
                    navController.navigate(Screen.Game.create(randomLevel)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            )
        }
    }

    // Game Complete overlay (after beating level 126)
    if (state.phase == GamePhase.WON && state.gameCompleted) {
        var showGameComplete by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(3000) // Let normal win celebration play first
            showGameComplete = true
        }
        if (showGameComplete) {
            val profileRepo = remember { ProfileRepository(context) }
            val gcTotalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
            val gcPerfectGames by progressRepo.perfectGamesFlow.collectAsState(initial = 0)
            val gcGamesPlayed by progressRepo.gamesPlayedFlow.collectAsState(initial = 0)
            var challengesCompleted by remember { mutableIntStateOf(0) }
            var playerName by remember { mutableStateOf("") }
            var diffLabel by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                val completions = progressRepo.getChallengeCompletions()
                challengesCompleted = completions.values.sum()
                val profile = profileRepo.loadProfile()
                playerName = profile.username
                diffLabel = Difficulty.fromId(profile.difficulty).label
            }
            GameCompleteOverlay(
                totalStars = gcTotalStars,
                perfectGames = gcPerfectGames,
                gamesPlayed = gcGamesPlayed,
                challengesCompleted = challengesCompleted,
                onSaveBadge = {
                    scope.launch {
                        val dateStr = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val uri = com.squaregarden.data.BadgeExporter.exportBadge(
                            context, playerName, diffLabel, gcTotalStars, gcPerfectGames, dateStr
                        )
                        if (uri != null) {
                            val shareIntent = com.squaregarden.data.BadgeExporter.shareIntent(uri)
                            context.startActivity(Intent.createChooser(shareIntent, "Share Mastery Badge"))
                        }
                    }
                },
                onReturnHome = {
                    MusicManager.stopWinMusic()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
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
                        Button(
                            onClick = {
                                viewModel.commitWinResult()
                                scope.launch {
                                    val profile = profileRepo.loadProfile()
                                    val isPro_Plus = state.difficulty == Difficulty.PRO_PLUS
                                    val maxLevel = if (isPro_Plus) 126 else 90
                                    val nextId = if (profile.proUpgradeDeclined && !isPro_Plus) {
                                        (37..90).random()
                                    } else {
                                        (state.level.id + 1).coerceAtMost(maxLevel)
                                    }
                                    navController.navigate(Screen.Game.create(nextId)) {
                                        popUpTo(Screen.Game.route) { inclusive = true }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Next Game", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Overgrown board generation loading overlay
    if (state.boardGenerating) {
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
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Generating new board...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarmBrown
                    )
                }
            }
        }
    }

    // Lose dialog
    if (state.phase == GamePhase.LOST) {
        val cs = state.challengeState
        if (cs?.type == ChallengeType.OVERGROWN) {
            // Overgrown: ask retry or take score
            OvergrownRetryDialog(
                triesRemaining = cs.triesRemaining - 1,
                currentStars = cs.overgrownStarScore,
                nextMultiplier = cs.overgrownTryMultiplier + 1,
                onRetry = { viewModel.overgrownAcceptRetry() },
                onTakeScore = { viewModel.overgrownDeclineRetry() },
                onBackToGame = backToGame
            )
        } else if (state.isChallenge) {
            LoseDialog(
                onRetry = null,
                onMenu = backToGame,
                onShowSolution = null
            )
        } else if (state.isMasterMode) {
            // Master Mode loss: streak broken dialog
            val mState = state.masterModeState
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Streak Broken") },
                text = {
                    Text(
                        "Your streak has been reset.\n" +
                        "Session stars: ${mState?.sessionStars ?: 0}"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        navController.navigate(Screen.Game.create(GameViewModel.MASTER_MODE_SIGNAL)) {
                            popUpTo(Screen.Game.route) { inclusive = true }
                        }
                    }) { Text("Retry", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("End Run") }
                }
            )
        } else {
            LoseDialog(
                onRetry = { viewModel.resetLevel() },
                onMenu = { navController.popBackStack() },
                onShowSolution = if (state.hasSolution) {{ viewModel.showSolution() }} else null
            )
        }
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
            onBeepSound = { viewModel.playBeepSound() },
            onClapSound = { viewModel.playClapSound() },
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

    // Challenge invitation dialog
    val pendingChallenge = state.pendingChallenge
    if (pendingChallenge != null && state.phase == GamePhase.WON) {
        Dialog(onDismissRequest = { viewModel.dismissChallenge() }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftWhite)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "\u2694\uFE0F Challenge Unlocked!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSage
                    )
                    Text(
                        text = pendingChallenge.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = pendingChallenge.description,
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "No lives at stake \u2022 Bonus stars + tokens on win!",
                        fontSize = 12.sp,
                        color = Sage,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.dismissChallenge() },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Skip", fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                MusicManager.stopWinMusic()
                                viewModel.dismissChallenge()
                                navController.navigate(Screen.Game.create(pendingChallenge.id))
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

}

private data class AwardedToken(val icon: String, val label: String, val accent: Color, val targetX: Float = 0.5f)

@Composable
private fun WinOverlay(stars: Int, levelName: String, unlockedWorldName: String? = null, shuffleTokenAwarded: Boolean = false, passthroughTokenAwarded: Boolean = false, unfreezeTokenAwarded: Boolean = false, redoTokenAwarded: Boolean = false, diagonalTokenAwarded: Boolean = false, perfectGame: Boolean = false, isChallenge: Boolean = false, challengeGoalsCleared: Int = 0, onStarLanded: () -> Unit = {}, onAllStarsLanded: () -> Unit = {}, onPerfectGameSound: () -> Unit = {}, onWorldUnlockSound: () -> Unit = {}, onChallengeCountUp: () -> Unit = {}, onChallengeMusic: () -> Unit = {}, onNext: (() -> Unit)?, onMenu: () -> Unit) {
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
    // Play world unlock sound when world unlocked
    LaunchedEffect(unlockedWorldName) {
        if (unlockedWorldName != null) {
            delay(400)
            onWorldUnlockSound()
        }
    }

    // Challenge count-up animation: 1s pause → count from 0 to stars → music starts → show card
    var challengeCountUpDone by remember { mutableStateOf(!isChallenge) }
    var challengeCountValue by remember { mutableIntStateOf(0) }
    val challengeCountScale = remember { Animatable(0f) }

    if (isChallenge) {
        LaunchedEffect(stars) {
            delay(1000) // 1 second pause
            onChallengeCountUp() // sound FX at count-up start
            challengeCountScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f))
            // Count up from 0 to stars
            val countDuration = (stars * 80L).coerceIn(800L, 2500L)
            val stepDelay = countDuration / stars.coerceAtLeast(1)
            for (i in 1..stars) {
                challengeCountValue = i
                delay(stepDelay)
            }
            delay(600) // pause after count finishes
            challengeCountUpDone = true
            onChallengeMusic() // music + confetti start with the card
        }
    }

    // Fullscreen overlay (same window layer — not a Dialog)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Challenge count-up animation (before card appears)
        if (isChallenge && !challengeCountUpDone) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\u2B50 +$challengeCountValue",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TileYellow,
                    modifier = Modifier.scale(challengeCountScale.value)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "STARS EARNED",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Scrollable wrapper so content is reachable on small screens
        if (challengeCountUpDone) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                if (isChallenge) {
                    // Challenge-specific congratulation
                    val chalHeadline = remember {
                        listOf(
                            "Challenge Complete!",
                            "Challenge Conquered!",
                            "Challenge Crushed!",
                            "You Nailed It!"
                        ).random()
                    }
                    Text(
                        text = chalHeadline,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkSage
                    )

                    if (challengeGoalsCleared > 0) {
                        Text(
                            text = "$challengeGoalsCleared goals cleared",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmBrown
                        )
                    }

                    // Big animated star score
                    val scoreScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(300)
                        scoreScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f))
                    }
                    Text(
                        text = "\u2B50 +$stars stars!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TileYellow,
                        modifier = Modifier.scale(scoreScale.value)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val chalSubtitle = remember(stars) {
                        when {
                            stars >= 20 -> listOf(
                                "Absolutely legendary!",
                                "Unstoppable! What a performance!",
                                "You're on fire! Incredible score!"
                            ).random()
                            stars >= 10 -> listOf(
                                "Impressive! You've got serious skills!",
                                "Amazing run! Keep that energy going!",
                                "That was epic! Well played!"
                            ).random()
                            stars >= 5 -> listOf(
                                "Great effort! Challenge rewards earned!",
                                "Solid performance! Stars well deserved!",
                                "Nice job! Every star counts!"
                            ).random()
                            else -> listOf(
                                "Good start! Try again for more stars!",
                                "Not bad! Practice makes perfect!",
                                "You did it! Aim higher next time!"
                            ).random()
                        }
                    }
                    Text(
                        text = chalSubtitle,
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Regular level win
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
                            "Outstanding!",
                            "Kapow!",
                            "Bazooka!",
                            "Unstoppable!",
                            "Legendary!",
                            "Boom!",
                            "Crushed it!"
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
                                "Stunning! All three stars!",
                                "Hat trick! Triple star glory!",
                                "Full marks! You're on fire!",
                                "Clean sweep! Nothing left behind!",
                                "Peak performance! Chef's kiss!",
                                "Absolute domination! Three stars!",
                                "Textbook win! Every move counted!"
                            ).random()
                            2 -> listOf(
                                "Great job! Almost perfect!",
                                "So close to perfection!",
                                "Two stars! Impressive work!",
                                "Nearly flawless! Great effort!",
                                "Solid win! One more star awaits!",
                                "Strong finish! Can you get three?",
                                "So close! One star away from glory!",
                                "Sharp moves! Almost had it all!",
                                "Two down, one to go!",
                                "Smooth sailing! Just a touch more!",
                                "Nailed it! Perfection is within reach!",
                                "Power play! Come back for the triple!"
                            ).random()
                            else -> listOf(
                                "Well done! Try again for more stars!",
                                "A win is a win! Keep going!",
                                "Good start! Room to grow!",
                                "You cleared it! Aim higher next time!",
                                "Nice work! More stars await!",
                                "Victory! Replay for a better score!",
                                "On the board! Stars are calling!",
                                "First step to greatness!",
                                "Got it done! Now aim for more!",
                                "Tough level, tough you!",
                                "That's a wrap! Try for two stars!",
                                "Scraped through! You'll nail it next time!"
                            ).random()
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )
                }

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

                // Power-up tokens awarded — compact icon chips with tap-for-label
                val awardedTokens = remember(
                    shuffleTokenAwarded, passthroughTokenAwarded, unfreezeTokenAwarded,
                    redoTokenAwarded, diagonalTokenAwarded
                ) {
                    listOfNotNull(
                        if (shuffleTokenAwarded) AwardedToken("\uD83D\uDD00", "Shuffle Token Earned!", TileYellow) else null,
                        if (passthroughTokenAwarded) AwardedToken("\uD83D\uDEE1\uFE0F", "Passthrough Token Earned!", Sage) else null,
                        if (unfreezeTokenAwarded) AwardedToken("\u2744\uFE0F", "Unfreeze Token Earned!", TileBlue) else null,
                        if (redoTokenAwarded) AwardedToken("\u21BB", "Redo Token Earned!", TileGreen) else null,
                        if (diagonalTokenAwarded) AwardedToken("\u2197\uFE0F", "Diagonal Token Earned!", TileViolet) else null
                    )
                }
                if (awardedTokens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    var revealedIndex by rememberSaveable { mutableStateOf(-1) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            awardedTokens.forEachIndexed { idx, tok ->
                                val chipScale = remember(tok.icon) { Animatable(0f) }
                                LaunchedEffect(tok.icon) {
                                    delay(800L + idx * 250L)
                                    chipScale.animateTo(
                                        1f,
                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = tok.accent.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .scale(chipScale.value)
                                        .clickable {
                                            revealedIndex = if (revealedIndex == idx) -1 else idx
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = tok.icon, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                        // Label area for tapped chip (reserves a stable height)
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .heightIn(min = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (revealedIndex in awardedTokens.indices) {
                                val tok = awardedTokens[revealedIndex]
                                Text(
                                    text = tok.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tok.accent
                                )
                            } else {
                                Text(
                                    text = "Tap an icon to see what you earned",
                                    fontSize = 12.sp,
                                    color = DarkSage.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Perfect game celebration
                if (perfectGame) {
                    val pgScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        val prior = listOf(shuffleTokenAwarded, passthroughTokenAwarded, unfreezeTokenAwarded, redoTokenAwarded, diagonalTokenAwarded).count { it }
                        delay(800L + prior * 800L)
                        pgScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 250f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TileYellow.copy(alpha = 0.25f)),
                        border = BorderStroke(2.dp, TileYellow),
                        modifier = Modifier.scale(pgScale.value)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("\u2B50 PERFECT GAME \u2B50", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DarkSage)
                            Text("2\u00D7 Stars + All Tokens!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (onNext != null) {
                    Button(onClick = onNext, shape = RoundedCornerShape(20.dp)) {
                        Text("Next Game")
                    }
                } else {
                    OutlinedButton(onClick = onMenu, shape = RoundedCornerShape(20.dp)) {
                        Text("Back to Game")
                    }
                }
            }
        }
        } // end scrollable wrapper

        // Star trail flies from card center to top-right (badge area)
        StarTrailOverlay(
            starCount = stars,
            targetOffset = Offset.Zero,
            onComplete = onAllStarsLanded,
            onStarLanded = onStarLanded
        )
        } // end challengeCountUpDone
    }
}

@Composable
private fun LoseDialog(onRetry: (() -> Unit)?, onMenu: () -> Unit, onShowSolution: (() -> Unit)?) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
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
                    text = if (onRetry != null) "Out of Moves" else "Challenge Failed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TileRed
                )

                Text(
                    text = if (onRetry != null) "Don't give up!\nTry a different approach." else "Better luck next time!",
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

                if (onRetry != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMenu,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Menu", maxLines = 1, fontSize = if (isTablet) 13.sp else 11.sp)
                        }
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Try Again", maxLines = 1, fontSize = if (isTablet) 13.sp else 11.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onMenu,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Back to Game", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OvergrownRetryDialog(
    triesRemaining: Int,
    currentStars: Int,
    nextMultiplier: Int,
    onRetry: () -> Unit,
    onTakeScore: () -> Unit,
    onBackToGame: () -> Unit
) {
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

                if (triesRemaining > 0) {
                    Text(
                        text = "You earned $currentStars star${if (currentStars != 1) "s" else ""} this round.\n\n" +
                                "Retry with ${nextMultiplier}\u00D7 multiplier?\n" +
                                "($triesRemaining ${if (triesRemaining == 1) "try" else "tries"} remaining)\n\n" +
                                "Warning: retrying forfeits this round\u2019s stars.",
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTakeScore,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Take $currentStars \u2B50",
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Retry ${nextMultiplier}\u00D7",
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Last try — no retry option
                    Text(
                        text = "You earned $currentStars star${if (currentStars != 1) "s" else ""} total.\nNo tries remaining.",
                        fontSize = 14.sp,
                        color = WarmBrown,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackToGame,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to Game", maxLines = 1, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onTakeScore,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Take $currentStars \u2B50",
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
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
    onBeepSound: () -> Unit = {},
    onClapSound: () -> Unit = {},
    onWinSound: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(-1) }
    var replayBoard by remember { mutableStateOf(initialBoard) }
    var completedGoalIds by remember { mutableStateOf(emptySet<String>()) }
    var completedGoalCells by remember { mutableStateOf<Map<String, Set<CellPos>>>(emptyMap()) }
    var swapAnim by remember { mutableStateOf<SwapAnimation?>(null) }
    var handCell by remember { mutableStateOf<CellPos?>(null) }
    var replayKey by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    // Reset state when replaying
    LaunchedEffect(replayKey) {
        currentStep = -1
        replayBoard = initialBoard
        completedGoalIds = emptySet()
        completedGoalCells = emptyMap()
        swapAnim = null
        handCell = null
        finished = false

        delay(600)

        for (i in steps.indices) {
            currentStep = i
            val from = steps[i].first
            val to = steps[i].second

            // Show hand at source before animating
            handCell = from
            delay(300)

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
            handCell = to
            onSwapSound()

            // Evaluate goals cumulatively (once met, stays met — matches game behavior)
            val prevCompleted = completedGoalIds
            val metNow = BoardEngine.evaluateGoals(replayBoard, goals)
            completedGoalIds = completedGoalIds + metNow
            if ((completedGoalIds - prevCompleted).isNotEmpty()) {
                onBeepSound()
            }
            // Lock completed-goal positions in place: only find positions for
            // newly-completed goals, leave earlier ones untouched even if the
            // same pattern re-forms elsewhere on the board.
            val newlyCompleted = completedGoalIds - prevCompleted
            if (newlyCompleted.isNotEmpty()) {
                val updatedGoalCells = completedGoalCells.toMutableMap()
                for (goal in goals) {
                    if (goal.id in newlyCompleted && goal.id !in updatedGoalCells) {
                        val cells = PatternMatcher.findGoalPositions(replayBoard, goal)
                        if (cells != null) updatedGoalCells[goal.id] = cells
                    }
                }
                completedGoalCells = updatedGoalCells
            }

            // Check if all goals met
            if (BoardEngine.checkWin(completedGoalIds, goals)) {
                finished = true
                handCell = null
                onWinSound()
                onClapSound()
                break
            }

            delay(500)
        }
        handCell = null
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
                    handCell = handCell,
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
