package com.chamundi.templete.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chamundi.templete.utils.AppConfig
import com.chamundi.templete.utils.ImageGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.chamundi.templete.R

data class BreakingNewsState(
    val selectedImageUri: Uri? = null,
    val breakingHeadline: String = "",
    val newsText: String = "",
    val generatedBitmap: Bitmap? = null,
    val thumbnailBitmap: Bitmap? = null,
    val fontSizeMultiplier: Float = 1.0f,
    val newsTextFontSizeMultiplier: Float = 1.0f,
    val lineGapMultiplier: Float = 0.6f,
    val headlineAlignment: String = "LEFT",
    val selectedResolution: String = "NONE",
    val overlayResId: Int = R.drawable.thumbnail,
    val resolutionImagePaddingBottom: Float = 20f,
    val resolutionImagePaddingSide: Float = 220f,
    val headlineTextOffsetX: Float = 0f,
    val headlineTextOffsetY: Float = 0f,
    val isGenerating: Boolean = false
)

class BreakingNewsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(BreakingNewsState())
    val state: StateFlow<BreakingNewsState> = _state.asStateFlow()

    private var debounceJob: Job? = null

    fun onImageSelected(uri: Uri?) {
        _state.update { it.copy(selectedImageUri = uri) }
        triggerGeneration()
    }

    fun onHeadlineChanged(text: String) {
        if (text.length <= 200) {
            _state.update { it.copy(breakingHeadline = text) }
            triggerGeneration()
        }
    }

    fun onNewsTextChanged(text: String) {
        if (text.length <= 400) {
            _state.update { it.copy(newsText = text) }
            triggerGeneration()
        }
    }

    fun onFontSizeChanged(value: Float) {
        _state.update { it.copy(fontSizeMultiplier = value) }
        triggerGeneration(debounce = true)
    }

    fun onNewsTextFontSizeChanged(value: Float) {
        _state.update { it.copy(newsTextFontSizeMultiplier = value) }
        triggerGeneration(debounce = true)
    }

    fun onLineGapChanged(value: Float) {
        _state.update { it.copy(lineGapMultiplier = value) }
        triggerGeneration(debounce = true)
    }

    fun onHeadlineAlignmentChanged() {
        _state.update {
            it.copy(headlineAlignment = if (it.headlineAlignment == "CENTER") "LEFT" else "CENTER")
        }
        triggerGeneration()
    }

    fun onResolutionChanged(resolution: String) {
        _state.update { it.copy(selectedResolution = resolution) }
        triggerGeneration()
    }

    fun onDragThumbnailText(dragAmountX: Float, dragAmountY: Float, previewSize: androidx.compose.ui.geometry.Size) {
        val currentThumbnail = _state.value.thumbnailBitmap ?: return

        val scaleX = currentThumbnail.width.toFloat() / previewSize.width
        val scaleY = currentThumbnail.height.toFloat() / previewSize.height

        _state.update {
            it.copy(
                headlineTextOffsetX = it.headlineTextOffsetX + dragAmountX * scaleX,
                headlineTextOffsetY = it.headlineTextOffsetY + dragAmountY * scaleY
            )
        }
        triggerGeneration(debounce = false) // Real-time update for drag might be heavy, but let's try
    }

    fun generatePost() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            if (currentState.selectedImageUri != null && (currentState.breakingHeadline.isNotEmpty() || currentState.newsText.isNotEmpty())) {
                _state.update { it.copy(isGenerating = true, thumbnailBitmap = null) } // Clear thumbnail

                val result = ImageGenerator.generateBreakingNewsImage(
                    getApplication(),
                    currentState.selectedImageUri,
                    currentState.breakingHeadline,
                    currentState.newsText,
                    currentState.fontSizeMultiplier,
                    currentState.newsTextFontSizeMultiplier,
                    currentState.lineGapMultiplier,
                    currentState.headlineAlignment
                )

                _state.update { it.copy(generatedBitmap = result, isGenerating = false) }
            }
        }
    }

    fun generateThumbnail() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            if (currentState.selectedImageUri != null && currentState.breakingHeadline.isNotEmpty()) {
                _state.update { it.copy(isGenerating = true, generatedBitmap = null) } // Clear generated image

                val resolutionResId = getResolutionResId(currentState.selectedResolution)

                val result = ImageGenerator.generateThumbnail(
                    getApplication(),
                    currentState.selectedImageUri,
                    currentState.breakingHeadline,
                    currentState.overlayResId,
                    currentState.fontSizeMultiplier,
                    currentState.lineGapMultiplier,
                    resolutionResId,
                    currentState.resolutionImagePaddingBottom,
                    currentState.resolutionImagePaddingSide,
                    currentState.headlineTextOffsetX,
                    currentState.headlineTextOffsetY,
                    currentState.headlineAlignment
                )

                _state.update { it.copy(thumbnailBitmap = result, isGenerating = false) }
            }
        }
    }

    private fun triggerGeneration(debounce: Boolean = false) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch(Dispatchers.IO) {
            if (debounce) delay(300)

            val currentState = _state.value

            // Prioritize what to generate based on what was last shown or modified?
            // For now, if thumbnail exists, update thumbnail. If generatedBitmap exists, update that.
            // If neither, update generatedBitmap if possible.

            if (currentState.thumbnailBitmap != null && currentState.selectedImageUri != null && currentState.breakingHeadline.isNotEmpty()) {
                val resolutionResId = getResolutionResId(currentState.selectedResolution)
                val result = ImageGenerator.generateThumbnail(
                    getApplication(),
                    currentState.selectedImageUri,
                    currentState.breakingHeadline,
                    currentState.overlayResId,
                    currentState.fontSizeMultiplier,
                    currentState.lineGapMultiplier,
                    resolutionResId,
                    currentState.resolutionImagePaddingBottom,
                    currentState.resolutionImagePaddingSide,
                    currentState.headlineTextOffsetX,
                    currentState.headlineTextOffsetY,
                    currentState.headlineAlignment
                )
                _state.update { it.copy(thumbnailBitmap = result) }
            } else if ((currentState.generatedBitmap != null || currentState.thumbnailBitmap == null) &&
                       currentState.selectedImageUri != null &&
                       (currentState.breakingHeadline.isNotEmpty() || currentState.newsText.isNotEmpty())) {
                val result = ImageGenerator.generateBreakingNewsImage(
                    getApplication(),
                    currentState.selectedImageUri,
                    currentState.breakingHeadline,
                    currentState.newsText,
                    currentState.fontSizeMultiplier,
                    currentState.newsTextFontSizeMultiplier,
                    currentState.lineGapMultiplier,
                    currentState.headlineAlignment
                )
                _state.update { it.copy(generatedBitmap = result) }
            }
        }
    }

    private fun getResolutionResId(resolution: String): Int? {
        return when (resolution) {
            "FULLHD" -> R.drawable.fullhd
            "2K" -> R.drawable.twok
            "4K" -> R.drawable.fourk
            "FILM1080" -> R.drawable.film1080
            "FILM2K" -> R.drawable.film2k
            "FILM4K" -> R.drawable.film4k
            else -> null
        }
    }

    fun saveImage(context: android.content.Context) {
        val currentState = _state.value
        if (currentState.thumbnailBitmap != null) {
            ImageGenerator.saveThumbnailToGallery(context, currentState.thumbnailBitmap)
        } else if (currentState.generatedBitmap != null) {
            ImageGenerator.saveToGallery(context, currentState.generatedBitmap)
        }
    }

    fun shareImage(context: android.content.Context, platform: String? = null) {
        val currentState = _state.value
        val bitmap = currentState.thumbnailBitmap ?: currentState.generatedBitmap ?: return

        if (platform != null) {
            ImageGenerator.shareToSocialMedia(context, bitmap, platform)
        } else {
            ImageGenerator.saveAndShareImage(context, bitmap)
        }
    }
}
