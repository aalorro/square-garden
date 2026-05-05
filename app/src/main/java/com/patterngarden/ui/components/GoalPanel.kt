package com.patterngarden.ui.components

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
import com.patterngarden.model.Goal
import com.patterngarden.model.TileColor
import com.patterngarden.ui.theme.*

@Composable
fun GoalPanel(
    goals: List<Goal>,
    completedIds: Set<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "GOALS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            goals.forEach { goal ->
                val completed = goal.id in completedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color swatch
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(goal.color.toComposeColor())
                    )
                    Text(
                        text = goal.description,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (completed)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    if (completed) {
                        Text(
                            text = "DONE",
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF43A047)
                        )
                    }
                }
            }
        }
    }
}
