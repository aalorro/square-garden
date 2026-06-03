package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.squaregarden.DevFlags
import com.squaregarden.audio.MusicManager
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.logic.LevelLoader
import com.squaregarden.model.ChallengeType
import com.squaregarden.model.Difficulty
import com.squaregarden.model.Level
import com.squaregarden.model.PlayerProgress
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.launch

private val worldStarsToUnlock = mapOf(
    // World 0 = Challenge Lab (dev/test only, free to access)
    0 to 0,
    1 to 0, 2 to 7, 3 to 14, 4 to 18, 5 to 42,
    6 to 68, 7 to 98, 8 to 132, 9 to 172, 10 to 240,
    // Pro+ worlds 11-14 (strict consecutive numbering)
    11 to 280, 12 to 14, 13 to 29, 14 to 43
)

// World theme colors: (tile background, tile text, accent/star color, gradient)
private data class WorldTheme(
    val tileColor: Color,
    val tileColorLight: Color,
    val textColor: Color,
    val starColor: Color,
    val gradient: List<Color> = emptyList()
)

private val worldThemes = mapOf(
    1 to WorldTheme(Color(0xFF81C784), Color(0xFFA5D6A7), Color(0xFF1B5E20), Color(0xFFFFCA28),
        listOf(Color(0xFFA8E063), Color(0xFF56AB2F), Color(0xFF3F7D20))),
    2 to WorldTheme(Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFF0D47A1), Color(0xFFFFCA28),
        listOf(Color(0xFF89F7FE), Color(0xFF66A6FF), Color(0xFF3F5EFB))),
    3 to WorldTheme(Color(0xFFA1887F), Color(0xFFBCAAA4), Color(0xFF3E2723), Color(0xFFFFD54F),
        listOf(Color(0xFF5A4A3A), Color(0xFF3E2723), Color(0xFF1B1410))),
    4 to WorldTheme(Color(0xFF4FC3F7), Color(0xFF81D4FA), Color(0xFF01579B), Color(0xFFFFE082),
        listOf(Color(0xFF43CBFF), Color(0xFF2563EB), Color(0xFF0F2E5C))),
    5 to WorldTheme(Color(0xFFCE93D8), Color(0xFFE1BEE7), Color(0xFF4A148C), Color(0xFFFFD740),
        listOf(Color(0xFFFF9A9E), Color(0xFFA18CD1), Color(0xFFFBC2EB))),
    6 to WorldTheme(Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFF263238), Color(0xFFFFE57F),
        listOf(Color(0xFF3E2A6E), Color(0xFF1A1530), Color(0xFF0A0817))),
    7 to WorldTheme(Color(0xFFFF8A65), Color(0xFFFFAB91), Color(0xFFBF360C), Color(0xFFFFD600),
        listOf(Color(0xFFFFB75E), Color(0xFFED8F03), Color(0xFF7C2D12))),
    8 to WorldTheme(Color(0xFFB39DDB), Color(0xFFD1C4E9), Color(0xFF311B92), Color(0xFFFFE082),
        listOf(Color(0xFF1B0E3F), Color(0xFF3D2A6E), Color(0xFF7B5FB8))),
    9 to WorldTheme(Color(0xFF4DB6AC), Color(0xFF80CBC4), Color(0xFF004D40), Color(0xFFFFD740),
        listOf(Color(0xFF0F4C5C), Color(0xFF0A2540), Color(0xFF000814))),
    10 to WorldTheme(Color(0xFFF48FB1), Color(0xFFF8BBD0), Color(0xFF880E4F), Color(0xFFFFE082),
        listOf(Color(0xFFFFD3E0), Color(0xFFC9B8FF), Color(0xFFA0E4FF))),
    // World 0 = Challenge Lab (testing) — bright orange theme
    0 to WorldTheme(Color(0xFFFF8A65), Color(0xFFFFAB91), Color(0xFFBF360C), Color(0xFFFFD600),
        listOf(Color(0xFFFF5722), Color(0xFFE64A19), Color(0xFFBF360C))),
    // Pro+ worlds (11-14)
    11 to WorldTheme(Color(0xFFBA68C8), Color(0xFFCE93D8), Color(0xFF4A148C), Color(0xFFFFD600),
        listOf(Color(0xFF1A0B2E), Color(0xFF4A148C), Color(0xFF7E57C2))),
    12 to WorldTheme(Color(0xFF4DD0E1), Color(0xFF80DEEA), Color(0xFF006064), Color(0xFFFFE082),
        listOf(Color(0xFF002730), Color(0xFF006E80), Color(0xFF26C6DA))),
    13 to WorldTheme(Color(0xFFF06292), Color(0xFFF8BBD0), Color(0xFF880E4F), Color(0xFFFFD740),
        listOf(Color(0xFF1A0014), Color(0xFF6A0033), Color(0xFFEC407A))),
    14 to WorldTheme(Color(0xFFFFD54F), Color(0xFFFFECB3), Color(0xFFFF6F00), Color(0xFFFF8F00),
        listOf(Color(0xFF1A1500), Color(0xFF7A5C00), Color(0xFFFFD600)))
)

@Composable
fun LevelSelectScreen(worldId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    var progress by remember { mutableStateOf(PlayerProgress()) }
    var levels by remember { mutableStateOf<List<Level>>(emptyList()) }
    val lastWonLevel by progressRepo.lastWonLevelFlow.collectAsState(initial = -1)
    val profile by profileRepo.profileFlow.collectAsState(initial = null)
    val difficulty = profile?.let { Difficulty.fromId(it.difficulty) }
    val lives by progressRepo.livesFlow.collectAsState(initial = 3)
    val cooldownUntil by progressRepo.cooldownUntilFlow.collectAsState(initial = 0L)
    val cooldownActive = lives <= 0 && cooldownUntil > System.currentTimeMillis()
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)

    // TODO: World 0 challenge lab — dev/test access only, remove before release
    val challengeEntries = remember {
        if (worldId == 0) ChallengeType.entries.toList() else emptyList()
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        progress = progressRepo.loadProgress()
        if (worldId != 0) {
            levels = LevelLoader.loadAllLevels(context).filter { it.world == worldId }
        }
    }

    val worldNames = mapOf(
        0 to "Challenge Lab",
        1 to "Seedling Garden", 2 to "Blooming Meadow", 3 to "Ancient Grove",
        4 to "Crystal Cavern", 5 to "Shattered Isles", 6 to "Void Fortress",
        7 to "Molten Core", 8 to "Starfall Summit", 9 to "Abyssal Depths", 10 to "Prism Citadel",
        11 to "Nebula Verge", 12 to "Quantum Lattice", 13 to "Singularity Spire", 14 to "Infinity Prism"
    )

    val theme = worldThemes[worldId] ?: worldThemes[1]!!

    // Perfect Game splash — shows once when player first enters World 5
    var splashDismissed by remember { mutableStateOf(false) }
    val currentProfile = profile
    val showPerfectGameSplash =
        worldId == 5 && currentProfile != null && !currentProfile.perfectGameSplashShown && !splashDismissed
    val splashScope = rememberCoroutineScope()
    if (showPerfectGameSplash) {
        LaunchedEffect(Unit) {
            MusicManager.startWinMusic(context, perfectGame = true)
        }
        PerfectGameSplashDialog(onDismiss = {
            splashDismissed = true
            MusicManager.stopWinMusic()
            splashScope.launch { profileRepo.markPerfectGameSplashShown() }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Gradient header strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = theme.gradient.ifEmpty { listOf(theme.tileColor, theme.tileColor) }
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(
                        "\u2190",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(end = 100.dp)) {
                    Text(
                        text = "WORLD $worldId",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = worldNames[worldId] ?: "World $worldId",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (worldId == 0) 2 else 3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 60.dp, bottom = 16.dp)
        ) {
            // TODO: Challenge lab cards — remove before release
            if (worldId == 0) {
                items(challengeEntries) { challenge ->
                    Card(
                        onClick = { navController.navigate(Screen.Game.create(challenge.id)) },
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.tileColorLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (challenge) {
                                    ChallengeType.BLITZ -> "\u23F1"
                                    ChallengeType.OVERGROWN -> "\uD83C\uDF3F"
                                    ChallengeType.SHIFTING -> "\uD83C\uDF0A"
                                    ChallengeType.MEMORY -> "\uD83E\uDDE0"
                                },
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = challenge.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = theme.textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = challenge.description,
                                fontSize = 9.sp,
                                color = theme.textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                // ── Pro+ Upgrade Test cards ──
                item {
                    Card(
                        onClick = {
                            navController.navigate(Screen.Game.create(90)) {
                                popUpTo(Screen.LevelSelect.create(0)) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD4A017)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("\uD83D\uDE80", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Play Game 90",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Play level 90 as Pro — win to trigger Pro+ upgrade prompt",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
                item {
                    val currentDiff = difficulty
                    Card(
                        onClick = {
                            scope.launch {
                                profileRepo.upgradeSkill(Difficulty.HARD, Difficulty.HARD.startingLevel)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentDiff == Difficulty.PRO_PLUS) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("\uD83D\uDD04", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reset to Pro",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentDiff == Difficulty.PRO_PLUS) "Currently Pro+ \u2014 tap to reset" else "Currently ${currentDiff?.label ?: "?"} \u2014 no reset needed",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            } else {
                items(levels) { level ->
                    val stars = progress.levelStars[level.id] ?: 0
                    val overrideLevel = profile?.overrideStartingLevel ?: 0
                    val effectiveStart = if (overrideLevel > 0) overrideLevel else (difficulty?.startingLevel ?: 1)
                    val highestUnlocked = progress.highestUnlockedLevel(effectiveStart)
                    val unlocked = level.id <= highestUnlocked
                    val isUpNext = level.id == highestUnlocked && progress.levelStars[level.id] == null
                    val isLastWon = level.id == lastWonLevel
                    val isFavorite = level.id in progress.favoriteLevels

                    Card(
                        onClick = {
                            if (unlocked && !cooldownActive) {
                                navController.navigate(Screen.Game.create(level.id))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .alpha(if (unlocked && !cooldownActive) 1f else if (unlocked) 0.6f else 0.4f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLastWon)
                                theme.tileColor
                            else if (unlocked)
                                theme.tileColorLight
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isLastWon) BorderStroke(3.dp, theme.tileColor) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 4.dp else 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!unlocked) {
                                    Text("\uD83D\uDD12", fontSize = if (isCompact) 11.sp else 24.sp, color = theme.textColor.copy(alpha = 0.6f))
                                    Text(
                                        text = level.name,
                                        fontSize = if (isCompact) 5.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textColor.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "${level.id}",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = if (isCompact) 13.sp else 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = theme.textColor
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 1.dp else 3.dp)) {
                                        repeat(3) { i ->
                                            Text(
                                                text = "\u2605",
                                                fontSize = if (isCompact) 6.sp else 14.sp,
                                                color = if (i < stars) theme.starColor
                                                else theme.textColor.copy(alpha = 0.25f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = level.name,
                                        fontSize = if (isCompact) 5.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textColor.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isUpNext) {
                                        Text(
                                            text = "Up Next",
                                            fontSize = if (isCompact) 5.sp else 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (isFavorite && unlocked) {
                                Text(
                                    text = "\u2605",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFD600),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Previous / Next world navigation row.
        // Worlds 1-14 are consecutive (Pro+ continues 11-14 after Standard's 10).
        // World 0 (Challenge Lab) has no prev/next. Previous-world navigation is
        // clamped to the player's visibility floor — they cannot dip below their
        // skill's starting world (Pro+ players can revisit 9-10 to farm stars
        // toward Pro+ unlock thresholds, mirroring WorldSelectScreen).
        val skillMultiplier = difficulty?.starMultiplier ?: 1
        val overrideLevel = profile?.overrideStartingLevel ?: 0
        val effectiveStart = if (overrideLevel > 0) overrideLevel else (difficulty?.startingLevel ?: 1)
        val startingWorld = (effectiveStart - 1) / 9 + 1
        val visibilityFloor = if (difficulty == Difficulty.PRO_PLUS) 9 else startingWorld

        val prevWorld: Int? = when {
            worldId in 2..14 && worldId - 1 >= visibilityFloor -> worldId - 1
            else -> null
        }
        val nextWorld: Int? = when {
            worldId in 1..9 -> worldId + 1
            worldId == 10 && difficulty == Difficulty.PRO_PLUS -> 11
            worldId in 11..13 -> worldId + 1
            else -> null
        }

        if (prevWorld != null || nextWorld != null) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (prevWorld != null) {
                    Button(
                        onClick = {
                            navController.navigate(Screen.LevelSelect.create(prevWorld)) {
                                popUpTo(Screen.LevelSelect.create(worldId)) { inclusive = true }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.tileColor.copy(alpha = 0.7f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "\u2190 ${worldNames[prevWorld] ?: "World $prevWorld"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (nextWorld != null) {
                    val nextThreshold = worldStarsToUnlock[nextWorld] ?: Int.MAX_VALUE
                    val starsNeeded = nextThreshold * skillMultiplier
                    val nextUnlocked = nextWorld <= startingWorld || totalStars >= starsNeeded
                    val nextName = worldNames[nextWorld] ?: "World $nextWorld"
                    Button(
                        onClick = {
                            navController.navigate(Screen.LevelSelect.create(nextWorld)) {
                                popUpTo(Screen.LevelSelect.create(worldId)) { inclusive = true }
                            }
                        },
                        enabled = nextUnlocked,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.tileColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (nextUnlocked) "$nextName \u2192"
                                   else "\uD83D\uDD12 \u2605$starsNeeded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Challenge Lab dev/test entry-point on World 10. Gated by DevFlags.CHALLENGE_LAB_ENABLED,
        // which MUST be false on every commit. The Challenge Lab is otherwise unreachable through the UI.
        if (worldId == 10 && DevFlags.CHALLENGE_LAB_ENABLED) {
            Button(
                onClick = {
                    navController.navigate(Screen.LevelSelect.create(0)) {
                        popUpTo(Screen.LevelSelect.create(10)) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "\uD83E\uDDEA Challenge Lab (Dev) \u2192",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

    }
}

@Composable
private fun PerfectGameSplashDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFD740),
                                Color(0xFFFF8F00),
                                Color(0xFFE65100)
                            )
                        )
                    )
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uD83C\uDFC6",
                    fontSize = 64.sp
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PERFECT GAME!",
                    fontFamily = DisplayFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome to World 5",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "From here on, aim for a PERFECT GAME — complete every level in the minimum number of moves (one move per goal).",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Rewards: \u2B50 2x stars  \u2022  \uD83C\uDFAB Bonus power-ups  \uD83C\uDF89",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8F00),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "GOT IT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
