package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.SettingsRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.model.PlayerProgress
import com.squaregarden.model.UserProfile
import com.squaregarden.ui.components.BasReliefAvatar
import com.squaregarden.ui.components.getAvatar
import com.squaregarden.ui.components.LogoMark
import com.squaregarden.ui.navigation.Screen
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
    val cooldownActive = lives <= 0 && cooldownUntil > System.currentTimeMillis()
    var profile by remember { mutableStateOf(UserProfile()) }
    var currentWorld by remember { mutableIntStateOf(1) }

    val settingsRepo = remember { SettingsRepository(context) }
    val musicEnabled by settingsRepo.musicEnabled.collectAsState(initial = true)

    LaunchedEffect(Unit) {
        profile = profileRepo.loadProfile()
        val progress = progressRepo.loadProgress()
        val difficulty = Difficulty.fromId(profile.difficulty)
        val highestUnlocked = progress.highestUnlockedLevel(difficulty.startingLevel)
        currentWorld = ((highestUnlocked - 1) / 9) + 1
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate(Screen.LevelSelect.create(currentWorld)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
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

            OutlinedButton(
                onClick = { navController.navigate(Screen.WorldSelect.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                enabled = !cooldownActive
            ) {
                Text("Worlds", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.Instructions.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("How to Play", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
