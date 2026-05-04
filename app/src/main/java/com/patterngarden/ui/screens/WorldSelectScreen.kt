package com.patterngarden.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.patterngarden.data.ProfileRepository
import com.patterngarden.data.ProgressRepository
import com.patterngarden.model.Difficulty
import com.patterngarden.ui.navigation.Screen
import com.patterngarden.ui.theme.*
import kotlin.math.sin

data class WorldInfo(
    val id: Int,
    val name: String,
    val subtitle: String,
    val starsToUnlock: Int,
    val color: Color
)

private val worlds = listOf(
    WorldInfo(1, "Seedling Garden", "Levels 1-8", 0, Sage),
    WorldInfo(2, "Blooming Meadow", "Levels 9-17", 8, TileBlue),
    WorldInfo(3, "Ancient Grove", "Levels 18-25", 20, WarmBrown),
    WorldInfo(4, "Crystal Cavern", "Levels 26-33", 40, Color(0xFF81D4FA)),
    WorldInfo(5, "Shattered Isles", "Levels 34-41", 65, Color(0xFFCE93D8)),
    WorldInfo(6, "Void Fortress", "Levels 42-49", 100, Color(0xFF78909C))
)

@Composable
fun WorldSelectScreen(navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
    val profile by profileRepo.profileFlow.collectAsState(initial = null)
    val difficulty = profile?.let { Difficulty.fromId(it.difficulty) }
    val playerLevel = profile?.playerLevel ?: 0

    val easyWorldsDisabled = when (difficulty) {
        Difficulty.HARD -> playerLevel >= 30
        Difficulty.MEDIUM -> playerLevel >= 40
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Worlds",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$totalStars stars",
                style = MaterialTheme.typography.bodyLarge,
                color = TileYellow,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            worlds.forEach { world ->
                val unlocked = totalStars >= world.starsToUnlock
                val tooEasy = easyWorldsDisabled && world.id <= 2
                val accessible = unlocked && !tooEasy

                Card(
                    onClick = {
                        if (accessible) {
                            navController.navigate(Screen.LevelSelect.create(world.id))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .alpha(if (accessible) 1f else 0.5f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Banner background illustration
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            when (world.id) {
                                1 -> drawSeedlingGarden(size)
                                2 -> drawBloomingMeadow(size)
                                3 -> drawAncientGrove(size)
                                4 -> drawCrystalCavern(size)
                                5 -> drawShatteredIsles(size)
                                6 -> drawVoidFortress(size)
                            }
                        }

                        // Text overlay
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = world.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = world.subtitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (tooEasy) {
                                Text(
                                    text = "Too easy",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCDD2)
                                )
                            } else if (!unlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("locked", fontSize = 12.sp, color = Color.White.copy(0.7f))
                                    Text(
                                        text = "${world.starsToUnlock} stars",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFE082)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Back", fontSize = 28.sp)
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

// ── Shared: simple cloud shape ──

private fun DrawScope.drawCloud(x: Float, y: Float, r: Float, color: Color = Color.White.copy(0.6f)) {
    drawCircle(color, radius = r, center = Offset(x, y))
    drawCircle(color, radius = r * 0.8f, center = Offset(x - r * 0.7f, y + r * 0.1f))
    drawCircle(color, radius = r * 0.9f, center = Offset(x + r * 0.8f, y + r * 0.05f))
    drawCircle(color, radius = r * 0.65f, center = Offset(x + r * 1.4f, y + r * 0.15f))
}
