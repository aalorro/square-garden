package com.squaregarden.ui.components

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a print-quality 2048x2048 mastery badge using native Android Canvas.
 * Designed to look ornate enough to print and frame.
 */
object MasteryBadgeRenderer {

    private val gold = Color.rgb(212, 160, 23)
    private val goldLight = Color.rgb(255, 215, 0)
    private val goldDark = Color.rgb(160, 120, 10)
    private val cream = Color.rgb(250, 245, 235)
    private val darkSage = Color.rgb(95, 138, 95)
    private val warmBrown = Color.rgb(139, 115, 85)

    // Tile colors matching the game
    private val tileColors = listOf(
        Color.rgb(231, 76, 60),   // red
        Color.rgb(52, 152, 219),  // blue
        Color.rgb(241, 196, 15),  // yellow
        Color.rgb(39, 174, 96),   // green
        Color.rgb(230, 126, 34),  // orange
        Color.rgb(142, 68, 173)   // violet
    )

    fun render(
        playerName: String,
        difficultyLabel: String,
        totalStars: Int,
        perfectGames: Int,
        dateString: String
    ): Bitmap {
        val size = 2048
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        // Background - parchment
        canvas.drawColor(cream)

        // Outer gold border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            style = Paint.Style.STROKE
            strokeWidth = 40f
        }
        canvas.drawRect(60f, 60f, size - 60f, size - 60f, borderPaint)

        // Inner border
        borderPaint.strokeWidth = 8f
        borderPaint.color = goldDark
        canvas.drawRect(100f, 100f, size - 100f, size - 100f, borderPaint)

        // Second inner border
        borderPaint.strokeWidth = 3f
        borderPaint.color = goldLight
        canvas.drawRect(115f, 115f, size - 115f, size - 115f, borderPaint)

        // Decorative embossed tiles around the perimeter (24 total, 4 per color)
        drawPerimeterTiles(canvas, size)

        // Corner flourishes
        drawCornerFlourish(canvas, 140f, 140f, 1f, 1f)
        drawCornerFlourish(canvas, size - 140f, 140f, -1f, 1f)
        drawCornerFlourish(canvas, 140f, size - 140f, 1f, -1f)
        drawCornerFlourish(canvas, size - 140f, size - 140f, -1f, -1f)

        // Decorative divider above title
        drawDivider(canvas, cy - 380f, size)

        // "MASTER OF THE GARDENS" title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 96f
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, Color.argb(80, 0, 0, 0))
        }
        canvas.drawText("MASTER OF THE", cx, cy - 280f, titlePaint)
        canvas.drawText("GARDENS", cx, cy - 170f, titlePaint)

        // "Square Garden" subtitle
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkSage
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textSize = 56f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Square Garden", cx, cy - 90f, subtitlePaint)

        // Decorative divider
        drawDivider(canvas, cy - 50f, size)

        // Player name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 50, 50)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(playerName, cx, cy + 50f, namePaint)

        // "Completed on [difficulty]"
        val diffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warmBrown
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Completed on $difficultyLabel", cx, cy + 120f, diffPaint)

        // Stars row
        drawStarsRow(canvas, cx, cy + 200f, totalStars)

        // Perfect games
        if (perfectGames > 0) {
            val perfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goldDark
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                textSize = 40f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("$perfectGames Perfect Games", cx, cy + 280f, perfPaint)
        }

        // Decorative divider
        drawDivider(canvas, cy + 320f, size)

        // Date
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warmBrown
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateString, cx, cy + 400f, datePaint)

        return bitmap
    }

    private fun drawPerimeterTiles(canvas: Canvas, size: Int) {
        val tileSize = 60f
        val margin = 70f
        val tilesPerSide = 6

        // Top edge
        val topSpacing = (size - 2 * margin - tileSize) / (tilesPerSide - 1)
        for (i in 0 until tilesPerSide) {
            drawMiniTile(canvas, margin + i * topSpacing, margin - tileSize / 2, tileSize, tileColors[i % tileColors.size])
        }
        // Bottom edge
        for (i in 0 until tilesPerSide) {
            drawMiniTile(canvas, margin + i * topSpacing, size - margin - tileSize / 2, tileSize, tileColors[i % tileColors.size])
        }
        // Left edge
        val sideSpacing = (size - 2 * margin - tileSize) / (tilesPerSide - 1)
        for (i in 0 until tilesPerSide) {
            drawMiniTile(canvas, margin - tileSize / 2, margin + i * sideSpacing, tileSize, tileColors[i % tileColors.size])
        }
        // Right edge
        for (i in 0 until tilesPerSide) {
            drawMiniTile(canvas, size - margin - tileSize / 2, margin + i * sideSpacing, tileSize, tileColors[i % tileColors.size])
        }
    }

    private fun drawMiniTile(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val r = size * 0.15f
        val lightColor = lightenColor(color, 0.4f)
        val darkColor = darkenColor(color, 0.35f)

        // Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(60, 0, 0, 0)
        }
        canvas.drawRoundRect(x + 3f, y + 4f, x + size, y + size, r, r, shadowPaint)

        // Dark bevel
        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = darkColor }
        canvas.drawRoundRect(x + 2f, y + 3f, x + size, y + size, r, r, darkPaint)

        // Light bevel
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = lightColor }
        canvas.drawRoundRect(x, y, x + size - 2f, y + size - 2f, r, r, lightPaint)

        // Main body
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val inset = size * 0.12f
        canvas.drawRoundRect(x + inset, y + inset, x + size - inset, y + size - inset, r * 0.8f, r * 0.8f, mainPaint)

        // Sheen
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(x + inset * 1.5f, y + inset, x + size - inset * 1.5f, y + size * 0.4f, r * 0.6f, r * 0.6f, sheenPaint)
    }

    private fun drawCornerFlourish(canvas: Canvas, x: Float, y: Float, sx: Float, sy: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkSage
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        // Simple leaf/vine flourish
        val path = Path().apply {
            moveTo(x, y)
            cubicTo(x + sx * 60, y + sy * 10, x + sx * 40, y + sy * 50, x + sx * 80, y + sy * 70)
            moveTo(x, y)
            cubicTo(x + sx * 10, y + sy * 60, x + sx * 50, y + sy * 40, x + sx * 70, y + sy * 80)
            // Small leaf
            moveTo(x + sx * 40, y + sy * 30)
            cubicTo(x + sx * 55, y + sy * 20, x + sx * 55, y + sy * 40, x + sx * 40, y + sy * 30)
        }
        canvas.drawPath(path, paint)

        // Fill leaf
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 95, 138, 95)
        val leafPath = Path().apply {
            moveTo(x + sx * 40, y + sy * 30)
            cubicTo(x + sx * 55, y + sy * 20, x + sx * 55, y + sy * 40, x + sx * 40, y + sy * 30)
        }
        canvas.drawPath(leafPath, paint)
    }

    private fun drawDivider(canvas: Canvas, y: Float, size: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            strokeWidth = 2f
        }
        val cx = size / 2f
        val halfWidth = 300f

        // Center line
        canvas.drawLine(cx - halfWidth, y, cx + halfWidth, y, paint)

        // Diamond ornament in center
        val diamondSize = 8f
        val diamondPath = Path().apply {
            moveTo(cx, y - diamondSize)
            lineTo(cx + diamondSize, y)
            lineTo(cx, y + diamondSize)
            lineTo(cx - diamondSize, y)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(diamondPath, paint)

        // Small dots at ends
        canvas.drawCircle(cx - halfWidth, y, 4f, paint)
        canvas.drawCircle(cx + halfWidth, y, 4f, paint)
    }

    private fun drawStarsRow(canvas: Canvas, cx: Float, y: Float, count: Int) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldLight
            textSize = 52f
            textAlign = Paint.Align.CENTER
        }
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 52f
            textAlign = Paint.Align.CENTER
        }

        // Draw 5 star icons + count
        val starStr = "\u2605".repeat(5)
        canvas.drawText(starStr, cx, y, starPaint)
        canvas.drawText("$count Stars", cx, y + 60f, countPaint)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = Color.red(color) + ((255 - Color.red(color)) * factor).toInt()
        val g = Color.green(color) + ((255 - Color.green(color)) * factor).toInt()
        val b = Color.blue(color) + ((255 - Color.blue(color)) * factor).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt()
        val g = (Color.green(color) * (1f - factor)).toInt()
        val b = (Color.blue(color) * (1f - factor)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}
