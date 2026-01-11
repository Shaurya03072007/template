package com.chamundi.templete.editor.persistence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.chamundi.templete.editor.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Handles serialization and deserialization of editor projects.
 * Projects are saved as JSON files with layer data.
 * 
 * Note: Image layers reference image files by path - images must exist on load.
 */
object ProjectSerializer {
    
    private const val VERSION = 1
    
    /**
     * Saves the editor state to a JSON file.
     * 
     * @param state The editor state to save
     * @param file The output file
     * @param context Context for file operations
     */
    fun saveProject(state: EditorState, file: File, context: Context) {
        val json = JSONObject().apply {
            put("version", VERSION)
            put("canvasWidth", state.canvasWidth)
            put("canvasHeight", state.canvasHeight)
            put("activeTool", serializeTool(state.activeTool))
            put("canvasTransform", serializeCanvasTransform(state.canvasTransform))
            put("layers", serializeLayers(state.layers, context, file.parentFile))
            // Fix: Store selectedLayerId or JSONObject.NULL explicitly
            if (state.selectedLayerId != null) {
                put("selectedLayerId", state.selectedLayerId)
            } else {
                put("selectedLayerId", JSONObject.NULL)
            }
        }
        
        file.writeText(json.toString(2))
    }
    
    /**
     * Loads an editor state from a JSON file.
     * 
     * @param file The input file
     * @param context Context for file operations
     * @return The loaded editor state, or null if loading fails
     */
    fun loadProject(file: File, context: Context): EditorState? {
        return try {
            val json = JSONObject(file.readText())
            val version = json.optInt("version", 1)
            
            EditorState(
                canvasWidth = json.getInt("canvasWidth"),
                canvasHeight = json.getInt("canvasHeight"),
                activeTool = deserializeTool(json.optString("activeTool", "None")),
                canvasTransform = deserializeCanvasTransform(json.optJSONObject("canvasTransform")),
                layers = deserializeLayers(json.getJSONArray("layers"), context, file.parentFile),
                selectedLayerId = json.optString("selectedLayerId", null)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Serialization helpers
    
    private fun serializeTool(tool: Tool): String = when (tool) {
        is Tool.None -> "None"
        is Tool.Move -> "Move"
        is Tool.Text -> "Text"
        is Tool.Crop -> "Crop"
    }
    
    private fun deserializeTool(name: String): Tool = when (name) {
        "Move" -> Tool.Move
        "Text" -> Tool.Text
        "Crop" -> Tool.Crop
        else -> Tool.None
    }
    
    private fun serializeCanvasTransform(transform: CanvasTransform): JSONObject {
        return JSONObject().apply {
            put("zoom", transform.zoom.toDouble())
            put("panX", transform.panX.toDouble())
            put("panY", transform.panY.toDouble())
        }
    }
    
    private fun deserializeCanvasTransform(json: JSONObject?): CanvasTransform {
        if (json == null) return CanvasTransform()
        return CanvasTransform(
            zoom = json.optDouble("zoom", 1.0).toFloat(),
            panX = json.optDouble("panX", 0.0).toFloat(),
            panY = json.optDouble("panY", 0.0).toFloat()
        )
    }
    
    private fun serializeTransform(transform: LayerTransform): JSONObject {
        return JSONObject().apply {
            put("offsetX", transform.offsetX.toDouble())
            put("offsetY", transform.offsetY.toDouble())
            put("scaleX", transform.scaleX.toDouble())
            put("scaleY", transform.scaleY.toDouble())
            put("rotation", transform.rotation.toDouble())
        }
    }
    
    private fun deserializeTransform(json: JSONObject?): LayerTransform {
        if (json == null) return LayerTransform()
        return LayerTransform(
            offsetX = json.optDouble("offsetX", 0.0).toFloat(),
            offsetY = json.optDouble("offsetY", 0.0).toFloat(),
            scaleX = json.optDouble("scaleX", 1.0).toFloat(),
            scaleY = json.optDouble("scaleY", 1.0).toFloat(),
            rotation = json.optDouble("rotation", 0.0).toFloat()
        )
    }
    
    private fun serializeColor(color: Color): String {
        return String.format("#%08X", color.toArgb())
    }
    
    private fun deserializeColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.Black
        }
    }
    
    private fun serializeLayers(layers: List<Layer>, context: Context, projectDir: File?): JSONArray {
        return JSONArray().apply {
            layers.forEach { layer ->
                put(serializeLayer(layer, context, projectDir))
            }
        }
    }
    
    private fun serializeLayer(layer: Layer, context: Context, projectDir: File?): JSONObject {
        return JSONObject().apply {
            put("id", layer.id)
            put("name", layer.name)
            put("visible", layer.visible)
            put("locked", layer.locked)
            put("opacity", layer.opacity.toDouble())
            put("transform", serializeTransform(layer.transform))
            
            when (layer) {
                is Layer.BackgroundLayer -> {
                    put("type", "background")
                    put("color", serializeColor(layer.color))
                    put("width", layer.width)
                    put("height", layer.height)
                }
                is Layer.TextLayer -> {
                    put("type", "text")
                    put("text", layer.text)
                    put("fontSize", layer.fontSize.toDouble())
                    put("textColor", serializeColor(layer.textColor))
                    put("fontFamily", layer.fontFamily)
                    put("textWidth", layer.textWidth.toDouble())
                    put("textHeight", layer.textHeight.toDouble())
                }
                is Layer.ImageLayer -> {
                    put("type", "image")
                    put("originalWidth", layer.originalWidth)
                    put("originalHeight", layer.originalHeight)
                    
                    // Save bitmap to file
                    projectDir?.let { dir ->
                        val imageFile = File(dir, "image_${layer.id}.png")
                        try {
                            FileOutputStream(imageFile).use { out ->
                                layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            put("imagePath", imageFile.name)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    
    private fun deserializeLayers(json: JSONArray, context: Context, projectDir: File?): List<Layer> {
        val layers = mutableListOf<Layer>()
        for (i in 0 until json.length()) {
            deserializeLayer(json.getJSONObject(i), context, projectDir)?.let {
                layers.add(it)
            }
        }
        return layers
    }
    
    private fun deserializeLayer(json: JSONObject, context: Context, projectDir: File?): Layer? {
        val id = json.getString("id")
        val name = json.getString("name")
        val visible = json.getBoolean("visible")
        val locked = json.getBoolean("locked")
        val opacity = json.getDouble("opacity").toFloat()
        val transform = deserializeTransform(json.optJSONObject("transform"))
        
        return when (json.getString("type")) {
            "background" -> Layer.BackgroundLayer(
                id = id,
                name = name,
                visible = visible,
                locked = locked,
                opacity = opacity,
                transform = transform,
                color = deserializeColor(json.getString("color")),
                width = json.getInt("width"),
                height = json.getInt("height")
            )
            "text" -> {
                val fontFamily = json.optString("fontFamily", "Default")
                Layer.TextLayer(
                    id = id,
                    name = name,
                    visible = visible,
                    locked = locked,
                    opacity = opacity,
                    transform = transform,
                    text = json.getString("text"),
                    fontSize = json.getDouble("fontSize").toFloat(),
                    textColor = deserializeColor(json.getString("textColor")),
                    fontFamily = fontFamily,
                    typeface = com.chamundi.templete.editor.utils.FontProvider.getTypeface(fontFamily),
                    textWidth = json.optDouble("textWidth", 0.0).toFloat(),
                    textHeight = json.optDouble("textHeight", 0.0).toFloat()
                )
            }
            "image" -> {
                // Fix: Properly restart image path from json
                val imagePath = if (json.has("imagePath")) json.getString("imagePath") else null
                val bitmap = if (imagePath != null && projectDir != null) {
                    val imageFile = File(projectDir, imagePath)
                    if (imageFile.exists()) {
                        BitmapFactory.decodeFile(imageFile.absolutePath)
                    } else null
                } else null
                
                if (bitmap != null) {
                    Layer.ImageLayer(
                        id = id,
                        name = name,
                        visible = visible,
                        locked = locked,
                        opacity = opacity,
                        transform = transform,
                        bitmap = bitmap,
                        originalWidth = json.getInt("originalWidth"),
                        originalHeight = json.getInt("originalHeight")
                    )
                } else null
            }
            else -> null
        }
    }
}
