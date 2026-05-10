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
                "Earn 1 token each time you unlock a new world or every 7 wins.\n\n" +
                "\uD83D\uDEE1\uFE0F Passthrough — your tile jumps over completed goal tiles and lands on the other side, keeping the goal intact. " +
                "Earn 1 token every 7 levels completed.\n\n" +
                "\u2744\uFE0F Unfreeze — tap a frozen tile to unfreeze it. " +
                "Earn 1 token for every 5 consecutive wins on World 3+.\n\n" +
                "\u21BB Redo — restart the level with a fresh board and full moves, no life lost. " +
                "Earn tokens by capturing redo tiles (marked with a \u21BB symbol) that appear on World 4+ boards. " +
                "Complete a goal that includes a redo tile to collect the token."
        )

        InstructionSection(
            title = "Tip: Lives",
            body = "You start with 3 lives. Losing a level costs one life. " +
                "To earn a life back, win 3 levels in a row. Only levels within 5 of your " +
                "highest completed level count toward the streak."
        )

        InstructionSection(
            title = "Tip: Perfect Game",
            body = "On World 5+, complete all goals in a number of moves equal to or fewer than the number of goals " +
                "(e.g. 5 goals in 5 moves) to earn a Perfect Game. " +
                "Reward: 2\u00D7 stars on top of all other multipliers, plus +1 of every power-up token " +
                "(Shuffle, Passthrough, Unfreeze, and Redo)."
        )

        InstructionSection(
            title = "Tip: Favorites",
            body = "Tap the \u2606 star next to the level name during gameplay to favorite a level. " +
                "Favorited levels show a \u2605 marker on the level select screen for easy access."
        )

        InstructionSection(
            title = "Challenge Rounds",
            body = "Starting from World 5, special challenge rounds can be unlocked by exceptional play. " +
                "Challenges cost no lives and award bonus stars + one of every power-up token on completion.\n\n" +
                "\u23F1 Blitz Garden — 60-second time attack! Clear as many goal sets as possible before time runs out. " +
                "Timer starts on your first swap. Clear 5+ goals to win. " +
                "Build combos for score multipliers (2\u00D7 at 3, 3\u00D7 at 6).\n" +
                "Trigger: 8 consecutive progressive level wins on World 5+.\n\n" +
                "\uD83C\uDF3F Overgrown Garden — Conquer a massive 9\u00D79 board loaded with goals and frozen tiles " +
                "in just 16 moves. You get 3 tries with a fresh board each time.\n" +
                "Trigger: Complete all 9 levels in a world (once per world).\n\n" +
                "\uD83C\uDF0A Shifting Sands — Every 3 swaps, all uncompleted tiles scramble into new positions! " +
                "Plan your moves around the chaos.\n" +
                "Trigger: Win 3 levels in a row without using any power-ups.\n\n" +
                "\uD83E\uDDE0 Memory Garden — Tiles are hidden! They reveal briefly at the start, " +
                "then you can only see tiles near where you swap. Remember the layout!\n" +
                "Trigger: Every perfect game (immediate)."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back")
        }

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
