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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.PlayGamesManager
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.SettingsRepository
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    val progressRepo = remember { ProgressRepository(context) }
    val scope = rememberCoroutineScope()

    var soundEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var currentThemeId by remember { mutableStateOf("light") }
    var currentProfile by remember { mutableStateOf(com.squaregarden.model.UserProfile()) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        soundEnabled = settingsRepo.soundEnabled.first()
        musicEnabled = settingsRepo.musicEnabled.first()
        currentProfile = profileRepo.loadProfile()
        currentThemeId = currentProfile.themeId
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Settings",
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme section
        SectionLabel("Theme")
        allThemes.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowThemes.forEach { theme ->
                    val selected = theme.id == currentThemeId
                    Card(
                        onClick = {
                            currentThemeId = theme.id
                            scope.launch {
                                currentProfile = currentProfile.copy(themeId = theme.id)
                                profileRepo.saveProfile(currentProfile)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = if (selected)
                            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        colors = CardDefaults.cardColors(containerColor = theme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .background(theme.primary, RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = theme.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.onBackground,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sound section
        SectionLabel("Sound & Haptics")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                ToggleRow(
                    label = "Sound effects",
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        soundEnabled = enabled
                        scope.launch { settingsRepo.setSoundEnabled(enabled) }
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.background,
                    thickness = 1.dp
                )
                ToggleRow(
                    label = "Background music",
                    checked = musicEnabled,
                    onCheckedChange = { enabled ->
                        musicEnabled = enabled
                        scope.launch { settingsRepo.setMusicEnabled(enabled) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile button
        OutlinedButton(
            onClick = { navController.navigate(Screen.Profile.route) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Edit Profile", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Leaderboards button (only if opted in)
        if (currentProfile.leaderboardOptIn) {
            OutlinedButton(
                onClick = {
                    val activity = context as? android.app.Activity ?: return@OutlinedButton
                    PlayGamesManager.checkSignIn(activity) { signedIn ->
                        val openLeaderboards = {
                            // Submit current progress before showing leaderboards
                            scope.launch {
                                val totalStars = progressRepo.totalStarsFlow.first()
                                val progress = progressRepo.loadProgress()
                                val diff = com.squaregarden.model.Difficulty.fromId(currentProfile.difficulty)
                                val highestLevel = progress.highestUnlockedLevel(diff.startingLevel)
                                PlayGamesManager.submitTotalStars(activity, diff, totalStars)
                                PlayGamesManager.submitHighestLevel(activity, diff, highestLevel)
                            }
                            PlayGamesManager.showAllLeaderboards(activity)
                        }
                        if (signedIn) {
                            openLeaderboards()
                        } else {
                            PlayGamesManager.signIn(activity) { success ->
                                if (success) openLeaderboards()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("\uD83C\uDFC6  Leaderboards", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // How to Play
        OutlinedButton(
            onClick = { navController.navigate(Screen.Instructions.route) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("How to Play", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // About
        OutlinedButton(
            onClick = { navController.navigate(Screen.About.route) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("About", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy Policy
        OutlinedButton(
            onClick = { navController.navigate(Screen.Privacy.route) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Privacy Policy", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reset progress
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC62828))
        ) {
            Text("Reset progress", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer
        Text(
            text = "Square Garden v1.3.0 \u00B7 Made with \uD83C\uDF31",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress?") },
            text = {
                Text(
                    "This will reset all your stars, level progress, and player level " +
                    "back to zero. You will be able to choose a new skill level.\n\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            progressRepo.clearAll()
                            profileRepo.resetPlayerLevel()
                            settingsRepo.setShapesExplainerDismissed(false)
                            navController.navigate(Screen.ProfileSetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
