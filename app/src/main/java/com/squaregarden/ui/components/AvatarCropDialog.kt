package com.squaregarden.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AvatarCropDialog(
    bitmap: ImageBitmap,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // Compute initial scale so image fills the crop circle
    val initialScale = remember(bitmap, viewSize) {
        if (viewSize.width <= 0) 1f
        else {
            val cropDiameter = min(viewSize.width, viewSize.height).toFloat()
            val minDim = min(bitmap.width, bitmap.height).toFloat()
            cropDiameter / minDim
        }
    }

    LaunchedEffect(initialScale) {
        if (initialScale > 0f) {
            scale = initialScale
            offset = Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 48.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Crop Avatar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Crop area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .onSizeChanged { viewSize = it },
                contentAlignment = Alignment.Center
            ) {
                if (viewSize.width > 0) {
                    val cropDiameter = min(viewSize.width, viewSize.height).toFloat()

                    Box(
                        modifier = Modifier
                            .size(with(LocalDensity.current) { cropDiameter.toDp() })
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Image with pan/zoom
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Crop preview",
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(
                                            initialScale * 0.5f,
                                            initialScale * 5f
                                        )
                                        offset += pan
                                    }
                                },
                            contentScale = ContentScale.None
                        )

                        // Circular mask overlay
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val r = size.minDimension / 2f
                            val path = Path().apply {
                                addRect(Rect(Offset.Zero, size))
                                addOval(
                                    Rect(
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = r
                                    )
                                )
                                fillType = PathFillType.EvenOdd
                            }
                            drawPath(path, Color.Black.copy(alpha = 0.55f))
                            drawCircle(
                                color = Color.White,
                                radius = r,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 15.sp)
                }
                Button(
                    onClick = {
                        val cropped = cropBitmap(bitmap, scale, offset, viewSize)
                        onCropped(cropped)
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun cropBitmap(
    source: ImageBitmap,
    scale: Float,
    offset: Offset,
    viewSize: IntSize
): Bitmap {
    val cropDiameter = min(viewSize.width, viewSize.height).toFloat()
    val cropRadius = cropDiameter / 2f

    // The image center in view coordinates is at (viewCenter + offset),
    // and the image is scaled by `scale`.
    // The crop circle is centered at the view center.
    // We need to find what portion of the source bitmap is visible in the crop circle.

    val viewCenterX = cropDiameter / 2f
    val viewCenterY = cropDiameter / 2f

    // Image center in view space
    val imgCenterX = viewCenterX + offset.x
    val imgCenterY = viewCenterY + offset.y

    // Crop circle bounds in source bitmap coordinates
    val srcCropLeft = ((viewCenterX - cropRadius - imgCenterX) / scale + source.width / 2f)
    val srcCropTop = ((viewCenterY - cropRadius - imgCenterY) / scale + source.height / 2f)
    val srcCropSize = (cropDiameter / scale)

    // Clamp to source bounds
    val sx = srcCropLeft.roundToInt().coerceIn(0, source.width - 1)
    val sy = srcCropTop.roundToInt().coerceIn(0, source.height - 1)
    val sw = srcCropSize.roundToInt().coerceIn(1, source.width - sx)
    val sh = srcCropSize.roundToInt().coerceIn(1, source.height - sy)

    // Extract region from source
    val androidBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(androidBitmap)
    val paint = android.graphics.Paint()
    // Draw the ImageBitmap onto an Android Bitmap
    canvas.drawBitmap(
        source.asAndroidBitmap(),
        0f, 0f, paint
    )

    val cropped = Bitmap.createBitmap(androidBitmap, sx, sy, sw, sh)

    // Make it square (use the smaller dimension)
    val side = min(cropped.width, cropped.height)
    val squareCropped = Bitmap.createBitmap(
        cropped,
        (cropped.width - side) / 2,
        (cropped.height - side) / 2,
        side, side
    )

    return squareCropped
}
