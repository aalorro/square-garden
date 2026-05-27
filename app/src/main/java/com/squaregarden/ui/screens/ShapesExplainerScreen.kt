package com.squaregarden.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.SettingsRepository
import com.squaregarden.model.CellPos
import com.squaregarden.model.ShapeType
import com.squaregarden.model.TileColor
import com.squaregarden.ui.components.drawEmbossedTile
import com.squaregarden.ui.components.drawTileMotif
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.DisplayFontFamily
import kotlinx.coroutines.launch

private data class ShapeDemo(
    val title: String,
    val description: String,
    val tileColor: TileColor,
    val gridWidth: Int,
    val gridHeight: Int,
    val cells: List<CellPos>
)

private val shapeDemos = listOf(
    ShapeDemo(
        title = "Line",
        description = "3 or more tiles of one color in a row or column",
        tileColor = TileColor.RED,
        gridWidth = 4, gridHeight = 1,
        cells = listOf(CellPos(0, 0), CellPos(0, 1), CellPos(0, 2), CellPos(0, 3))
    ),
    ShapeDemo(
        title = "Square",
        description = "A 2\u00D72 block of one color",
        tileColor = TileColor.BLUE,
        gridWidth = 2, gridHeight = 2,
        cells = listOf(CellPos(0, 0), CellPos(0, 1), CellPos(1, 0), CellPos(1, 1))
    ),
    ShapeDemo(
        title = ShapeType.L_SHAPE.label,
        description = "An L-shaped formation of one color",
        tileColor = TileColor.GREEN,
        gridWidth = 2, gridHeight = 4,
        cells = ShapeType.L_SHAPE.offsets
    ),
    ShapeDemo(
        title = ShapeType.T_SHAPE.label,
        description = "A T-shaped formation of one color",
        tileColor = TileColor.YELLOW,
        gridWidth = 3, gridHeight = 3,
        cells = ShapeType.T_SHAPE.offsets
    ),
    ShapeDemo(
        title = ShapeType.CROSS.label,
        description = "A cross / plus formation of one color",
        tileColor = TileColor.ORANGE,
        gridWidth = 3, gridHeight = 3,
        cells = ShapeType.CROSS.offsets
    ),
    ShapeDemo(
        title = ShapeType.Z_SHAPE.label,
        description = "A zigzag formation of one color",
        tileColor = TileColor.RED,
        gridWidth = 3, gridHeight = 2,
        cells = ShapeType.Z_SHAPE.offsets
    ),
    ShapeDemo(
        title = ShapeType.U_SHAPE.label,
        description = "A U-shaped formation of one color",
        tileColor = TileColor.BLUE,
        gridWidth = 3, gridHeight = 2,
        cells = ShapeType.U_SHAPE.offsets
    ),
    ShapeDemo(
        title = ShapeType.X_SHAPE.label,
        description = "A diagonal X / cross formation of one color (Pro+)",
        tileColor = TileColor.VIOLET,
        gridWidth = 3, gridHeight = 3,
        cells = ShapeType.X_SHAPE.offsets
    ),
    ShapeDemo(
        title = ShapeType.Y_SHAPE.label,
        description = "A Y-shaped formation of one color (Pro+)",
        tileColor = TileColor.VIOLET,
        gridWidth = 3, gridHeight = 4,
        cells = ShapeType.Y_SHAPE.offsets
    )
)

@Composable
fun ShapesExplainerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var currentDemoIndex by remember { mutableIntStateOf(0) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }

    val demo = shapeDemos[currentDemoIndex]

    // Per-cell animation progress (0f to 1f)
    val cellAnimations = remember(currentDemoIndex, replayTrigger) {
        List(demo.cells.size) { Animatable(0f) }
    }

    // Staggered pop-in animation
    LaunchedEffect(currentDemoIndex, replayTrigger) {
        cellAnimations.forEach { it.snapTo(0f) }
        demo.cells.indices.forEach { i ->
            kotlinx.coroutines.delay(250L)
            launch {
                cellAnimations[i].animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shape Patterns",
            fontFamily = DisplayFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Match these patterns to complete goals",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Shape title
        Text(
            text = demo.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mini canvas with board background
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE8E0D4),
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxGridDim = maxOf(demo.gridWidth, demo.gridHeight)
                Canvas(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .aspectRatio(demo.gridWidth.toFloat() / demo.gridHeight)
                ) {
                    val cs = size.width / demo.gridWidth
                    val cornerR = cs * 0.18f

                    // Draw empty cell backgrounds
                    for (r in 0 until demo.gridHeight) {
                        for (c in 0 until demo.gridWidth) {
                            val inset = cs * 0.05f
                            drawRoundRect(
                                color = Color(0xFFF5F0E8),
                                topLeft = Offset(c * cs + inset, r * cs + inset),
                                size = Size(cs - inset * 2, cs - inset * 2),
                                cornerRadius = CornerRadius(cornerR)
                            )
                        }
                    }

                    // Draw animated tiles
                    demo.cells.forEachIndexed { i, cell ->
                        val progress = cellAnimations[i].value
                        if (progress > 0f) {
                            val scale = progress.coerceIn(0f, 1f)
                            val tileCs = cs * scale
                            val offsetX = cell.col * cs + (cs - tileCs) / 2f
                            val offsetY = cell.row * cs + (cs - tileCs) / 2f
                            drawEmbossedTile(demo.tileColor, offsetX, offsetY, tileCs, cornerR * scale)
                            drawTileMotif(demo.tileColor, offsetX, offsetY, tileCs)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = demo.description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shapeDemos.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .size(if (i == currentDemoIndex) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == currentDemoIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Previous / Replay / Next buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { if (currentDemoIndex > 0) currentDemoIndex-- },
                enabled = currentDemoIndex > 0
            ) {
                Text("Previous", fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = { replayTrigger++ },
                shape = RoundedCornerShape(50)
            ) {
                Text("Replay", fontSize = 14.sp)
            }

            TextButton(
                onClick = { if (currentDemoIndex < shapeDemos.lastIndex) currentDemoIndex++ },
                enabled = currentDemoIndex < shapeDemos.lastIndex
            ) {
                Text("Next", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Don't show again checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
        ) {
            Checkbox(
                checked = dontShowAgain,
                onCheckedChange = { dontShowAgain = it }
            )
            Text(
                text = "Don't show again",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue button
        Button(
            onClick = {
                scope.launch {
                    if (dontShowAgain) {
                        settingsRepo.setShapesExplainerDismissed(true)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ShapesExplainer.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
