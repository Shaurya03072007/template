package com.chamundi.templete.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.compose.ui.geometry.Offset
import com.chamundi.templete.editor.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Photoshop-style editor.
 * Manages the EditorState and provides actions for modifying it.
 * 
 * All state mutations go through this ViewModel to ensure consistency.
 */
class EditorViewModel : ViewModel() {
    
    // Private mutable state
    private val _state = MutableStateFlow(createInitialState())
    
    // Public immutable state
    val state: StateFlow<EditorState> = _state.asStateFlow()
    
    /**
     * Creates the initial editor state with a white background layer
     */
    private fun createInitialState(): EditorState {
        val backgroundLayer = Layer.BackgroundLayer(
            name = "Background",
            color = androidx.compose.ui.graphics.Color.White
        )
        return EditorState(
            activeTool = Tool.None,
            layers = listOf(backgroundLayer),
            selectedLayerId = null
        )
    }
    
    // ===== TOOL ACTIONS =====
    
    /**
     * Selects a tool. Only one tool can be active at a time.
     * This changes the interpretation of gestures globally.
     */
    fun selectTool(tool: Tool) {
        _state.value = _state.value.withActiveTool(tool)
    }
    
    // ===== LAYER SELECTION ACTIONS =====
    
    /**
     * Selects a layer by ID.
     * If the layer is locked, selection is still allowed (but transforms won't be).
     */
    fun selectLayer(layerId: String?) {
        _state.value = _state.value.withSelectedLayer(layerId)
    }
    
    /**
     * Deselects the current layer
     */
    fun deselectLayer() {
        _state.value = _state.value.withSelectedLayer(null)
    }
    
    // ===== LAYER MANAGEMENT ACTIONS =====
    
    /**
     * Adds a new layer to the top of the stack and selects it
     */
    fun addLayer(layer: Layer) {
        _state.value = _state.value.addLayer(layer)
    }
    
    /**
     * Removes a layer by ID
     */
    fun deleteLayer(layerId: String) {
        _state.value = _state.value.removeLayer(layerId)
    }
    
    /**
     * Toggles layer visibility
     */
    fun toggleLayerVisibility(layerId: String) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            layer.withVisibility(!layer.visible)
        }
    }
    
    /**
     * Toggles layer lock state
     */
    fun toggleLayerLock(layerId: String) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            layer.withLock(!layer.locked)
        }
    }
    
    /**
     * Updates layer opacity
     */
    fun updateLayerOpacity(layerId: String, opacity: Float) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            layer.withOpacity(opacity)
        }
    }
    
    /**
     * Reorders layers (for drag and drop in layers panel)
     */
    fun reorderLayers(fromIndex: Int, toIndex: Int) {
        _state.value = _state.value.reorderLayers(fromIndex, toIndex)
    }
    
    // ===== LAYER TRANSFORM ACTIONS =====
    
    /**
     * Updates the transform of a layer.
     * NO-OP if the layer is locked.
     */
    fun updateLayerTransform(layerId: String, transform: LayerTransform) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer.locked) {
                layer // No change if locked
            } else {
                layer.withTransform(transform)
            }
        }
    }
    
    /**
     * Translates a layer by delta (used during drag in Move Tool)
     */
    fun translateLayer(layerId: String, dx: Float, dy: Float) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer.locked) {
                layer
            } else {
                layer.withTransform(layer.transform.translate(dx, dy))
            }
        }
    }
    
    /**
     * Scales a layer by delta (used during handle resize)
     */
    fun scaleLayer(layerId: String, dsx: Float, dsy: Float) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer.locked) {
                layer
            } else {
                layer.withTransform(layer.transform.scale(dsx, dsy))
            }
        }
    }
    
    /**
     * Rotates a layer by delta degrees (used during rotation handle drag)
     */
    fun rotateLayer(layerId: String, degrees: Float) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer.locked) {
                layer
            } else {
                layer.withTransform(layer.transform.rotate(degrees))
            }
        }
    }

    /**
     * Updates layer transform based on handle interaction using TransformLogic.
     */
    fun transformLayerWithHandle(layerId: String, handle: TransformHandle, dragDelta: Offset) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer.locked) return@updateLayer layer

            val width = when (layer) {
                is Layer.BackgroundLayer -> layer.width.toFloat()
                is Layer.ImageLayer -> layer.originalWidth.toFloat()
                is Layer.TextLayer -> layer.textWidth
            }
            
            val height = when (layer) {
                is Layer.BackgroundLayer -> layer.height.toFloat()
                is Layer.ImageLayer -> layer.originalHeight.toFloat()
                is Layer.TextLayer -> layer.textHeight
            }

            val newTransform = TransformLogic.calculateNewTransform(
                initialTransform = layer.transform,
                handle = handle,
                dragDelta = dragDelta,
                originalWidth = width,
                originalHeight = height
            )

            layer.withTransform(newTransform)
        }
    }
    
    // ===== TEXT LAYER SPECIFIC ACTIONS =====
    
    /**
     * Updates text content of a TextLayer
     */
    fun updateTextContent(layerId: String, text: String) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer is Layer.TextLayer) {
                layer.copy(text = text)
            } else {
                layer
            }
        }
    }
    
    /**
     * Updates text properties of a TextLayer
     */
    fun updateTextProperties(
        layerId: String,
        fontSize: Float? = null,
        textColor: androidx.compose.ui.graphics.Color? = null,
        fontFamily: String? = null,
        typeface: android.graphics.Typeface? = null
    ) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer is Layer.TextLayer) {
                layer.copy(
                    fontSize = fontSize ?: layer.fontSize,
                    textColor = textColor ?: layer.textColor,
                    fontFamily = fontFamily ?: layer.fontFamily,
                    typeface = typeface ?: layer.typeface
                )
            } else {
                layer
            }
        }
    }

    /**
     * Updates background layer color
     */
    fun updateBackgroundColor(layerId: String, color: androidx.compose.ui.graphics.Color) {
        _state.value = _state.value.updateLayer(layerId) { layer ->
            if (layer is Layer.BackgroundLayer) {
                layer.copy(color = color)
            } else {
                layer
            }
        }
    }
    
    // ===== CANVAS TRANSFORM ACTIONS =====
    
    /**
     * Updates the canvas transform (zoom and pan)
     */
    fun updateCanvasTransform(transform: CanvasTransform) {
        _state.value = _state.value.withCanvasTransform(transform)
    }
    
    /**
     * Applies zoom delta to canvas
     */
    fun zoomCanvas(zoomDelta: Float) {
        val currentTransform = _state.value.canvasTransform
        _state.value = _state.value.withCanvasTransform(
            currentTransform.applyZoom(zoomDelta)
        )
    }
    
    /**
     * Applies pan delta to canvas
     */
    fun panCanvas(dx: Float, dy: Float) {
        val currentTransform = _state.value.canvasTransform
        _state.value = _state.value.withCanvasTransform(
            currentTransform.applyPan(dx, dy)
        )
    }
    
    /**
     * Resets canvas transform to default (zoom = 1, pan = 0)
     */
    fun resetCanvasTransform() {
        _state.value = _state.value.withCanvasTransform(CanvasTransform())
    }

    /**
     * Crops the canvas to the specified dimensions and offset.
     * Adjusts layers so they maintain their relative position to the visual content.
     */
    fun cropCanvas(width: Int, height: Int, offsetX: Int, offsetY: Int) {
        val currentState = _state.value

        // 1. Update layers: shift them by -offsetX, -offsetY
        val newLayers = currentState.layers.map { layer ->
            val newTransform = layer.transform.translate(-offsetX.toFloat(), -offsetY.toFloat())
            if (layer is Layer.BackgroundLayer) {
                // Background layer usually resizes to canvas, but here we treat it as just resizing
                layer.copy(
                    width = width,
                    height = height,
                    transform = newTransform.copy(offsetX = 0f, offsetY = 0f) // Keep bg at 0,0? Or let it shift?
                    // Actually, for crop, if we crop into the image, we want the content to move relative to origin (0,0).
                    // So yes, everything shifts left/up by offset.
                )
            } else {
                layer.withTransform(newTransform)
            }
        }

        // 2. Update state with new dimensions and layers
        _state.value = currentState.copy(
            canvasWidth = width,
            canvasHeight = height,
            layers = newLayers,
            // Reset to Move tool after crop
            activeTool = Tool.Move
        )
    }
    
    // ===== LAYER CREATION HELPERS =====
    
    /**
     * Creates a new text layer at the specified position
     */
    fun createTextLayer(text: String, x: Float, y: Float) {
        val textLayer = Layer.TextLayer(
            name = "Text ${_state.value.layers.size}",
            text = text,
            transform = LayerTransform(offsetX = x, offsetY = y)
        )
        addLayer(textLayer)
    }
    
    /**
     * Creates a new image layer from a bitmap
     */
    fun createImageLayer(bitmap: Bitmap, x: Float = 0f, y: Float = 0f) {
        val imageLayer = Layer.ImageLayer(
            name = "Image ${_state.value.layers.size}",
            bitmap = bitmap,
            transform = LayerTransform(offsetX = x, offsetY = y)
        )
        addLayer(imageLayer)
    }
    
    // ===== UTILITY FUNCTIONS =====
    
    /**
     * Checks if the currently selected layer is locked
     */
    fun isSelectedLayerLocked(): Boolean {
        val selectedLayer = _state.value.getSelectedLayer()
        return selectedLayer?.locked == true
    }
    
    /**
     * Gets the current tool
     */
    fun getCurrentTool(): Tool = _state.value.activeTool
    
    /**
     * Gets the selected layer
     */
    fun getSelectedLayer(): Layer? = _state.value.getSelectedLayer()
    
    // ===== PERSISTENCE FUNCTIONS =====
    
    /**
     * Gets the current state for serialization
     */
    fun getState(): EditorState = _state.value
    
    /**
     * Loads a new state (for project loading)
     */
    fun loadState(newState: EditorState) {
        _state.value = newState
    }
    
    /**
     * Resets the editor to a fresh state
     */
    fun resetEditor() {
        _state.value = createInitialState()
    }

    /**
     * Loads preset from file
     */
    fun loadPreset(context: android.content.Context, file: java.io.File) {
        val loadedState = com.chamundi.templete.editor.persistence.ProjectSerializer.loadProject(file, context)
        if (loadedState != null) {
            _state.value = loadedState
        }
    }

    /**
     * Saves current state as preset
     */
    fun savePreset(context: android.content.Context, file: java.io.File) {
        com.chamundi.templete.editor.persistence.ProjectSerializer.saveProject(_state.value, file, context)
    }
    
    // ===== LAYER REORDERING =====
    
    /**
     * Moves a layer up in the stack (higher Z-order)
     */
    fun moveLayerUp(layerId: String) {
        val layers = _state.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == layerId }
        
        // Check if layer exists and isn't already at the top
        if (index != -1 && index < layers.size - 1) {
            val tmp = layers[index]
            layers[index] = layers[index + 1]
            layers[index + 1] = tmp
            _state.value = _state.value.copy(layers = layers)
        }
    }
    
    /**
     * Moves a layer down in the stack (lower Z-order)
     */
    fun moveLayerDown(layerId: String) {
        val layers = _state.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == layerId }
        
        // Check if layer exists and isn't at the bottom (or just above background)
        if (index > 0) {
            // If the layer at index 0 is background, don't allow moving below it
            if (index == 1 && layers[0] is Layer.BackgroundLayer) {
                return
            }
            
            val tmp = layers[index]
            layers[index] = layers[index - 1]
            layers[index - 1] = tmp
            _state.value = _state.value.copy(layers = layers)
        }
    }
}
