package com.chamundi.templete.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.chamundi.templete.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageGenerator {

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

    fun cropToRatio(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()

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

    fun loadCustomFont(context: Context): Typeface? {
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
            val saturationMatrix = ColorMatrix().apply {
                setSaturation(AppConfig.BreakingNews.SATURATION_MULTIPLIER) // Increase saturation for more vibrant colors
            }
            val contrastBrightnessMatrix = ColorMatrix().apply {
                val contrast = AppConfig.BreakingNews.CONTRAST_MULTIPLIER
                val brightness = AppConfig.BreakingNews.BRIGHTNESS_OFFSET
                set(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,  // Red channel
                    0f, contrast, 0f, 0f, brightness,  // Green channel
                    0f, 0f, contrast, 0f, brightness,  // Blue channel
                    0f, 0f, 0f, 1f, 0f                 // Alpha channel unchanged
                ))
            }
            val colorMatrix = ColorMatrix().apply {
                postConcat(saturationMatrix)
                postConcat(contrastBrightnessMatrix)
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
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

    fun generateBreakingNewsImage(
        context: Context,
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
                return null
            }

            // Enhance image quality automatically
            val enhancedBitmap = enhanceImageQuality(originalBitmap)
            if (enhancedBitmap != originalBitmap) {
                originalBitmap.recycle() // Free memory if new bitmap was created
            }

            // Create 1080x1350 output bitmap
            val outputBitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            // Load and draw breaking.png as background
            try {
                val backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.breaking)
                if (backgroundBitmap != null) {
                    val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, 1080, 1350, true)
                    canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                } else {
                    val backgroundPaint = Paint().apply {
                        color = Color.BLACK
                    }
                    canvas.drawRect(0f, 0f, 1080f, 1350f, backgroundPaint)
                }
            } catch (e: Exception) {
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
            val tempFont = loadCustomFont(context) ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val tempPaint = Paint().apply {
                textSize = 100f
                typeface = tempFont
            }
            val textAscentValue = Math.abs(tempPaint.ascent())
            val marginFromImage = AppConfig.BreakingNews.MARGIN_FROM_IMAGE
            val textStartY = imageBottom + marginFromImage + textAscentValue
            val textPadding = AppConfig.BreakingNews.TEXT_PADDING
            val maxTextWidth = 1080f - (textPadding * 2f)
            val centerX = 1080f / 2f
            var currentY = textStartY
            val shadowOffset = AppConfig.BreakingNews.NEWS_TEXT_SHADOW_OFFSET

            val customFont = tempFont

            try {
                // Draw headline if provided
                if (headline.isNotEmpty()) {

                    val marginBetweenHeadlineAndNews = AppConfig.BreakingNews.MARGIN_BETWEEN_HEADLINE_AND_NEWS
                    val bottomMargin = AppConfig.BreakingNews.BOTTOM_MARGIN
                    val maxHeadlineHeight = outputHeight - imageBottom - marginFromImage - marginBetweenHeadlineAndNews - bottomMargin

                    val headlineAlign = if (headlineAlignment == "LEFT") Paint.Align.LEFT else Paint.Align.CENTER

                    val optimalFontSize = calculateOptimalFontSizeForMultiLine(
                        text = headline,
                        maxWidth = maxTextWidth,
                        maxHeight = maxHeadlineHeight,
                        typeface = customFont,
                        minSize = AppConfig.BreakingNews.HEADLINE_FONT_MIN_SIZE * headlineFontSizeMultiplier,
                        maxSize = AppConfig.BreakingNews.HEADLINE_FONT_MAX_SIZE * headlineFontSizeMultiplier
                    ) * headlineFontSizeMultiplier

                    val headlinePaint = Paint().apply {
                        color = AppConfig.BreakingNews.HEADLINE_TEXT_COLOR
                        textSize = optimalFontSize
                        isAntiAlias = true
                        typeface = customFont
                        textAlign = headlineAlign
                        isUnderlineText = false
                        style = Paint.Style.FILL
                    }

                    val measurePaint = Paint().apply {
                        textSize = optimalFontSize
                        typeface = customFont
                        textAlign = Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    val headlineLines = breakTextIntoLines(headline, measurePaint, maxTextWidth)
                    val displayLines = headlineLines.take(3)

                    val headlineBackgroundPaint = Paint().apply {
                        color = AppConfig.BreakingNews.HEADLINE_BACKGROUND_COLOR
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    val headlineStrokePaint = Paint().apply {
                        color = AppConfig.BreakingNews.HEADLINE_BORDER_COLOR
                        style = Paint.Style.STROKE
                        strokeWidth = AppConfig.BreakingNews.HEADLINE_BORDER_WIDTH
                        isAntiAlias = true
                    }

                    val backgroundPaddingHorizontal = AppConfig.BreakingNews.HEADLINE_BACKGROUND_PADDING_H
                    val heightReduction = AppConfig.BreakingNews.HEADLINE_HEIGHT_REDUCTION
                    val cornerRadius = AppConfig.BreakingNews.HEADLINE_CORNER_RADIUS

                    val textAscent = headlinePaint.ascent()
                    val textDescent = headlinePaint.descent()
                    val lineHeight = textDescent - textAscent
                    val lineSpacing = lineHeight * 0.5f

                    val maxLineWidth = displayLines.maxOfOrNull { measurePaint.measureText(it) } ?: 0f
                    val totalTextHeight = if (displayLines.size > 1) {
                        lineHeight + (displayLines.size - 1) * lineSpacing
                    } else {
                        lineHeight
                    }

                    val bgLeft = if (headlineAlignment == "LEFT") {
                        textPadding
                    } else {
                        centerX - maxLineWidth / 2f - backgroundPaddingHorizontal
                    }
                    val bgRight = if (headlineAlignment == "LEFT") {
                        minOf(textPadding + maxLineWidth + (backgroundPaddingHorizontal * 2f), 1080f - textPadding)
                    } else {
                        centerX + maxLineWidth / 2f + backgroundPaddingHorizontal
                    }
                    val reducedHeight = totalTextHeight * (1f - heightReduction)
                    val heightOffset = (totalTextHeight - reducedHeight) / 2f

                    val initialY = imageBottom + marginFromImage - textAscent - heightOffset
                    val bgTop = initialY + textAscent + heightOffset
                    val bgBottom = initialY + textAscent + totalTextHeight - heightOffset

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

                    val boxCenter = (finalBgTop + finalBgBottom) / 2f
                    val centeredLineY = boxCenter - textAscent - totalTextHeight / 2f

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

                    canvas.drawRoundRect(
                        bgLeft,
                        finalBgTop,
                        bgRight,
                        finalBgBottom,
                        cornerRadius,
                        cornerRadius,
                        headlineBackgroundPaint
                    )

                    canvas.drawRoundRect(
                        bgLeft,
                        finalBgTop,
                        bgRight,
                        finalBgBottom,
                        cornerRadius,
                        cornerRadius,
                        headlineStrokePaint
                    )

                    var lineY = centeredLineY
                    val textX = if (headlineAlignment == "LEFT") {
                        bgLeft + backgroundPaddingHorizontal
                    } else {
                        centerX
                    }
                    displayLines.forEach { line ->
                        canvas.drawText(line, textX, lineY, headlinePaint)
                        lineY += lineSpacing
                    }

                    currentY = finalBgBottom
                }

                if (newsText.isNotEmpty()) {
                    val marginBetweenHeadlineAndNews = AppConfig.BreakingNews.MARGIN_BETWEEN_HEADLINE_AND_NEWS
                    val bottomMargin = AppConfig.BreakingNews.BOTTOM_MARGIN
                    val availableHeight = outputHeight - currentY - marginBetweenHeadlineAndNews - bottomMargin

                    val optimalNewsFontSize = calculateOptimalFontSizeForMultiLine(
                        text = newsText,
                        maxWidth = maxTextWidth,
                        maxHeight = availableHeight,
                        typeface = customFont,
                        minSize = AppConfig.BreakingNews.NEWS_TEXT_FONT_MIN_SIZE * newsTextFontSizeMultiplier,
                        maxSize = AppConfig.BreakingNews.NEWS_TEXT_FONT_MAX_SIZE * newsTextFontSizeMultiplier
                    ) * newsTextFontSizeMultiplier

                    val newsShadowPaint = Paint().apply {
                        color = AppConfig.BreakingNews.NEWS_TEXT_SHADOW_COLOR
                        textSize = optimalNewsFontSize
                        isAntiAlias = true
                        typeface = customFont
                        textAlign = Paint.Align.CENTER
                        style = Paint.Style.FILL
                    }

                    val newsPaint = Paint().apply {
                        color = AppConfig.BreakingNews.NEWS_TEXT_COLOR
                        textSize = optimalNewsFontSize
                        isAntiAlias = true
                        typeface = customFont
                        textAlign = Paint.Align.CENTER
                        style = Paint.Style.FILL
                    }

                    val newsTextAscent = Math.abs(newsPaint.ascent())
                    currentY = currentY + marginBetweenHeadlineAndNews + newsTextAscent

                    val measurePaint = Paint().apply {
                        textSize = optimalNewsFontSize
                        typeface = customFont
                        textAlign = Paint.Align.LEFT
                    }
                    val newsLines = breakTextIntoLines(newsText, measurePaint, maxTextWidth)

                    val lineSpacing = (newsPaint.descent() - newsPaint.ascent() + AppConfig.BreakingNews.NEWS_TEXT_LINE_SPACING_GAP) * lineGapMultiplier

                    for (line in newsLines) {
                        if (currentY > outputHeight - bottomMargin) break
                        if (line.isNotEmpty()) {
                            canvas.drawText(line, centerX + shadowOffset, currentY + shadowOffset, newsShadowPaint)
                            canvas.drawText(line, centerX, currentY, newsPaint)
                            currentY += lineSpacing
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return outputBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateThumbnail(
        context: Context,
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
        headlineAlignment: String = "CENTER"
    ): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(baseImageUri) ?: return null
            val baseBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (baseBitmap == null) return null

            val thumbnailBitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(thumbnailBitmap)

            val imageTargetWidth = 830
            val imageTargetHeight = 720
            val baseWidth = baseBitmap.width
            val baseHeight = baseBitmap.height
            val targetRatio = imageTargetWidth.toFloat() / imageTargetHeight.toFloat()

            val bottomCropPercent = 0.05f
            val bottomCropAmount = (baseHeight * bottomCropPercent).toInt()
            val adjustedHeight = baseHeight - bottomCropAmount
            val adjustedRatio = baseWidth.toFloat() / adjustedHeight.toFloat()

            var workingBitmap = baseBitmap
            if (adjustedRatio > targetRatio) {
                val newWidth = (adjustedHeight * targetRatio).toInt()
                val xOffset = (baseWidth - newWidth) / 2
                workingBitmap = Bitmap.createBitmap(baseBitmap, xOffset, 0, newWidth, adjustedHeight)
            } else if (adjustedRatio < targetRatio) {
                val newHeight = (baseWidth / targetRatio).toInt()
                val finalHeight = minOf(newHeight, adjustedHeight)
                val yOffset = maxOf(0, adjustedHeight - finalHeight)
                workingBitmap = Bitmap.createBitmap(baseBitmap, 0, yOffset, baseWidth, finalHeight)
            } else {
                workingBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseWidth, adjustedHeight)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(workingBitmap, imageTargetWidth, imageTargetHeight, true)

            val imageLeft = (1280 - imageTargetWidth).toFloat()
            canvas.drawBitmap(scaledBitmap, imageLeft, 0f, null)

            try {
                val thumbnailOverlay = BitmapFactory.decodeResource(context.resources, overlayResId)
                if (thumbnailOverlay != null) {
                    val overlayBitmap = if (thumbnailOverlay.width != 1280 || thumbnailOverlay.height != 720) {
                        Bitmap.createScaledBitmap(thumbnailOverlay, 1280, 720, true)
                    } else {
                        thumbnailOverlay
                    }
                    canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val customFont = loadCustomFont(context) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val leftAreaWidth = 1280f - imageTargetWidth
            val horizontalPadding = AppConfig.Thumbnail.HORIZONTAL_PADDING
            val maxTextWidth = leftAreaWidth - horizontalPadding
            val optimalFontSize = calculateOptimalFontSize(
                text = headlineText,
                maxWidth = maxTextWidth,
                typeface = customFont,
                minSize = AppConfig.Thumbnail.HEADLINE_FONT_MIN_SIZE * fontSizeMultiplier,
                maxSize = AppConfig.Thumbnail.HEADLINE_FONT_MAX_SIZE * fontSizeMultiplier
            ) * fontSizeMultiplier

            val thumbnailAlign = if (headlineAlignment == "LEFT") Paint.Align.LEFT else Paint.Align.CENTER

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

            val shadowPaint = Paint().apply {
                color = Color.BLACK
                textSize = optimalFontSize
                isAntiAlias = true
                typeface = customFont
                textAlign = thumbnailAlign
                style = Paint.Style.FILL
                alpha = 800
            }

            val lines = headlineText.split('\n').map { it.trim() }

            if (lines.isEmpty()) {
                canvas.drawText("", 0f, -textPaint.ascent(), textPaint)
            }

            val fontHeight = textPaint.descent() - textPaint.ascent()
            val lineCount = lines.count { it.isNotEmpty() }
            val availableHeight = 720f * AppConfig.Thumbnail.AVAILABLE_HEIGHT_PERCENT
            val totalTextHeight = maxOf(1, lineCount) * fontHeight

            val baseLineSpacing = fontHeight * lineGapMultiplier

            val extraSpacing = if (lineCount > 1 && availableHeight > totalTextHeight) {
                ((availableHeight - totalTextHeight) / (lineCount - 1)) * (lineGapMultiplier - 1.0f) * 0.5f
            } else {
                0f
            }

            var currentY = -textPaint.ascent()
            val textAreaWidth = leftAreaWidth - horizontalPadding
            val centerX = horizontalPadding + (textAreaWidth / 2f) + textOffsetX
            val leftX = horizontalPadding + textOffsetX
            val textX = if (headlineAlignment == "LEFT") leftX else centerX
            val shadowOffsetX = 5f
            val shadowOffsetY = 5f

            lines.forEach { line ->
                if (line.isNotEmpty()) {
                    val adjustedY = currentY + textOffsetY
                    canvas.drawText(line, textX + shadowOffsetX, adjustedY + shadowOffsetY, shadowPaint)
                    canvas.drawText(line, textX, adjustedY, strokePaint)
                    canvas.drawText(line, textX, adjustedY, textPaint)
                }
                currentY += baseLineSpacing + extraSpacing
            }

            resolutionResId?.let { resId ->
                try {
                    val resolutionBitmap = BitmapFactory.decodeResource(context.resources, resId)
                    if (resolutionBitmap != null) {
                        val resolutionSize = 120f
                        val scaledResolution = Bitmap.createScaledBitmap(
                            resolutionBitmap,
                            resolutionSize.toInt(),
                            resolutionSize.toInt(),
                            true
                        )
                        val resolutionX = 1280f - resolutionSize
                        val resolutionY = 0f

                        canvas.drawBitmap(scaledResolution, resolutionX, resolutionY, null)

                        val outerBorderWidth = 4f
                        val innerBorderWidth = 2f
                        val gapBetweenBorders = 2f

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
                    e.printStackTrace()
                }
            }

            if (scaledBitmap != workingBitmap) {
                scaledBitmap.recycle()
            }
            if (workingBitmap != baseBitmap) {
                workingBitmap.recycle()
            }
            baseBitmap.recycle()

            return thumbnailBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getImageFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val dateTime = dateFormat.format(Date())
        return "BreakingNews_$dateTime.jpg"
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val filename = getImageFileName()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BreakingNews")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.flush()

                        val updateValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(uri, updateValues, null, null)
                    }
                    true
                } else {
                    false
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (imagesDir == null || !imagesDir.exists()) {
                    return false
                }

                val breakingNewsDir = File(imagesDir, "BreakingNews")
                if (!breakingNewsDir.exists()) {
                    breakingNewsDir.mkdirs()
                }

                val file = File(breakingNewsDir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()

                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = Uri.fromFile(file)
                    context.sendBroadcast(intent)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveThumbnailToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val filename = getImageFileName().replace("BreakingNews_", "Thumbnail_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        outputStream.flush()

                        val updateValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(uri, updateValues, null, null)
                    }
                    true
                } else {
                    false
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (imagesDir != null && imagesDir.exists()) {
                    val breakingNewsDir = File(imagesDir, "BreakingNews")
                    if (!breakingNewsDir.exists()) {
                        breakingNewsDir.mkdirs()
                    }

                    val file = File(breakingNewsDir, filename)
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        outputStream.flush()

                        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        intent.data = Uri.fromFile(file)
                        context.sendBroadcast(intent)
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveAndShareImage(context: Context, bitmap: Bitmap) {
        try {
            val filename = getImageFileName()
            val file = File(context.getExternalFilesDir(null), filename)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

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

    fun shareToSocialMedia(context: Context, bitmap: Bitmap, platform: String) {
        try {
            val filename = getImageFileName()
            val file = File(context.getExternalFilesDir(null), filename)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

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

            when (platform) {
                "YouTube" -> {
                    shareIntent.setPackage(null)
                    context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
                    return
                }
                "Facebook" -> shareIntent.setPackage("com.facebook.katana")
                "Instagram" -> shareIntent.setPackage("com.instagram.android")
                "Twitter" -> {
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
                        }
                    }
                }
                "WhatsApp" -> shareIntent.setPackage("com.whatsapp")
            }

            try {
                if (platform != "Twitter") {
                    context.startActivity(shareIntent)
                } else {
                    shareIntent.setPackage(null)
                    context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
                }
            } catch (e: Exception) {
                shareIntent.setPackage(null)
                context.startActivity(Intent.createChooser(shareIntent, "Share Breaking News Image"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
