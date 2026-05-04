package com.patterngarden.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.patterngarden.model.Board
import com.patterngarden.model.CellPos
import com.patterngarden.model.SwapAnimation
import com.patterngarden.model.TileColor
import com.patterngarden.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

fun TileColor.toComposeColor(): Color = when (this) {
    TileColor.RED -> TileRed
    TileColor.BLUE -> TileBlue
    TileColor.YELLOW -> TileYellow
    TileColor.GREEN -> TileGreen
    TileColor.ORANGE -> TileOrange
}

fun TileColor.toLightColor(): Color = when (this) {
    TileColor.RED -> TileRedLight
    TileColor.BLUE -> TileBlueLight
    TileColor.YELLOW -> TileYellowLight
    TileColor.GREEN -> TileGreenLight
    TileColor.ORANGE -> TileOrangeLight
}

fun TileColor.toDarkColor(): Color = when (this) {
    TileColor.RED -> TileRedDark
    TileColor.BLUE -> TileBlueDark
    TileColor.YELLOW -> TileYellowDark
    TileColor.GREEN -> TileGreenDark
    TileColor.ORANGE -> TileOrangeDark
}

@Composable
fun GameBoardCanvas(
    board: Board,
    selectedCell: CellPos?,
    hintCells: Set<CellPos>,
    swapAnim: SwapAnimation?,
    completedGoalCells: Set<CellPos>,
    onDragSwap: (from: CellPos, to: CellPos) -> Unit,
    modifier: Modifier = Modifier
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }

    // Track drag state: which cell the drag started in, cumulative drag offset
    var dragStartCell by remember { mutableStateOf<CellPos?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragCommitted by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .aspectRatio(board.width.toFloat() / board.height)
            .pointerInput(board.width, board.height) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (cellSizePx > 0f) {
                            val col = (offset.x / cellSizePx).toInt().coerceIn(0, board.width - 1)
                            val row = (offset.y / cellSizePx).toInt().coerceIn(0, board.height - 1)
                            // Don't start drag on void or frozen cells
                            if (!board.isVoid(row, col) && !board.tileAt(row, col).frozen) {
                                dragStartCell = CellPos(row, col)
                                dragOffset = Offset.Zero
                                dragCommitted = false
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!dragCommitted) {
                            dragOffset += dragAmount
                            val cs = cellSizePx
                            // If drag exceeds half a cell in one direction, commit the swap
                            val threshold = cs * 0.35f
                            val start = dragStartCell ?: return@detectDragGestures
                            val target = when {
                                dragOffset.x > threshold && start.col < board.width - 1 ->
                                    CellPos(start.row, start.col + 1)
                                dragOffset.x < -threshold && start.col > 0 ->
                                    CellPos(start.row, start.col - 1)
                                dragOffset.y > threshold && start.row < board.height - 1 ->
                                    CellPos(start.row + 1, start.col)
                                dragOffset.y < -threshold && start.row > 0 ->
                                    CellPos(start.row - 1, start.col)
                                else -> null
                            }
                            if (target != null) {
                                dragCommitted = true
                                dragOffset = Offset.Zero
                                dragStartCell = null
                                onDragSwap(start, target)
                            }
                        }
                    },
                    onDragEnd = {
                        dragStartCell = null
                        dragOffset = Offset.Zero
                        dragCommitted = false
                    },
                    onDragCancel = {
                        dragStartCell = null
                        dragOffset = Offset.Zero
                        dragCommitted = false
                    }
                )
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
                    drawRoundRect(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
                        topLeft = Offset(c * cs + tileInset, r * cs + tileInset),
                        size = Size(cs - tileInset * 2, cs - tileInset * 2),
                        cornerRadius = CornerRadius(cornerR * 0.9f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Goal completion border — bright green with glow
                if (pos in completedGoalCells) {
                    val tileInset = cs * 0.05f
                    // Soft green glow behind the border
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

private fun DrawScope.drawEmbossedTile(
    color: TileColor,
    x: Float,
    y: Float,
    cs: Float,
    cornerR: Float
) {
    val tileInset = cs * 0.08f
    val embossWidth = cs * 0.045f
    val tileCorner = cornerR * 0.9f
    val tileX = x + tileInset
    val tileY = y + tileInset
    val tileW = cs - tileInset * 2
    val tileH = cs - tileInset * 2

    // Shadow edge (bottom-right) — drawn first, slightly offset down-right
    drawRoundRect(
        color = color.toDarkColor().copy(alpha = 0.55f),
        topLeft = Offset(tileX + embossWidth * 0.3f, tileY + embossWidth * 0.6f),
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(tileCorner)
    )

    // Main tile body
    drawRoundRect(
        color = color.toComposeColor(),
        topLeft = Offset(tileX, tileY),
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(tileCorner)
    )

    // Highlight edge (top-left inner bevel)
    drawRoundRect(
        color = color.toLightColor().copy(alpha = 0.6f),
        topLeft = Offset(tileX, tileY),
        size = Size(tileW - embossWidth, tileH - embossWidth),
        cornerRadius = CornerRadius(tileCorner)
    )

    // Re-draw center to clean the bevel — slightly inset from highlight
    drawRoundRect(
        color = color.toComposeColor(),
        topLeft = Offset(tileX + embossWidth, tileY + embossWidth),
        size = Size(tileW - embossWidth * 2, tileH - embossWidth * 2),
        cornerRadius = CornerRadius(tileCorner * 0.8f)
    )

    // Subtle inner glow / sheen at top
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(tileX + embossWidth * 1.5f, tileY + embossWidth),
        size = Size(tileW - embossWidth * 3, tileH * 0.35f),
        cornerRadius = CornerRadius(tileCorner * 0.7f)
    )
}

private fun DrawScope.drawTileMotif(color: TileColor, x: Float, y: Float, cs: Float) {
    val cx = x + cs / 2
    val cy = y + cs / 2
    val motifColor = color.toDarkColor().copy(alpha = 0.25f)
    val motifSize = cs * 0.16f

    when (color) {
        TileColor.RED -> {
            // Flower: small circle center + petals
            drawCircle(color = motifColor, radius = motifSize * 0.4f, center = Offset(cx, cy))
            for (i in 0 until 6) {
                val angle = Math.toRadians(i * 60.0)
                val px = cx + (motifSize * Math.cos(angle)).toFloat()
                val py = cy + (motifSize * Math.sin(angle)).toFloat()
                drawCircle(color = motifColor, radius = motifSize * 0.35f, center = Offset(px, py))
            }
        }
        TileColor.BLUE -> {
            // Water droplet
            val path = Path().apply {
                moveTo(cx, cy - motifSize * 1.2f)
                cubicTo(
                    cx + motifSize, cy - motifSize * 0.2f,
                    cx + motifSize * 0.8f, cy + motifSize,
                    cx, cy + motifSize * 1.1f
                )
                cubicTo(
                    cx - motifSize * 0.8f, cy + motifSize,
                    cx - motifSize, cy - motifSize * 0.2f,
                    cx, cy - motifSize * 1.2f
                )
                close()
            }
            drawPath(path, color = motifColor)
        }
        TileColor.YELLOW -> {
            // Sun with rays
            drawCircle(color = motifColor, radius = motifSize * 0.5f, center = Offset(cx, cy))
            for (i in 0 until 8) {
                val angle = Math.toRadians(i * 45.0)
                val sx = cx + (motifSize * 0.7f * Math.cos(angle)).toFloat()
                val sy = cy + (motifSize * 0.7f * Math.sin(angle)).toFloat()
                val ex = cx + (motifSize * 1.1f * Math.cos(angle)).toFloat()
                val ey = cy + (motifSize * 1.1f * Math.sin(angle)).toFloat()
                drawLine(color = motifColor, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = 2f)
            }
        }
        TileColor.GREEN -> {
            // Leaf shape
            val path = Path().apply {
                moveTo(cx - motifSize, cy)
                quadraticTo(cx, cy - motifSize * 1.3f, cx + motifSize, cy)
                quadraticTo(cx, cy + motifSize * 1.3f, cx - motifSize, cy)
                close()
            }
            drawPath(path, color = motifColor)
            // Leaf vein
            drawLine(
                color = motifColor.copy(alpha = 0.5f),
                start = Offset(cx - motifSize * 0.6f, cy),
                end = Offset(cx + motifSize * 0.6f, cy),
                strokeWidth = 1.5f
            )
        }
        TileColor.ORANGE -> {
            // Citrus slice: circle with segment lines
            drawCircle(color = motifColor, radius = motifSize * 0.9f, center = Offset(cx, cy))
            for (i in 0 until 6) {
                val angle = Math.toRadians(i * 60.0 + 30.0)
                val ex = cx + (motifSize * 0.85f * Math.cos(angle)).toFloat()
                val ey = cy + (motifSize * 0.85f * Math.sin(angle)).toFloat()
                drawLine(motifColor, Offset(cx, cy), Offset(ex, ey), strokeWidth = 1.5f)
            }
            drawCircle(color = motifColor.copy(alpha = 0.4f), radius = motifSize * 0.3f, center = Offset(cx, cy))
        }
    }
}
