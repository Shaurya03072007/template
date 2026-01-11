package com.chamundi.templete.editor.utils

import android.content.Context
import android.graphics.Typeface
import java.io.File

object FontProvider {

    data class FontInfo(
        val name: String,
        val typeface: Typeface
    )

    private var fonts: MutableList<FontInfo> = mutableListOf()

    fun initialize(context: Context) {
        if (fonts.isNotEmpty()) return

        // Standard System Fonts
        fonts.add(FontInfo("Default", Typeface.DEFAULT))
        fonts.add(FontInfo("Serif", Typeface.SERIF))
        fonts.add(FontInfo("Sans Serif", Typeface.SANS_SERIF))
        fonts.add(FontInfo("Monospace", Typeface.MONOSPACE))

        // Asset Fonts
        val fontFiles = listOf(
            "Ramabhadra" to "fonts/Ramabhadra-Regular.ttf",
            "Open Sans" to "fonts/OpenSans-Regular.ttf",
            "Lato" to "fonts/Lato-Regular.ttf",
            "Montserrat" to "fonts/Montserrat-Regular.ttf",
            "Oswald" to "fonts/Oswald-Regular.ttf",
            "Source Sans Pro" to "fonts/SourceSansPro-Regular.ttf",
            "Raleway" to "fonts/Raleway-Regular.ttf"
        )

        for ((name, path) in fontFiles) {
            try {
                val typeface = Typeface.createFromAsset(context.assets, path)
                fonts.add(FontInfo(name, typeface))
            } catch (e: Exception) {
                android.util.Log.e("FontProvider", "Failed to load font: $name from $path", e)
            }
        }
    }

    fun getAvailableFonts(): List<FontInfo> {
        return fonts
    }

    fun getTypeface(fontName: String): Typeface {
        return fonts.find { it.name == fontName }?.typeface ?: Typeface.DEFAULT
    }

    fun getTypefaceWithStyle(fontName: String, isBold: Boolean, isItalic: Boolean): Typeface {
        val baseTypeface = getTypeface(fontName)
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(baseTypeface, style)
    }
}
