package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.MasterModeRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.model.MasterModeState
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.DisplayFontFamily
import com.squaregarden.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun MasterModeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val masterRepo = remember { MasterModeRepository(context) }
    val progressRepo = remember { ProgressRepository(context) }
    val scope = rememberCoroutineScope()

    var masterState by remember { mutableStateOf(MasterModeState()) }
    val lives by progressRepo.livesFlow.collectAsState(initial = 3)
    val cooldownUntil by progressRepo.cooldownUntilFlow.collectAsState(initial = 0L)
    val cooldownActive = lives <= 0 && cooldownUntil > System.currentTimeMillis()

    LaunchedEffect(Unit) {
        masterState = masterRepo.loadState()
    }

    val goldColor = Color(0xFFB8860B)

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Master Mode",
                fontFamily = DisplayFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = goldColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Crown icon
            Text(
                text = "\uD83D\uDC51",
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Infinite Challenge",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Streak card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current streak with fire scaling
                    val fireEmoji = when {
                        masterState.currentStreak >= 24 -> "\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25"
                        masterState.currentStreak >= 12 -> "\uD83D\uDD25\uD83D\uDD25"
                        masterState.currentStreak >= 3 -> "\uD83D\uDD25"
                        else -> ""
                    }
                    Text(
                        text = "$fireEmoji ${masterState.currentStreak} $fireEmoji",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak multiplier bar
                    Text(
                        text = "Multiplier: ${String.format("%.1f", masterState.streakMultiplier)}x",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (masterState.streakMultiplier / 5.0f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = goldColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Best Streak", "${masterState.bestStreak}")
                        StatItem("Session \u2605", "${masterState.sessionStars}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("All-Time \u2605", "${masterState.totalMasterStars}")
                        StatItem("Games Won", "${masterState.gamesWon}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enter the Garden button
            Button(
                onClick = {
                    scope.launch { masterRepo.startNewSession() }
                    navController.navigate(Screen.Game.create(GameViewModel.MASTER_MODE_SIGNAL))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                enabled = !cooldownActive
            ) {
                Text(
                    text = if (cooldownActive) "\u2764\uFE0F  Resting..." else "\uD83C\uDF31  Enter the Garden",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
