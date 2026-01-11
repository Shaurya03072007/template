package com.chamundi.templete.editor.utils

import android.content.Context
import android.graphics.Typeface


object FontProvider {

    data class FontInfo(
        val name: String,
        val typeface: Typeface
    )

    private var fonts: List<FontInfo> = emptyList()

    fun initialize(context: Context) {
        val systemFonts = listOf(
            FontInfo("Default", Typeface.DEFAULT),
            FontInfo("Serif", Typeface.SERIF),
            FontInfo("Sans Serif", Typeface.SANS_SERIF),
            FontInfo("Monospace", Typeface.MONOSPACE),
        )

        val customFonts = mutableListOf<FontInfo>()

        // Try to load Ramabhadra from assets if it exists (known from previous exploration)
        try {
            val ramabhadra = Typeface.createFromAsset(context.assets, "fonts/Ramabhadra-Regular.ttf")
            customFonts.add(FontInfo("Ramabhadra", ramabhadra))
        } catch (e: Exception) {
            // Ignore if not found
        }

        fonts = systemFonts + customFonts
    }

    fun getAvailableFonts(): List<FontInfo> {
        return fonts
    }

    fun getTypeface(fontName: String): Typeface {
        return fonts.find { it.name == fontName }?.typeface ?: Typeface.DEFAULT
    }
}
