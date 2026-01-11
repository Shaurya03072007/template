package com.chamundi.templete.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chamundi.templete.editor.models.Preset
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Dialog for managing editor presets with a grid layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsDialog(
    presets: List<Preset>,
    onLoadPreset: (Preset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (Preset) -> Unit,
    onDismiss: () -> Unit
) {
    var showSaveInput by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presets Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Toolbar / Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { 
                            newPresetName = "Preset ${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}"
                            showSaveInput = true 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Current State")
                    }
                }

                // Grid Content
                if (presets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved presets found.\nSave your current work as a preset to see it here.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(presets) { preset ->
                            PresetGridItem(
                                preset = preset,
                                onLoad = { 
                                    onLoadPreset(preset)
                                    onDismiss() 
                                },
                                onDelete = { onDeletePreset(preset) }
                            )
                        }
                    }
                }
            }
        }

        // Save Dialog Overlay
        if (showSaveInput) {
            AlertDialog(
                onDismissRequest = { showSaveInput = false },
                title = { Text("Save Preset") },
                text = {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPresetName.isNotBlank()) {
                                onSavePreset(newPresetName)
                                showSaveInput = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveInput = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PresetGridItem(
    preset: Preset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square card
            .clickable { onLoad() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview Placeholder (Top 2/3)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                // In a real app, we would load a thumbnail here.
                // For now, just a large initial letter
                Text(
                    text = preset.name.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Black
                )
                
                // Delete button top-right
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            // Info (Bottom 1/3)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy").format(preset.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
