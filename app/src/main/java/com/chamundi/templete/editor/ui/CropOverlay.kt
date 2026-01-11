package com.chamundi.templete.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.withTransform
import com.chamundi.templete.editor.models.CanvasTransform

/**
 * Overlay for cropping the canvas.
 * Shows a grid over the current canvas area and allows resizing it.
 */
@Composable
fun CropOverlay(
    canvasWidth: Int,
    canvasHeight: Int,
    canvasTransform: CanvasTransform,
    viewportSize: Size,
    onApplyCrop: (Int, Int, Int, Int) -> Unit, // width, height, offsetX, offsetY
    onCancel: () -> Unit
) {
    // Current crop rect in canvas coordinates
    var cropRect by remember {
        mutableStateOf(
            androidx.compose.ui.geometry.Rect(
                0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat()
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Overlay Canvas for drawing grid and handles
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Hit test handles here ideally, for now just bottom-right resize
                        // Convert drag to canvas space
                        val zoom = canvasTransform.zoom
                        val dx = dragAmount.x / zoom
                        val dy = dragAmount.y / zoom

                        // Simple logic: dragging adjusts the crop rect
                        // This is a simplified implementation. Full implementation needs handle hit testing.
                        // Assuming dragging anywhere resizes from bottom right for now
                        val newWidth = (cropRect.width + dx).coerceAtLeast(100f)
                        val newHeight = (cropRect.height + dy).coerceAtLeast(100f)
                        cropRect = cropRect.copy(
                            right = cropRect.left + newWidth,
                            bottom = cropRect.top + newHeight
                        )
                    }
                }
        ) {
            val zoom = canvasTransform.zoom
            val panX = canvasTransform.panX
            val panY = canvasTransform.panY

            // Calculate canvas position in viewport to align overlay
            val viewportCenterX = viewportSize.width / 2
            val viewportCenterY = viewportSize.height / 2
            val scaledCanvasWidth = canvasWidth * zoom
            val scaledCanvasHeight = canvasHeight * zoom
            val canvasLeft = viewportCenterX - scaledCanvasWidth / 2 + panX
            val canvasTop = viewportCenterY - scaledCanvasHeight / 2 + panY

            translate(left = canvasLeft, top = canvasTop) {
                // Draw dimmed background outside crop area
                // (Simplified: just draw crop border and grid)

                // Scale context to match canvas zoom
                withTransform({
                    scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
                }) {
                    // Draw Crop Rect Border
                    drawRect(
                        color = Color.White,
                        topLeft = cropRect.topLeft,
                        size = cropRect.size,
                        style = Stroke(width = 2f / zoom)
                    )

                    // Draw Grid (Thirds)
                    val width = cropRect.width
                    val height = cropRect.height
                    val left = cropRect.left
                    val top = cropRect.top

                    // Vertical lines
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(left + width/3, top), Offset(left + width/3, top + height), strokeWidth = 1f / zoom)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(left + 2*width/3, top), Offset(left + 2*width/3, top + height), strokeWidth = 1f / zoom)

                    // Horizontal lines
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(left, top + height/3), Offset(left + width, top + height/3), strokeWidth = 1f / zoom)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(left, top + 2*height/3), Offset(left + width, top + 2*height/3), strokeWidth = 1f / zoom)

                    // Draw corner handles
                    val handleSize = 20f / zoom
                    drawCircle(Color.White, radius = handleSize, center = cropRect.topLeft)
                    drawCircle(Color.White, radius = handleSize, center = cropRect.topRight)
                    drawCircle(Color.White, radius = handleSize, center = cropRect.bottomLeft)
                    drawCircle(Color.White, radius = handleSize, center = cropRect.bottomRight)
                }
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Cancel")
            }
            Button(onClick = {
                onApplyCrop(
                    cropRect.width.toInt(),
                    cropRect.height.toInt(),
                    cropRect.left.toInt(),
                    cropRect.top.toInt()
                )
            }) {
                Text("Apply")
            }
        }

        // Info text
        Text(
            text = "${cropRect.width.toInt()} x ${cropRect.height.toInt()}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(4.dp)
        )
    }
}
