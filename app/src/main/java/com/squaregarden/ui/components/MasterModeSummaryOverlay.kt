package com.squaregarden.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squaregarden.model.MasterTier
import com.squaregarden.ui.theme.DisplayFontFamily
import kotlinx.coroutines.delay

private val GoldColor = Color(0xFFB8860B)
private val StarYellow = Color(0xFFFFD54F)

@Composable
fun MasterModeSummaryOverlay(
    starsEarned: Int,
    tierBaseStars: Int,
    gameDiffMultiplier: Float,
    streakMultiplier: Float,
    skillMultiplier: Int,
    currentStreak: Int,
    tier: MasterTier,
    onNextGame: () -> Unit,
    onEndRun: () -> Unit
) {
    // Count-up animation
    var countUpDone by remember { mutableStateOf(false) }
    var countValue by remember { mutableIntStateOf(0) }
    val countScale = remember { Animatable(0f) }

    LaunchedEffect(starsEarned) {
        countScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f))
        val steps = starsEarned.coerceAtLeast(1)
        val stepDelay = (steps * 60L).coerceIn(600L, 2000L) / steps
        for (i in 1..starsEarned) {
            countValue = i
            delay(stepDelay)
        }
        delay(400)
        countUpDone = true
    }

    // Streak fire
    val fireEmoji = when {
        currentStreak >= 24 -> "\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25"
        currentStreak >= 12 -> "\uD83D\uDD25\uD83D\uDD25"
        currentStreak >= 3 -> "\uD83D\uDD25"
        else -> ""
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        // Count-up phase
        if (!countUpDone) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\u2B50 +$countValue",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StarYellow,
                    modifier = Modifier.scale(countScale.value)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MASTER STARS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Summary card
        if (countUpDone) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tier label
                    Text(
                        text = tier.label.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = GoldColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stars earned
                    Text(
                        text = "\u2B50 +$starsEarned",
                        fontFamily = DisplayFontFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StarYellow,
                        modifier = Modifier.scale(starScale)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Multiplier breakdown
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Star Breakdown",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$tierBaseStars base \u00D7 ${String.format("%.1f", gameDiffMultiplier)} difficulty \u00D7 ${String.format("%.1f", streakMultiplier)} streak \u00D7 ${skillMultiplier} skill",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak display
                    Text(
                        text = "$fireEmoji $currentStreak Win Streak $fireEmoji",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldColor
                    )
                    Text(
                        text = "${String.format("%.1f", streakMultiplier)}x multiplier",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onEndRun,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("End Run", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onNextGame,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Next Game", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
