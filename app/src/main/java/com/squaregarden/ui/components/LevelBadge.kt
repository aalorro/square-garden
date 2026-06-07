package com.squaregarden.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
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
    masteryBadge: Boolean = false,
    masterMode: Boolean = false,
    onHomeClick: () -> Unit,
    onMasterModeClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onStarPositioned: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }
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

    // Tablet = 30% bigger than phone
    val avatarSize = if (isCompact) 38.dp else 75.dp
    val cornerRadius = if (isCompact) 10.dp else 21.dp
    val hPad = if (isCompact) 5.dp else 10.dp
    val vPad = if (isCompact) 3.dp else 8.dp
    val gap = if (isCompact) 4.dp else 8.dp
    val levelFontSize = if (isCompact) 13.sp else 26.sp
    val starFontSize = if (isCompact) 12.sp else 23.sp
    val smallFontSize = if (isCompact) 10.sp else 18.sp

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            // Avatar — tap opens menu, horizontal swipe toggles collapse/expand
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (dragAmount.x > 8f) expanded = false
                            else if (dragAmount.x < -8f) expanded = true
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { showMenu = true }
                    }
            ) {
                BasReliefAvatar(
                    emoji = avatarEmoji,
                    size = avatarSize,
                    animate = false,
                    imageBitmap = avatarImageBitmap,
                    masteryBadge = masteryBadge
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Level
                    Text(
                        text = "Lv$playerLevel",
                        fontSize = levelFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Stars (with position tracking) — crown icon in Master Mode
                    Text(
                        text = if (masterMode) "\uD83D\uDC51$displayedStars" else "$displayedStars\u2605",
                        fontSize = starFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (masterMode) Color(0xFFB8860B) else Color(0xFFD4A017),
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
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Home") },
                onClick = {
                    showMenu = false
                    onHomeClick()
                }
            )
            if (onMasterModeClick != null) {
                DropdownMenuItem(
                    text = { Text("Master Mode") },
                    onClick = {
                        showMenu = false
                        onMasterModeClick()
                    }
                )
            }
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
