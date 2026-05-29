package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.ui.components.BasReliefAvatar
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val progressRepo = remember { ProgressRepository(context) }

    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
    val gamesPlayed by progressRepo.gamesPlayedFlow.collectAsState(initial = 0)
    val perfectGames by progressRepo.perfectGamesFlow.collectAsState(initial = 0)
    val totalSwaps by progressRepo.totalSwapsFlow.collectAsState(initial = 0)
    val tokensUsed by progressRepo.tokensUsedFlow.collectAsState(initial = 0)
    val gameCompleted by progressRepo.gameCompletedFlow.collectAsState(initial = false)

    var levelsThreeStarred by remember { mutableIntStateOf(0) }
    var challengeCompletions by remember { mutableStateOf(emptyMap<String, Int>()) }
    var completedDifficulties by remember { mutableStateOf(emptySet<String>()) }
    var avatarEmoji by remember { mutableStateOf("") }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var masteryBadge by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        levelsThreeStarred = progressRepo.getLevelsThreeStarred()
        challengeCompletions = progressRepo.getChallengeCompletions()
        completedDifficulties = progressRepo.getCompletedDifficulties()
        val profile = profileRepo.loadProfile()
        masteryBadge = profile.masteryBadgeEarned
        avatarEmoji = if (profile.hasCustomAvatar) "" else com.squaregarden.ui.components.getAvatar(profile.avatarId).emoji
        if (profile.hasCustomAvatar) {
            withContext(Dispatchers.IO) {
                avatarBitmap = com.squaregarden.data.AvatarStorage.loadAvatar(profile.customAvatarPath)
                    ?.asImageBitmap()
            }
        }
        loaded = true
    }

    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    "\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Lifetime Stats",
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar
        BasReliefAvatar(
            emoji = avatarEmoji,
            size = 100.dp,
            imageBitmap = avatarBitmap,
            masteryBadge = masteryBadge
        )

        if (gameCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Master of the Gardens",
                fontFamily = DisplayFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD4A017),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats grid
        StatsCard(title = "Overall") {
            StatsRow(label = "Total Stars", value = "\u2605 $totalStars")
            StatsRow(label = "Games Played", value = "$gamesPlayed")
            StatsRow(label = "Perfect Games", value = "\uD83C\uDFC6 $perfectGames")
            StatsRow(label = "Levels 3-Starred", value = "$levelsThreeStarred")
            StatsRow(label = "Total Swaps", value = "$totalSwaps")
            StatsRow(label = "Power-Ups Used", value = "$tokensUsed")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Challenges
        val totalChallenges = challengeCompletions.values.sum()
        if (totalChallenges > 0) {
            StatsCard(title = "Challenges") {
                val blitz = challengeCompletions["blitz"] ?: 0
                val overgrown = challengeCompletions["overgrown"] ?: 0
                val shifting = challengeCompletions["shifting"] ?: 0
                val memory = challengeCompletions["memory"] ?: 0
                if (blitz > 0) StatsRow(label = "\u23F1 Blitz Garden", value = "$blitz")
                if (overgrown > 0) StatsRow(label = "\uD83C\uDF3F Overgrown Garden", value = "$overgrown")
                if (shifting > 0) StatsRow(label = "\uD83C\uDF0A Shifting Sands", value = "$shifting")
                if (memory > 0) StatsRow(label = "\uD83E\uDDE0 Memory Garden", value = "$memory")
                StatsRow(label = "Total", value = "$totalChallenges")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Difficulty completions
        if (completedDifficulties.isNotEmpty()) {
            StatsCard(title = "Game Completed On") {
                Difficulty.entries.forEach { diff ->
                    if (diff.id in completedDifficulties) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = diff.label,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\u2713",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSage
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkSage
        )
    }
}
