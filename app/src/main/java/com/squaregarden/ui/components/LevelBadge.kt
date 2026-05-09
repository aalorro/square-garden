package com.squaregarden.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
    avatarImageBitmap: ImageBitmap? = null,
    playerLevel: Int,
    totalStars: Int,
    gamesPlayed: Int,
    lives: Int,
    perfectGames: Int = 0,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onStarPositioned: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompact = screenWidthDp < 600
    val isLargeTablet = screenWidthDp >= 840

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

    val avatarSize = when { isCompact -> 38.dp; isLargeTablet -> 80.dp; else -> 58.dp }
    val cornerRadius = when { isCompact -> 10.dp; isLargeTablet -> 20.dp; else -> 16.dp }
    val hPad = when { isCompact -> 5.dp; isLargeTablet -> 12.dp; else -> 8.dp }
    val vPad = when { isCompact -> 3.dp; isLargeTablet -> 8.dp; else -> 6.dp }
    val gap = when { isCompact -> 4.dp; isLargeTablet -> 8.dp; else -> 6.dp }
    val levelFontSize = when { isCompact -> 13.sp; isLargeTablet -> 26.sp; else -> 20.sp }
    val starFontSize = when { isCompact -> 12.sp; isLargeTablet -> 24.sp; else -> 18.sp }
    val smallFontSize = when { isCompact -> 10.sp; isLargeTablet -> 18.sp; else -> 14.sp }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(cornerRadius)
                )
                .clickable { showMenu = true }
                .padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            // Avatar
            BasReliefAvatar(
                emoji = avatarEmoji,
                size = avatarSize,
                animate = false,
                imageBitmap = avatarImageBitmap
            )

            // Level
            Text(
                text = "Lv$playerLevel",
                fontSize = levelFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Stars (with position tracking)
            Text(
                text = "$displayedStars\u2605",
                fontSize = starFontSize,
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

            // Lives
            Text(
                text = "\u2764".repeat(lives),
                fontSize = smallFontSize,
                color = Color(0xFFE53935)
            )

            // Perfect games (only if > 0)
            if (perfectGames > 0) {
                Text(
                    text = "\uD83C\uDFC6$perfectGames",
                    fontSize = smallFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4A017)
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
