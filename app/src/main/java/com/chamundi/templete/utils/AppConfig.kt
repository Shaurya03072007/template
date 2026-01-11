package com.chamundi.templete.utils

import android.graphics.Color

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
        val HEADLINE_BACKGROUND_COLOR = Color.rgb(220, 20, 60) // Red rose (crimson)
        val HEADLINE_TEXT_COLOR = Color.WHITE
        val HEADLINE_BORDER_COLOR = Color.BLACK
        const val HEADLINE_BORDER_WIDTH = 3f
        const val HEADLINE_BACKGROUND_PADDING_H = 8f
        const val HEADLINE_HEIGHT_REDUCTION = 0.30f // 30% height reduction
        const val HEADLINE_CORNER_RADIUS = 15f
        const val HEADLINE_SHADOW_OFFSET_X = 20f
        const val HEADLINE_SHADOW_OFFSET_Y = 20f
        val HEADLINE_SHADOW_COLOR = Color.argb(220, 0, 0, 0) // Semi-transparent black

        // News text settings
        const val NEWS_TEXT_FONT_MIN_SIZE = 15f
        const val NEWS_TEXT_FONT_MAX_SIZE = 80f
        val NEWS_TEXT_COLOR = Color.WHITE
        val NEWS_TEXT_SHADOW_COLOR = Color.BLACK
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
        val TEXT_COLOR = Color.WHITE
        val STROKE_COLOR = Color.BLACK

        // Layout settings
        const val AVAILABLE_HEIGHT_PERCENT = 0.8f // 80% of thumbnail height
        const val HORIZONTAL_PADDING = 20f // Left margin for text area
    }
}
