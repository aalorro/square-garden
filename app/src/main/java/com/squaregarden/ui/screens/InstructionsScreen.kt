package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.ui.theme.DisplayFontFamily

@Composable
fun InstructionsScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    "\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "How to Play",
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        InstructionSection(
            title = "Goal",
            body = "Complete all pattern goals before you run out of moves. " +
                "Each level has a set of goals — match the required patterns on the board to earn stars."
        )

        InstructionSection(
            title = "Swapping Tiles",
            body = "Drag a tile onto an adjacent tile (up, down, left, or right) to swap them. " +
                "Each swap uses one move. Plan carefully!"
        )

        InstructionSection(
            title = "Patterns",
            body = "Goals ask you to form patterns with colored tiles:\n" +
                "\u2022 Line — 3 or more tiles in a row or column\n" +
                "\u2022 Square — a 2\u00D72 block of the same color\n" +
                "\u2022 Shape — L, T, Cross, Z, or U formations"
        )

        InstructionSection(
            title = "Stars & Difficulty",
            body = "Earn up to 3 stars per level based on moves remaining. " +
                "Each game is rated Easy to Extremely Hard based on the randomized board. " +
                "Harder games multiply your stars — up to 2\u00D7 on Extremely Hard!"
        )

        InstructionSection(
            title = "Borders",
            body = "When you complete a goal, those tiles become bordered. " +
                "Swapping through bordered tiles breaks that goal and you'll need to re-form it. " +
                "Pro players cannot swap through borders at all."
        )

        InstructionSection(
            title = "Frozen Tiles & Void Cells",
            body = "Frozen tiles (World 4+) can't be moved but count toward goals. " +
                "Void cells (World 5+) are empty spaces that create irregular board shapes. " +
                "Orange tiles debut in World 5."
        )

        InstructionSection(
            title = "Worlds",
            body = "Progress through 10 worlds, each with unique themes and increasing difficulty. " +
                "Earn stars to unlock new worlds. Unlock thresholds scale by skill level."
        )

        InstructionSection(
            title = "Skill",
            body = "Choose your skill level when creating your profile:\n" +
                "\u2022 Casual — more moves, start at World 1\n" +
                "\u2022 Standard — balanced, start at World 2\n" +
                "\u2022 Pro — fewer moves, start at World 3\n\n" +
                "Skill is locked after creation. Reset progress in Settings to change."
        )

        InstructionSection(
            title = "Power-Ups",
            body = "Earn tokens by playing well:\n\n" +
                "\uD83D\uDCA1 Hint — highlights a good move area\n\n" +
                "\uD83D\uDD00 Shuffle — randomizes the board tiles. " +
                "Earn 1 token each time you unlock a new world.\n\n" +
                "\uD83D\uDEE1\uFE0F Passthrough — your next swap through a border won't break the goal. " +
                "Earn 1 token every 7 levels completed.\n\n" +
                "\u2744\uFE0F Unfreeze — tap a frozen tile to unfreeze it. " +
                "Earn 1 token for every 5 consecutive wins on World 3+."
        )

        InstructionSection(
            title = "Tip: Lives",
            body = "You start with 3 lives. Losing a level costs one life. " +
                "To earn a life back, win 3 levels in a row. Only levels within 5 of your " +
                "highest completed level count toward the streak."
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InstructionSection(title: String, body: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
