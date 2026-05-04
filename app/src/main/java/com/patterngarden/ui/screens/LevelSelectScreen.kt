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
import androidx.compose.ui.graphics.Color
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
import com.patterngarden.ui.theme.*

// World theme colors: (tile background, tile text, accent/star color)
private data class WorldTheme(
    val tileColor: Color,
    val tileColorLight: Color,
    val textColor: Color,
    val starColor: Color
)

private val worldThemes = mapOf(
    1 to WorldTheme(Color(0xFF81C784), Color(0xFFA5D6A7), Color(0xFF1B5E20), Color(0xFFFFCA28)),
    2 to WorldTheme(Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFF0D47A1), Color(0xFFFFCA28)),
    3 to WorldTheme(Color(0xFFA1887F), Color(0xFFBCAAA4), Color(0xFF3E2723), Color(0xFFFFD54F)),
    4 to WorldTheme(Color(0xFF4FC3F7), Color(0xFF81D4FA), Color(0xFF01579B), Color(0xFFFFE082)),
    5 to WorldTheme(Color(0xFFCE93D8), Color(0xFFE1BEE7), Color(0xFF4A148C), Color(0xFFFFD740)),
    6 to WorldTheme(Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFF263238), Color(0xFFFFE57F)),
    7 to WorldTheme(Color(0xFFFF8A65), Color(0xFFFFAB91), Color(0xFFBF360C), Color(0xFFFFD600)),
    8 to WorldTheme(Color(0xFFB39DDB), Color(0xFFD1C4E9), Color(0xFF311B92), Color(0xFFFFE082)),
    9 to WorldTheme(Color(0xFF4DB6AC), Color(0xFF80CBC4), Color(0xFF004D40), Color(0xFFFFD740)),
    10 to WorldTheme(Color(0xFFF48FB1), Color(0xFFF8BBD0), Color(0xFF880E4F), Color(0xFFFFE082))
)

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

    val theme = worldThemes[worldId] ?: worldThemes[1]!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = worldNames[worldId] ?: "World $worldId",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
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
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .alpha(if (unlocked) 1f else 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLastWon)
                            theme.tileColor
                        else if (unlocked)
                            theme.tileColorLight
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isLastWon) BorderStroke(3.dp, theme.textColor) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 4.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${level.id}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                        Text(
                            text = level.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 18.sp
                        )
                        if (stars > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayStars = stars.coerceAtMost(3)
                            Text(
                                text = "\u2605".repeat(displayStars) + "\u2606".repeat(3 - displayStars),
                                fontSize = 20.sp,
                                color = theme.starColor,
                                textAlign = TextAlign.Center
                            )
                            if (stars > 3) {
                                Text(
                                    text = "${stars}\u2605",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.starColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, theme.textColor)
        ) {
            Text("Back", fontSize = 28.sp, color = theme.textColor)
        }
    }
}
