package com.chamundi.templete.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chamundi.templete.editor.EditorViewModel
import com.chamundi.templete.editor.models.EditorState
import com.chamundi.templete.editor.models.Layer

/**
 * PropertiesPanel - Contextual properties panel for editing layer attributes.
 * 
 * Shows different controls based on selected layer type:
 * - TextLayer: Font size slider, color picker
 * - All Layers: Transform display (position, scale, rotation), opacity
 */
@Composable
fun PropertiesPanel(
    editorState: EditorState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLayer = editorState.getSelectedLayer()
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Panel header
            Text(
                text = "PROPERTIES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (selectedLayer == null) {
                // No layer selected
                Text(
                    text = "Select a layer to edit properties",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Layer name
                Text(
                    text = selectedLayer.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Transform section
                TransformSection(layer = selectedLayer)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Type-specific properties
                when (selectedLayer) {
                    is Layer.TextLayer -> {
                        TextLayerProperties(
                            layer = selectedLayer,
                            onFontSizeChange = { size ->
                                viewModel.updateTextProperties(selectedLayer.id, fontSize = size)
                            },
                            onColorChange = { color ->
                                viewModel.updateTextProperties(selectedLayer.id, textColor = color)
                            }
                        )
                    }
                    is Layer.ImageLayer -> {
                        ImageLayerProperties(layer = selectedLayer)
                    }
                    is Layer.BackgroundLayer -> {
                        BackgroundLayerProperties(layer = selectedLayer)
                    }
                }
            }
        }
    }
}

/**
 * Transform display section (read-only for now)
 */
@Composable
private fun TransformSection(layer: Layer) {
    val transform = layer.transform
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Transform",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PropertyValuePair(label = "X", value = String.format("%.1f", transform.offsetX))
            PropertyValuePair(label = "Y", value = String.format("%.1f", transform.offsetY))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PropertyValuePair(label = "Scale X", value = String.format("%.0f%%", transform.scaleX * 100))
            PropertyValuePair(label = "Scale Y", value = String.format("%.0f%%", transform.scaleY * 100))
        }
        
        PropertyValuePair(
            label = "Rotation",
            value = String.format("%.1f°", transform.rotation)
        )
    }
}

/**
 * Helper composable for property label-value pairs
 */
@Composable
private fun PropertyValuePair(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Text layer specific properties
 */
@Composable
private fun TextLayerProperties(
    layer: Layer.TextLayer,
    onFontSizeChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Text Properties",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        // Text preview
        Text(
            text = "\"${layer.text.take(30)}${if (layer.text.length > 30) "..." else ""}\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Font size slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${layer.fontSize.toInt()}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = layer.fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..200f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Color picker (preset colors)
        Column {
            Text(
                text = "Text Color",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ColorPalette(
                selectedColor = layer.textColor,
                onColorSelected = onColorChange
            )
        }
    }
}

/**
 * Image layer specific properties
 */
@Composable
private fun ImageLayerProperties(layer: Layer.ImageLayer) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Image Properties",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        PropertyValuePair(
            label = "Original Size",
            value = "${layer.originalWidth}×${layer.originalHeight}"
        )
        
        PropertyValuePair(
            label = "Opacity",
            value = "${(layer.opacity * 100).toInt()}%"
        )
    }
}

/**
 * Background layer specific properties
 */
@Composable
private fun BackgroundLayerProperties(layer: Layer.BackgroundLayer) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Background Properties",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        PropertyValuePair(
            label = "Canvas Size",
            value = "${layer.width}×${layer.height}"
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Color:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(layer.color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

/**
 * Color palette for text color selection
 */
@Composable
private fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Black,
        Color.White,
        Color(0xFFE53935), // Red
        Color(0xFFFB8C00), // Orange
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF43A047), // Green
        Color(0xFF1E88E5), // Blue
        Color(0xFF8E24AA), // Purple
        Color(0xFF6D4C41), // Brown
        Color(0xFF546E7A)  // Blue Gray
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            val isSelected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}
