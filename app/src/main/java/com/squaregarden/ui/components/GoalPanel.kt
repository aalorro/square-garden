package com.squaregarden.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squaregarden.model.CellPos
import com.squaregarden.model.GameDifficulty
import com.squaregarden.model.Goal
import com.squaregarden.ui.theme.*

/** Extract the cell positions to draw for any goal type. */
private fun Goal.cellPositions(): List<CellPos> = when (this) {
    is Goal.Line -> (0 until length).map { CellPos(0, it) }
    is Goal.Square -> listOf(CellPos(0, 0), CellPos(0, 1), CellPos(1, 0), CellPos(1, 1))
    is Goal.Shape -> shapeType.offsets
}

/** Compute (rows, cols) needed to draw this goal's shape. */
private fun Goal.gridBounds(): Pair<Int, Int> {
    val cells = cellPositions()
    return Pair(cells.maxOf { it.row } + 1, cells.maxOf { it.col } + 1)
}

/** Determine grid layout (layoutRows, layoutCols) based on goal count. */
private fun gridLayout(count: Int): Pair<Int, Int> = when (count) {
    1 -> 1 to 1
    2 -> 1 to 2
    3 -> 1 to 3
    4 -> 2 to 2
    5 -> 2 to 3
    6 -> 2 to 3
    7 -> 2 to 4
    else -> 2 to 4
}

@Composable
fun GoalPanel(
    goals: List<Goal>,
    completedIds: Set<String>,
    movesRemaining: Int = -1,
    movesMax: Int = -1,
    gameDifficulty: GameDifficulty? = null,
    modifier: Modifier = Modifier
) {
    val isSmallScreen = LocalConfiguration.current.screenWidthDp < 800

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isSmallScreen) 8.dp else 14.dp,
                    vertical = if (isSmallScreen) 6.dp else 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 3.dp else 5.dp)
        ) {
            // Goal grid
            val (layoutRows, layoutCols) = gridLayout(goals.size)
            val rowHeight: Dp = if (layoutRows == 1) {
                if (isSmallScreen) 48.dp else 64.dp
            } else {
                if (isSmallScreen) 36.dp else 48.dp
            }

            val rows = goals.chunked(layoutCols)
            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isSmallScreen) 4.dp else 6.dp
                    )
                ) {
                    for (goal in row) {
                        GoalBox(
                            goal = goal,
                            completed = goal.id in completedIds,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    // Spacer fillers for uneven last rows
                    val missing = layoutCols - row.size
                    repeat(missing) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Moves + Difficulty row (unchanged)
            if (movesRemaining >= 0) {
                Spacer(modifier = Modifier.height(if (isSmallScreen) 2.dp else 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 8.dp)
                ) {
                    val lowMoves = movesRemaining <= 3
                    Text(
                        text = "MOVES",
                        fontSize = if (isSmallScreen) 11.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "$movesRemaining",
                        fontFamily = DisplayFontFamily,
                        fontSize = if (isSmallScreen) 18.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (lowMoves) Color(0xFFC62828) else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "/$movesMax",
                        fontSize = if (isSmallScreen) 11.sp else 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (gameDifficulty != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
                        val diffColor = when (gameDifficulty) {
                            GameDifficulty.EASY -> if (isDark) Color(0xFF81C784) else Color(0xFF43A047)
                            GameDifficulty.MEDIUM -> if (isDark) Color(0xFF64B5F6) else Color(0xFF1E88E5)
                            GameDifficulty.HARD -> if (isDark) Color(0xFFFFB74D) else Color(0xFFEF6C00)
                            GameDifficulty.VERY_HARD -> if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                            GameDifficulty.EXTREMELY_HARD -> if (isDark) Color(0xFFCE93D8) else Color(0xFF6A1B9A)
                        }
                        Text(
                            text = gameDifficulty.label,
                            fontSize = if (isSmallScreen) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalBox(
    goal: Goal,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val cells = goal.cellPositions()
    val (shapeRows, shapeCols) = goal.gridBounds()
    val tileColor = goal.color
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val dimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val availW = size.width
            val availH = size.height
            val cellSize = minOf(availW / shapeCols, availH / shapeRows)
            val totalW = shapeCols * cellSize
            val totalH = shapeRows * cellSize
            val offsetX = (availW - totalW) / 2f
            val offsetY = (availH - totalH) / 2f
            val cornerR = cellSize * 0.18f

            for (cell in cells) {
                val x = offsetX + cell.col * cellSize
                val y = offsetY + cell.row * cellSize
                drawEmbossedTile(tileColor, x, y, cellSize, cornerR)
                if (cellSize > 20f) {
                    drawTileMotif(tileColor, x, y, cellSize)
                }
            }
        }

        // Completed overlay with white grid
        if (completed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(dimColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // White grid cells showing tile structure
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val availW = size.width
                    val availH = size.height
                    val cellSize = minOf(availW / shapeCols, availH / shapeRows)
                    val totalW = shapeCols * cellSize
                    val totalH = shapeRows * cellSize
                    val offsetX = (availW - totalW) / 2f
                    val offsetY = (availH - totalH) / 2f
                    val gap = (cellSize * 0.12f).coerceAtLeast(1.5f)
                    val cr = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.18f)

                    for (cell in cells) {
                        val x = offsetX + cell.col * cellSize + gap / 2f
                        val y = offsetY + cell.row * cellSize + gap / 2f
                        val s = cellSize - gap
                        // Filled white cell with high visibility
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(x, y),
                            size = Size(s, s),
                            cornerRadius = cr
                        )
                        // Bold white border
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(x, y),
                            size = Size(s, s),
                            cornerRadius = cr,
                            style = Stroke(width = (cellSize * 0.1f).coerceIn(2f, 5f))
                        )
                    }
                }
                Text(
                    text = "\u2714",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF43A047)
                )
            }
        }
    }
}
