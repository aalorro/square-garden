package com.squaregarden.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.squaregarden.model.Board
import com.squaregarden.model.CellPos
import com.squaregarden.model.SwapAnimation
import com.squaregarden.model.TileColor
import com.squaregarden.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameBoardCanvas(
    board: Board,
    selectedCell: CellPos?,
    hintCells: Set<CellPos>,
    swapAnim: SwapAnimation?,
    completedGoalCells: Set<CellPos>,
    passthroughActive: Boolean = false,
    onDragSwap: (from: CellPos, to: CellPos) -> Unit,
    onCellTapped: ((row: Int, col: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }

    // Use rememberUpdatedState so the gesture coroutine always sees the latest
    // callbacks and board without restarting pointerInput on every recomposition
    val currentBoard by rememberUpdatedState(board)
    val currentOnDragSwap by rememberUpdatedState(onDragSwap)
    val currentOnCellTapped by rememberUpdatedState(onCellTapped)

    // Track drag state: which cell the drag started in, cumulative drag offset
    var dragStartCell by remember { mutableStateOf<CellPos?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragCommitted by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .aspectRatio(board.width.toFloat() / board.height)
            .pointerInput(board.width, board.height) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (cellSizePx <= 0f) return@awaitEachGesture
                    val b = currentBoard
                    val startCol = (down.position.x / cellSizePx).toInt().coerceIn(0, b.width - 1)
                    val startRow = (down.position.y / cellSizePx).toInt().coerceIn(0, b.height - 1)
                    val startCell = CellPos(startRow, startCol)
                    val canDrag = !b.isVoid(startRow, startCol) && !b.tileAt(startRow, startCol).frozen
                    var totalDrag = Offset.Zero
                    var committed = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            // Finger lifted — if no drag committed, it's a tap
                            if (!committed) {
                                currentOnCellTapped?.invoke(startRow, startCol)
                            }
                            break
                        }
                        if (committed || !canDrag) continue
                        val delta = change.position - change.previousPosition
                        totalDrag += delta
                        change.consume()
                        val threshold = cellSizePx * 0.35f
                        val target = when {
                            totalDrag.x > threshold && startCol < b.width - 1 ->
                                CellPos(startRow, startCol + 1)
                            totalDrag.x < -threshold && startCol > 0 ->
                                CellPos(startRow, startCol - 1)
                            totalDrag.y > threshold && startRow < b.height - 1 ->
                                CellPos(startRow + 1, startCol)
                            totalDrag.y < -threshold && startRow > 0 ->
                                CellPos(startRow - 1, startCol)
                            else -> null
                        }
                        if (target != null) {
                            committed = true
                            currentOnDragSwap(startCell, target)
                        }
                    }
                    dragStartCell = null
                    dragOffset = Offset.Zero
                    dragCommitted = false
                }
            }
    ) {
        val cs = size.width / board.width
        cellSizePx = cs
        val padding = cs * 0.06f
        val cornerR = cs * 0.18f

        // Draw board background
        drawRoundRect(
            color = Color(0xFFE8E0D4),
            topLeft = Offset(-padding, -padding),
            size = Size(size.width + padding * 2, size.height + padding * 2),
            cornerRadius = CornerRadius(cornerR * 1.5f)
        )

        // Draw cell backgrounds first (so animated tiles slide over them)
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if (board.isVoid(r, c)) continue
                val x = c * cs
                val y = r * cs
                val inset = cs * 0.05f
                drawRoundRect(
                    color = Color(0xFFF5F0E8),
                    topLeft = Offset(x + inset, y + inset),
                    size = Size(cs - inset * 2, cs - inset * 2),
                    cornerRadius = CornerRadius(cornerR)
                )
            }
        }

        // Determine animated tile positions
        val animFrom = swapAnim?.from
        val animTo = swapAnim?.to
        val animProgress = swapAnim?.progress ?: 0f

        // Draw tiles (non-animated first, then animated on top)
        // First pass: non-animated tiles
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                val pos = CellPos(r, c)
                if (board.isVoid(r, c)) continue
                if (pos == animFrom || pos == animTo) continue // drawn later

                val tile = board.tileAt(r, c)
                var x = c * cs
                var y = r * cs

                // Apply live drag offset to the dragged tile
                if (pos == dragStartCell && !dragCommitted) {
                    x += dragOffset.x.coerceIn(-cs, cs)
                    y += dragOffset.y.coerceIn(-cs, cs)
                }

                drawEmbossedTile(tile.color, x, y, cs, cornerR)
                drawTileMotif(tile.color, x, y, cs)

                // Frozen overlay: light frost + diagonal lines + thick ice border
                // Base tile color stays clearly visible
                if (tile.frozen) {
                    val tileInset = cs * 0.08f
                    val tx = x + tileInset
                    val ty = y + tileInset
                    val tw = cs - tileInset * 2
                    val th = cs - tileInset * 2
                    // Light frost wash — subtle so tile color stays vivid
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.20f),
                        topLeft = Offset(tx, ty),
                        size = Size(tw, th),
                        cornerRadius = CornerRadius(cornerR * 0.9f)
                    )
                    // Diagonal frost lines (subtle ice cracks)
                    val lineColor = Color.White.copy(alpha = 0.55f)
                    val lineWidth = 1.5f
                    val step = cs * 0.22f
                    for (i in 1..3) {
                        val offset = step * i
                        drawLine(lineColor, Offset(tx + offset, ty), Offset(tx, ty + offset), strokeWidth = lineWidth)
                        drawLine(lineColor, Offset(tx + tw - offset, ty + th), Offset(tx + tw, ty + th - offset), strokeWidth = lineWidth)
                    }
                    // Small frost shine at top corner
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.30f),
                        topLeft = Offset(tx + tileInset, ty),
                        size = Size(tw * 0.4f, th * 0.18f),
                        cornerRadius = CornerRadius(cornerR * 0.5f)
                    )
                    // Thick ice border — primary frozen indicator
                    drawRoundRect(
                        color = Color(0xFF0288D1).copy(alpha = 0.85f),
                        topLeft = Offset(tx, ty),
                        size = Size(tw, th),
                        cornerRadius = CornerRadius(cornerR * 0.9f),
                        style = Stroke(width = 3.5f.dp.toPx())
                    )
                }

                // Selection highlight
                if (selectedCell == pos) {
                    val tileInset = cs * 0.08f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                        size = Size(cs - tileInset * 2, cs - tileInset * 2),
                        cornerRadius = CornerRadius(cornerR * 0.9f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Hint highlight
                if (pos in hintCells) {
                    val tileInset = cs * 0.08f
                    // Filled yellow glow
                    drawRoundRect(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.35f),
                        topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                        size = Size(cs - tileInset * 2, cs - tileInset * 2),
                        cornerRadius = CornerRadius(cornerR * 0.9f)
                    )
                    // Bold yellow border
                    drawRoundRect(
                        color = Color(0xFFFFD600),
                        topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                        size = Size(cs - tileInset * 2, cs - tileInset * 2),
                        cornerRadius = CornerRadius(cornerR * 0.9f),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Goal completion border — green normally, cyan shield when passthrough active
                if (pos in completedGoalCells) {
                    val tileInset = cs * 0.05f
                    if (passthroughActive) {
                        // Shield glow — cyan/teal to indicate passthrough protection
                        drawRoundRect(
                            color = Color(0xFF00ACC1).copy(alpha = 0.35f),
                            topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                            size = Size(cs - tileInset * 2, cs - tileInset * 2),
                            cornerRadius = CornerRadius(cornerR * 0.95f)
                        )
                        // Bold cyan shield border
                        drawRoundRect(
                            color = Color(0xFF00838F),
                            topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                            size = Size(cs - tileInset * 2, cs - tileInset * 2),
                            cornerRadius = CornerRadius(cornerR * 0.95f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    } else {
                        // Normal green glow
                        drawRoundRect(
                            color = Color(0xFF43A047).copy(alpha = 0.25f),
                            topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                            size = Size(cs - tileInset * 2, cs - tileInset * 2),
                            cornerRadius = CornerRadius(cornerR * 0.95f)
                        )
                        // Bold green border
                        drawRoundRect(
                            color = Color(0xFF2E7D32),
                            topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                            size = Size(cs - tileInset * 2, cs - tileInset * 2),
                            cornerRadius = CornerRadius(cornerR * 0.95f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // Second pass: animated tiles (drawn on top for proper layering)
        if (animFrom != null && animTo != null) {
            val dx = (animTo.col - animFrom.col) * cs * animProgress
            val dy = (animTo.row - animFrom.row) * cs * animProgress

            // "from" tile slides toward "to" position
            val fromTile = board.tileAt(animFrom.row, animFrom.col)
            val fromX = animFrom.col * cs + dx
            val fromY = animFrom.row * cs + dy
            drawEmbossedTile(fromTile.color, fromX, fromY, cs, cornerR)
            drawTileMotif(fromTile.color, fromX, fromY, cs)

            // "to" tile slides toward "from" position
            val toTile = board.tileAt(animTo.row, animTo.col)
            val toX = animTo.col * cs - dx
            val toY = animTo.row * cs - dy
            drawEmbossedTile(toTile.color, toX, toY, cs, cornerR)
            drawTileMotif(toTile.color, toX, toY, cs)
        }
    }
}
