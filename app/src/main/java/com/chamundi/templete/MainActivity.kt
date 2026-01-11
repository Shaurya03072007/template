package com.chamundi.templete

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.chamundi.templete.ui.theme.TempleteTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Configuration object for all customizable values
object AppConfig {
    // Breaking News Image Configuration
    object BreakingNews {
        // Image settings
        const val IMAGE_CROP_RATIO = 20f / 9f // 20:9 aspect ratio
        const val IMAGE_SIZE_REDUCTION = 0.95f // 5% size reduction
        const val IMAGE_BORDER_WIDTH = 4f
        
        // Headline settings
        const val HEADLINE_FONT_MIN_SIZE = 20f
        const val HEADLINE_FONT_MAX_SIZE = 300f
        @JvmStatic val HEADLINE_BACKGROUND_COLOR = Color.rgb(220, 20, 60) // Red rose (crimson)
        @JvmStatic val HEADLINE_TEXT_COLOR = Color.WHITE
        @JvmStatic val HEADLINE_BORDER_COLOR = Color.BLACK
        const val HEADLINE_BORDER_WIDTH = 3f
        const val HEADLINE_BACKGROUND_PADDING_H = 8f
        const val HEADLINE_HEIGHT_REDUCTION = 0.30f // 30% height reduction
        const val HEADLINE_CORNER_RADIUS = 15f
        const val HEADLINE_SHADOW_OFFSET_X = 20f
        const val HEADLINE_SHADOW_OFFSET_Y = 20f
        @JvmStatic val HEADLINE_SHADOW_COLOR = Color.argb(220, 0, 0, 0) // Semi-transparent black
        
        // News text settings
        const val NEWS_TEXT_FONT_MIN_SIZE = 15f
        const val NEWS_TEXT_FONT_MAX_SIZE = 80f
        @JvmStatic val NEWS_TEXT_COLOR = Color.WHITE
        @JvmStatic val NEWS_TEXT_SHADOW_COLOR = Color.BLACK
        const val NEWS_TEXT_SHADOW_OFFSET = 3f
        const val NEWS_TEXT_LINE_SPACING_GAP = 8f
        
        // Spacing and margins
        const val TEXT_PADDING = 40f
        const val MARGIN_FROM_IMAGE = 0.5f
        const val MARGIN_BETWEEN_HEADLINE_AND_NEWS = 10f
        const val BOTTOM_MARGIN = 100f
        
        // Image enhancement
        const val SATURATION_MULTIPLIER = 1.2f
        const val CONTRAST_MULTIPLIER = 1.15f
        const val BRIGHTNESS_OFFSET = 10f
    }
    
    // Thumbnail Configuration
    object Thumbnail {
        // Font settings
        const val HEADLINE_FONT_MIN_SIZE = 100f
        const val HEADLINE_FONT_MAX_SIZE = 300f
        const val STROKE_WIDTH_MULTIPLIER = 0.10f // Stroke width = fontSize * multiplier
        
        // Colors
        @JvmStatic val TEXT_COLOR = Color.WHITE
        @JvmStatic val STROKE_COLOR = Color.BLACK
        
        // Layout settings
        const val AVAILABLE_HEIGHT_PERCENT = 0.8f // 80% of thumbnail height
        const val HORIZONTAL_PADDING = 20f // Left margin for text area
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle incoming share intent
        val sharedLink = handleIncomingShare()
        
        setContent {
            TempleteTheme {
                BreakingNewsApp(initialSharedLink = sharedLink)
            }
        }
    }
    
    private fun handleIncomingShare(): String? {
        return if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                android.util.Log.d("MainActivity", "Received shared link: $sharedText")
                Toast.makeText(this, "Link received!", Toast.LENGTH_SHORT).show()
                sharedText
            } else {
                null
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type == "text/plain") {
            // Handle multiple text shares (take first one)
            val sharedTexts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
            if (sharedTexts != null && sharedTexts.isNotEmpty()) {
                val firstText = sharedTexts[0]
                android.util.Log.d("MainActivity", "Received shared link (multiple): $firstText")
                Toast.makeText(this, "Link received!", Toast.LENGTH_SHORT).show()
                firstText
            } else {
                null
            }
        } else {
            null
        }
    }
}

fun breakTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    val words = text.split(" ")
    var currentLine = ""
    
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val width = paint.measureText(testLine)
        
        if (width <= maxWidth) {
            currentLine = testLine
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            // If a single word is too long, break it
            if (paint.measureText(word) > maxWidth) {
                var remainingWord = word
                while (remainingWord.isNotEmpty()) {
                    var charCount = remainingWord.length
                    while (charCount > 0 && paint.measureText(remainingWord.substring(0, charCount)) > maxWidth) {
                        charCount--
                    }
                    if (charCount == 0) charCount = 1 // At least one character
                    lines.add(remainingWord.substring(0, charCount))
                    remainingWord = remainingWord.substring(charCount)
                }
                currentLine = ""
            } else {
                currentLine = word
            }
        }
    }
    
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }
    
    return lines
}

fun calculateOptimalFontSize(text: String, maxWidth: Float, typeface: Typeface, minSize: Float = 20f, maxSize: Float = 200f): Float {
    val paint = Paint().apply {
        this.typeface = typeface
        isAntiAlias = true
    }
    
    // Binary search for optimal font size
    var low = minSize
    var high = maxSize
    var optimalSize = minSize
    
    while (low <= high) {
        val mid = (low + high) / 2f
        paint.textSize = mid
        val textWidth = paint.measureText(text)
        
        if (textWidth <= maxWidth) {
            optimalSize = mid
            low = mid + 1f // Try larger size
                        } else {
            high = mid - 1f // Try smaller size
        }
    }
    
    return optimalSize
}

fun calculateOptimalFontSizeForMultiLine(
    text: String,
    maxWidth: Float,
    maxHeight: Float,
    typeface: Typeface,
    minSize: Float = 15f,
    maxSize: Float = 100f
): Float {
    val paint = Paint().apply {
        this.typeface = typeface
        isAntiAlias = true
        textAlign = Paint.Align.LEFT // LEFT for measurement
    }
    
    // Binary search for optimal font size that fits both width and height
    var low = minSize
    var high = maxSize
    var optimalSize = minSize
    
    while (low <= high) {
        val mid = (low + high) / 2f
        paint.textSize = mid
        
        // Break text into lines
        val lines = breakTextIntoLines(text, paint, maxWidth)
        
        // Calculate total height needed
        val lineSpacing = paint.descent() - paint.ascent() + 8f
        val totalHeight = lines.size * lineSpacing
        
        // Check if it fits
        if (totalHeight <= maxHeight && lines.isNotEmpty()) {
            optimalSize = mid
            low = mid + 0.5f // Try slightly larger size
                    } else {
            high = mid - 0.5f // Try smaller size
        }
    }
    
    return optimalSize
}

fun drawJustifiedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
    val words = text.trim().split("\\s+".toRegex())
    if (words.isEmpty()) return
    
    // Measure the text as it is (with normal spaces)
    val textWidth = paint.measureText(text)
    
    // If text fits naturally or is a single word, just draw it left-aligned
    if (words.size == 1 || textWidth >= maxWidth * 0.95f) {
        canvas.drawText(text, x, y, paint)
        return
    }
    
    // Calculate total width of words without spaces
    val wordsWidth = words.sumOf { paint.measureText(it).toDouble() }.toFloat()
    val spacesCount = words.size - 1
    if (spacesCount == 0) {
        canvas.drawText(text, x, y, paint)
        return
    }
    
    // Use natural text width + small percentage to maintain uniform gap
    // This keeps spacing more uniform instead of stretching across full width
    val naturalSpaceWidth = paint.measureText(" ") // Measure a normal space
    val naturalTotalSpacesWidth = naturalSpaceWidth * spacesCount
    val naturalTextWidth = wordsWidth + naturalTotalSpacesWidth
    
    // Only justify slightly beyond natural width (max 5% more) to keep uniform spacing
    val justifiedWidth = minOf(maxWidth, naturalTextWidth * 1.05f)
    val totalSpacesWidth = justifiedWidth - wordsWidth
    val spaceWidth = totalSpacesWidth / spacesCount
    
    // Draw words with uniform justified spacing
    var currentX = x
    for (i in words.indices) {
        canvas.drawText(words[i], currentX, y, paint)
        currentX += paint.measureText(words[i])
        if (i < words.size - 1) {
            currentX += spaceWidth
        }
    }
}

fun drawCenterJustifiedText(canvas: Canvas, text: String, centerX: Float, y: Float, maxWidth: Float, paint: Paint) {
    val words = text.trim().split("\\s+".toRegex())
    if (words.isEmpty()) return
    
    // Measure the text as it is (with normal spaces)
    val textWidth = paint.measureText(text)
    
    // If text fits naturally or is a single word, just draw it centered
    if (words.size == 1 || textWidth >= maxWidth * 0.95f) {
        canvas.drawText(text, centerX, y, paint)
        return
    }
    
    // Calculate total width of words without spaces
    val wordsWidth = words.sumOf { paint.measureText(it).toDouble() }.toFloat()
    val spacesCount = words.size - 1
    if (spacesCount == 0) {
        canvas.drawText(text, centerX, y, paint)
        return
    }
    
    // Use natural text width + small percentage to maintain uniform gap
    val naturalSpaceWidth = paint.measureText(" ") // Measure a normal space
    val naturalTotalSpacesWidth = naturalSpaceWidth * spacesCount
    val naturalTextWidth = wordsWidth + naturalTotalSpacesWidth
    
    // Only justify slightly beyond natural width (max 5% more) to keep uniform spacing
    val justifiedWidth = minOf(maxWidth, naturalTextWidth * 1.05f)
    val totalSpacesWidth = justifiedWidth - wordsWidth
    val spaceWidth = totalSpacesWidth / spacesCount
    
    // Calculate starting X position to center the justified text
    val startX = centerX - justifiedWidth / 2f
    
    // Draw words with uniform justified spacing, centered
    var currentX = startX
    for (i in words.indices) {
        canvas.drawText(words[i], currentX, y, paint)
        currentX += paint.measureText(words[i])
        if (i < words.size - 1) {
            currentX += spaceWidth
        }
    }
}

fun cropToRatio(bitmap: Bitmap, targetRatio: Float): Bitmap {
    val originalWidth = bitmap.width.toFloat()
    val originalHeight = bitmap.height.toFloat()
    val originalRatio = originalWidth / originalHeight
    
    var cropWidth = originalWidth
    var cropHeight = originalHeight
    var cropX = 0f
    var cropY = 0f
    
    // Crop 5% from bottom
    val bottomCropPercent = 0.05f
    val bottomCropAmount = originalHeight * bottomCropPercent
    val adjustedHeight = originalHeight - bottomCropAmount
    val adjustedRatio = originalWidth / adjustedHeight
    
    if (adjustedRatio > targetRatio) {
        // Image is wider than target ratio, crop width (from sides, center horizontally)
        cropWidth = adjustedHeight * targetRatio
        cropX = (originalWidth - cropWidth) / 2f
        cropHeight = adjustedHeight
        cropY = 0f // Start from top, bottom 5% already removed
    } else {
        // Image is taller than target ratio, crop height (from top, keep bottom)
        cropHeight = originalWidth / targetRatio
        // Position from top, but ensure we don't go below the 5% bottom crop
        val maxCropY = originalHeight - bottomCropAmount - cropHeight
        cropY = maxOf(0f, maxCropY) // Start from top, but respect bottom 5% crop
        cropWidth = originalWidth
    }
    
    // Create cropped bitmap (5% from bottom removed, then fit to ratio)
    return Bitmap.createBitmap(
        bitmap,
        cropX.toInt(),
        cropY.toInt(),
        cropWidth.toInt(),
        cropHeight.toInt()
    )
}

fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    
    // Calculate scaling factor to fit within bounds while maintaining aspect ratio
    val scaleX = maxWidth.toFloat() / originalWidth
    val scaleY = maxHeight.toFloat() / originalHeight
    val scale = minOf(scaleX, scaleY)
    
    val scaledWidth = (originalWidth * scale).toInt()
    val scaledHeight = (originalHeight * scale).toInt()
    
    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

fun loadCustomFont(context: android.content.Context): Typeface? {
    return try {
        Typeface.createFromAsset(context.assets, "fonts/Ramabhadra-Regular.ttf")
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error loading custom font: ${e.message}", e)
        null
    }
}

fun enhanceImageQuality(bitmap: Bitmap): Bitmap {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        
        // Apply contrast, brightness, and saturation enhancement using ColorMatrix
        val saturationMatrix = android.graphics.ColorMatrix().apply {
            setSaturation(AppConfig.BreakingNews.SATURATION_MULTIPLIER) // Increase saturation for more vibrant colors
        }
        val contrastBrightnessMatrix = android.graphics.ColorMatrix().apply {
            val contrast = AppConfig.BreakingNews.CONTRAST_MULTIPLIER
            val brightness = AppConfig.BreakingNews.BRIGHTNESS_OFFSET
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,  // Red channel
                0f, contrast, 0f, 0f, brightness,  // Green channel
                0f, 0f, contrast, 0f, brightness,  // Blue channel
                0f, 0f, 0f, 1f, 0f                 // Alpha channel unchanged
            ))
        }
        val colorMatrix = android.graphics.ColorMatrix().apply {
            postConcat(saturationMatrix)
            postConcat(contrastBrightnessMatrix)
        }
        
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            isAntiAlias = true
            isFilterBitmap = true
            isDither = false
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        enhancedBitmap
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error enhancing image: ${e.message}", e)
        bitmap // Return original if enhancement fails
    }
}

fun applyUnsharpMask(bitmap: Bitmap): Bitmap {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Simple sharpening kernel
        val kernel = arrayOf(
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(-1f, 5f, -1f),
            floatArrayOf(0f, -1f, 0f)
        )
        
        val sharpenedPixels = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val idx = (y + ky) * width + (x + kx)
                        val pixel = pixels[idx]
                        val weight = kernel[ky + 1][kx + 1]
                        
                        r += android.graphics.Color.red(pixel) * weight
                        g += android.graphics.Color.green(pixel) * weight
                        b += android.graphics.Color.blue(pixel) * weight
                    }
                }
                
                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)
                
                val alpha = android.graphics.Color.alpha(pixels[y * width + x])
                sharpenedPixels[y * width + x] = android.graphics.Color.argb(
                    alpha,
                    r.toInt(),
                    g.toInt(),
                    b.toInt()
                )
            }
        }
        
        // Copy edges
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    sharpenedPixels[y * width + x] = pixels[y * width + x]
                }
            }
        }
        
        val sharpenedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpenedBitmap.setPixels(sharpenedPixels, 0, width, 0, 0, width, height)
        sharpenedBitmap
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error applying unsharp mask: ${e.message}", e)
        bitmap
    }
}

fun generateBreakingNewsImage(
    context: android.content.Context, 
    imageUri: Uri, 
    headline: String, 
    newsText: String,
    headlineFontSizeMultiplier: Float = 1.0f,
    newsTextFontSizeMultiplier: Float = 1.0f,
    lineGapMultiplier: Float = 1.0f,
    headlineAlignment: String = "CENTER" // "CENTER" or "LEFT"
): Bitmap? {
    return try {
        // Load the selected image with high quality options
        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            android.util.Log.e("BreakingNews", "Failed to open input stream for URI: $imageUri")
            return null
        }
        
        // Use high quality decoding options
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888 // High quality color format
            inSampleSize = 1 // No downsampling
            inScaled = false // Don't scale during decode
            inDither = false // No dithering
            inJustDecodeBounds = false
        }
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()
        
        if (originalBitmap == null) {
            android.util.Log.e("BreakingNews", "Failed to decode bitmap from URI: $imageUri")
            // Create a fallback image with breaking.png background
            val fallbackBitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
            val fallbackCanvas = Canvas(fallbackBitmap)
            
            // Load and draw breaking.png as background
            try {
                val backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.breaking)
                if (backgroundBitmap != null) {
                    val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, 1080, 1350, true)
                    fallbackCanvas.drawBitmap(scaledBackground, 0f, 0f, null)
                } else {
                    val fallbackPaint = Paint().apply {
                        color = Color.GRAY
                    }
                    fallbackCanvas.drawRect(0f, 0f, 1080f, 1350f, fallbackPaint)
                }
            } catch (e: Exception) {
                val fallbackPaint = Paint().apply {
                    color = Color.GRAY
                }
                fallbackCanvas.drawRect(0f, 0f, 1080f, 1350f, fallbackPaint)
            }
            return fallbackBitmap
        }
        
        android.util.Log.d("BreakingNews", "Original bitmap size: ${originalBitmap.width}x${originalBitmap.height}")

        // Enhance image quality automatically
        val enhancedBitmap = enhanceImageQuality(originalBitmap)
        if (enhancedBitmap != originalBitmap) {
            originalBitmap.recycle() // Free memory if new bitmap was created
        }
        android.util.Log.d("BreakingNews", "Image enhancement applied")

        // Create 1080x1350 output bitmap
        val outputBitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Load and draw breaking.png as background
        try {
            val backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.breaking)
            if (backgroundBitmap != null) {
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, 1080, 1350, true)
                canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                android.util.Log.d("BreakingNews", "Background image loaded successfully")
            } else {
                // Fallback to black if background not found
                val backgroundPaint = Paint().apply {
                    color = Color.BLACK
                }
                canvas.drawRect(0f, 0f, 1080f, 1350f, backgroundPaint)
            }
        } catch (e: Exception) {
            android.util.Log.e("BreakingNews", "Error loading background: ${e.message}")
            // Fallback to black
            val backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            canvas.drawRect(0f, 0f, 1080f, 1350f, backgroundPaint)
        }

        // Position image in top half, 10% from top with margins and black stroke
        val outputHeight = 1350f
        val topMargin = outputHeight * 0.15f // 10% from top = 135px
        val sideMargin = 20f // 20px margins on left and right
        val bottomMargin = 20f // 20px margin from the middle
        val topHalfHeight = outputHeight / 2f // Top half = 675px
        
        // Calculate available area for the image
        val availableWidth = 1080f - (sideMargin * 2f) // 1040px
        val availableHeight = topHalfHeight - topMargin - bottomMargin // 520px (675 - 135 - 20)
        
        // First crop the image to configured ratio from center
        val croppedBitmap = cropToRatio(enhancedBitmap, AppConfig.BreakingNews.IMAGE_CROP_RATIO)
        
        // Then scale the cropped image to fit within available area while maintaining ratio
        val scaledBitmap = scaleBitmapToFit(croppedBitmap, availableWidth.toInt(), availableHeight.toInt())
        
        // Reduce image size by configured percentage
        val sizeReduction = AppConfig.BreakingNews.IMAGE_SIZE_REDUCTION
        val finalWidth = (scaledBitmap.width * sizeReduction).toInt()
        val finalHeight = (scaledBitmap.height * sizeReduction).toInt()
        val finalScaledBitmap = Bitmap.createScaledBitmap(scaledBitmap, finalWidth, finalHeight, true)
        
        // Center horizontally, position at 10% from top
        val x = (1080f - finalScaledBitmap.width) / 2f
        val y = topMargin + (availableHeight - finalScaledBitmap.height) / 2f
        
        // Draw black stroke/border around the image
        val borderWidth = AppConfig.BreakingNews.IMAGE_BORDER_WIDTH
        val strokePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        
        // Draw the stroke rectangle
        canvas.drawRect(
            x - borderWidth / 2f,
            y - borderWidth / 2f,
            x + finalScaledBitmap.width + borderWidth / 2f,
            y + finalScaledBitmap.height + borderWidth / 2f,
            strokePaint
        )
        
        // Draw the scaled image (5% reduced size)
        canvas.drawBitmap(finalScaledBitmap, x, y, null)
        
        // Calculate image bottom position (including border)
        val imageBottom = y + finalScaledBitmap.height + (borderWidth / 2f)

        // Draw text in the bottom half with margin from image
        // We need to account for text ascent (which is negative) to prevent overlap
        // Load font first to calculate text metrics
        val tempFont = loadCustomFont(context) ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        // Use a reasonable estimate for font size (mid-range) for initial positioning
        // The actual font size will be calculated dynamically for the headline
        val tempPaint = Paint().apply {
            textSize = 100f  // Estimate for initial positioning calculation
            typeface = tempFont
        }
        val textAscentValue = Math.abs(tempPaint.ascent()) // Get absolute value of ascent (positive)
        val marginFromImage = AppConfig.BreakingNews.MARGIN_FROM_IMAGE
        // Position text so background top (currentY + textAscent) is marginFromImage below image
        // Since textAscent is negative: bgTop = currentY + textAscent = textStartY + textAscent
        // We want: bgTop >= imageBottom + marginFromImage
        // So: textStartY >= imageBottom + marginFromImage - textAscent
        // Since textAscent is negative, we add its absolute value
        val textStartY = imageBottom + marginFromImage + textAscentValue
        val textPadding = AppConfig.BreakingNews.TEXT_PADDING
        val maxTextWidth = 1080f - (textPadding * 2f)
        val centerX = 1080f / 2f // Center X position for center alignment
        var currentY = textStartY
        val shadowOffset = AppConfig.BreakingNews.NEWS_TEXT_SHADOW_OFFSET

        // Use the font we already loaded
        val customFont = tempFont
        android.util.Log.d("BreakingNews", "Custom font loaded: ${customFont != null}")

        try {
            // Draw headline if provided
            if (headline.isNotEmpty()) {
                android.util.Log.d("BreakingNews", "Drawing headline: $headline")
                
                // Calculate available height for headline (max 3 lines)
                val marginBetweenHeadlineAndNews = AppConfig.BreakingNews.MARGIN_BETWEEN_HEADLINE_AND_NEWS
                val bottomMargin = AppConfig.BreakingNews.BOTTOM_MARGIN
                val maxHeadlineHeight = outputHeight - imageBottom - marginFromImage - marginBetweenHeadlineAndNews - bottomMargin
                
                // Determine alignment first
                val headlineAlign = if (headlineAlignment == "LEFT") Paint.Align.LEFT else Paint.Align.CENTER
                
                // Create temporary paint for font size calculation (use LEFT for measurement)
                val tempPaint = Paint().apply {
                    textSize = AppConfig.BreakingNews.HEADLINE_FONT_MAX_SIZE
                    typeface = customFont
                    textAlign = Paint.Align.LEFT // Always use LEFT for measurement
                    isAntiAlias = true
                }
                
                // Calculate optimal font size to fit headline in max 3 lines
                val optimalFontSize = calculateOptimalFontSizeForMultiLine(
                    text = headline,
                    maxWidth = maxTextWidth,
                    maxHeight = maxHeadlineHeight,
                    typeface = customFont,
                    minSize = AppConfig.BreakingNews.HEADLINE_FONT_MIN_SIZE * headlineFontSizeMultiplier,
                    maxSize = AppConfig.BreakingNews.HEADLINE_FONT_MAX_SIZE * headlineFontSizeMultiplier
                ) * headlineFontSizeMultiplier
                android.util.Log.d("BreakingNews", "Optimal headline font size: $optimalFontSize")
                
                // Headline main paint (white color, no underline, no drop shadow)
                val headlinePaint = Paint().apply {
                    color = AppConfig.BreakingNews.HEADLINE_TEXT_COLOR
                    textSize = optimalFontSize
                    isAntiAlias = true
                    typeface = customFont
                    textAlign = headlineAlign
                    isUnderlineText = false
                    style = Paint.Style.FILL
                }
                
                // Break headline into lines (max 3 lines) - use LEFT alignment for measurement
                val measurePaint = Paint().apply {
                    textSize = optimalFontSize
                    typeface = customFont
                    textAlign = Paint.Align.LEFT // Always LEFT for measurement
                    isAntiAlias = true
                }
                val headlineLines = breakTextIntoLines(headline, measurePaint, maxTextWidth)
                val displayLines = headlineLines.take(3) // Allow up to 3 lines
                
                // Background paint for headline (red rose color)
                val headlineBackgroundPaint = Paint().apply {
                    color = AppConfig.BreakingNews.HEADLINE_BACKGROUND_COLOR
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                
                // Stroke paint for black border
                val headlineStrokePaint = Paint().apply {
                    color = AppConfig.BreakingNews.HEADLINE_BORDER_COLOR
                    style = Paint.Style.STROKE
                    strokeWidth = AppConfig.BreakingNews.HEADLINE_BORDER_WIDTH
                    isAntiAlias = true
                }
                
                val backgroundPaddingHorizontal = AppConfig.BreakingNews.HEADLINE_BACKGROUND_PADDING_H
                val heightReduction = AppConfig.BreakingNews.HEADLINE_HEIGHT_REDUCTION
                val cornerRadius = AppConfig.BreakingNews.HEADLINE_CORNER_RADIUS
                
                // Calculate text dimensions for all lines
                val textAscent = headlinePaint.ascent()
                val textDescent = headlinePaint.descent()
                val lineHeight = textDescent - textAscent
                // Use reduced spacing to bring lines closer together
                // Reduce spacing to 50% of line height to make lines much closer
                val lineSpacing = lineHeight * 0.5f
                
                // Calculate total width (widest line) - use measurePaint for consistent measurement
                val maxLineWidth = displayLines.maxOfOrNull { measurePaint.measureText(it) } ?: 0f
                // Total height with reduced spacing between lines
                val totalTextHeight = if (displayLines.size > 1) {
                    lineHeight + (displayLines.size - 1) * lineSpacing
                } else {
                    lineHeight
                }
                
                // Calculate background rectangle bounds (single background for all lines)
                val bgLeft = if (headlineAlignment == "LEFT") {
                    textPadding
                } else {
                    centerX - maxLineWidth / 2f - backgroundPaddingHorizontal
                }
                val bgRight = if (headlineAlignment == "LEFT") {
                    // Ensure background doesn't exceed available width
                    minOf(textPadding + maxLineWidth + (backgroundPaddingHorizontal * 2f), 1080f - textPadding)
                } else {
                    centerX + maxLineWidth / 2f + backgroundPaddingHorizontal
                }
                val reducedHeight = totalTextHeight * (1f - heightReduction)
                val heightOffset = (totalTextHeight - reducedHeight) / 2f
                
                // Position headline to stay within bounds
                // First calculate background box position
                val initialY = imageBottom + marginFromImage - textAscent - heightOffset
                val bgTop = initialY + textAscent + heightOffset
                val bgBottom = initialY + textAscent + totalTextHeight - heightOffset
                
                // Ensure background doesn't go out of bounds
                val maxBgBottom = outputHeight - bottomMargin - marginBetweenHeadlineAndNews
                val finalBgTop: Float
                val finalBgBottom: Float
                
                if (bgBottom > maxBgBottom) {
                    val adjustment = bgBottom - maxBgBottom
                    finalBgTop = bgTop - adjustment
                    finalBgBottom = bgBottom - adjustment
                } else {
                    finalBgTop = bgTop
                    finalBgBottom = bgBottom
                }
                
                // Calculate text position to center it vertically in the box
                val boxCenter = (finalBgTop + finalBgBottom) / 2f
                // Center the text block within the box
                // Text block center = lineY + textAscent + totalTextHeight / 2
                // We want: lineY + textAscent + totalTextHeight / 2 = boxCenter
                // So: lineY = boxCenter - textAscent - totalTextHeight / 2
                val centeredLineY = boxCenter - textAscent - totalTextHeight / 2f
                
                // Draw drop shadow for headline box (offset to bottom-left)
                val shadowOffsetX = AppConfig.BreakingNews.HEADLINE_SHADOW_OFFSET_X
                val shadowOffsetY = AppConfig.BreakingNews.HEADLINE_SHADOW_OFFSET_Y
                val shadowPaint = Paint().apply {
                    color = AppConfig.BreakingNews.HEADLINE_SHADOW_COLOR
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(
                    bgLeft + shadowOffsetX,
                    finalBgTop + shadowOffsetY,
                    bgRight + shadowOffsetX,
                    finalBgBottom + shadowOffsetY,
                    cornerRadius,
                    cornerRadius,
                    shadowPaint
                )
                
                // Draw single rounded rectangle background (red rose fill) for all lines
                canvas.drawRoundRect(
                    bgLeft,
                    finalBgTop,
                    bgRight,
                    finalBgBottom,
                    cornerRadius,
                    cornerRadius,
                    headlineBackgroundPaint
                )
                
                // Draw black stroke border
                canvas.drawRoundRect(
                    bgLeft,
                    finalBgTop,
                    bgRight,
                    finalBgBottom,
                    cornerRadius,
                    cornerRadius,
                    headlineStrokePaint
                )
                
                // Draw text lines (max 3 lines) - centered vertically in box
                var lineY = centeredLineY
                val textX = if (headlineAlignment == "LEFT") {
                    // For LEFT alignment, text starts at left edge of background box
                    bgLeft + backgroundPaddingHorizontal
                } else {
                    // For CENTER alignment, text is centered
                    centerX
                }
                android.util.Log.d("BreakingNews", "Headline alignment: $headlineAlignment, textX: $textX, bgLeft: $bgLeft, bgRight: $bgRight")
                displayLines.forEach { line ->
                    canvas.drawText(line, textX, lineY, headlinePaint)
                    lineY += lineSpacing
                }
                
                currentY = finalBgBottom
            }

            // Draw news text if provided
            if (newsText.isNotEmpty()) {
                android.util.Log.d("BreakingNews", "Drawing news text: $newsText")
                
                // Calculate available height for news text
                val marginBetweenHeadlineAndNews = AppConfig.BreakingNews.MARGIN_BETWEEN_HEADLINE_AND_NEWS
                val bottomMargin = AppConfig.BreakingNews.BOTTOM_MARGIN
                val availableHeight = outputHeight - currentY - marginBetweenHeadlineAndNews - bottomMargin
                
                // Calculate optimal font size to fit in available area
                val optimalNewsFontSize = calculateOptimalFontSizeForMultiLine(
                    text = newsText,
                    maxWidth = maxTextWidth,
                    maxHeight = availableHeight,
                    typeface = customFont,
                    minSize = AppConfig.BreakingNews.NEWS_TEXT_FONT_MIN_SIZE * newsTextFontSizeMultiplier,
                    maxSize = AppConfig.BreakingNews.NEWS_TEXT_FONT_MAX_SIZE * newsTextFontSizeMultiplier
                ) * newsTextFontSizeMultiplier
                android.util.Log.d("BreakingNews", "Optimal news text font size: $optimalNewsFontSize")
                
                // News text shadow paint (black drop shadow) - CENTER align for center-justified text
                val newsShadowPaint = Paint().apply {
                    color = AppConfig.BreakingNews.NEWS_TEXT_SHADOW_COLOR
                    textSize = optimalNewsFontSize
                    isAntiAlias = true
                    typeface = customFont
                    textAlign = Paint.Align.CENTER // CENTER for center-justified text
                    style = Paint.Style.FILL
                }
                
                // News text main paint (white color) - CENTER align for center-justified text
                val newsPaint = Paint().apply {
                    color = AppConfig.BreakingNews.NEWS_TEXT_COLOR
                    textSize = optimalNewsFontSize
                    isAntiAlias = true
                    typeface = customFont
                    textAlign = Paint.Align.CENTER // CENTER for center-justified text
                    style = Paint.Style.FILL
                }
                
                // Calculate proper position for news text to avoid overlap
                // Account for text ascent (negative value) and add small margin
                val newsTextAscent = Math.abs(newsPaint.ascent()) // Get absolute value (positive)
                // Position news text baseline so its top is margin below headline box bottom
                currentY = currentY + marginBetweenHeadlineAndNews + newsTextAscent
                
                // Break news text into multiple lines if needed
                // Use LEFT alignment for measurement (alignment doesn't affect measurement, but we need consistent measurement)
                val measurePaint = Paint().apply {
                    textSize = optimalNewsFontSize
                    typeface = customFont
                    textAlign = Paint.Align.LEFT // LEFT for measurement
                }
                val newsLines = breakTextIntoLines(newsText, measurePaint, maxTextWidth)
                android.util.Log.d("BreakingNews", "News text lines: ${newsLines.size}")
                
                // Calculate proper line spacing based on text metrics
                val lineSpacing = (newsPaint.descent() - newsPaint.ascent() + AppConfig.BreakingNews.NEWS_TEXT_LINE_SPACING_GAP) * lineGapMultiplier
                
                for (line in newsLines) {
                    if (currentY > outputHeight - bottomMargin) break // Don't draw beyond bottom margin
                    if (line.isNotEmpty()) {
                        // All lines: centered only (simple center alignment)
                        // Draw black drop shadow first (offset down and right)
                        canvas.drawText(line, centerX + shadowOffset, currentY + shadowOffset, newsShadowPaint)
                        // Draw white text on top
                        canvas.drawText(line, centerX, currentY, newsPaint)
                        currentY += lineSpacing // Proper line spacing
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BreakingNews", "Error drawing text: ${e.message}", e)
            e.printStackTrace()
        }

        android.util.Log.d("BreakingNews", "Image generation completed successfully")
        outputBitmap
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error generating image: ${e.message}", e)
        e.printStackTrace()
        null
    }
}

fun generateTestImage(context: android.content.Context, newsText: String): Bitmap? {
    return try {
        // Create output bitmap with breaking.png background
        val outputBitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        
        // Load and draw breaking.png as background
        try {
            val backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.breaking)
            if (backgroundBitmap != null) {
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, 1080, 1350, true)
                canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                android.util.Log.d("BreakingNews", "Background image loaded successfully")
            } else {
                // Fallback to gray if background not found
                val paint = Paint().apply {
                    color = Color.DKGRAY
                }
                canvas.drawRect(0f, 0f, 1080f, 1350f, paint)
            }
        } catch (e: Exception) {
            android.util.Log.e("BreakingNews", "Error loading background: ${e.message}")
            // Fallback to gray
            val paint = Paint().apply {
                color = Color.DKGRAY
            }
            canvas.drawRect(0f, 0f, 1080f, 1350f, paint)
        }
        
        android.util.Log.d("BreakingNews", "Test image generated successfully")
        outputBitmap
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error generating test image: ${e.message}", e)
        null
    }
}

@Composable
fun FullScreenImageView(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Full Screen Generated Image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun getImageFileName(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    val dateTime = dateFormat.format(Date())
    return "BreakingNews_$dateTime.jpg"
}

fun saveToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        val filename = getImageFileName()
        android.util.Log.d("BreakingNews", "Attempting to save image: $filename")
        
        var saved = false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+) - Use MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BreakingNews")
                // Mark as pending initially (Android 10+)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.flush()
                        android.util.Log.d("BreakingNews", "Image compressed: $compressed, URI: $uri")
                        
                        if (compressed) {
                            // Mark as not pending to make it visible in gallery
                            val updateValues = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            context.contentResolver.update(uri, updateValues, null, null)
                            
                            // Verify the file exists
                            val filePath = uri.toString()
                            android.util.Log.d("BreakingNews", "Image saved successfully. URI: $filePath")
                            saved = true
                            Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            android.util.Log.e("BreakingNews", "Failed to compress image")
                        }
            } ?: run {
                        android.util.Log.e("BreakingNews", "Failed to open output stream for URI: $uri")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BreakingNews", "Error writing image: ${e.message}", e)
                    // Delete the entry if writing failed
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (deleteEx: Exception) {
                        android.util.Log.e("BreakingNews", "Error deleting failed entry: ${deleteEx.message}")
                    }
            }
        } else {
                android.util.Log.e("BreakingNews", "Failed to create URI for image")
            }
        } else {
            // For older Android versions (API < 29)
            try {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (imagesDir == null || !imagesDir.exists()) {
                    android.util.Log.e("BreakingNews", "Pictures directory not available")
                    return false
                }
                
            val breakingNewsDir = File(imagesDir, "BreakingNews")
            if (!breakingNewsDir.exists()) {
                    val created = breakingNewsDir.mkdirs()
                    android.util.Log.d("BreakingNews", "Directory created: $created, Path: ${breakingNewsDir.absolutePath}")
                    if (!created) {
                        return false
                    }
            }
            
            val file = File(breakingNewsDir, filename)
                FileOutputStream(file).use { outputStream ->
                    val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
                    android.util.Log.d("BreakingNews", "Image saved: $compressed, Path: ${file.absolutePath}, Size: ${file.length()} bytes")
            
                    if (compressed && file.exists() && file.length() > 0) {
            // Notify gallery about the new image
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            context.sendBroadcast(intent)
                        saved = true
                        android.util.Log.d("BreakingNews", "Image saved successfully: $filename")
                        Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        android.util.Log.e("BreakingNews", "File not created or empty. Exists: ${file.exists()}, Size: ${file.length()}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BreakingNews", "Error saving to external storage: ${e.message}", e)
                e.printStackTrace()
            }
        }
        
        saved
    } catch (e: Exception) {
        android.util.Log.e("BreakingNews", "Error saving to gallery: ${e.message}", e)
        e.printStackTrace()
        false
    }
}

fun saveAndShareImage(context: android.content.Context, bitmap: Bitmap) {
    try {
        // Save to internal storage
        val filename = getImageFileName()
        val file = File(context.getExternalFilesDir(null), filename)
        
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        // Create share intent
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
        
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun shareToSocialMedia(context: android.content.Context, bitmap: Bitmap, platform: String) {
    try {
        // Save to internal storage
        val filename = getImageFileName()
        val file = File(context.getExternalFilesDir(null), filename)
        
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        // Create share intent
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Set specific package based on platform
        when (platform) {
            "YouTube" -> {
                // YouTube doesn't support direct image sharing, show chooser instead
                shareIntent.setPackage(null) // Remove package restriction
                context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
                return
            }
            "Facebook" -> shareIntent.setPackage("com.facebook.katana")
            "Instagram" -> shareIntent.setPackage("com.instagram.android")
            "Twitter" -> {
                // Try Twitter first, then X (new name)
                try {
                    shareIntent.setPackage("com.twitter.android")
                    context.startActivity(shareIntent)
                    return
                } catch (e: Exception) {
                    try {
                        shareIntent.setPackage("com.twitter.x")
                        context.startActivity(shareIntent)
                        return
                    } catch (e2: Exception) {
                        // Will fall through to chooser
                    }
                }
            }
            "WhatsApp" -> shareIntent.setPackage("com.whatsapp")
        }

        try {
            if (platform != "Twitter") {
                context.startActivity(shareIntent)
            } else {
                // For Twitter, if both failed, show chooser
                shareIntent.setPackage(null) // Remove package restriction
                context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
            }
        } catch (e: Exception) {
            // If specific app is not installed, show chooser
            shareIntent.setPackage(null) // Remove package restriction
            context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun generateThumbnail(
    context: android.content.Context,
    baseImageUri: Uri,
    headlineText: String,
    overlayResId: Int,
    fontSizeMultiplier: Float = 1.0f,
    lineGapMultiplier: Float = 1.0f,
    resolutionResId: Int? = null,
    resolutionImagePaddingBottom: Float = 20f,
    resolutionImagePaddingSide: Float = 150f,
    textOffsetX: Float = 0f,
    textOffsetY: Float = 0f,
    headlineAlignment: String = "CENTER" // "CENTER" or "LEFT"
): Bitmap? {
    return try {
        // Load the base image
        val inputStream = context.contentResolver.openInputStream(baseImageUri)
        if (inputStream == null) {
            android.util.Log.e("Thumbnail", "Failed to open input stream for URI: $baseImageUri")
            return null
        }
        
        val baseBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        if (baseBitmap == null) {
            android.util.Log.e("Thumbnail", "Failed to decode base bitmap")
            return null
        }
        
        // Create 1280x720 output bitmap
        val thumbnailBitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(thumbnailBitmap)
        
        // Prepare base image to fit right side area (830x720)
        val imageTargetWidth = 830
        val imageTargetHeight = 720
        val baseWidth = baseBitmap.width
        val baseHeight = baseBitmap.height
        val targetRatio = imageTargetWidth.toFloat() / imageTargetHeight.toFloat()
        val baseRatio = baseWidth.toFloat() / baseHeight.toFloat()

        // Crop 5% from bottom first
        val bottomCropPercent = 0.05f
        val bottomCropAmount = (baseHeight * bottomCropPercent).toInt()
        val adjustedHeight = baseHeight - bottomCropAmount
        val adjustedRatio = baseWidth.toFloat() / adjustedHeight.toFloat()
        
        var workingBitmap = baseBitmap
        if (adjustedRatio > targetRatio) {
            // Crop width to match target ratio (center horizontally)
            val newWidth = (adjustedHeight * targetRatio).toInt()
            val xOffset = (baseWidth - newWidth) / 2
            workingBitmap = Bitmap.createBitmap(baseBitmap, xOffset, 0, newWidth, adjustedHeight)
        } else if (adjustedRatio < targetRatio) {
            // Crop height to match target ratio (from top, bottom 5% already removed)
            val newHeight = (baseWidth / targetRatio).toInt()
            // Ensure we don't exceed the adjusted height (after 5% bottom crop)
            val finalHeight = minOf(newHeight, adjustedHeight)
            val yOffset = maxOf(0, adjustedHeight - finalHeight)
            workingBitmap = Bitmap.createBitmap(baseBitmap, 0, yOffset, baseWidth, finalHeight)
        } else {
            // Already matches ratio, just remove bottom 5%
            workingBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseWidth, adjustedHeight)
        }

        val scaledBitmap = Bitmap.createScaledBitmap(workingBitmap, imageTargetWidth, imageTargetHeight, true)

        // Draw scaled base image on the right half
        val imageLeft = (1280 - imageTargetWidth).toFloat() // start at x = 450
        canvas.drawBitmap(scaledBitmap, imageLeft, 0f, null)
        
        // Load and overlay thumbnail.png
        try {
            val thumbnailOverlay = BitmapFactory.decodeResource(context.resources, overlayResId)
            if (thumbnailOverlay != null) {
                // Scale overlay to exactly 1280x720 if needed
                val overlayBitmap = if (thumbnailOverlay.width != 1280 || thumbnailOverlay.height != 720) {
                    Bitmap.createScaledBitmap(thumbnailOverlay, 1280, 720, true)
                } else {
                    thumbnailOverlay
                }
                canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("Thumbnail", "Error loading thumbnail overlay: ${e.message}", e)
        }
        
        // Load custom font
        val customFont = loadCustomFont(context) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Calculate optimal font size for headline text (left side, vertically centered)
        val leftAreaWidth = 1280f - imageTargetWidth
        val horizontalPadding = AppConfig.Thumbnail.HORIZONTAL_PADDING
        // Use full available width minus left margin
        val maxTextWidth = leftAreaWidth - horizontalPadding
        val optimalFontSize = calculateOptimalFontSize(
            text = headlineText,
            maxWidth = maxTextWidth,
            typeface = customFont,
            minSize = AppConfig.Thumbnail.HEADLINE_FONT_MIN_SIZE * fontSizeMultiplier,
            maxSize = AppConfig.Thumbnail.HEADLINE_FONT_MAX_SIZE * fontSizeMultiplier
        ) * fontSizeMultiplier
        

        // Determine alignment
        val thumbnailAlign = if (headlineAlignment == "LEFT") Paint.Align.LEFT else Paint.Align.CENTER
        
        // Create stroke and fill paints for bold headline text (no drop shadow)
        val strokePaint = Paint().apply {
            color = AppConfig.Thumbnail.STROKE_COLOR
            textSize = optimalFontSize
            isAntiAlias = true
            typeface = customFont
            textAlign = thumbnailAlign
            style = Paint.Style.STROKE
            strokeWidth = optimalFontSize * AppConfig.Thumbnail.STROKE_WIDTH_MULTIPLIER
        }

        val textPaint = Paint().apply {
            color = AppConfig.Thumbnail.TEXT_COLOR
            textSize = optimalFontSize
            isAntiAlias = true
            typeface = customFont
            textAlign = thumbnailAlign
            style = Paint.Style.FILL
        }
        
        // Create shadow paint for drop shadow effect
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            textSize = optimalFontSize
            isAntiAlias = true
            typeface = customFont
            textAlign = thumbnailAlign
            style = Paint.Style.FILL
            alpha = 800 // Semi-transparent black for shadow
        }
        
        // Prepare text lines - preserve user's exact line breaks
        val lines = headlineText.split('\n')
            .map { it.trim() }
        
        // If no lines, just draw empty text
        if (lines.isEmpty()) {
            canvas.drawText("", 0f, -textPaint.ascent(), textPaint)
        }

        val trimmedLines = lines

        val fontHeight = textPaint.descent() - textPaint.ascent()
        val lineCount = trimmedLines.count { it.isNotEmpty() }
        val availableHeight = 720f * AppConfig.Thumbnail.AVAILABLE_HEIGHT_PERCENT
        val totalTextHeight = maxOf(1, lineCount) * fontHeight
        
        // Base line spacing that increases with multiplier
        val baseLineSpacing = fontHeight * lineGapMultiplier
        
        // Calculate additional spacing if there's extra space available
        // This ensures gap always increases with multiplier
        val extraSpacing = if (lineCount > 1 && availableHeight > totalTextHeight) {
            // When text fits, distribute extra space proportionally to multiplier
            ((availableHeight - totalTextHeight) / (lineCount - 1)) * (lineGapMultiplier - 1.0f) * 0.5f
        } else {
            0f
        }

        var currentY = -textPaint.ascent() // start at top, no top margin
        // Calculate X position within the text area (accounting for left margin and touch offset)
        val textAreaWidth = leftAreaWidth - horizontalPadding
        val centerX = horizontalPadding + (textAreaWidth / 2f) + textOffsetX
        val leftX = horizontalPadding + textOffsetX // Left edge position
        val textX = if (headlineAlignment == "LEFT") leftX else centerX
        // Shadow offset for drop shadow effect
        val shadowOffsetX = 5f
        val shadowOffsetY = 5f
        
        android.util.Log.d("Thumbnail", "Headline alignment: $headlineAlignment, textX: $textX, centerX: $centerX, leftX: $leftX")
        
        trimmedLines.forEach { line ->
            if (line.isNotEmpty()) {
                // Draw text with alignment and touch-adjusted position
                val adjustedY = currentY + textOffsetY
                // Draw drop shadow first (offset position)
                canvas.drawText(line, textX + shadowOffsetX, adjustedY + shadowOffsetY, shadowPaint)
                // Draw stroke (black outline)
                canvas.drawText(line, textX, adjustedY, strokePaint)
                // Draw fill (white text)
                canvas.drawText(line, textX, adjustedY, textPaint)
            }
            // Total spacing increases with multiplier
            currentY += baseLineSpacing + extraSpacing
        }
        
        // Draw resolution image at top right corner if selected
        resolutionResId?.let { resId ->
            try {
                val resolutionBitmap = BitmapFactory.decodeResource(context.resources, resId)
                if (resolutionBitmap != null) {
                    // Scale resolution image to fit top right corner (e.g., 150x150)
                    val resolutionSize = 120f
                    val scaledResolution = Bitmap.createScaledBitmap(
                        resolutionBitmap, 
                        resolutionSize.toInt(), 
                        resolutionSize.toInt(), 
                        true
                    )
                    // Position at top right end (minimal padding)
                    val resolutionX = 1280f - resolutionSize
                    val resolutionY = 0f // Top edge, no top padding
                    
                    // Draw the resolution image
                    canvas.drawBitmap(scaledResolution, resolutionX, resolutionY, null)
                    
                    // Draw double black stroke/border around the resolution image
                    val outerBorderWidth = 4f
                    val innerBorderWidth = 2f
                    val gapBetweenBorders = 2f
                    
                    // Outer stroke
                    val outerStrokePaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = outerBorderWidth
                        isAntiAlias = true
                    }
                    val outerOffset = outerBorderWidth / 2f
                    canvas.drawRect(
                        resolutionX - outerOffset - gapBetweenBorders,
                        resolutionY - outerOffset - gapBetweenBorders,
                        resolutionX + resolutionSize + outerOffset + gapBetweenBorders,
                        resolutionY + resolutionSize + outerOffset + gapBetweenBorders,
                        outerStrokePaint
                    )
                    
                    // Inner stroke
                    val innerStrokePaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = innerBorderWidth
                        isAntiAlias = true
                    }
                    val innerOffset = innerBorderWidth / 2f
                    canvas.drawRect(
                        resolutionX - innerOffset,
                        resolutionY - innerOffset,
                        resolutionX + resolutionSize + innerOffset,
                        resolutionY + resolutionSize + innerOffset,
                        innerStrokePaint
                    )
                    
                    scaledResolution.recycle()
                }
            } catch (e: Exception) {
                android.util.Log.e("Thumbnail", "Error loading resolution image: ${e.message}", e)
            }
        }
        
        // Clean up
        if (scaledBitmap != workingBitmap) {
            scaledBitmap.recycle()
        }
        if (workingBitmap != baseBitmap) {
            workingBitmap.recycle()
        }
        baseBitmap.recycle()
        
        android.util.Log.d("Thumbnail", "Thumbnail generated successfully: 1280x720")
        thumbnailBitmap
    } catch (e: Exception) {
        android.util.Log.e("Thumbnail", "Error generating thumbnail: ${e.message}", e)
        e.printStackTrace()
        null
    }
}

fun saveThumbnailToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        val filename = getImageFileName().replace("BreakingNews_", "Thumbnail_")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BreakingNews")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    outputStream.flush()
                    
                    if (saved) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(uri, updateValues, null, null)
                        android.util.Log.d("Thumbnail", "Thumbnail saved successfully: $filename")
                        Toast.makeText(context, "Thumbnail saved successfully", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
        } else {
            // For older Android versions
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (imagesDir != null && imagesDir.exists()) {
                val breakingNewsDir = File(imagesDir, "BreakingNews")
                if (!breakingNewsDir.exists()) {
                    breakingNewsDir.mkdirs()
                }
                
                val file = File(breakingNewsDir, filename)
                FileOutputStream(file).use { outputStream ->
                    val saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    outputStream.flush()
                    
                    if (saved && file.exists()) {
                        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        intent.data = Uri.fromFile(file)
                        context.sendBroadcast(intent)
                        android.util.Log.d("Thumbnail", "Thumbnail saved successfully: $filename")
                        Toast.makeText(context, "Thumbnail saved successfully", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
        }
        false
    } catch (e: Exception) {
        android.util.Log.e("Thumbnail", "Error saving thumbnail: ${e.message}", e)
        e.printStackTrace()
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLinkScreen(
    onDismiss: () -> Unit,
    context: android.content.Context,
    initialLink: String? = null
) {
    // Use initialLink if provided, otherwise use default
    var youtubeLink by remember(initialLink) { 
        mutableStateOf(initialLink ?: "https://www.youtube.com/watch?v=B5MijkEMJfg") 
    }
    var newsDescription by remember { mutableStateOf("   నుండి ప్రత్యక్ష ప్రసారం...") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Update link if initialLink changes
    LaunchedEffect(initialLink) {
        if (initialLink != null) {
            youtubeLink = initialLink
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share YouTube Link") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                
                // YouTube Link Input
                OutlinedTextField(
                    value = youtubeLink,
                    onValueChange = { youtubeLink = it },
                    label = { Text("Enter YouTube Link") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    )
                )
                
                // News Description Input
                OutlinedTextField(
                    value = newsDescription,
                    onValueChange = { newsDescription = it },
                    label = { Text("News Description") },
                    placeholder = { Text("Enter news description...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
                
                // Link View (Display the link)
                if (youtubeLink.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Link Preview:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = youtubeLink,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Formatted Text Preview below link view
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Preview:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatShareText(youtubeLink, newsDescription),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
            // Share Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (youtubeLink.isNotEmpty()) {
                        shareYouTubeLink(context, youtubeLink, newsDescription)
                    } else {
                        Toast.makeText(context, "Please enter a YouTube link", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                enabled = youtubeLink.isNotEmpty()
            ) {
                Text("Share Link", fontSize = 16.sp)
            }
        }
    }
}

fun formatShareText(youtubeLink: String, newsDescription: String): String {
    return buildString {
        append(newsDescription)
        append("\n")
        append("బ్లూలింకు కొట్టి వీడియో చూడగలరు\n")
        append(youtubeLink)
        append("\n")
        append("క్రింది బ్లూలింకును నొక్కి LIKE/FOLLOW/SUBSCRIBE బటన్ నొక్కండి\n")
        append("Facebook:- ")
        append("https://www.facebook.com/CHAMUNDINEWS24X7\n")
        append("Youtube:- ")
        append("https://www.youtube.com/@CHAMUNDITV?sub_confirmation=1\n")
        append("Instagram:- ")
        append("https://www.instagram.com/chamunditvtelugu\n")
        append("VIEW CHANNEL నొక్కి,FOLLOW బటన్ నొక్కండి")
    }
}

fun shareYouTubeLink(context: android.content.Context, link: String, newsDescription: String = "") {
    try {
        val shareText = formatShareText(link, newsDescription)
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "YouTube Link")
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share YouTube Link").apply {
            // Ensure it opens in full screen
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // For Android 10+ (API 29+), ensure proper full screen behavior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
        }
        
        // If context is an Activity, start with proper flags
        if (context is android.app.Activity) {
            context.startActivity(chooserIntent)
        } else {
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    } catch (e: Exception) {
        android.util.Log.e("YouTubeLink", "Error sharing link: ${e.message}", e)
        Toast.makeText(context, "Failed to share link", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakingNewsApp(initialSharedLink: String? = null) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var breakingHeadline by remember { mutableStateOf("") }
    var newsText by remember { mutableStateOf("") }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var overlayResId by remember { mutableStateOf(R.drawable.thumbnail) }
    var selectedResolution by remember { mutableStateOf("NONE") }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showYouTubeLinkScreen by remember { mutableStateOf(initialSharedLink != null) }
    var initialLink = remember { initialSharedLink }
    
    // Font size and line gap adjustments
    var fontSizeMultiplier by remember { mutableStateOf(1.0f) } // 0.5 to 2.0 (50% to 200%) - Common for headline and thumbnail
    var newsTextFontSizeMultiplier by remember { mutableStateOf(1.0f) } // 0.5 to 2.0
    var lineGapMultiplier by remember { mutableStateOf(0.6f) } // -1.0 to 2.0, default 60%
    var headlineAlignment by remember { mutableStateOf("LEFT") } // "CENTER" or "LEFT", default LEFT
    var resolutionImagePaddingBottom by remember { mutableStateOf(20f) } // Bottom padding for resolution image (fixed)
    var resolutionImagePaddingSide by remember { mutableStateOf(220f) } // Side (right) padding for resolution image (fixed)
    var headlineTextOffsetX by remember { mutableStateOf(0f) } // Horizontal offset for headline text (adjustable by touch)
    var headlineTextOffsetY by remember { mutableStateOf(0f) } // Vertical offset for headline text (adjustable by touch)
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Auto-regenerate preview when line gap changes (with debounce)
    LaunchedEffect(lineGapMultiplier) {
        // Small delay to debounce rapid slider movements
        kotlinx.coroutines.delay(300)
        
        // Only auto-regenerate if there's already a preview
        if (generatedBitmap != null && selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())) {
            // Regenerate breaking news image
            generatedBitmap?.recycle()
            generatedBitmap = null
            
            val result = generateBreakingNewsImage(
                context,
                selectedImageUri!!,
                breakingHeadline,
                newsText,
                fontSizeMultiplier,
                newsTextFontSizeMultiplier,
                lineGapMultiplier,
                headlineAlignment
            )
            if (result != null) {
                generatedBitmap = result
            }
        } else if (thumbnailBitmap != null && selectedImageUri != null && breakingHeadline.isNotEmpty()) {
            // Regenerate thumbnail
            thumbnailBitmap?.recycle()
            thumbnailBitmap = null
            
            val resolutionResId = when (selectedResolution) {
                "FULLHD" -> R.drawable.fullhd
                "2K" -> R.drawable.twok
                "4K" -> R.drawable.fourk
                "FILM1080" -> R.drawable.film1080
                "FILM2K" -> R.drawable.film2k
                "FILM4K" -> R.drawable.film4k
                else -> null
            }
            
            val thumbnail = generateThumbnail(
                context,
                selectedImageUri!!,
                breakingHeadline,
                overlayResId,
                fontSizeMultiplier,
                lineGapMultiplier,
                resolutionResId,
                resolutionImagePaddingBottom,
                resolutionImagePaddingSide,
                headlineTextOffsetX,
                headlineTextOffsetY,
                headlineAlignment
            )
            if (thumbnail != null) {
                thumbnailBitmap = thumbnail
            }
        }
    }
    
    // Auto-regenerate preview when news text font size changes (with debounce)
    LaunchedEffect(newsTextFontSizeMultiplier) {
        // Small delay to debounce rapid slider movements
        kotlinx.coroutines.delay(300)
        
        // Only auto-regenerate breaking news image if there's already a preview
        // News text font size only affects breaking news images, not thumbnails
        if (generatedBitmap != null && selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())) {
            // Regenerate breaking news image
            generatedBitmap?.recycle()
            generatedBitmap = null
            
            val result = generateBreakingNewsImage(
                context,
                selectedImageUri!!,
                breakingHeadline,
                newsText,
                fontSizeMultiplier,
                newsTextFontSizeMultiplier,
                lineGapMultiplier,
                headlineAlignment
            )
            if (result != null) {
                generatedBitmap = result
            }
        }
    }
    
    // Auto-regenerate preview when headline/thumbnail font size changes (with debounce)
    LaunchedEffect(fontSizeMultiplier) {
        // Small delay to debounce rapid slider movements
        kotlinx.coroutines.delay(300)
        
        // Auto-regenerate if there's already a preview
        if (generatedBitmap != null && selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())) {
            // Regenerate breaking news image
            generatedBitmap?.recycle()
            generatedBitmap = null
            
            val result = generateBreakingNewsImage(
                context,
                selectedImageUri!!,
                breakingHeadline,
                newsText,
                fontSizeMultiplier,
                newsTextFontSizeMultiplier,
                lineGapMultiplier,
                headlineAlignment
            )
            if (result != null) {
                generatedBitmap = result
            }
        } else if (thumbnailBitmap != null && selectedImageUri != null && breakingHeadline.isNotEmpty()) {
            // Regenerate thumbnail
            thumbnailBitmap?.recycle()
            thumbnailBitmap = null
            
            val resolutionResId = when (selectedResolution) {
                "FULLHD" -> R.drawable.fullhd
                "2K" -> R.drawable.twok
                "4K" -> R.drawable.fourk
                "FILM1080" -> R.drawable.film1080
                "FILM2K" -> R.drawable.film2k
                "FILM4K" -> R.drawable.film4k
                else -> null
            }
            
            val thumbnail = generateThumbnail(
                context,
                selectedImageUri!!,
                breakingHeadline,
                overlayResId,
                fontSizeMultiplier,
                lineGapMultiplier,
                resolutionResId,
                resolutionImagePaddingBottom,
                resolutionImagePaddingSide,
                headlineTextOffsetX,
                headlineTextOffsetY,
                headlineAlignment
            )
            if (thumbnail != null) {
                thumbnailBitmap = thumbnail
            }
        }
    }
    
    // Auto-regenerate preview when resolution changes
    LaunchedEffect(selectedResolution) {
        // Only regenerate thumbnail if it exists (resolution only affects thumbnails)
        if (thumbnailBitmap != null && selectedImageUri != null && breakingHeadline.isNotEmpty()) {
            thumbnailBitmap?.recycle()
            thumbnailBitmap = null
            
            val resolutionResId = when (selectedResolution) {
                "FULLHD" -> R.drawable.fullhd
                "2K" -> R.drawable.twok
                "4K" -> R.drawable.fourk
                "FILM1080" -> R.drawable.film1080
                "FILM2K" -> R.drawable.film2k
                "FILM4K" -> R.drawable.film4k
                else -> null
            }
            
            val thumbnail = generateThumbnail(
                context,
                selectedImageUri!!,
                breakingHeadline,
                overlayResId,
                fontSizeMultiplier,
                lineGapMultiplier,
                resolutionResId,
                resolutionImagePaddingBottom,
                resolutionImagePaddingSide,
                headlineTextOffsetX,
                headlineTextOffsetY,
                headlineAlignment
            )
            if (thumbnail != null) {
                thumbnailBitmap = thumbnail
            }
        }
    }
    
    // Auto-regenerate preview when alignment changes
    LaunchedEffect(headlineAlignment) {
        // Small delay to debounce
        kotlinx.coroutines.delay(200)
        
        // Auto-regenerate if there's already a preview
        if (generatedBitmap != null && selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())) {
            // Regenerate breaking news image
            generatedBitmap?.recycle()
            generatedBitmap = null
            
            val result = generateBreakingNewsImage(
                context,
                selectedImageUri!!,
                breakingHeadline,
                newsText,
                fontSizeMultiplier,
                newsTextFontSizeMultiplier,
                lineGapMultiplier,
                headlineAlignment
            )
            if (result != null) {
                generatedBitmap = result
            }
        } else if (thumbnailBitmap != null && selectedImageUri != null && breakingHeadline.isNotEmpty()) {
            // Regenerate thumbnail
            thumbnailBitmap?.recycle()
            thumbnailBitmap = null
            
            val resolutionResId = when (selectedResolution) {
                "FULLHD" -> R.drawable.fullhd
                "2K" -> R.drawable.twok
                "4K" -> R.drawable.fourk
                "FILM1080" -> R.drawable.film1080
                "FILM2K" -> R.drawable.film2k
                "FILM4K" -> R.drawable.film4k
                else -> null
            }
            
            val thumbnail = generateThumbnail(
                context,
                selectedImageUri!!,
                breakingHeadline,
                overlayResId,
                fontSizeMultiplier,
                lineGapMultiplier,
                resolutionResId,
                resolutionImagePaddingBottom,
                resolutionImagePaddingSide,
                headlineTextOffsetX,
                headlineTextOffsetY,
                headlineAlignment
            )
            if (thumbnail != null) {
                thumbnailBitmap = thumbnail
            }
        }
    }

    // Image picker launcher
    // Note: GetContent() doesn't require any permissions - it uses the system picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            android.util.Log.d("BreakingNews", "Image selected: $uri")
        } else {
            android.util.Log.d("BreakingNews", "No image selected")
        }
    }

    // YouTube Link Screen - Full Screen Navigation
    if (showYouTubeLinkScreen) {
        YouTubeLinkScreen(
            onDismiss = { 
                showYouTubeLinkScreen = false
                initialLink = null // Clear initial link after dismissing
            },
            context = context,
            initialLink = initialLink
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.breaking),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Breaking News Generator") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Select Image Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape
                ) {
                    Text("Select Image", fontSize = 20.sp)
                }
            }

            // Breaking Headline Input
            OutlinedTextField(
                value = breakingHeadline,
                onValueChange = {
                    if (it.length <= 200) {
                        breakingHeadline = it
                    }
                },
                label = { Text("Breaking Headline (${breakingHeadline.length}/200)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            // Breaking News Text Input
            OutlinedTextField(
                value = newsText,
                onValueChange = { 
                    if (it.length <= 400) {
                        newsText = it
                    }
                },
                label = { Text("Breaking News Text (${newsText.length}/400)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            // Preview Image - Show thumbnail if exists, otherwise show generated image
            (thumbnailBitmap ?: generatedBitmap)?.let { bitmap ->
                android.util.Log.d("BreakingNews", "Displaying preview. Bitmap size: ${bitmap.width}x${bitmap.height}")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = if (thumbnailBitmap != null) "Generated Thumbnail" else "Generated Breaking News Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Touch area for adjusting text position (only for thumbnails)
                        thumbnailBitmap?.let { currentThumbnail ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(currentThumbnail.width, currentThumbnail.height) {
                                        val thumbnailWidth = currentThumbnail.width.toFloat()
                                        val thumbnailHeight = currentThumbnail.height.toFloat()
                                        val previewWidth = size.width.toFloat()
                                        val previewHeight = size.height.toFloat()
                                        
                                        detectDragGestures { change, dragAmount ->
                                            // Scale drag amount from preview size to thumbnail size
                                            val scaleX = thumbnailWidth / previewWidth
                                            val scaleY = thumbnailHeight / previewHeight
                                            
                                            headlineTextOffsetX += dragAmount.x * scaleX
                                            headlineTextOffsetY += dragAmount.y * scaleY
                                            
                                            // Regenerate thumbnail with new offset
                                            if (selectedImageUri != null && breakingHeadline.isNotEmpty()) {
                                                currentThumbnail.recycle()
                                                thumbnailBitmap = null
                                                
                                                val resolutionResId = when (selectedResolution) {
                                                    "FULLHD" -> R.drawable.fullhd
                                                    "2K" -> R.drawable.twok
                                                    "4K" -> R.drawable.fourk
                                                    "FILM1080" -> R.drawable.film1080
                                                    "FILM2K" -> R.drawable.film2k
                                                    "FILM4K" -> R.drawable.film4k
                                                    else -> null
                                                }
                                                
                                                val thumbnail = generateThumbnail(
                                                    context,
                                                    selectedImageUri!!,
                                                    breakingHeadline,
                                                    overlayResId,
                                                    fontSizeMultiplier,
                                                    lineGapMultiplier,
                                                    resolutionResId,
                                                    resolutionImagePaddingBottom,
                                                    resolutionImagePaddingSide,
                                                    headlineTextOffsetX,
                                                    headlineTextOffsetY,
                                                    headlineAlignment
                                                )
                                                if (thumbnail != null) {
                                                    thumbnailBitmap = thumbnail
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Font Size and Line Gap Adjustments
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Font Size & Line Gap Adjustments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    
                    // Common Font Size (for Headline and Thumbnail)
                    Column(
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Headline & Thumbnail Font Size: ${(fontSizeMultiplier * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Slider(
                            value = fontSizeMultiplier,
                            onValueChange = { fontSizeMultiplier = it },
                            valueRange = -0.1f..2.0f,
                            steps = 40,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // News Text Font Size
                    Column(
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "News Text Font Size: ${(newsTextFontSizeMultiplier * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Slider(
                            value = newsTextFontSizeMultiplier,
                            onValueChange = { newsTextFontSizeMultiplier = it },
                            valueRange = 0.5f..2.0f,
                            steps = 40,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Line Gap
                    Column(
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Line Gap: ${(lineGapMultiplier * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Slider(
                            value = lineGapMultiplier,
                            onValueChange = { lineGapMultiplier = it },
                            valueRange = -1.0f..2.0f,
                            steps = 40,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Headline Alignment Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Headline Alignment:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                headlineAlignment = if (headlineAlignment == "CENTER") "LEFT" else "CENTER"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (headlineAlignment == "CENTER") 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = headlineAlignment,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                }
            }

            // First Row: Resolutions, Post, Thumbnail (3 buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resolutions Button
                Button(
                    onClick = {
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape
                ) {
                    Text(selectedResolution, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                // Generate Post Button
                Button(
                    onClick = {
                        // Hide keyboard
                        keyboardController?.hide()
                        
                        if (selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())) {
                            // Clear thumbnail when generating new post
                            thumbnailBitmap?.recycle()
                            thumbnailBitmap = null
                            
                            val result = generateBreakingNewsImage(
                                context,
                                selectedImageUri!!,
                                breakingHeadline,
                                newsText,
                                fontSizeMultiplier,
                                newsTextFontSizeMultiplier,
                                lineGapMultiplier,
                                headlineAlignment
                            )
                            if (result != null) {
                                android.util.Log.d("BreakingNews", "Image generated successfully. Size: ${result.width}x${result.height}")
                                generatedBitmap = result
                                android.util.Log.d("BreakingNews", "Bitmap set to state. generatedBitmap is now: ${if (generatedBitmap != null) "not null" else "null"}")
                            } else {
                                android.util.Log.e("BreakingNews", "Image generation returned null. Check logs above for details.")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    enabled = selectedImageUri != null && (breakingHeadline.isNotEmpty() || newsText.isNotEmpty())
                ) {
                    Text("Post", fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                // Create Thumbnail Button
                Button(
                    onClick = {
                        // Hide keyboard
                        keyboardController?.hide()
                        
                        if (selectedImageUri != null && breakingHeadline.isNotEmpty()) {
                            // Clear generated image when creating thumbnail
                            generatedBitmap?.recycle()
                            generatedBitmap = null
                            
                            // Get resolution drawable resource ID
                            val resolutionResId = when (selectedResolution) {
                                "FULLHD" -> R.drawable.fullhd
                                "2K" -> R.drawable.twok
                                "4K" -> R.drawable.fourk
                                "FILM1080" -> R.drawable.film1080
                                "FILM2K" -> R.drawable.film2k
                                "FILM4K" -> R.drawable.film4k
                                else -> null
                            }
                            
                            val thumbnail = generateThumbnail(
                                context, 
                                selectedImageUri!!, 
                                breakingHeadline, 
                                overlayResId,
                                fontSizeMultiplier,
                                lineGapMultiplier,
                                resolutionResId,
                                resolutionImagePaddingBottom,
                                resolutionImagePaddingSide,
                                headlineTextOffsetX,
                                headlineTextOffsetY,
                                headlineAlignment
                            )
                            if (thumbnail != null) {
                                thumbnailBitmap = thumbnail
                                android.util.Log.d("Thumbnail", "Thumbnail generated and preview set")
                            } else {
                                android.util.Log.e("Thumbnail", "Failed to generate thumbnail")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    enabled = selectedImageUri != null && breakingHeadline.isNotEmpty()
                ) {
                    Text(
                        text = "Thumbnail",
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Second Row: Link, Save, Share (3 buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // YouTube Link Button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        showYouTubeLinkScreen = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Link",
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Save to Gallery Button
                Button(
                    onClick = {
                        // Save thumbnail if it exists, otherwise save generated image
                        val saved = thumbnailBitmap?.let { bitmap ->
                            saveThumbnailToGallery(context, bitmap)
                        } ?: generatedBitmap?.let { bitmap ->
                            saveToGallery(context, bitmap)
                        } ?: false
                        
                        if (!saved) {
                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    enabled = (thumbnailBitmap ?: generatedBitmap) != null
                ) {
                    Text("Save", fontSize = 16.sp)
                }
                
                // Share Button
                Button(
                    onClick = {
                        (thumbnailBitmap ?: generatedBitmap)?.let { bitmap ->
                            saveAndShareImage(context, bitmap)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = (thumbnailBitmap ?: generatedBitmap) != null
                ) {
                    Text("Share", fontSize = 16.sp)
                }
            }
            
            // Resolution Selection Dialog
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
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedResolution == resolution,
                                        onClick = {
                                            selectedResolution = resolution
                                            showResolutionDialog = false
                                            // Regenerate thumbnail if it exists
                                            if (thumbnailBitmap != null && selectedImageUri != null && breakingHeadline.isNotEmpty()) {
                                                thumbnailBitmap?.recycle()
                                                thumbnailBitmap = null
                                                
                                                val resolutionResId = when (selectedResolution) {
                                                    "FULLHD" -> R.drawable.fullhd
                                                    "2K" -> R.drawable.twok
                                                    "4K" -> R.drawable.fourk
                                                    "FILM1080" -> R.drawable.film1080
                                                    "FILM2K" -> R.drawable.film2k
                                                    "FILM4K" -> R.drawable.film4k
                                                    else -> null
                                                }
                                                
                                                val thumbnail = generateThumbnail(
                                                    context,
                                                    selectedImageUri!!,
                                                    breakingHeadline,
                                                    overlayResId,
                                                    fontSizeMultiplier,
                                                    lineGapMultiplier,
                                                    resolutionResId,
                                                    resolutionImagePaddingBottom,
                                                    resolutionImagePaddingSide,
                                                    headlineTextOffsetX,
                                                    headlineTextOffsetY,
                                                    headlineAlignment
                                                )
                                                if (thumbnail != null) {
                                                    thumbnailBitmap = thumbnail
                                                }
                                            }
                                        }
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
                        TextButton(
                            onClick = { showResolutionDialog = false },
                            shape = RectangleShape
                        ) {
                            Text("Close", fontSize = 16.sp)
                        }
                    }
                )
            }
            }
        }
        
        // Share Dialog
        val shareBitmap = thumbnailBitmap ?: generatedBitmap
        if (showShareDialog && shareBitmap != null) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Share to Social Media") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                shareToSocialMedia(context, shareBitmap, "YouTube")
                                showShareDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Text("Share to YouTube", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                shareToSocialMedia(context, shareBitmap, "Facebook")
                                showShareDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Text("Share to Facebook", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                shareToSocialMedia(context, shareBitmap, "Instagram")
                                showShareDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Text("Share to Instagram", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                shareToSocialMedia(context, shareBitmap, "Twitter")
                                showShareDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Text("Share to Twitter", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                shareToSocialMedia(context, shareBitmap, "WhatsApp")
                                showShareDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Text("Share to WhatsApp", fontSize = 16.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showShareDialog = false },
                        shape = RectangleShape
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                }
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BreakingNewsAppPreview() {
    TempleteTheme {
        BreakingNewsApp()
    }
}