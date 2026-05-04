package com.patterngarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.patterngarden.data.ProgressRepository
import com.patterngarden.logic.LevelLoader
import com.patterngarden.model.Level
import com.patterngarden.model.PlayerProgress
import com.patterngarden.ui.navigation.Screen
import com.patterngarden.ui.theme.TileYellow

@Composable
fun LevelSelectScreen(worldId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    var progress by remember { mutableStateOf(PlayerProgress()) }
    var levels by remember { mutableStateOf<List<Level>>(emptyList()) }
    val lastWonLevel by progressRepo.lastWonLevelFlow.collectAsState(initial = -1)

    LaunchedEffect(Unit) {
        progress = progressRepo.loadProgress()
        levels = LevelLoader.loadAllLevels(context).filter { it.world == worldId }
    }

    val worldNames = mapOf(
        1 to "Seedling Garden", 2 to "Blooming Meadow", 3 to "Ancient Grove",
        4 to "Crystal Cavern", 5 to "Shattered Isles", 6 to "Void Fortress",
        7 to "Molten Core", 8 to "Starfall Summit", 9 to "Abyssal Depths", 10 to "Prism Citadel"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = worldNames[worldId] ?: "World $worldId",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(60.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(levels) { level ->
                val stars = progress.levelStars[level.id] ?: 0
                val unlocked = level.id <= progress.highestUnlockedLevel()
                val isLastWon = level.id == lastWonLevel

                Card(
                    onClick = {
                        if (unlocked) {
                            navController.navigate(Screen.Game.create(level.id))
                        }
                    },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .alpha(if (unlocked) 1f else 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLastWon)
                            MaterialTheme.colorScheme.primaryContainer
                        else if (unlocked)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isLastWon) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 2.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${level.id}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = level.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        if (stars > 0) {
                            val displayStars = stars.coerceAtMost(3)
                            Text(
                                text = "★".repeat(displayStars) + "☆".repeat(3 - displayStars),
                                fontSize = 10.sp,
                                color = TileYellow,
                                textAlign = TextAlign.Center
                            )
                            if (stars > 3) {
                                Text(
                                    text = "${stars}★",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TileYellow
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Back", fontSize = 28.sp)
        }
    }
}
