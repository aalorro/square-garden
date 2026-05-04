package com.patterngarden.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.patterngarden.data.ProgressRepository
import com.patterngarden.ui.navigation.Screen
import com.patterngarden.ui.theme.*

data class WorldInfo(
    val id: Int,
    val name: String,
    val subtitle: String,
    val starsToUnlock: Int,
    val color: androidx.compose.ui.graphics.Color
)

private val worlds = listOf(
    WorldInfo(1, "Seedling Garden", "Levels 1-8", 0, Sage),
    WorldInfo(2, "Blooming Meadow", "Levels 9-17", 8, TileBlue),
    WorldInfo(3, "Ancient Grove", "Levels 18-25", 20, WarmBrown),
    WorldInfo(4, "Crystal Cavern", "Levels 26-33", 40, androidx.compose.ui.graphics.Color(0xFF81D4FA)),
    WorldInfo(5, "Shattered Isles", "Levels 34-41", 65, androidx.compose.ui.graphics.Color(0xFFCE93D8)),
    WorldInfo(6, "Void Fortress", "Levels 42-49", 100, androidx.compose.ui.graphics.Color(0xFF78909C))
)

@Composable
fun WorldSelectScreen(navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Worlds",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${totalStars} stars",
                style = MaterialTheme.typography.bodyLarge,
                color = TileYellow,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        worlds.forEach { world ->
            val unlocked = totalStars >= world.starsToUnlock
            Card(
                onClick = {
                    if (unlocked) {
                        navController.navigate(Screen.LevelSelect.create(world.id))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .alpha(if (unlocked) 1f else 0.5f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = world.color.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = world.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = world.subtitle,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    if (!unlocked) {
                        Text(
                            text = "${world.starsToUnlock} stars",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        } // end scrollable column

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
