package com.squaregarden.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private data class TokenParticle(
    val startX: Float,
    val startY: Float,
    val controlOffsetX: Float,
    val controlOffsetY: Float,
    val delay: Float,
    val size: Float,
    val rotSpeed: Float,
    val trailLength: Int
)

/**
 * Dramatic cascading trail effect for captured power-up tokens.
 * Particles burst from center-screen and arc down to the target
 * power-up icon position at the bottom bar, with glowing trails.
 */
@Composable
fun TokenTrailOverlay(
    icon: String,
    accentColor: Color,
    targetXFraction: Float = 0.5f,
    onComplete: () -> Unit,
    onLanded: () -> Unit = {}
) {
    val totalParticles = 6
    val durationMs = 2200

    val particles = remember(icon) {
        List(totalParticles) { i ->
            TokenParticle(
                startX = 0.30f + Random.nextFloat() * 0.40f,
                startY = 0.25f + Random.nextFloat() * 0.15f,
                controlOffsetX = -0.30f + Random.nextFloat() * 0.60f,
                controlOffsetY = -0.15f + Random.nextFloat() * 0.20f,
                delay = i.toFloat() / totalParticles * 0.45f,
                size = 9f + Random.nextFloat() * 7f,
                rotSpeed = 200f + Random.nextFloat() * 300f,
                trailLength = 5 + Random.nextInt(4)
            )
        }
    }

    val progress = remember { Animatable(0f) }
    val landed = remember { mutableIntStateOf(0) }

    // Main flight animation
    LaunchedEffect(icon) {
        landed.intValue = 0
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMs, easing = LinearEasing))
        delay(200)
        onComplete()
    }

    // Track landings for sound callback
    LaunchedEffect(icon) {
        while (true) {
            delay(80)
            val t = progress.value
            var newLanded = 0
            for (p in particles) {
                val localT = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (localT >= 0.95f) newLanded++
            }
            val prev = landed.intValue
            if (newLanded > prev) {
                onLanded()
                landed.intValue = newLanded
            }
            if (t >= 1f) break
        }
    }

    val t = progress.value
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Target: the power-up icon at the bottom bar
        val tx = w * targetXFraction
        val ty = h * 0.92f

        // Draw particles
        particles.forEach { p ->
            val localT = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (localT <= 0f) return@forEach

            val eased = 1f - (1f - localT).pow(2.5f)

            val sx = p.startX * w
            val sy = p.startY * h
            val cx = (sx + tx) / 2f + p.controlOffsetX * w
            val cy = (sy + ty) / 2f + p.controlOffsetY * h

            // Sparkle trail
            for (trail in p.trailLength downTo 1) {
                val trailT = (eased - trail * 0.035f).coerceIn(0f, 1f)
                val omt = 1f - trailT
                val trailX = omt * omt * sx + 2f * omt * trailT * cx + trailT * trailT * tx
                val trailY = omt * omt * sy + 2f * omt * trailT * cy + trailT * trailT * ty
                val trailAlpha = (0.5f - trail * 0.06f).coerceIn(0.05f, 0.5f)
                val trailSize = p.size * (0.3f + (1f - trail.toFloat() / p.trailLength) * 0.5f)
                drawCircle(
                    color = accentColor.copy(alpha = trailAlpha),
                    radius = trailSize,
                    center = Offset(trailX, trailY)
                )
            }

            // Main particle position
            val oneMinusT = 1f - eased
            val posX = oneMinusT * oneMinusT * sx + 2f * oneMinusT * eased * cx + eased * eased * tx
            val posY = oneMinusT * oneMinusT * sy + 2f * oneMinusT * eased * cy + eased * eased * ty

            // Scale: burst big, shrink at target
            val scale = when {
                eased < 0.1f -> 0.5f + eased * 5f
                eased > 0.85f -> 1f - (eased - 0.85f) * 4f
                else -> 1f
            }.coerceIn(0.2f, 1.5f)

            val alpha = if (eased > 0.9f) ((1f - eased) / 0.1f).coerceIn(0f, 1f) else 1f

            // Outer glow
            drawCircle(
                color = accentColor.copy(alpha = alpha * 0.3f),
                radius = p.size * scale * 3f,
                center = Offset(posX, posY)
            )
            // Mid glow
            drawCircle(
                color = accentColor.copy(alpha = alpha * 0.5f),
                radius = p.size * scale * 2f,
                center = Offset(posX, posY)
            )
            // Core bright circle
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = p.size * scale * 0.9f,
                center = Offset(posX, posY)
            )
            // Colored inner
            drawCircle(
                color = accentColor.copy(alpha = alpha * 0.9f),
                radius = p.size * scale * 0.65f,
                center = Offset(posX, posY)
            )
        }

        // Draw the main icon emoji at the lead particle position (first particle)
        if (t > 0f) {
            val lead = particles.first()
            val leadT = ((t - lead.delay) / (1f - lead.delay)).coerceIn(0f, 1f)
            if (leadT > 0f) {
                val eased = 1f - (1f - leadT).pow(2.5f)
                val sx = lead.startX * w
                val sy = lead.startY * h
                val cx2 = (sx + tx) / 2f + lead.controlOffsetX * w
                val cy2 = (sy + ty) / 2f + lead.controlOffsetY * h
                val oneMinusT2 = 1f - eased
                val posX = oneMinusT2 * oneMinusT2 * sx + 2f * oneMinusT2 * eased * cx2 + eased * eased * tx
                val posY = oneMinusT2 * oneMinusT2 * sy + 2f * oneMinusT2 * eased * cy2 + eased * eased * ty

                val iconScale = when {
                    eased < 0.1f -> 0.5f + eased * 5f
                    eased > 0.85f -> 1f - (eased - 0.85f) * 4f
                    else -> 1f
                }.coerceIn(0.2f, 1.5f)
                val iconAlpha = if (eased > 0.9f) ((1f - eased) / 0.1f).coerceIn(0f, 1f) else 1f

                val iconResult = textMeasurer.measure(
                    text = AnnotatedString(icon),
                    style = TextStyle(
                        fontSize = (22 * iconScale).sp,
                        color = Color.White.copy(alpha = iconAlpha)
                    )
                )
                drawText(
                    iconResult,
                    topLeft = Offset(
                        posX - iconResult.size.width / 2f,
                        posY - iconResult.size.height / 2f
                    )
                )
            }
        }
    }
}
