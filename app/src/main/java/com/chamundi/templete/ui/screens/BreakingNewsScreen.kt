package com.chamundi.templete.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chamundi.templete.viewmodel.BreakingNewsViewModel
import com.chamundi.templete.utils.ImageGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakingNewsScreen(
    viewModel: BreakingNewsViewModel = viewModel(),
    onNavigateToLinkShare: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    var showShareDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breaking News") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToLinkShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Link")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { viewModel.generatePost() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Generate Post")
                    }
                    IconButton(onClick = { viewModel.generateThumbnail() }) {
                        Icon(Icons.Filled.AccountBox, contentDescription = "Generate Thumbnail")
                    }
                    IconButton(onClick = { showResolutionDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Resolution")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if ((state.generatedBitmap != null || state.thumbnailBitmap != null)) {
                                showShareDialog = true
                            }
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Filled.Share, "Share Image")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Preview Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio( if (state.thumbnailBitmap != null) 16f/9f else 4f/5f )
                    .clickable { imagePickerLauncher.launch("image/*") },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val displayBitmap = state.thumbnailBitmap ?: state.generatedBitmap
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(displayBitmap) {
                                    if (state.thumbnailBitmap != null) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            viewModel.onDragThumbnailText(dragAmount.x, dragAmount.y, size.toSize())
                                        }
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    } else if (state.selectedImageUri != null) {
                        // Show loading or original image if no generation yet?
                        // For now just icon
                         Icon(
                            imageVector = Icons.Filled.AccountBox,
                            contentDescription = "Tap to generate",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Tap to select image")
                        }
                    }

                    if (state.isGenerating) {
                        CircularProgressIndicator()
                    }
                }
            }

            // 2. Input Fields
            OutlinedTextField(
                value = state.breakingHeadline,
                onValueChange = viewModel::onHeadlineChanged,
                label = { Text("Headline") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            OutlinedTextField(
                value = state.newsText,
                onValueChange = viewModel::onNewsTextChanged,
                label = { Text("News Text (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // 3. Settings Expander
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSettingsSheet = !showSettingsSheet }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Appearance Settings", style = MaterialTheme.typography.titleMedium)
                        Icon(
                            if (showSettingsSheet) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Toggle Settings"
                        )
                    }

                    AnimatedVisibility(visible = showSettingsSheet) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Font Size
                            Column {
                                Text("Headline Size: ${(state.fontSizeMultiplier * 100).toInt()}%")
                                Slider(
                                    value = state.fontSizeMultiplier,
                                    onValueChange = viewModel::onFontSizeChanged,
                                    valueRange = 0.5f..2.0f
                                )
                            }
                            // News Size
                            Column {
                                Text("Body Size: ${(state.newsTextFontSizeMultiplier * 100).toInt()}%")
                                Slider(
                                    value = state.newsTextFontSizeMultiplier,
                                    onValueChange = viewModel::onNewsTextFontSizeChanged,
                                    valueRange = 0.5f..2.0f
                                )
                            }
                            // Line Gap
                            Column {
                                Text("Line Gap: ${(state.lineGapMultiplier * 100).toInt()}%")
                                Slider(
                                    value = state.lineGapMultiplier,
                                    onValueChange = viewModel::onLineGapChanged,
                                    valueRange = -0.5f..1.5f
                                )
                            }
                            // Alignment
                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Headline Alignment")
                                Button(onClick = viewModel::onHeadlineAlignmentChanged) {
                                    Text(state.headlineAlignment)
                                }
                            }
                        }
                    }
                }
            }

            // Spacer for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Dialogs
        if (showResolutionDialog) {
             AlertDialog(
                onDismissRequest = { showResolutionDialog = false },
                title = { Text("Select Resolution") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val resolutions = listOf("NONE", "FULLHD", "2K", "4K", "FILM1080", "FILM2K", "FILM4K")
                        resolutions.forEach { resolution ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onResolutionChanged(resolution)
                                        showResolutionDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.selectedResolution == resolution,
                                    onClick = null
                                )
                                Text(
                                    text = resolution,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResolutionDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showShareDialog) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Share Image") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { viewModel.shareImage(context, "YouTube"); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("YouTube") }
                        Button(onClick = { viewModel.shareImage(context, "Facebook"); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Facebook") }
                        Button(onClick = { viewModel.shareImage(context, "Instagram"); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Instagram") }
                        Button(onClick = { viewModel.shareImage(context, "Twitter"); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Twitter") }
                        Button(onClick = { viewModel.shareImage(context, "WhatsApp"); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("WhatsApp") }
                        OutlinedButton(onClick = { viewModel.saveImage(context); showShareDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Save to Gallery") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
