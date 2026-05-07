package com.squaregarden.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.logic.LevelLoader
import com.squaregarden.model.Difficulty
import com.squaregarden.model.Level
import com.squaregarden.model.PlayerProgress
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*

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
        listOf(Color(0xFFFFD3E0), Color(0xFFC9B8FF), Color(0xFFA0E4FF)))
)

@Composable
fun LevelSelectScreen(worldId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    var progress by remember { mutableStateOf(PlayerProgress()) }
    var levels by remember { mutableStateOf<List<Level>>(emptyList()) }
    val lastWonLevel by progressRepo.lastWonLevelFlow.collectAsState(initial = -1)
    val profile by profileRepo.profileFlow.collectAsState(initial = null)
    val difficulty = profile?.let { Difficulty.fromId(it.difficulty) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WORLD $worldId",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = worldNames[worldId] ?: "World $worldId",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 60.dp, bottom = 4.dp)
        ) {
            items(levels) { level ->
                val stars = progress.levelStars[level.id] ?: 0
                val unlocked = level.id <= progress.highestUnlockedLevel(difficulty?.startingLevel ?: 1)
                val isLastWon = level.id == lastWonLevel
                val isFavorite = level.id in progress.favoriteLevels

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
                                Text("\uD83D\uDD12", fontSize = 22.sp, color = theme.textColor.copy(alpha = 0.6f))
                                Text(
                                    text = level.name,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "${level.id}",
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.textColor
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(3) { i ->
                                        Text(
                                            text = "\u2605",
                                            fontSize = 12.sp,
                                            color = if (i < stars) theme.starColor
                                            else theme.textColor.copy(alpha = 0.25f)
                                        )
                                    }
                                }
                                Text(
                                    text = level.name,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
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
}
