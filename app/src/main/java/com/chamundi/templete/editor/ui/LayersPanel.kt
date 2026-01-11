package com.chamundi.templete.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chamundi.templete.editor.EditorViewModel
import com.chamundi.templete.editor.models.EditorState
import com.chamundi.templete.editor.models.Layer

/**
 * LayersPanel - Collapsible panel showing layer list with controls.
 * 
 * Photoshop Principle: Layer panel is authoritative.
 * - Tap on layer → selects it
 * - Visibility toggle → eye icon
 * - Lock toggle → lock icon
 * - Opacity slider
 * - Delete button
 */
@Composable
fun LayersPanel(
    editorState: EditorState,
    viewModel: EditorViewModel,
    onAddImageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Panel header
            Text(
                text = "LAYERS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Layer list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reverse order: top layer first
                itemsIndexed(editorState.layers.asReversed()) { index, layer ->
                    LayerItem(
                        layer = layer,
                        isSelected = editorState.isLayerSelected(layer.id),
                        onLayerClick = { viewModel.selectLayer(layer.id) },
                        onVisibilityToggle = { viewModel.toggleLayerVisibility(layer.id) },
                        onLockToggle = { viewModel.toggleLayerLock(layer.id) },
                        onDelete = { viewModel.deleteLayer(layer.id) },
                        onOpacityChange = { opacity -> viewModel.updateLayerOpacity(layer.id, opacity) },
                        onMoveUp = { viewModel.moveLayerUp(layer.id) },
                        onMoveDown = { viewModel.moveLayerDown(layer.id) }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Add layer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Add Text Layer button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            viewModel.selectTool(com.chamundi.templete.editor.models.Tool.Text)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Text Layer"
                        )
                    }
                    Text(
                        text = "Text",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Add Image Layer button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onAddImageClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Image Layer"
                        )
                    }
                    Text(
                        text = "Image",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Individual layer item in the layers panel
 */
@Composable
private fun LayerItem(
    layer: Layer,
    isSelected: Boolean,
    onLayerClick: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLayerClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Layer header: name, type icon, controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layer type icon
                Icon(
                    imageVector = when (layer) {
                        is Layer.TextLayer -> Icons.Default.Edit
                        is Layer.ImageLayer -> Icons.Default.Add
                        is Layer.BackgroundLayer -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Layer name
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Visibility toggle
                IconButton(
                    onClick = onVisibilityToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (layer.visible) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = "Toggle Visibility",
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Lock toggle
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (layer.locked) {
                            Icons.Default.Lock
                        } else {
                            Icons.Default.Info
                        },
                        contentDescription = "Toggle Lock",
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Move Up
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Move Down
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Delete (only if not background)
                if (layer !is Layer.BackgroundLayer) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Layer",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Opacity slider
            if (isSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opacity",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        Slider(
                            value = layer.opacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "${(layer.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }
    }
}
