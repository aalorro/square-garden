package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.squaregarden.audio.MusicManager
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.data.GameStateSerializer
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.SavedGameRepository
import com.squaregarden.data.SettingsRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.model.PlayerProgress
import com.squaregarden.model.UserProfile
import kotlinx.coroutines.launch
import com.squaregarden.logic.LevelLoader
import com.squaregarden.ui.components.BasReliefAvatar
import com.squaregarden.ui.components.FavoritesDialog
import com.squaregarden.ui.components.getAvatar
import com.squaregarden.ui.components.LogoMark
import androidx.compose.ui.graphics.Color
import com.squaregarden.ui.theme.DisplayFontFamily

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
    val perfectGames by progressRepo.perfectGamesFlow.collectAsState(initial = 0)
    val lives by progressRepo.livesFlow.collectAsState(initial = 3)
    val cooldownUntil by progressRepo.cooldownUntilFlow.collectAsState(initial = 0L)
    val gamesPlayed by progressRepo.gamesPlayedFlow.collectAsState(initial = 0)
    val cooldownActive = lives <= 0 && cooldownUntil > System.currentTimeMillis()
    var profile by remember { mutableStateOf(UserProfile()) }
    var currentWorld by remember { mutableIntStateOf(1) }
    var nextLevel by remember { mutableIntStateOf(1) }
    var showFavorites by remember { mutableStateOf(false) }
    val allLevels = remember { LevelLoader.loadAllLevels(context) }
    var progress by remember { mutableStateOf(PlayerProgress()) }

    val settingsRepo = remember { SettingsRepository(context) }
    val savedGameRepo = remember { SavedGameRepository(context) }
    val musicEnabled by settingsRepo.musicEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Redirect to saved in-progress game if one exists
        val savedJson = savedGameRepo.loadGame()
        if (savedJson != null) {
            try {
                val savedLevelId = GameStateSerializer.extractLevelId(savedJson)
                navController.navigate(Screen.Game.create(savedLevelId)) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
                return@LaunchedEffect
            } catch (_: Exception) {
                savedGameRepo.clearSavedGame()
            }
        }

        profile = profileRepo.loadProfile()
        progress = progressRepo.loadProgress()
        val difficulty = Difficulty.fromId(profile.difficulty)
        val effectiveStart = if (profile.overrideStartingLevel > 0)
            profile.overrideStartingLevel else difficulty.startingLevel
        val isPro_Plus = difficulty == Difficulty.PRO_PLUS
        val maxLevel = if (isPro_Plus) 126 else 90
        nextLevel = if (profile.proUpgradeDeclined && !isPro_Plus) {
            (37..90).random()
        } else {
            progress.highestUnlockedLevel(effectiveStart).coerceAtMost(maxLevel)
        }
        // Worlds run 1..14 (Pro+ adds 11-14); clamp to the full range so
        // Pro+ players land on their current world, not world 10.
        val maxWorld = if (isPro_Plus) 14 else 10
        currentWorld = (((nextLevel - 1) / 9) + 1).coerceIn(1, maxWorld)
    }

    // Intro music: plays while HomeScreen is visible, respects music toggle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, musicEnabled) {
        if (musicEnabled) MusicManager.startIntro(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) MusicManager.stopIntro()
            if (event == Lifecycle.Event.ON_RESUME && musicEnabled) MusicManager.startIntro(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            MusicManager.stopIntro()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Top greeting bar — end padding avoids PlayerBadge overlay
        Column(modifier = Modifier.padding(end = 100.dp)) {
            Text(
                text = "Welcome back,",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${profile.username.ifBlank { "Gardener" }}!",
                fontFamily = DisplayFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }

        // Star count chip
        if (totalStars > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "\u2605", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "$totalStars stars",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (perfectGames > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "\uD83C\uDFC6", fontSize = 14.sp)
                        Text(
                            text = "$perfectGames perfect",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Center content: logo + title
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark(size = 110.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Square",
                fontFamily = DisplayFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.03).sp
            )
            Text(
                text = "Garden",
                fontFamily = DisplayFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.03).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A calm puzzle game",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }

        // Bottom CTAs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { navController.navigate(Screen.Game.create(nextLevel)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                enabled = !cooldownActive
            ) {
                Text(
                    text = if (cooldownActive) "\u2764\uFE0F  Resting..." else "\u25B6  Play",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (profile.masteryBadgeEarned) {
                Button(
                    onClick = { navController.navigate(Screen.MasterMode.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB8860B),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !cooldownActive
                ) {
                    Text("\uD83D\uDC51  Master Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Worlds + Favorites row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.WorldSelect.route) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !cooldownActive
                ) {
                    Text("Worlds", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                if (progress.favoriteLevels.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFavorites = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !cooldownActive
                    ) {
                        Text("\u2605  Favorites", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Settings + How to Play row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Instructions.route) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("How to Play", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Stats + Leaderboards row
            if (gamesPlayed > 0 || profile.leaderboardOptIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gamesPlayed > 0) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.Stats.route) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("\uD83D\uDCCA  Stats", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (profile.leaderboardOptIn) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.Leaderboard.route) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("\uD83C\uDFC6  Leaderboards", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (com.squaregarden.DevFlags.ENDGAME_SIM_ENABLED) {
                Button(
                    onClick = {
                        navController.navigate(
                            Screen.Game.create(com.squaregarden.viewmodel.GameViewModel.ENDGAME_SIM_SIGNAL)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF8B0000),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Text("DEV: Sim 126", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showFavorites) {
        FavoritesDialog(
            allLevels = allLevels,
            progress = progress,
            progressRepo = progressRepo,
            onLevelClick = { levelId ->
                showFavorites = false
                navController.navigate(Screen.Game.create(levelId))
            },
            onDismiss = {
                showFavorites = false
                // Reload progress so favorites button visibility updates
                scope.launch { progress = progressRepo.loadProgress() }
            }
        )
    }
}
