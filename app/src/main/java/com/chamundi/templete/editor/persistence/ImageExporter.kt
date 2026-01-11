package com.chamundi.templete.editor.persistence

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.chamundi.templete.editor.models.EditorState
import com.chamundi.templete.editor.models.Layer
import java.io.File
import java.io.FileOutputStream

/**
 * Handles exporting editor projects as flattened image files.
 */
object ImageExporter {
    
    /**
     * Exports the current editor state as a flattened PNG image.
     * All visible layers are rendered in order and composited.
     * 
     * @param state The editor state to export
     * @param outputFile The output PNG file
     * @return true if export was successful
     */
    fun exportAsPng(state: EditorState, outputFile: File): Boolean {
        return try {
            val bitmap = flattenLayers(state)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Exports the current editor state as a JPEG image.
     * 
     * @param state The editor state to export
     * @param outputFile The output JPEG file
     * @param quality JPEG quality (0-100)
     * @return true if export was successful
     */
    fun exportAsJpeg(state: EditorState, outputFile: File, quality: Int = 90): Boolean {
        return try {
            val bitmap = flattenLayers(state)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Creates a flattened bitmap from all visible layers.
     * 
     * @param state The editor state to flatten
     * @return A new Bitmap containing the flattened result
     */
    fun flattenLayers(state: EditorState): Bitmap {
        val width = state.canvasWidth
        val height = state.canvasHeight
        
        // Create output bitmap
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Render each visible layer from bottom to top
        state.layers.filter { it.visible }.forEach { layer ->
            renderLayerToBitmap(canvas, layer)
        }
        
        return result
    }
    
    /**
     * Renders a single layer to the canvas.
     */
    private fun renderLayerToBitmap(canvas: Canvas, layer: Layer) {
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (layer.opacity * 255).toInt()
        }
        
        canvas.save()
        
        // Apply transform
        val transform = layer.transform
        canvas.translate(transform.offsetX, transform.offsetY)
        canvas.rotate(transform.rotation)
        canvas.scale(transform.scaleX, transform.scaleY)
        
        when (layer) {
            is Layer.BackgroundLayer -> {
                val bgPaint = Paint().apply {
                    color = layer.color.toArgb()
                    alpha = (layer.opacity * 255).toInt()
                }
                canvas.drawRect(0f, 0f, layer.width.toFloat(), layer.height.toFloat(), bgPaint)
            }
            is Layer.ImageLayer -> {
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
            is Layer.TextLayer -> {
                val textPaint = Paint().apply {
                    textSize = layer.fontSize
                    color = layer.textColor.toArgb()
                    alpha = (layer.opacity * 255).toInt()
                    typeface = layer.typeface
                    isAntiAlias = true
                }
                canvas.drawText(layer.text, 0f, layer.fontSize, textPaint)
            }
        }
        
        canvas.restore()
    }
    
    /**
     * Creates a thumbnail preview of the project.
     * 
     * @param state The editor state
     * @param maxSize Maximum dimension (width or height)
     * @return A scaled-down preview bitmap
     */
    fun createThumbnail(state: EditorState, maxSize: Int = 256): Bitmap {
        val full = flattenLayers(state)
        
        val scale = if (full.width > full.height) {
            maxSize.toFloat() / full.width
        } else {
            maxSize.toFloat() / full.height
        }
        
        val thumbWidth = (full.width * scale).toInt()
        val thumbHeight = (full.height * scale).toInt()
        
        val thumbnail = Bitmap.createScaledBitmap(full, thumbWidth, thumbHeight, true)
        full.recycle()
        
        return thumbnail
    }
}
