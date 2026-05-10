package com.squaregarden.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import kotlin.math.sin

data class WorldInfo(
    val id: Int,
    val name: String,
    val subtitle: String,
    val baseStarsToUnlock: Int,
    val color: Color
)

private val worlds = listOf(
    WorldInfo(1, "Seedling Garden", "Levels 1-9", 0, Sage),
    WorldInfo(2, "Blooming Meadow", "Levels 10-18", 8, TileBlue),
    WorldInfo(3, "Ancient Grove", "Levels 19-27", 20, WarmBrown),
    WorldInfo(4, "Crystal Cavern", "Levels 28-36", 35, Color(0xFF81D4FA)),
    WorldInfo(5, "Shattered Isles", "Levels 37-45", 55, Color(0xFFCE93D8)),
    WorldInfo(6, "Void Fortress", "Levels 46-54", 80, Color(0xFF78909C)),
    WorldInfo(7, "Molten Core", "Levels 55-63", 110, Color(0xFFFF6D00)),
    WorldInfo(8, "Starfall Summit", "Levels 64-72", 145, Color(0xFF7C4DFF)),
    WorldInfo(9, "Abyssal Depths", "Levels 73-81", 185, Color(0xFF00897B)),
    WorldInfo(10, "Prism Citadel", "Levels 82-90", 230, Color(0xFFE91E63)),
    // TODO: Challenge Lab — uncomment to re-enable for testing
    // WorldInfo(11, "Challenge Lab", "4 Challenge Modes", 0, Color(0xFFFF5722))
)

@Composable
fun WorldSelectScreen(navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
    val profile by profileRepo.profileFlow.collectAsState(initial = null)
    val difficulty = profile?.let { Difficulty.fromId(it.difficulty) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Worlds",
                    fontFamily = DisplayFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("\u2605", fontSize = 14.sp, color = Color(0xFFFFB800))
                    Text("$totalStars", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val startingWorld = difficulty?.startingWorld ?: 1
            val skillMultiplier = difficulty?.starMultiplier ?: 1
            worlds.forEach { world ->
                val starsToUnlock = world.baseStarsToUnlock * skillMultiplier
                val belowSkill = world.id < startingWorld
                val unlocked = !belowSkill && (totalStars >= starsToUnlock || world.id <= startingWorld)
                val accessible = unlocked

                Card(
                    onClick = {
                        if (accessible) {
                            navController.navigate(Screen.LevelSelect.create(world.id))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .alpha(if (accessible) 1f else 0.65f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Banner background illustration
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                        ) {
                            when (world.id) {
                                1 -> drawSeedlingGarden(size)
                                2 -> drawBloomingMeadow(size)
                                3 -> drawAncientGrove(size)
                                4 -> drawCrystalCavern(size)
                                5 -> drawShatteredIsles(size)
                                6 -> drawVoidFortress(size)
                                7 -> drawMoltenCore(size)
                                8 -> drawStarfallSummit(size)
                                9 -> drawAbyssalDepths(size)
                                10 -> drawPrismCitadel(size)
                                else -> drawRect(Brush.linearGradient(listOf(world.color, world.color.copy(alpha = 0.6f))))
                            }
                        }

                        // Dark scrim overlay (left-to-right gradient)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.55f),
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Text overlay
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WORLD ${world.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = world.name,
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = world.subtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (belowSkill) {
                                Text(
                                    text = "Below skill",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCDD2)
                                )
                            } else if (!unlocked) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("\uD83D\uDD12", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = "\u2605 $starsToUnlock needed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFE082),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

    }
}

// ── World 1: Seedling Garden — sunny hills with sprouts ──

private fun DrawScope.drawSeedlingGarden(sz: Size) {
    // Sky gradient
    drawRect(Brush.verticalGradient(listOf(Color(0xFF87CEEB), Color(0xFFB8E6B8))))
    // Sun
    drawCircle(Color(0xFFFFE082), radius = sz.height * 0.3f, center = Offset(sz.width * 0.85f, sz.height * 0.15f))
    drawCircle(Color(0xFFFFF59D), radius = sz.height * 0.22f, center = Offset(sz.width * 0.85f, sz.height * 0.15f))
    // Rolling hills
    val hill = Path().apply {
        moveTo(0f, sz.height * 0.65f)
        quadraticTo(sz.width * 0.25f, sz.height * 0.35f, sz.width * 0.5f, sz.height * 0.6f)
        quadraticTo(sz.width * 0.75f, sz.height * 0.45f, sz.width, sz.height * 0.55f)
        lineTo(sz.width, sz.height)
        lineTo(0f, sz.height)
        close()
    }
    drawPath(hill, Color(0xFF66BB6A))
    val hill2 = Path().apply {
        moveTo(0f, sz.height * 0.75f)
        quadraticTo(sz.width * 0.35f, sz.height * 0.55f, sz.width * 0.6f, sz.height * 0.72f)
        quadraticTo(sz.width * 0.85f, sz.height * 0.6f, sz.width, sz.height * 0.68f)
        lineTo(sz.width, sz.height)
        lineTo(0f, sz.height)
        close()
    }
    drawPath(hill2, Color(0xFF4CAF50))
    // Tiny sprouts
    val sproutColor = Color(0xFF2E7D32)
    for (xFrac in listOf(0.15f, 0.35f, 0.55f, 0.72f, 0.88f)) {
        val sx = sz.width * xFrac
        val sy = sz.height * 0.78f
        drawLine(sproutColor, Offset(sx, sy), Offset(sx, sy - sz.height * 0.1f), strokeWidth = 3f)
        drawCircle(Color(0xFF81C784), radius = sz.height * 0.04f, center = Offset(sx - sz.height * 0.03f, sy - sz.height * 0.12f))
        drawCircle(Color(0xFF81C784), radius = sz.height * 0.04f, center = Offset(sx + sz.height * 0.03f, sy - sz.height * 0.12f))
    }
    // Scrim for text readability
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.35f), Color.Transparent)), size = Size(sz.width * 0.65f, sz.height))
}

// ── World 2: Blooming Meadow — flowers and butterflies ──

private fun DrawScope.drawBloomingMeadow(sz: Size) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFFC8E6C9))))
    // Grass
    val grass = Path().apply {
        moveTo(0f, sz.height * 0.6f)
        quadraticTo(sz.width * 0.3f, sz.height * 0.5f, sz.width * 0.5f, sz.height * 0.58f)
        quadraticTo(sz.width * 0.7f, sz.height * 0.52f, sz.width, sz.height * 0.55f)
        lineTo(sz.width, sz.height)
        lineTo(0f, sz.height)
        close()
    }
    drawPath(grass, Color(0xFF66BB6A))
    drawPath(Path().apply {
        moveTo(0f, sz.height * 0.72f)
        lineTo(sz.width, sz.height * 0.65f)
        lineTo(sz.width, sz.height)
        lineTo(0f, sz.height)
        close()
    }, Color(0xFF4CAF50))
    // Flowers
    val flowerColors = listOf(Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFFAB47BC), Color(0xFFFF7043), Color(0xFFEC407A))
    val flowerX = listOf(0.2f, 0.38f, 0.55f, 0.7f, 0.85f)
    val flowerY = listOf(0.58f, 0.52f, 0.56f, 0.50f, 0.54f)
    for (i in flowerX.indices) {
        val fx = sz.width * flowerX[i]
        val fy = sz.height * flowerY[i]
        val r = sz.height * 0.055f
        // Stem
        drawLine(Color(0xFF388E3C), Offset(fx, fy), Offset(fx, fy + sz.height * 0.18f), strokeWidth = 3f)
        // Petals
        val col = flowerColors[i % flowerColors.size]
        for (a in 0 until 5) {
            val angle = Math.toRadians(a * 72.0)
            val px = fx + (r * 0.9f * Math.cos(angle)).toFloat()
            val py = fy + (r * 0.9f * Math.sin(angle)).toFloat()
            drawCircle(col, radius = r * 0.55f, center = Offset(px, py))
        }
        drawCircle(Color(0xFFFFF59D), radius = r * 0.35f, center = Offset(fx, fy))
    }
    // Small clouds
    drawCloud(sz.width * 0.15f, sz.height * 0.15f, sz.height * 0.08f)
    drawCloud(sz.width * 0.65f, sz.height * 0.1f, sz.height * 0.06f)
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)), size = Size(sz.width * 0.6f, sz.height))
}

// ── World 3: Ancient Grove — big trees, mystical ──

private fun DrawScope.drawAncientGrove(sz: Size) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF1B5E20))))
    // Misty layer
    drawRect(Color(0xFF81C784).copy(alpha = 0.15f))
    // Big trees
    fun drawTree(x: Float, trunkW: Float, canopyR: Float) {
        drawRoundRect(Color(0xFF5D4037), Offset(x - trunkW / 2, sz.height * 0.35f), Size(trunkW, sz.height * 0.65f), CornerRadius(trunkW * 0.3f))
        drawCircle(Color(0xFF2E7D32), radius = canopyR, center = Offset(x, sz.height * 0.35f))
        drawCircle(Color(0xFF388E3C), radius = canopyR * 0.75f, center = Offset(x - canopyR * 0.3f, sz.height * 0.28f))
        drawCircle(Color(0xFF43A047), radius = canopyR * 0.6f, center = Offset(x + canopyR * 0.35f, sz.height * 0.3f))
    }
    drawTree(sz.width * 0.78f, sz.width * 0.06f, sz.height * 0.28f)
    drawTree(sz.width * 0.92f, sz.width * 0.05f, sz.height * 0.22f)
    drawTree(sz.width * 0.65f, sz.width * 0.04f, sz.height * 0.2f)
    // Glowing particles
    val glowColor = Color(0xFFA5D6A7).copy(alpha = 0.6f)
    for (frac in listOf(0.6f, 0.7f, 0.75f, 0.82f, 0.88f)) {
        drawCircle(glowColor, radius = 3f, center = Offset(sz.width * frac, sz.height * (0.2f + frac * 0.3f)))
    }
    // Ground
    drawRect(Color(0xFF33691E), Offset(0f, sz.height * 0.82f), Size(sz.width, sz.height * 0.18f))
    // Vines
    val vineColor = Color(0xFF558B2F)
    drawLine(vineColor, Offset(sz.width * 0.72f, 0f), Offset(sz.width * 0.68f, sz.height * 0.4f), strokeWidth = 3f)
    drawLine(vineColor, Offset(sz.width * 0.85f, 0f), Offset(sz.width * 0.82f, sz.height * 0.35f), strokeWidth = 2.5f)
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent)), size = Size(sz.width * 0.6f, sz.height))
}

// ── World 4: Crystal Cavern — crystals and gems ──

private fun DrawScope.drawCrystalCavern(sz: Size) {
    // Dark cave background
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))
    // Cave ceiling stalactites
    val stalaColor = Color(0xFF37474F)
    for (frac in listOf(0.1f, 0.25f, 0.42f, 0.58f, 0.75f, 0.9f)) {
        val sx = sz.width * frac
        val h = sz.height * (0.15f + (frac * 17 % 1) * 0.15f)
        val path = Path().apply {
            moveTo(sx - sz.width * 0.03f, 0f)
            lineTo(sx, h)
            lineTo(sx + sz.width * 0.03f, 0f)
            close()
        }
        drawPath(path, stalaColor)
    }
    // Big crystals
    fun drawCrystal(cx: Float, baseY: Float, h: Float, w: Float, color: Color) {
        val path = Path().apply {
            moveTo(cx, baseY - h)
            lineTo(cx + w / 2, baseY)
            lineTo(cx - w / 2, baseY)
            close()
        }
        drawPath(path, color)
        // Shine strip
        val shine = Path().apply {
            moveTo(cx - w * 0.1f, baseY - h * 0.9f)
            lineTo(cx + w * 0.15f, baseY - h * 0.2f)
            lineTo(cx, baseY - h * 0.2f)
            lineTo(cx - w * 0.2f, baseY - h * 0.85f)
            close()
        }
        drawPath(shine, Color.White.copy(alpha = 0.25f))
    }
    val baseY = sz.height * 0.92f
    drawCrystal(sz.width * 0.7f, baseY, sz.height * 0.55f, sz.width * 0.1f, Color(0xFF4FC3F7))
    drawCrystal(sz.width * 0.8f, baseY, sz.height * 0.4f, sz.width * 0.08f, Color(0xFF81D4FA))
    drawCrystal(sz.width * 0.62f, baseY, sz.height * 0.35f, sz.width * 0.07f, Color(0xFFB3E5FC))
    drawCrystal(sz.width * 0.88f, baseY, sz.height * 0.45f, sz.width * 0.09f, Color(0xFF29B6F6))
    // Sparkles
    val sparkle = Color(0xFFE1F5FE)
    for ((sx, sy) in listOf(0.65f to 0.3f, 0.75f to 0.2f, 0.85f to 0.35f, 0.58f to 0.45f, 0.92f to 0.25f)) {
        drawCircle(sparkle, radius = 2.5f, center = Offset(sz.width * sx, sz.height * sy))
    }
    // Cave floor
    drawRect(Color(0xFF263238), Offset(0f, sz.height * 0.88f), Size(sz.width, sz.height * 0.12f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.35f), Color.Transparent)), size = Size(sz.width * 0.6f, sz.height))
}

// ── World 5: Shattered Isles — floating islands with clouds ──

private fun DrawScope.drawShatteredIsles(sz: Size) {
    // Purple-orange sunset sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFE65100), Color(0xFFFF8F00))))
    // Distant clouds
    drawCloud(sz.width * 0.1f, sz.height * 0.7f, sz.height * 0.06f, Color.White.copy(0.3f))
    drawCloud(sz.width * 0.5f, sz.height * 0.8f, sz.height * 0.05f, Color.White.copy(0.25f))
    // Floating islands
    fun drawIsland(cx: Float, cy: Float, w: Float, h: Float) {
        // Island top (grass)
        val top = Path().apply {
            moveTo(cx - w / 2, cy)
            quadraticTo(cx - w * 0.3f, cy - h * 0.4f, cx, cy - h * 0.5f)
            quadraticTo(cx + w * 0.3f, cy - h * 0.4f, cx + w / 2, cy)
            close()
        }
        drawPath(top, Color(0xFF66BB6A))
        // Underside (rocky)
        val bottom = Path().apply {
            moveTo(cx - w / 2, cy)
            quadraticTo(cx - w * 0.2f, cy + h * 0.8f, cx, cy + h)
            quadraticTo(cx + w * 0.2f, cy + h * 0.8f, cx + w / 2, cy)
            close()
        }
        drawPath(bottom, Color(0xFF795548))
        drawPath(Path().apply {
            moveTo(cx - w * 0.35f, cy + h * 0.1f)
            quadraticTo(cx - w * 0.15f, cy + h * 0.6f, cx, cy + h * 0.75f)
            quadraticTo(cx + w * 0.05f, cy + h * 0.5f, cx + w * 0.2f, cy + h * 0.1f)
            close()
        }, Color(0xFF5D4037))
        // Tiny tree on island
        drawLine(Color(0xFF4E342E), Offset(cx, cy - h * 0.5f), Offset(cx, cy - h * 0.9f), strokeWidth = 3f)
        drawCircle(Color(0xFF388E3C), radius = w * 0.12f, center = Offset(cx, cy - h * 0.95f))
    }
    drawIsland(sz.width * 0.72f, sz.height * 0.4f, sz.width * 0.22f, sz.height * 0.3f)
    drawIsland(sz.width * 0.88f, sz.height * 0.55f, sz.width * 0.14f, sz.height * 0.2f)
    drawIsland(sz.width * 0.55f, sz.height * 0.6f, sz.width * 0.16f, sz.height * 0.22f)
    // Clouds around islands
    drawCloud(sz.width * 0.6f, sz.height * 0.35f, sz.height * 0.05f, Color.White.copy(0.4f))
    drawCloud(sz.width * 0.82f, sz.height * 0.48f, sz.height * 0.04f, Color.White.copy(0.35f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent)), size = Size(sz.width * 0.55f, sz.height))
}

// ── World 6: Void Fortress — dark towers, energy ──

private fun DrawScope.drawVoidFortress(sz: Size) {
    // Dark stormy sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))))
    // Ominous energy glow at horizon
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xFF6C63FF).copy(0.3f), Color(0xFF6C63FF).copy(0.15f), Color.Transparent),
            startY = sz.height * 0.4f, endY = sz.height
        )
    )
    // Fortress towers
    fun drawTower(cx: Float, w: Float, h: Float) {
        val baseY = sz.height
        // Tower body
        drawRect(Color(0xFF263238), Offset(cx - w / 2, baseY - h), Size(w, h))
        // Battlements
        val bw = w / 5
        for (i in 0 until 5 step 2) {
            drawRect(Color(0xFF263238), Offset(cx - w / 2 + bw * i, baseY - h - bw), Size(bw, bw))
        }
        // Window glow
        drawRoundRect(Color(0xFF7C4DFF).copy(0.8f), Offset(cx - w * 0.15f, baseY - h * 0.7f), Size(w * 0.3f, w * 0.35f), CornerRadius(2f))
        drawRoundRect(Color(0xFF7C4DFF).copy(0.6f), Offset(cx - w * 0.15f, baseY - h * 0.4f), Size(w * 0.3f, w * 0.35f), CornerRadius(2f))
    }
    drawTower(sz.width * 0.7f, sz.width * 0.1f, sz.height * 0.7f)
    drawTower(sz.width * 0.82f, sz.width * 0.08f, sz.height * 0.55f)
    drawTower(sz.width * 0.92f, sz.width * 0.07f, sz.height * 0.45f)
    drawTower(sz.width * 0.6f, sz.width * 0.06f, sz.height * 0.4f)
    // Energy beam from center tower
    val beamPath = Path().apply {
        moveTo(sz.width * 0.69f, sz.height * 0.3f)
        lineTo(sz.width * 0.71f, sz.height * 0.3f)
        lineTo(sz.width * 0.73f, 0f)
        lineTo(sz.width * 0.67f, 0f)
        close()
    }
    drawPath(beamPath, Color(0xFF7C4DFF).copy(0.3f))
    // Floating void particles
    val voidColor = Color(0xFFB388FF)
    for ((px, py) in listOf(0.55f to 0.25f, 0.65f to 0.15f, 0.78f to 0.22f, 0.85f to 0.12f, 0.6f to 0.35f, 0.9f to 0.3f)) {
        drawCircle(voidColor.copy(alpha = 0.5f), radius = 3f, center = Offset(sz.width * px, sz.height * py))
    }
    // Ground shadow
    drawRect(Color(0xFF0D0D1A), Offset(0f, sz.height * 0.9f), Size(sz.width, sz.height * 0.1f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent)), size = Size(sz.width * 0.55f, sz.height))
}

// ── World 7: Molten Core — lava, volcanoes, fire ──

private fun DrawScope.drawMoltenCore(sz: Size) {
    // Dark volcanic sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFFBF360C), Color(0xFFFF6F00))))
    // Lava river at bottom
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFFF9100), Color(0xFFFF6D00), Color(0xFFDD2C00))),
        topLeft = Offset(0f, sz.height * 0.78f),
        size = Size(sz.width, sz.height * 0.22f)
    )
    // Lava bubbles
    for ((bx, by) in listOf(0.2f to 0.82f, 0.5f to 0.85f, 0.75f to 0.8f, 0.35f to 0.88f)) {
        drawCircle(Color(0xFFFFAB00).copy(0.7f), radius = sz.height * 0.03f, center = Offset(sz.width * bx, sz.height * by))
    }
    // Volcanoes
    fun drawVolcano(cx: Float, baseW: Float, h: Float) {
        val path = Path().apply {
            moveTo(cx - baseW / 2, sz.height * 0.78f)
            lineTo(cx - baseW * 0.12f, sz.height * 0.78f - h)
            lineTo(cx + baseW * 0.12f, sz.height * 0.78f - h)
            lineTo(cx + baseW / 2, sz.height * 0.78f)
            close()
        }
        drawPath(path, Color(0xFF4E342E))
        // Crater glow
        drawRoundRect(
            Color(0xFFFF6D00).copy(0.8f),
            Offset(cx - baseW * 0.1f, sz.height * 0.78f - h - sz.height * 0.02f),
            Size(baseW * 0.2f, sz.height * 0.04f),
            CornerRadius(4f)
        )
    }
    drawVolcano(sz.width * 0.75f, sz.width * 0.28f, sz.height * 0.5f)
    drawVolcano(sz.width * 0.9f, sz.width * 0.18f, sz.height * 0.35f)
    drawVolcano(sz.width * 0.6f, sz.width * 0.15f, sz.height * 0.3f)
    // Ember particles
    val emberColor = Color(0xFFFFD600)
    for ((ex, ey) in listOf(0.7f to 0.15f, 0.78f to 0.22f, 0.82f to 0.1f, 0.65f to 0.3f, 0.88f to 0.28f)) {
        drawCircle(emberColor.copy(0.6f), radius = 2.5f, center = Offset(sz.width * ex, sz.height * ey))
    }
    // Smoke wisps
    drawCircle(Color.White.copy(0.15f), radius = sz.height * 0.06f, center = Offset(sz.width * 0.73f, sz.height * 0.18f))
    drawCircle(Color.White.copy(0.1f), radius = sz.height * 0.04f, center = Offset(sz.width * 0.76f, sz.height * 0.12f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent)), size = Size(sz.width * 0.6f, sz.height))
}

// ── World 8: Starfall Summit — cosmic mountains, shooting stars ──

private fun DrawScope.drawStarfallSummit(sz: Size) {
    // Deep space gradient
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0D0221), Color(0xFF190A3A), Color(0xFF2D1B69))))
    // Stars (tiny dots)
    val starColor = Color.White
    val starPositions = listOf(
        0.1f to 0.1f, 0.2f to 0.25f, 0.35f to 0.08f, 0.45f to 0.3f,
        0.6f to 0.12f, 0.75f to 0.05f, 0.85f to 0.2f, 0.92f to 0.15f,
        0.15f to 0.4f, 0.5f to 0.18f, 0.68f to 0.28f, 0.38f to 0.35f
    )
    for ((sx, sy) in starPositions) {
        val r = if ((sx * 100).toInt() % 3 == 0) 2.5f else 1.5f
        drawCircle(starColor.copy(0.8f), radius = r, center = Offset(sz.width * sx, sz.height * sy))
    }
    // Shooting star trails
    val trailColor = Color(0xFFB388FF)
    drawLine(trailColor.copy(0.6f), Offset(sz.width * 0.3f, sz.height * 0.05f), Offset(sz.width * 0.5f, sz.height * 0.2f), strokeWidth = 2f)
    drawLine(trailColor.copy(0.4f), Offset(sz.width * 0.7f, sz.height * 0.1f), Offset(sz.width * 0.85f, sz.height * 0.25f), strokeWidth = 1.5f)
    // Mountain peaks
    val mtColor = Color(0xFF311B92)
    val mt1 = Path().apply {
        moveTo(sz.width * 0.4f, sz.height)
        lineTo(sz.width * 0.65f, sz.height * 0.35f)
        lineTo(sz.width * 0.9f, sz.height)
        close()
    }
    drawPath(mt1, mtColor)
    val mt2 = Path().apply {
        moveTo(sz.width * 0.6f, sz.height)
        lineTo(sz.width * 0.8f, sz.height * 0.42f)
        lineTo(sz.width, sz.height)
        close()
    }
    drawPath(mt2, Color(0xFF4A148C))
    val mt3 = Path().apply {
        moveTo(0f, sz.height)
        lineTo(sz.width * 0.25f, sz.height * 0.5f)
        lineTo(sz.width * 0.5f, sz.height)
        close()
    }
    drawPath(mt3, Color(0xFF4527A0))
    // Snow caps
    val snow = Path().apply {
        moveTo(sz.width * 0.62f, sz.height * 0.35f)
        lineTo(sz.width * 0.65f, sz.height * 0.35f)
        lineTo(sz.width * 0.68f, sz.height * 0.42f)
        lineTo(sz.width * 0.62f, sz.height * 0.42f)
        close()
    }
    drawPath(snow, Color.White.copy(0.5f))
    // Nebula glow
    drawCircle(Color(0xFF7C4DFF).copy(0.15f), radius = sz.height * 0.3f, center = Offset(sz.width * 0.3f, sz.height * 0.2f))
    drawCircle(Color(0xFFE040FB).copy(0.1f), radius = sz.height * 0.2f, center = Offset(sz.width * 0.7f, sz.height * 0.15f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.35f), Color.Transparent)), size = Size(sz.width * 0.6f, sz.height))
}

// ── World 9: Abyssal Depths — deep ocean, bioluminescent ──

private fun DrawScope.drawAbyssalDepths(sz: Size) {
    // Deep ocean gradient
    drawRect(Brush.verticalGradient(listOf(Color(0xFF001F3F), Color(0xFF003366), Color(0xFF004D40))))
    // Underwater light beams
    val beamColor = Color(0xFF80CBC4).copy(0.08f)
    for (frac in listOf(0.2f, 0.5f, 0.75f)) {
        val path = Path().apply {
            moveTo(sz.width * frac - sz.width * 0.02f, 0f)
            lineTo(sz.width * frac + sz.width * 0.08f, sz.height)
            lineTo(sz.width * frac - sz.width * 0.08f, sz.height)
            lineTo(sz.width * frac + sz.width * 0.02f, 0f)
            close()
        }
        drawPath(path, beamColor)
    }
    // Bioluminescent jellyfish
    fun drawJellyfish(cx: Float, cy: Float, r: Float, color: Color) {
        // Bell
        val bell = Path().apply {
            moveTo(cx - r, cy)
            quadraticTo(cx - r, cy - r * 1.2f, cx, cy - r * 1.3f)
            quadraticTo(cx + r, cy - r * 1.2f, cx + r, cy)
            quadraticTo(cx + r * 0.5f, cy + r * 0.3f, cx, cy + r * 0.2f)
            quadraticTo(cx - r * 0.5f, cy + r * 0.3f, cx - r, cy)
            close()
        }
        drawPath(bell, color.copy(0.5f))
        // Glow
        drawCircle(color.copy(0.2f), radius = r * 1.5f, center = Offset(cx, cy))
        // Tentacles
        for (dx in listOf(-0.5f, 0f, 0.5f)) {
            drawLine(color.copy(0.4f), Offset(cx + r * dx, cy + r * 0.2f), Offset(cx + r * dx * 0.8f, cy + r * 1.5f), strokeWidth = 1.5f)
        }
    }
    drawJellyfish(sz.width * 0.75f, sz.height * 0.3f, sz.height * 0.1f, Color(0xFF00E5FF))
    drawJellyfish(sz.width * 0.6f, sz.height * 0.55f, sz.height * 0.07f, Color(0xFF69F0AE))
    drawJellyfish(sz.width * 0.88f, sz.height * 0.5f, sz.height * 0.06f, Color(0xFFB388FF))
    // Sea floor
    val floor = Path().apply {
        moveTo(0f, sz.height * 0.88f)
        quadraticTo(sz.width * 0.2f, sz.height * 0.82f, sz.width * 0.4f, sz.height * 0.87f)
        quadraticTo(sz.width * 0.6f, sz.height * 0.84f, sz.width * 0.8f, sz.height * 0.86f)
        quadraticTo(sz.width * 0.9f, sz.height * 0.83f, sz.width, sz.height * 0.85f)
        lineTo(sz.width, sz.height)
        lineTo(0f, sz.height)
        close()
    }
    drawPath(floor, Color(0xFF1A3A3A))
    // Coral
    for ((cx, cColor) in listOf(0.65f to Color(0xFFFF6D00), 0.8f to Color(0xFFE91E63), 0.9f to Color(0xFF76FF03))) {
        val baseY = sz.height * 0.85f
        drawLine(cColor.copy(0.6f), Offset(sz.width * cx, baseY), Offset(sz.width * cx, baseY - sz.height * 0.08f), strokeWidth = 3f)
        drawCircle(cColor.copy(0.5f), radius = sz.height * 0.025f, center = Offset(sz.width * cx - 4f, baseY - sz.height * 0.09f))
        drawCircle(cColor.copy(0.5f), radius = sz.height * 0.025f, center = Offset(sz.width * cx + 4f, baseY - sz.height * 0.09f))
    }
    // Bubbles
    for ((bx, by) in listOf(0.3f to 0.6f, 0.45f to 0.4f, 0.55f to 0.7f, 0.7f to 0.65f)) {
        drawCircle(Color.White.copy(0.15f), radius = sz.height * 0.015f, center = Offset(sz.width * bx, sz.height * by))
    }
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent)), size = Size(sz.width * 0.55f, sz.height))
}

// ── World 10: Prism Citadel — rainbow crystal palace ──

private fun DrawScope.drawPrismCitadel(sz: Size) {
    // Prismatic gradient sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF3E5F5), Color(0xFFE8EAF6), Color(0xFFE0F7FA))))
    // Rainbow arc
    val rainbowColors = listOf(
        Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFFEB3B),
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
    )
    for (i in rainbowColors.indices) {
        val r = sz.height * 0.7f - i * sz.height * 0.03f
        drawCircle(
            rainbowColors[i].copy(0.3f), radius = r,
            center = Offset(sz.width * 0.5f, sz.height * 0.9f),
            style = Stroke(width = sz.height * 0.025f)
        )
    }
    // Crystal palace towers
    val crystalColors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4))
    fun drawCrystalTower(cx: Float, h: Float, w: Float, color: Color) {
        // Tower body
        val path = Path().apply {
            moveTo(cx - w / 2, sz.height * 0.9f)
            lineTo(cx - w * 0.3f, sz.height * 0.9f - h)
            lineTo(cx, sz.height * 0.9f - h - sz.height * 0.06f)
            lineTo(cx + w * 0.3f, sz.height * 0.9f - h)
            lineTo(cx + w / 2, sz.height * 0.9f)
            close()
        }
        drawPath(path, color.copy(0.7f))
        // Shine
        val shine = Path().apply {
            moveTo(cx - w * 0.1f, sz.height * 0.9f - h - sz.height * 0.04f)
            lineTo(cx + w * 0.05f, sz.height * 0.9f - h * 0.3f)
            lineTo(cx - w * 0.05f, sz.height * 0.9f - h * 0.3f)
            close()
        }
        drawPath(shine, Color.White.copy(0.3f))
    }
    drawCrystalTower(sz.width * 0.7f, sz.height * 0.5f, sz.width * 0.1f, crystalColors[0])
    drawCrystalTower(sz.width * 0.78f, sz.height * 0.4f, sz.width * 0.08f, crystalColors[1])
    drawCrystalTower(sz.width * 0.62f, sz.height * 0.35f, sz.width * 0.07f, crystalColors[2])
    drawCrystalTower(sz.width * 0.86f, sz.height * 0.3f, sz.width * 0.06f, crystalColors[3])
    // Floating prism shards
    for ((px, py, color) in listOf(
        Triple(0.55f, 0.25f, Color(0xFFFF4081)),
        Triple(0.65f, 0.15f, Color(0xFF7C4DFF)),
        Triple(0.8f, 0.2f, Color(0xFF00E5FF)),
        Triple(0.9f, 0.12f, Color(0xFFFFD740))
    )) {
        val diamond = Path().apply {
            val dx = sz.width * px; val dy = sz.height * py; val s = sz.height * 0.025f
            moveTo(dx, dy - s)
            lineTo(dx + s * 0.6f, dy)
            lineTo(dx, dy + s)
            lineTo(dx - s * 0.6f, dy)
            close()
        }
        drawPath(diamond, color.copy(0.5f))
    }
    // Ground
    drawRect(Color(0xFFCE93D8).copy(0.3f), Offset(0f, sz.height * 0.88f), Size(sz.width, sz.height * 0.12f))
    // Sparkles
    for ((sx, sy) in listOf(0.5f to 0.35f, 0.72f to 0.08f, 0.85f to 0.3f, 0.6f to 0.42f)) {
        drawCircle(Color.White.copy(0.6f), radius = 2f, center = Offset(sz.width * sx, sz.height * sy))
    }
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)), size = Size(sz.width * 0.55f, sz.height))
}

// ── Shared: simple cloud shape ──

private fun DrawScope.drawCloud(x: Float, y: Float, r: Float, color: Color = Color.White.copy(0.6f)) {
    drawCircle(color, radius = r, center = Offset(x, y))
    drawCircle(color, radius = r * 0.8f, center = Offset(x - r * 0.7f, y + r * 0.1f))
    drawCircle(color, radius = r * 0.9f, center = Offset(x + r * 0.8f, y + r * 0.05f))
    drawCircle(color, radius = r * 0.65f, center = Offset(x + r * 1.4f, y + r * 0.15f))
}
