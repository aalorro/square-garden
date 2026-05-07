package com.squaregarden.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PlayerBadge(
    avatarEmoji: String,
    playerLevel: Int,
    totalStars: Int,
    gamesPlayed: Int,
    lives: Int,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onStarPositioned: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    // Animated star counter: counts up over max 5 seconds then snaps to total
    var displayedStars by remember { mutableIntStateOf(totalStars) }
    LaunchedEffect(totalStars) {
        if (totalStars > displayedStars) {
            val diff = totalStars - displayedStars
            val frameDelay = 50L
            val maxFrames = (5000L / frameDelay).toInt() // 100 frames in 5s
            val increment = maxOf(1, diff / maxFrames)
            while (displayedStars < totalStars) {
                delay(frameDelay)
                displayedStars = minOf(displayedStars + increment, totalStars)
            }
        } else {
            displayedStars = totalStars
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(if (isCompact) 12.dp else 16.dp)
                )
                .clickable { showMenu = true }
                .padding(
                    horizontal = if (isCompact) 5.dp else 8.dp,
                    vertical = if (isCompact) 3.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(if (isCompact) 34.dp else 60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = avatarEmoji, fontSize = if (isCompact) 18.sp else 32.sp)
            }

            // Level + Games + Stars + Lives stacked
            Column {
                Text(
                    text = "Lv $playerLevel",
                    fontSize = if (isCompact) 15.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$gamesPlayed played",
                    fontSize = if (isCompact) 9.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "$displayedStars ★",
                    fontSize = if (isCompact) 13.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD4A017),
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        val size = coords.size
                        onStarPositioned?.invoke(
                            Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                        )
                    }
                )
                Text(
                    text = "\u2764".repeat(lives),
                    fontSize = if (isCompact) 11.sp else 16.sp,
                    color = Color(0xFFE53935)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    showMenu = false
                    onSettingsClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Exit") },
                onClick = {
                    showMenu = false
                    onExitClick()
                }
            )
        }
    }
}
