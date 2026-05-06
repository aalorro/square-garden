package com.squaregarden.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squaregarden.model.Goal
import com.squaregarden.model.TileColor
import com.squaregarden.ui.theme.*

@Composable
fun GoalPanel(
    goals: List<Goal>,
    completedIds: Set<String>,
    movesRemaining: Int = -1,
    movesMax: Int = -1,
    difficultyLabel: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "GOALS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            goals.forEach { goal ->
                val completed = goal.id in completedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Color swatch
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(goal.color.toComposeColor())
                    )
                    Text(
                        text = goal.description,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (completed)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (completed) {
                        Text(
                            text = "\u2714",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43A047)
                        )
                    }
                }
            }

            // Moves + Difficulty row below goals
            if (movesRemaining >= 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val lowMoves = movesRemaining <= 3
                    Text(
                        text = "MOVES",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "$movesRemaining",
                        fontFamily = DisplayFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (lowMoves) Color(0xFFC62828) else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "/$movesMax",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (difficultyLabel.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Skill:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = difficultyLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
