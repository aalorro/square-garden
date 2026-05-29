package com.squaregarden.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squaregarden.model.TileColor
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameCompleteOverlay(
    totalStars: Int,
    perfectGames: Int,
    gamesPlayed: Int,
    challengesCompleted: Int,
    onSaveBadge: () -> Unit,
    onReturnHome: () -> Unit
) {
    // Entrance animation
    val titleScale = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val tilesAlpha = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        titleScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f))
    }
    LaunchedEffect(Unit) {
        delay(400)
        subtitleAlpha.animateTo(1f, tween(600))
    }
    LaunchedEffect(Unit) {
        delay(800)
        tilesAlpha.animateTo(1f, tween(500))
    }
    LaunchedEffect(Unit) {
        delay(1200)
        statsAlpha.animateTo(1f, tween(600))
    }
    LaunchedEffect(Unit) {
        delay(1800)
        buttonsAlpha.animateTo(1f, tween(500))
    }

    // Continuously looping celebration — cycle key every 30s to restart particles
    var celebrationCycle by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            celebrationCycle++
        }
    }

    key(celebrationCycle) {
        ConfettiOverlay(stars = 3, perfectGame = true)
        BalloonOverlay(stars = 3, perfectGame = true)
        StarBurstOverlay(stars = 3, perfectGame = true)
    }

    // Dark scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SoftWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Game Complete!",
                    fontFamily = DisplayFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4A017),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(titleScale.value)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "Master of the Gardens",
                    fontFamily = DisplayFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = DarkSage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(subtitleAlpha.value)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Decorative row of 6 mini embossed tiles
                Canvas(
                    modifier = Modifier
                        .width(240.dp)
                        .height(40.dp)
                        .alpha(tilesAlpha.value)
                ) {
                    val tileSize = size.height * 0.8f
                    val spacing = (size.width - tileSize * 6) / 7f
                    val y = (size.height - tileSize) / 2f
                    TileColor.entries.forEachIndexed { i, color ->
                        val x = spacing + i * (tileSize + spacing)
                        drawEmbossedTile(
                            color = color,
                            x = x, y = y,
                            cs = tileSize,
                            cornerR = tileSize * 0.15f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats grid
                Column(
                    modifier = Modifier.alpha(statsAlpha.value),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow(label = "Total Stars", value = "$totalStars")
                    StatRow(label = "Perfect Games", value = "$perfectGames")
                    StatRow(label = "Games Played", value = "$gamesPlayed")
                    StatRow(label = "Challenges Completed", value = "$challengesCompleted")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Column(
                    modifier = Modifier.alpha(buttonsAlpha.value),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onSaveBadge,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4A017)
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "Save Mastery Badge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onReturnHome,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "Return to Garden",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = WarmBrown
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkSage
        )
    }
}
