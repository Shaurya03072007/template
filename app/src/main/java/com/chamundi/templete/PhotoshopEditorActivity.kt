package com.chamundi.templete

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.chamundi.templete.editor.EditorViewModel
import com.chamundi.templete.editor.persistence.ImageExporter
import com.chamundi.templete.editor.persistence.ProjectSerializer
import com.chamundi.templete.editor.ui.ToolBar
import com.chamundi.templete.editor.ui.EditorCanvas
import com.chamundi.templete.editor.ui.LayersPanel
import com.chamundi.templete.editor.ui.PropertiesPanel
import com.chamundi.templete.editor.ui.PresetsDialog
import com.chamundi.templete.editor.models.Preset
import com.chamundi.templete.ui.theme.TempleteTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PhotoshopEditorActivity - Main activity for the Photoshop-style raster editor.
 * 
 * Layout Architecture:
 * - ToolBar (left): Vertical tool strip
 * - EditorCanvas (center): Main canvas with layers
 * - LayersPanel (right): Layers panel (collapsible)
 * 
 * Photoshop Principles:
 * - Tool-first workflow: Select tool before action
 * - Explicit selection: Layers must be selected to be edited
 * - Stateful modes: Gestures interpreted based on active tool
 * - Layer panel is authoritative: Controls selection and visibility
 */
class PhotoshopEditorActivity : ComponentActivity() {
    
    private val viewModel: EditorViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TempleteTheme {
                PhotoshopEditorScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Main editor screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoshopEditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editorState by viewModel.state.collectAsState()
    var showLayersPanel by remember { mutableStateOf(true) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // Presets Management
    val presetsDir = File(context.filesDir, "presets")
    if (!presetsDir.exists()) presetsDir.mkdirs()

    var presets by remember {
        mutableStateOf(
            presetsDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { Preset(it, it.nameWithoutExtension, Date(it.lastModified())) }
                ?.sortedByDescending { it.date }
                ?: emptyList()
        )
    }

    fun refreshPresets() {
        presets = presetsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { Preset(it, it.nameWithoutExtension, Date(it.lastModified())) }
            ?.sortedByDescending { it.date }
            ?: emptyList()
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.createImageLayer(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Helper function to get projects directory
    fun getProjectsDir(): File {
        val dir = File(context.getExternalFilesDir(null), "PhotoshopProjects")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    // Helper function to get exports directory
    fun getExportsDir(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "PhotoshopEditor"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    // Save project
    fun saveProject() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val projectDir = File(getProjectsDir(), "project_$timestamp")
            projectDir.mkdirs()
            val projectFile = File(projectDir, "project.json")
            
            ProjectSerializer.saveProject(viewModel.getState(), projectFile, context)
            Toast.makeText(context, "Project saved to ${projectDir.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save project: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Export as PNG
    fun exportAsPng(ratio: Float? = null) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(getExportsDir(), "export_$timestamp.png")
            
            if (ImageExporter.exportAsPng(viewModel.getState(), exportFile, ratio)) {
                Toast.makeText(context, "Exported to ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
                
                // Notify media scanner
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(exportFile.absolutePath),
                    null,
                    null
                )
            } else {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photoshop Editor") },
                navigationIcon = {
                    IconButton(onClick = { showPresetsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Presets Library"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // Menu button
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    
                    // Dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // File operations section
                        DropdownMenuItem(
                            text = { Text("New Project") },
                            onClick = {
                                viewModel.resetEditor()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save Project") },
                            onClick = {
                                saveProject()
                                showMenu = false
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // Export section
                        DropdownMenuItem(
                            text = { Text("Export as PNG") },
                            onClick = {
                                showExportDialog = true
                                showMenu = false
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // View/Canvas operations
                        DropdownMenuItem(
                            text = { Text("Reset Canvas Zoom") },
                            onClick = {
                                viewModel.resetCanvasTransform()
                                showMenu = false
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // Image operations
                        DropdownMenuItem(
                            text = { Text("Add Image from Gallery") },
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                                showMenu = false
                            }
                        )
                    }

                    // Toggle layers panel
                    IconButton(onClick = { showLayersPanel = !showLayersPanel }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Layers Panel"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {


            // Left: Tool Bar
            ToolBar(
                activeTool = editorState.activeTool,
                onToolSelected = { tool ->
                     // Toggle Presets if Tool is None (optional logic, for now simple button in toolbar maybe?)
                     viewModel.selectTool(tool)
                },
                modifier = Modifier.fillMaxHeight()
            )
            
            // Center: Editor Canvas
            EditorCanvas(
                editorState = editorState,
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Right: Layers and Properties Panels (collapsible)
            if (showLayersPanel) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                ) {
                    // Layers Panel (top half)
                    LayersPanel(
                        editorState = editorState,
                        viewModel = viewModel,
                        onAddImageClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    
                    // Properties Panel (bottom half)
                    PropertiesPanel(
                        editorState = editorState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Image") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Select Aspect Ratio:")
                        Button(
                            onClick = { exportAsPng(null); showExportDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Original")
                        }
                        Button(
                            onClick = { exportAsPng(1f); showExportDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1:1 (Square)")
                        }
                        Button(
                            onClick = { exportAsPng(4f/5f); showExportDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("4:5 (Portrait)")
                        }
                        Button(
                            onClick = { exportAsPng(16f/9f); showExportDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("16:9 (Landscape)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Presets Dialog
    if (showPresetsDialog) {
        PresetsDialog(
            presets = presets,
            onLoadPreset = { preset ->
                viewModel.loadPreset(context, preset.file)
            },
            onSavePreset = { name ->
                val file = File(presetsDir, "$name.json")
                viewModel.savePreset(context, file)
                refreshPresets()
            },
            onDeletePreset = { preset ->
                if (preset.file.exists()) preset.file.delete()
                refreshPresets()
            },
            onDismiss = { showPresetsDialog = false }
        )
    }
}
