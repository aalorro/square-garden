package com.squaregarden.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A cartoony, bas-relief styled avatar display.
 * Renders the emoji inside a raised, beveled circular medallion
 * with a subtle breathing animation for a lively feel.
 */
@Composable
fun BasReliefAvatar(
    emoji: String,
    size: Dp,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    imageBitmap: ImageBitmap? = null,
    masteryBadge: Boolean = false
) {
    // Gentle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animate) 1.045f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )

    val baseColor = MaterialTheme.colorScheme.primaryContainer
    val rimColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val px = this.size.width
            val center = Offset(px / 2f, px / 2f)
            val outerR = px / 2f

            // 1. Drop shadow (soft, offset down-right)
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = outerR * 0.94f,
                center = center + Offset(outerR * 0.045f, outerR * 0.07f)
            )

            // 2. Dark bevel rim (bottom-right)
            drawCircle(
                color = rimColor.copy(alpha = 0.85f).darken(0.35f),
                radius = outerR * 0.96f,
                center = center + Offset(outerR * 0.02f, outerR * 0.025f)
            )

            // 3. Light bevel rim (top-left)
            drawCircle(
                color = rimColor.copy(alpha = 0.9f).lighten(0.45f),
                radius = outerR * 0.96f,
                center = center - Offset(outerR * 0.02f, outerR * 0.025f)
            )

            // 4. Main medallion body
            drawCircle(
                color = baseColor,
                radius = outerR * 0.88f,
                center = center
            )

            // 5. Inner shadow ring (recessed edge)
            drawCircle(
                color = Color.Black.copy(alpha = 0.12f),
                radius = outerR * 0.88f,
                center = center,
                style = Stroke(width = outerR * 0.06f)
            )

            // 6. Inner highlight arc (top-left glow)
            drawArc(
                color = Color.White.copy(alpha = 0.35f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - outerR * 0.82f, center.y - outerR * 0.82f),
                size = androidx.compose.ui.geometry.Size(outerR * 1.64f, outerR * 1.64f),
                style = Stroke(width = outerR * 0.05f, cap = StrokeCap.Round)
            )

            // 7. Bottom-right shadow arc (depth)
            drawArc(
                color = Color.Black.copy(alpha = 0.18f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - outerR * 0.82f, center.y - outerR * 0.82f),
                size = androidx.compose.ui.geometry.Size(outerR * 1.64f, outerR * 1.64f),
                style = Stroke(width = outerR * 0.05f, cap = StrokeCap.Round)
            )

            // 8. Specular highlight dot (top-left shine)
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = outerR * 0.1f,
                center = center - Offset(outerR * 0.35f, outerR * 0.38f)
            )

            // 9. Cartoon outline ring
            drawCircle(
                color = rimColor.darken(0.5f).copy(alpha = 0.6f),
                radius = outerR * 0.96f,
                center = center,
                style = Stroke(width = outerR * 0.04f)
            )

            // 10. Mastery golden ring + crown + colored dots
            if (masteryBadge) {
                val gold = Color(0xFFD4A017)
                val goldLight = Color(0xFFFFD700)
                // Golden ring
                drawCircle(
                    color = gold,
                    radius = outerR * 0.99f,
                    center = center,
                    style = Stroke(width = outerR * 0.06f)
                )
                // Inner gold shimmer
                drawCircle(
                    color = goldLight.copy(alpha = 0.5f),
                    radius = outerR * 0.97f,
                    center = center,
                    style = Stroke(width = outerR * 0.02f)
                )
                // 6 colored dots around the ring (one per TileColor)
                val dotColors = listOf(
                    Color(0xFFE74C3C), // red
                    Color(0xFF3498DB), // blue
                    Color(0xFFF1C40F), // yellow
                    Color(0xFF27AE60), // green
                    Color(0xFFE67E22), // orange
                    Color(0xFF8E44AD)  // violet
                )
                val dotR = outerR * 0.055f
                val ringR = outerR * 0.99f
                dotColors.forEachIndexed { i, dotColor ->
                    val angle = Math.toRadians((i * 60.0 + 30.0))
                    val dx = (kotlin.math.cos(angle) * ringR).toFloat()
                    val dy = (kotlin.math.sin(angle) * ringR).toFloat()
                    drawCircle(color = dotColor, radius = dotR, center = center + Offset(dx, dy))
                }
                // Crown motif: 3 small gold triangles at top
                val crownY = center.y - outerR * 1.02f
                for (tri in -1..1) {
                    val cx = center.x + tri * outerR * 0.16f
                    val triSize = outerR * 0.1f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, crownY - triSize)
                        lineTo(cx - triSize * 0.6f, crownY + triSize * 0.3f)
                        lineTo(cx + triSize * 0.6f, crownY + triSize * 0.3f)
                        close()
                    }
                    drawPath(path, gold)
                }
            }
        }

        if (imageBitmap != null) {
            // Custom avatar image clipped to circle inside medallion
            Image(
                bitmap = imageBitmap,
                contentDescription = "Custom avatar",
                modifier = Modifier
                    .size(size * 0.76f)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Emoji with cartoon shadow for depth
            val emojiSize = (size.value * 0.48f).sp
            Text(
                text = emoji,
                style = TextStyle(
                    fontSize = emojiSize,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(1.5f, 2.5f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/** Darken a color by mixing toward black. */
private fun Color.darken(factor: Float): Color {
    return Color(
        red = red * (1f - factor),
        green = green * (1f - factor),
        blue = blue * (1f - factor),
        alpha = alpha
    )
}

/** Lighten a color by mixing toward white. */
private fun Color.lighten(factor: Float): Color {
    return Color(
        red = red + (1f - red) * factor,
        green = green + (1f - green) * factor,
        blue = blue + (1f - blue) * factor,
        alpha = alpha
    )
}
