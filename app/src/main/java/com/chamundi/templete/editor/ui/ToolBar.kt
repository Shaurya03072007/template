package com.chamundi.templete.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chamundi.templete.editor.models.Tool

/**
 * ToolBar composable - displays available tools in a vertical strip.
 * Only ONE tool can be active at a time.
 * 
 * Photoshop principle: Tools first, objects second.
 */
@Composable
fun ToolBar(
    activeTool: Tool,
    onToolSelected: (Tool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(72.dp)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Title
            Text(
                text = "TOOLS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Selection Tool (None)
            ToolButton(
                icon = Icons.Default.CheckCircle,
                label = "Select",
                isActive = activeTool is Tool.None,
                onClick = { onToolSelected(Tool.None) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Move Tool
            ToolButton(
                icon = Icons.Default.Place,
                label = "Move",
                isActive = activeTool is Tool.Move,
                onClick = { onToolSelected(Tool.Move) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Text Tool
            ToolButton(
                icon = Icons.Default.Edit,
                label = "Text",
                isActive = activeTool is Tool.Text,
                onClick = { onToolSelected(Tool.Text) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Crop Tool (disabled for now)
            ToolButton(
                icon = Icons.Default.Settings,
                label = "Crop",
                isActive = activeTool is Tool.Crop,
                enabled = false, // Disabled in MVP
                onClick = { onToolSelected(Tool.Crop) }
            )
        }
    }
}

/**
 * Individual tool button with icon and active state indicator
 */
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (isActive) {
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        Modifier
                    }
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
