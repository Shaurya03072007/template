package com.chamundi.templete.editor.models

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Sealed class hierarchy for editor tools.
 * Only ONE tool can be active at a time.
 * Gestures are interpreted based on the active tool.
 */
sealed class Tool {
    object None : Tool()
    object Move : Tool()
    object Text : Tool()
    object Crop : Tool()
    // Future: Brush, Erase
    
    fun getDisplayName(): String = when (this) {
        is None -> "Select"
        is Move -> "Move"
        is Text -> "Text"
        is Crop -> "Crop"
    }
}

/**
 * Transform state for each layer.
 * Represents the layer's position, scale, and rotation.
 */
data class LayerTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f // degrees
) {
    /**
     * Creates a copy with updated offset (translation)
     */
    fun withOffset(x: Float, y: Float) = copy(offsetX = x, offsetY = y)
    
    /**
     * Creates a copy with updated scale
     */
    fun withScale(sx: Float, sy: Float) = copy(scaleX = sx, scaleY = sy)
    
    /**
     * Creates a copy with updated rotation
     */
    fun withRotation(degrees: Float) = copy(rotation = degrees)
    
    /**
     * Applies a delta translation
     */
    fun translate(dx: Float, dy: Float) = copy(
        offsetX = offsetX + dx,
        offsetY = offsetY + dy
    )
    
    /**
     * Applies a delta scale
     */
    fun scale(dsx: Float, dsy: Float) = copy(
        scaleX = scaleX * dsx,
        scaleY = scaleY * dsy
    )
    
    /**
     * Applies a delta rotation
     */
    fun rotate(degrees: Float) = copy(
        rotation = rotation + degrees
    )
}

/**
 * Sealed class hierarchy for editor layers.
 * All layers have common properties: visibility, lock, opacity, transform.
 */
sealed class Layer {
    abstract val id: String
    abstract val name: String
    abstract var visible: Boolean
    abstract var locked: Boolean
    abstract var opacity: Float // 0.0 to 1.0
    abstract var transform: LayerTransform
    
    /**
     * Background layer - solid color fill
     */
    data class BackgroundLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Background",
        override var visible: Boolean = true,
        override var locked: Boolean = false,
        override var opacity: Float = 1f,
        override var transform: LayerTransform = LayerTransform(),
        val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        val width: Int = 1080,
        val height: Int = 1350
    ) : Layer()
    
    /**
     * Image layer - raster bitmap
     */
    data class ImageLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Image",
        override var visible: Boolean = true,
        override var locked: Boolean = false,
        override var opacity: Float = 1f,
        override var transform: LayerTransform = LayerTransform(),
        val bitmap: Bitmap,
        val originalWidth: Int = bitmap.width,
        val originalHeight: Int = bitmap.height
    ) : Layer()
    
    /**
     * Text layer - rendered text with properties
     */
    data class TextLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Text",
        override var visible: Boolean = true,
        override var locked: Boolean = false,
        override var opacity: Float = 1f,
        override var transform: LayerTransform = LayerTransform(),
        var text: String = "",
        var fontSize: Float = 48f,
        var textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
        var typeface: Typeface = Typeface.DEFAULT,
        var fontFamily: String = "Default",
        // Text bounds (calculated during render)
        var textWidth: Float = 0f,
        var textHeight: Float = 0f
    ) : Layer()
    
    /**
     * Creates a copy of the layer with updated transform
     */
    fun withTransform(newTransform: LayerTransform): Layer = when (this) {
        is BackgroundLayer -> copy(transform = newTransform)
        is ImageLayer -> copy(transform = newTransform)
        is TextLayer -> copy(transform = newTransform)
    }
    
    /**
     * Creates a copy of the layer with updated visibility
     */
    fun withVisibility(isVisible: Boolean): Layer = when (this) {
        is BackgroundLayer -> copy(visible = isVisible)
        is ImageLayer -> copy(visible = isVisible)
        is TextLayer -> copy(visible = isVisible)
    }
    
    /**
     * Creates a copy of the layer with updated lock state
     */
    fun withLock(isLocked: Boolean): Layer = when (this) {
        is BackgroundLayer -> copy(locked = isLocked)
        is ImageLayer -> copy(locked = isLocked)
        is TextLayer -> copy(locked = isLocked)
    }
    
    /**
     * Creates a copy of the layer with updated opacity
     */
    fun withOpacity(newOpacity: Float): Layer = when (this) {
        is BackgroundLayer -> copy(opacity = newOpacity.coerceIn(0f, 1f))
        is ImageLayer -> copy(opacity = newOpacity.coerceIn(0f, 1f))
        is TextLayer -> copy(opacity = newOpacity.coerceIn(0f, 1f))
    }
}

/**
 * Canvas viewport transform.
 * Controls zoom and pan of the viewport, NOT the layers.
 */
data class CanvasTransform(
    val zoom: Float = 1f, // 0.1 to 5.0
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    /**
     * Creates a copy with updated zoom
     */
    fun withZoom(newZoom: Float) = copy(zoom = newZoom.coerceIn(0.1f, 5f))
    
    /**
     * Creates a copy with updated pan
     */
    fun withPan(x: Float, y: Float) = copy(panX = x, panY = y)
    
    /**
     * Applies a delta zoom
     */
    fun applyZoom(zoomDelta: Float) = copy(
        zoom = (zoom * zoomDelta).coerceIn(0.1f, 5f)
    )
    
    /**
     * Applies a delta pan
     */
    fun applyPan(dx: Float, dy: Float) = copy(
        panX = panX + dx,
        panY = panY + dy
    )
}

/**
 * Editor state - single source of truth.
 * Immutable data class managed by ViewModel.
 */
data class EditorState(
    val activeTool: Tool = Tool.None,
    val selectedLayerId: String? = null,
    val layers: List<Layer> = emptyList(),
    val canvasTransform: CanvasTransform = CanvasTransform(),
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1350,
    val canvasBackgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2B2B2B) // Neutral dark gray
) {
    /**
     * Gets the currently selected layer, if any
     */
    fun getSelectedLayer(): Layer? = selectedLayerId?.let { id ->
        layers.find { it.id == id }
    }
    
    /**
     * Checks if a layer is selected
     */
    fun isLayerSelected(layerId: String): Boolean = selectedLayerId == layerId
    
    /**
     * Creates a copy with a new active tool
     */
    fun withActiveTool(tool: Tool) = copy(activeTool = tool)
    
    /**
     * Creates a copy with a new selected layer
     */
    fun withSelectedLayer(layerId: String?) = copy(selectedLayerId = layerId)
    
    /**
     * Creates a copy with updated layers
     */
    fun withLayers(newLayers: List<Layer>) = copy(layers = newLayers)
    
    /**
     * Creates a copy with updated canvas transform
     */
    fun withCanvasTransform(newTransform: CanvasTransform) = copy(canvasTransform = newTransform)
    
    /**
     * Adds a layer to the top of the stack
     */
    fun addLayer(layer: Layer): EditorState {
        return copy(layers = layers + layer, selectedLayerId = layer.id)
    }
    
    /**
     * Removes a layer by ID
     */
    fun removeLayer(layerId: String): EditorState {
        val newLayers = layers.filterNot { it.id == layerId }
        val newSelectedId = if (selectedLayerId == layerId) {
            newLayers.lastOrNull()?.id
        } else {
            selectedLayerId
        }
        return copy(layers = newLayers, selectedLayerId = newSelectedId)
    }
    
    /**
     * Updates a layer by ID
     */
    fun updateLayer(layerId: String, updater: (Layer) -> Layer): EditorState {
        val newLayers = layers.map { layer ->
            if (layer.id == layerId) updater(layer) else layer
        }
        return copy(layers = newLayers)
    }
    
    /**
     * Reorders layers (drag and drop)
     */
    fun reorderLayers(fromIndex: Int, toIndex: Int): EditorState {
        val newLayers = layers.toMutableList()
        val item = newLayers.removeAt(fromIndex)
        newLayers.add(toIndex, item)
        return copy(layers = newLayers)
    }
}

/**
 * Transform handle types for selection UI
 */
enum class TransformHandle {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    ROTATION // Above top edge
}

/**
 * Represents a bounding box for layer selection
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val centerX: Float = (left + right) / 2f,
    val centerY: Float = (top + bottom) / 2f,
    val width: Float = right - left,
    val height: Float = bottom - top
) {
    /**
     * Gets the position of a specific handle
     */
    fun getHandlePosition(handle: TransformHandle): Pair<Float, Float> = when (handle) {
        TransformHandle.TOP_LEFT -> left to top
        TransformHandle.TOP_CENTER -> centerX to top
        TransformHandle.TOP_RIGHT -> right to top
        TransformHandle.CENTER_LEFT -> left to centerY
        TransformHandle.CENTER_RIGHT -> right to centerY
        TransformHandle.BOTTOM_LEFT -> left to bottom
        TransformHandle.BOTTOM_CENTER -> centerX to bottom
        TransformHandle.BOTTOM_RIGHT -> right to bottom
        TransformHandle.ROTATION -> centerX to (top - 50f) // 50px above top edge
    }
    
    /**
     * Checks if a point is inside the bounding box
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
}
