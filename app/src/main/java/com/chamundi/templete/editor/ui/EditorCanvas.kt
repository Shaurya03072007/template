package com.chamundi.templete.editor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chamundi.templete.editor.EditorViewModel
import com.chamundi.templete.editor.models.*
import kotlin.math.sqrt

/**
 * EditorCanvas - Main canvas composable for rendering layers and handling gestures.
 * 
 * Photoshop Principles:
 * - Neutral dark gray background (NOT pure black)
 * - Canvas floats in workspace
 * - Zoomable and pannable independently of layers
 * - Strict gesture rules based on active tool
 * 
 * Gesture Rules:
 * - Pinch: Canvas zoom (NOT layer scale)
 * - Two-finger drag: Canvas pan
 * - One-finger drag: Move layer ONLY in Move Tool
 * - Tap: Select layer or create text (tool-dependent)
 */
@Composable
fun EditorCanvas(
    editorState: EditorState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var dialogPosition by remember { mutableStateOf(Offset.Zero) }
    var editingTextLayerId by remember { mutableStateOf<String?>(null) }
    
    val density = LocalDensity.current
    
    // Workspace background (neutral dark gray)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(editorState.canvasBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        // Canvas with gesture handling
        val canvasTransform = editorState.canvasTransform
        val zoom = canvasTransform.zoom
        val panX = canvasTransform.panX
        val panY = canvasTransform.panY
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pinch-to-zoom and two-finger pan
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        // Apply zoom
                        if (zoomChange != 1f) {
                            viewModel.zoomCanvas(zoomChange)
                        }
                        // Pan is handled by two-finger drag
                        viewModel.panCanvas(pan.x, pan.y)
                    }
                }
                .pointerInput(editorState.activeTool, editorState.selectedLayerId) {
                    // One-finger gestures (tap and drag)
                    detectTapAndDragGestures(
                        editorState = editorState,
                        viewModel = viewModel,
                        onTap = { contentPosition ->
                            handleTap(
                                offset = contentPosition,
                                editorState = editorState,
                                viewModel = viewModel,
                                onShowTextDialog = { position, layerId ->
                                    dialogPosition = position
                                    editingTextLayerId = layerId
                                    showTextDialog = true
                                }
                            )
                        }
                    )
                }
        ) {
            val canvasWidth = editorState.canvasWidth.toFloat()
            val canvasHeight = editorState.canvasHeight.toFloat()
            
            // Calculate center offset to center the canvas in the viewport
            val viewportCenterX = size.width / 2
            val viewportCenterY = size.height / 2
            val scaledCanvasWidth = canvasWidth * zoom
            val scaledCanvasHeight = canvasHeight * zoom
            val canvasLeft = viewportCenterX - scaledCanvasWidth / 2 + panX
            val canvasTop = viewportCenterY - scaledCanvasHeight / 2 + panY
            
            // Translate to canvas position, then apply zoom
            translate(left = canvasLeft, top = canvasTop) {
                scale(scale = zoom, pivot = Offset.Zero) {
                    // White canvas background
                    drawRect(
                        color = Color.White,
                        size = Size(canvasWidth, canvasHeight)
                    )
                    
                    // Render all visible layers
                    editorState.layers.filter { it.visible }.forEach { layer ->
                        renderLayer(
                            layer = layer,
                            isSelected = editorState.isLayerSelected(layer.id),
                            showTransformHandles = editorState.activeTool is Tool.Move
                        )
                    }
                }
            }
        }
        // Crop Overlay
        if (editorState.activeTool is Tool.Crop) {
            CropOverlay(
                canvasWidth = editorState.canvasWidth,
                canvasHeight = editorState.canvasHeight,
                canvasTransform = editorState.canvasTransform,
                viewportSize = viewportSize,
                onApplyCrop = { w, h, x, y ->
                    viewModel.cropCanvas(w, h, x, y)
                },
                onCancel = {
                    viewModel.selectTool(Tool.Move)
                }
            )
        }
    }

    // Text input dialog
    if (showTextDialog) {
        val existingLayer = editingTextLayerId?.let { id ->
            editorState.layers.filterIsInstance<Layer.TextLayer>().find { it.id == id }
        }
        
        TextInputDialog(
            initialText = existingLayer?.text ?: "",
            onConfirm = { text ->
                if (editingTextLayerId != null) {
                    // Update existing text layer
                    viewModel.updateTextContent(editingTextLayerId!!, text)
                } else {
                    // Create new text layer at dialog position
                    viewModel.createTextLayer(text, dialogPosition.x, dialogPosition.y)
                }
                showTextDialog = false
                editingTextLayerId = null
            },
            onDismiss = {
                showTextDialog = false
                editingTextLayerId = null
            }
        )
    }
}

/**
 * Detects one-finger tap and drag gestures
 */
private suspend fun PointerInputScope.detectTapAndDragGestures(
    editorState: EditorState,
    viewModel: EditorViewModel,
    onTap: (Offset) -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            
            // Skip if this looks like a multi-touch gesture starting
            if (currentEvent.changes.size > 1) {
                continue
            }
            
            val downTime = System.currentTimeMillis()
            val downPosition = down.position
            
            // Convert screen position to content (canvas) coordinates
            val contentPosition = screenToContentCoordinates(
                screenPosition = downPosition,
                editorState = editorState,
                viewportSize = size
            )
            
            // Check for handle hit first (if Move Tool active and layer selected)
            var activeHandle: TransformHandle? = null
            var capturedLayerId: String? = null
            
            if (editorState.activeTool is Tool.Move && editorState.selectedLayerId != null) {
                val layer = editorState.getSelectedLayer()
                if (layer != null && !layer.locked) {
                    val bounds = calculateLayerBounds(layer)
                    activeHandle = findHitHandle(contentPosition, bounds, editorState.canvasTransform.zoom)
                    if (activeHandle != null) {
                        capturedLayerId = layer.id
                    }
                }
            }
            
            var totalDragDistance = 0f
            var isDragging = false
            
            // Process drag or wait for release
            while (true) {
                val event = awaitPointerEvent()
                
                // Check if finger lifted
                if (event.changes.none { it.pressed }) {
                    break
                }
                
                // Skip if multi-touch (let transform gesture handler take over)
                if (event.changes.size > 1) {
                    break
                }
                
                val change = event.changes.firstOrNull() ?: break
                val dragDelta = change.positionChange()
                
                if (dragDelta != Offset.Zero) {
                    totalDragDistance += sqrt(dragDelta.x * dragDelta.x + dragDelta.y * dragDelta.y)
                    
                    if (totalDragDistance > 10f) {
                        isDragging = true
                    }
                    
                    change.consume()
                    
                    // Convert drag delta to content space
                    val zoom = editorState.canvasTransform.zoom
                    val contentDelta = dragDelta / zoom
                    
                    when {
                        activeHandle != null && capturedLayerId != null -> {
                            // Handle drag (resize/rotate)
                            viewModel.transformLayerWithHandle(capturedLayerId, activeHandle, contentDelta)
                        }
                        editorState.activeTool is Tool.Move && editorState.selectedLayerId != null -> {
                            // Layer drag (move)
                            val layer = editorState.getSelectedLayer()
                            if (layer != null && !layer.locked) {
                                viewModel.translateLayer(editorState.selectedLayerId, contentDelta.x, contentDelta.y)
                            }
                        }
                        else -> {
                            // Default: pan canvas
                            viewModel.panCanvas(dragDelta.x, dragDelta.y)
                        }
                    }
                }
            }
            
            // Check if this was a tap (minimal drag, quick release)
            val upTime = System.currentTimeMillis()
            if (!isDragging && (upTime - downTime) < 300) {
                onTap(contentPosition)
            }
        }
    }
}

/**
 * Converts screen coordinates to canvas content coordinates
 */
private fun screenToContentCoordinates(
    screenPosition: Offset,
    editorState: EditorState,
    viewportSize: androidx.compose.ui.unit.IntSize
): Offset {
    val zoom = editorState.canvasTransform.zoom
    val panX = editorState.canvasTransform.panX
    val panY = editorState.canvasTransform.panY
    val canvasWidth = editorState.canvasWidth.toFloat()
    val canvasHeight = editorState.canvasHeight.toFloat()
    
    // Calculate canvas position in viewport
    val viewportCenterX = viewportSize.width / 2f
    val viewportCenterY = viewportSize.height / 2f
    val scaledCanvasWidth = canvasWidth * zoom
    val scaledCanvasHeight = canvasHeight * zoom
    val canvasLeft = viewportCenterX - scaledCanvasWidth / 2 + panX
    val canvasTop = viewportCenterY - scaledCanvasHeight / 2 + panY
    
    // Convert screen position to content position
    val contentX = (screenPosition.x - canvasLeft) / zoom
    val contentY = (screenPosition.y - canvasTop) / zoom
    
    return Offset(contentX, contentY)
}

/**
 * Finds which transform handle was hit, if any
 */
private fun findHitHandle(
    contentPosition: Offset,
    bounds: BoundingBox,
    zoom: Float
): TransformHandle? {
    // Touch radius in content space (adjusted for zoom so screen touch target stays constant)
    val touchRadius = 30f / zoom
    
    // Check all handles, rotation first (highest priority), then corners, then edges
    val handlesInOrder = listOf(
        TransformHandle.ROTATION,
        TransformHandle.TOP_LEFT, TransformHandle.TOP_RIGHT,
        TransformHandle.BOTTOM_LEFT, TransformHandle.BOTTOM_RIGHT,
        TransformHandle.TOP_CENTER, TransformHandle.BOTTOM_CENTER,
        TransformHandle.CENTER_LEFT, TransformHandle.CENTER_RIGHT
    )
    
    for (handle in handlesInOrder) {
        val (hx, hy) = bounds.getHandlePosition(handle)
        val dx = contentPosition.x - hx
        val dy = contentPosition.y - hy
        val distanceSquared = dx * dx + dy * dy
        
        if (distanceSquared <= touchRadius * touchRadius) {
            return handle
        }
    }
    
    return null
}

/**
 * Handles tap gestures based on active tool
 */
private fun handleTap(
    offset: Offset,
    editorState: EditorState,
    viewModel: EditorViewModel,
    onShowTextDialog: (Offset, String?) -> Unit
) {
    when (editorState.activeTool) {
        is Tool.Text -> {
            // Check if tapping on existing text layer
            val tappedTextLayer = editorState.layers
                .filterIsInstance<Layer.TextLayer>()
                .filter { it.visible }
                .reversed() // Check from top to bottom
                .find { layer ->
                    val layerBounds = calculateLayerBounds(layer)
                    layerBounds.contains(offset.x, offset.y)
                }
            
            if (tappedTextLayer != null) {
                // Edit existing text
                viewModel.selectLayer(tappedTextLayer.id)
                onShowTextDialog(offset, tappedTextLayer.id)
            } else {
                // Create new text layer
                onShowTextDialog(offset, null)
            }
        }
        is Tool.Move -> {
            // Select layer at tap position
            val tappedLayer = editorState.layers
                .filter { it.visible }
                .asReversed() // Check from top to bottom
                .find { layer ->
                    val layerBounds = calculateLayerBounds(layer)
                    layerBounds.contains(offset.x, offset.y)
                }
            
            if (tappedLayer != null) {
                viewModel.selectLayer(tappedLayer.id)
            } else {
                viewModel.deselectLayer()
            }
        }
        else -> {
            // Other tools: no action on tap
        }
    }
}

/**
 * Calculates the content-space bounds of a layer
 */
/**
 * Calculates the content-space bounds of a layer
 */
private fun calculateLayerBounds(layer: Layer): BoundingBox {
    var width = when (layer) {
        is Layer.BackgroundLayer -> layer.width.toFloat()
        is Layer.ImageLayer -> layer.bitmap.width * layer.transform.scaleX
        is Layer.TextLayer -> {
            // Estimate text width if not set
            if (layer.textWidth > 0) layer.textWidth * layer.transform.scaleX
            else layer.text.length * layer.fontSize * 0.6f * layer.transform.scaleX
        }
    }
    
    var height = when (layer) {
        is Layer.BackgroundLayer -> layer.height.toFloat()
        is Layer.ImageLayer -> layer.bitmap.height * layer.transform.scaleY
        is Layer.TextLayer -> {
            if (layer.textHeight > 0) layer.textHeight * layer.transform.scaleY
            else layer.fontSize * 1.2f * layer.transform.scaleY
        }
    }
    
    // Safety checks for invalid dimensions
    if (width.isNaN() || width.isInfinite()) width = 100f
    if (height.isNaN() || height.isInfinite()) height = 100f
    if (width <= 1f) width = 1f
    if (height <= 1f) height = 1f
    
    val left = layer.transform.offsetX
    val top = layer.transform.offsetY
    
    return BoundingBox(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height
    )
}

/**
 * Renders a single layer to the canvas
 */
private fun DrawScope.renderLayer(
    layer: Layer,
    isSelected: Boolean,
    showTransformHandles: Boolean
) {
    val transform = layer.transform
    
    // Apply layer transform
    translate(
        left = transform.offsetX,
        top = transform.offsetY
    ) {
        rotate(
            degrees = transform.rotation,
            pivot = Offset.Zero
        ) {
            scale(
                scaleX = transform.scaleX,
                scaleY = transform.scaleY,
                pivot = Offset.Zero
            ) {
                // Render based on layer type
                when (layer) {
                    is Layer.BackgroundLayer -> {
                        drawRect(
                            color = layer.color,
                            size = Size(layer.width.toFloat(), layer.height.toFloat())
                        )
                    }
                    is Layer.ImageLayer -> {
                        drawImage(
                            image = layer.bitmap.asImageBitmap(),
                            alpha = layer.opacity
                        )
                    }
                    is Layer.TextLayer -> {
                        // Text rendering using native canvas
                        val paint = android.graphics.Paint().apply {
                            textSize = layer.fontSize
                            color = android.graphics.Color.argb(
                                (layer.opacity * 255).toInt(),
                                (layer.textColor.red * 255).toInt(),
                                (layer.textColor.green * 255).toInt(),
                                (layer.textColor.blue * 255).toInt()
                            )
                            typeface = layer.typeface
                            isAntiAlias = true
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            layer.text,
                            0f,
                            layer.fontSize, // Baseline
                            paint
                        )
                    }
                }
            }
        }
    }
    
    // Draw transform handles if selected and in Move Tool
    if (isSelected && showTransformHandles) {
        drawTransformHandles(layer)
    }
}

/**
 * Draws transform handles on selected layer
 */
private fun DrawScope.drawTransformHandles(layer: Layer) {
    val bounds = calculateLayerBounds(layer)
    
    // Ensure valid bounds for Size
    if (bounds.width.isNaN() || bounds.height.isNaN() || bounds.width <= 0 || bounds.height <= 0) {
        return
    }
    
    // Selection outline (blue)
    drawRect(
        color = Color(0xFF2196F3),
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 2f)
    )
    
    // Draw 8 resize handles
    val handleSize = 12f
    val resizeHandles = listOf(
        TransformHandle.TOP_LEFT,
        TransformHandle.TOP_CENTER,
        TransformHandle.TOP_RIGHT,
        TransformHandle.CENTER_LEFT,
        TransformHandle.CENTER_RIGHT,
        TransformHandle.BOTTOM_LEFT,
        TransformHandle.BOTTOM_CENTER,
        TransformHandle.BOTTOM_RIGHT
    )
    
    resizeHandles.forEach { handle ->
        val (x, y) = bounds.getHandlePosition(handle)
        drawCircle(
            color = Color(0xFF2196F3),
            radius = handleSize,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White,
            radius = handleSize - 2f,
            center = Offset(x, y)
        )
    }
    
    // Rotation handle (above top edge) - green
    val (rotX, rotY) = bounds.getHandlePosition(TransformHandle.ROTATION)
    
    // Draw line from top center to rotation handle
    val (topCenterX, topCenterY) = bounds.getHandlePosition(TransformHandle.TOP_CENTER)
    drawLine(
        color = Color(0xFF4CAF50),
        start = Offset(topCenterX, topCenterY),
        end = Offset(rotX, rotY),
        strokeWidth = 2f
    )
    
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = handleSize,
        center = Offset(rotX, rotY)
    )
    drawCircle(
        color = Color.White,
        radius = handleSize - 2f,
        center = Offset(rotX, rotY)
    )
}
