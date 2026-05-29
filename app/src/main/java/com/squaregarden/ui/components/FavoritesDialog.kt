package com.squaregarden.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.squaregarden.data.ProgressRepository
import com.squaregarden.model.Level
import com.squaregarden.model.PlayerProgress
import com.squaregarden.ui.theme.DisplayFontFamily
import kotlinx.coroutines.launch

private val worldNames = mapOf(
    1 to "Seedling Garden", 2 to "Blooming Meadow", 3 to "Ancient Grove",
    4 to "Crystal Cavern", 5 to "Shattered Isles", 6 to "Void Fortress",
    7 to "Molten Core", 8 to "Starfall Summit", 9 to "Abyssal Depths", 10 to "Prism Citadel",
    11 to "Nebula Verge", 12 to "Quantum Lattice", 13 to "Singularity Spire", 14 to "Infinity Prism"
)

@Composable
fun FavoritesDialog(
    allLevels: List<Level>,
    progress: PlayerProgress,
    progressRepo: ProgressRepository,
    onLevelClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentProgress by remember { mutableStateOf(progress) }
    val scope = rememberCoroutineScope()

    val favoriteLevels = remember(currentProgress.favoriteLevels) {
        allLevels.filter { it.id in currentProgress.favoriteLevels }
            .sortedBy { it.id }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Text(
                    text = "Favorites",
                    fontFamily = DisplayFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (favoriteLevels.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "\u2605",
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No favorites yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the star on any level\nto add it here",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteLevels, key = { it.id }) { level ->
                            val stars = currentProgress.levelStars[level.id] ?: 0
                            val worldName = worldNames[level.world] ?: "World ${level.world}"

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLevelClick(level.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Level number
                                    Text(
                                        text = "${level.id}",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(44.dp)
                                    )

                                    // Level info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = level.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = worldName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        // Stars
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(3) { i ->
                                                Text(
                                                    text = "\u2605",
                                                    fontSize = 12.sp,
                                                    color = if (i < stars)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                                )
                                            }
                                        }
                                    }

                                    // Unfavorite button
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                progressRepo.toggleFavorite(level.id)
                                                currentProgress = currentProgress.copy(
                                                    favoriteLevels = currentProgress.favoriteLevels - level.id
                                                )
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "\u2605",
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
