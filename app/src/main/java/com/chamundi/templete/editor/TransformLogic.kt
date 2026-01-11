package com.chamundi.templete.editor

import androidx.compose.ui.geometry.Offset
import com.chamundi.templete.editor.models.LayerTransform
import com.chamundi.templete.editor.models.TransformHandle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Logic for calculating new layer transforms based on handle interactions.
 */
object TransformLogic {

    /**
     * Calculates the new transform for a layer when a handle is dragged.
     */
    fun calculateNewTransform(
        initialTransform: LayerTransform,
        handle: TransformHandle,
        dragDelta: Offset,
        originalWidth: Float,
        originalHeight: Float
    ): LayerTransform {
        return when (handle) {
            TransformHandle.ROTATION -> calculateRotation(initialTransform, dragDelta, originalWidth, originalHeight)
            else -> calculateResize(initialTransform, handle, dragDelta, originalWidth, originalHeight)
        }
    }

    private fun calculateRotation(
        transform: LayerTransform,
        dragDelta: Offset,
        width: Float,
        height: Float
    ): LayerTransform {
        // Simple rotation based on drag X for now, or relative to center?
        // Better: Calculate angle from center to touch point.
        // For drag delta approach, we can just map x distance to degrees.
        // Sensitivity: 1px = 0.5 degrees
        val degrees = dragDelta.x * 0.5f
        return transform.rotate(degrees)
    }

    private fun calculateResize(
        transform: LayerTransform,
        handle: TransformHandle,
        dragDelta: Offset,
        width: Float,
        height: Float
    ): LayerTransform {
        // This is a simplified resizing logic.
        // True "local" resizing requires rotating the drag delta into local space.
        
        // 1. Rotate drag delta to match layer rotation (inverse)
        val rad = -Math.toRadians(transform.rotation.toDouble())
        val cos = cos(rad)
        val sin = sin(rad)
        
        // Rotated delta (local delta)
        val dx = (dragDelta.x * cos - dragDelta.y * sin).toFloat()
        val dy = (dragDelta.x * sin + dragDelta.y * cos).toFloat()
        
        // 2. Apply scale based on handle
        var newScaleX = transform.scaleX
        var newScaleY = transform.scaleY
        var newOffsetX = transform.offsetX
        var newOffsetY = transform.offsetY
        
        // Calculate current dimensions
        val currentWidth = width * transform.scaleX
        val currentHeight = height * transform.scaleY
        
        when (handle) {
            TransformHandle.TOP_LEFT -> {
                // Dragging TL modifies ScaleX (negative), ScaleY (negative), OffsetX, OffsetY
                // Simplified: Just scale around center? No, standard resize anchors opposite corner.
                // For MVP, implementing "Center Scale" is easier and acceptable for pinch, 
                // but for handles, we typically want anchor-based.
                
                // Let's implement "Scale from Center" for now as a first step, 
                // as true anchor-based resizing with rotation is complex to get right 
                // without a matrix library.
                
                // If we assume Scale from Center:
                newScaleX -= dx / width * 2
                newScaleY -= dy / height * 2
            }
            TransformHandle.TOP_RIGHT -> {
                newScaleX += dx / width * 2
                newScaleY -= dy / height * 2
            }
            TransformHandle.BOTTOM_LEFT -> {
                newScaleX -= dx / width * 2
                newScaleY += dy / height * 2
            }
            TransformHandle.BOTTOM_RIGHT -> {
                newScaleX += dx / width * 2
                newScaleY += dy / height * 2
            }
            TransformHandle.TOP_CENTER -> {
                newScaleY -= dy / height * 2
            }
            TransformHandle.BOTTOM_CENTER -> {
                newScaleY += dy / height * 2
            }
            TransformHandle.CENTER_LEFT -> {
                newScaleX -= dx / width * 2
            }
            TransformHandle.CENTER_RIGHT -> {
                newScaleX += dx / width * 2
            }
            else -> {}
        }
        
        // Prevent negative scale (flipping)
        if (newScaleX < 0.1f) newScaleX = 0.1f
        if (newScaleY < 0.1f) newScaleY = 0.1f
        
        return transform.copy(scaleX = newScaleX, scaleY = newScaleY)
    }
}
