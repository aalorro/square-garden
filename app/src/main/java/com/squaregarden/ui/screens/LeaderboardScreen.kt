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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.LeaderboardRepository
import com.squaregarden.data.ProfileRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.model.LeaderboardEntry
import com.squaregarden.ui.theme.DarkSage
import com.squaregarden.ui.theme.DisplayFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val leaderboardRepo = remember { LeaderboardRepository() }
    val scope = rememberCoroutineScope()

    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var playerEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastRefreshed by remember { mutableLongStateOf(0L) }

    // Load profile to set default difficulty; warm up Firebase connection
    LaunchedEffect(Unit) {
        leaderboardRepo.invalidateCache()
        leaderboardRepo.ensureAuthenticated()
        val profile = profileRepo.loadProfile()
        selectedDifficulty = Difficulty.fromId(profile.difficulty)
    }

    // Fetch leaderboard when difficulty changes (auto-retry once on failure)
    LaunchedEffect(selectedDifficulty) {
        loading = true
        error = null
        for (attempt in 1..2) {
            try {
                val result = leaderboardRepo.fetchLeaderboard(selectedDifficulty)
                entries = result
                playerEntry = leaderboardRepo.findPlayerRank(result)
                lastRefreshed = System.currentTimeMillis()
                error = null
                break
            } catch (e: Exception) {
                if (attempt == 1) {
                    delay(1500)
                    leaderboardRepo.invalidateCache()
                } else {
                    error = "Could not load leaderboard. Check your connection."
                }
            }
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    "\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Leaderboards",
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Difficulty chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { diff ->
                FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = {
                        if (selectedDifficulty != diff) {
                            selectedDifficulty = diff
                            leaderboardRepo.invalidateCache()
                        }
                    },
                    label = {
                        Text(
                            diff.label,
                            fontSize = 11.sp,
                            fontWeight = if (selectedDifficulty == diff) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                leaderboardRepo.invalidateCache()
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        val result = leaderboardRepo.fetchLeaderboard(selectedDifficulty)
                                        entries = result
                                        playerEntry = leaderboardRepo.findPlayerRank(result)
                                        lastRefreshed = System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        error = "Could not load leaderboard. Check your connection."
                                    }
                                    loading = false
                                }
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            entries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\uD83C\uDFC6",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No scores yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Be the first to join the leaderboard!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                // Leaderboard list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header row
                            Text(
                                text = "Total Stars \u2022 ${selectedDifficulty.label}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            entries.forEach { entry ->
                                val isPlayer = entry.uid == leaderboardRepo.currentUid
                                LeaderboardRow(
                                    entry = entry,
                                    isCurrentPlayer = isPlayer
                                )
                            }

                            // Show player rank below if not in top 50
                            if (playerEntry == null && leaderboardRepo.currentUid != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "Your score is not in the top 50 yet. Keep playing!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Refresh button + last updated
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lastRefreshed > 0) {
                val agoMinutes = ((System.currentTimeMillis() - lastRefreshed) / 60000).toInt()
                Text(
                    text = if (agoMinutes < 1) "Just now" else "${agoMinutes}m ago",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            OutlinedButton(
                onClick = {
                    leaderboardRepo.invalidateCache()
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val result = leaderboardRepo.fetchLeaderboard(selectedDifficulty)
                            entries = result
                            playerEntry = leaderboardRepo.findPlayerRank(result)
                            lastRefreshed = System.currentTimeMillis()
                        } catch (e: Exception) {
                            error = "Could not load leaderboard. Check your connection."
                        }
                        loading = false
                    }
                },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Refresh", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, isCurrentPlayer: Boolean) {
    val bgColor = if (isCurrentPlayer)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#${entry.rank}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when (entry.rank) {
                    1 -> DarkSage
                    2 -> DarkSage.copy(alpha = 0.8f)
                    3 -> DarkSage.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.width(40.dp)
            )

            // Emoji avatar
            Text(
                text = entry.emoji.ifBlank { "\uD83C\uDF31" },
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name.ifBlank { "Gardener" },
                    fontSize = 15.sp,
                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (entry.level > 0) {
                    Text(
                        text = "Level ${entry.level}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Score
            Text(
                text = "\u2605 ${entry.score}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSage
            )
        }
    }
}
