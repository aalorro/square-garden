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
                "\u2022 Shape — L, T, or Cross formations"
        )

        InstructionSection(
            title = "Stars",
            body = "Earn up to 3 stars per level based on how many moves you have left:\n" +
                "\u2022 Complete the level = 1 star\n" +
                "\u2022 Finish with moves to spare = 2 stars\n" +
                "\u2022 Finish well under the limit = 3 stars"
        )

        InstructionSection(
            title = "Hints",
            body = "Stuck? Tap the Hint button during gameplay to highlight a suggested swap. " +
                "Use hints wisely — they show a good move but not always the optimal one."
        )

        InstructionSection(
            title = "Worlds",
            body = "Progress through 10 worlds, each with unique themes and increasing difficulty. " +
                "Earn stars to unlock new worlds. Boss levels (last level of each world) are extra challenging!"
        )

        InstructionSection(
            title = "Skill",
            body = "Choose your skill level in your profile:\n" +
                "\u2022 Casual — more moves available\n" +
                "\u2022 Standard — balanced challenge\n" +
                "\u2022 Pro — fewer moves, for experts"
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
