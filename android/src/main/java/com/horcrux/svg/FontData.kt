package com.horcrux.svg

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.uimanager.ViewProps
import com.horcrux.svg.TextProperties.FontVariantLigatures
import com.horcrux.svg.TextProperties.TextAnchor
import com.horcrux.svg.TextProperties.TextDecoration

class FontData {
    internal object AbsoluteFontWeight {
        const val normal = 400
        private val WEIGHTS = arrayOf(
                TextProperties.FontWeight.w100,
                TextProperties.FontWeight.w100,
                TextProperties.FontWeight.w200,
                TextProperties.FontWeight.w300,
                TextProperties.FontWeight.Normal,
                TextProperties.FontWeight.w500,
                TextProperties.FontWeight.w600,
                TextProperties.FontWeight.Bold,
                TextProperties.FontWeight.w800,
                TextProperties.FontWeight.w900,
                TextProperties.FontWeight.w900)

        fun nearestFontWeight(absoluteFontWeight: Int): TextProperties.FontWeight {
            return WEIGHTS[Math.round(absoluteFontWeight / 100f)]
        }

        private val absoluteFontWeights = intArrayOf(400, 700, 100, 200, 300, 400, 500, 600, 700, 800, 900)

        // https://drafts.csswg.org/css-fonts-4/#relative-weights
        fun from(fontWeight: TextProperties.FontWeight?, parent: FontData?): Int {
            return if (fontWeight == TextProperties.FontWeight.Bolder) {
                bolder(parent!!.absoluteFontWeight)
            } else if (fontWeight == TextProperties.FontWeight.Lighter) {
                lighter(parent!!.absoluteFontWeight)
            } else {
                absoluteFontWeights[fontWeight!!.ordinal]
            }
        }

        private fun bolder(inherited: Int): Int {
            return if (inherited < 350) {
                400
            } else if (inherited < 550) {
                700
            } else if (inherited < 900) {
                900
            } else {
                inherited
            }
        }

        private fun lighter(inherited: Int): Int {
            return if (inherited < 100) {
                inherited
            } else if (inherited < 550) {
                100
            } else if (inherited < 750) {
                400
            } else {
                700
            }
        }
    }

    val fontSize: Double
    val fontFamily: String?
    val fontStyle: TextProperties.FontStyle
    val fontData: ReadableMap?
    var fontWeight: TextProperties.FontWeight? = null
    var absoluteFontWeight = 0
    val fontFeatureSettings: String?
    val fontVariationSettings: String?
    val fontVariantLigatures: FontVariantLigatures
    val textAnchor: TextAnchor
    private val textDecoration: TextDecoration?
    val kerning: Double
    val wordSpacing: Double
    val letterSpacing: Double
    val manualKerning: Boolean

    private constructor() {
        fontData = null
        fontFamily = ""
        fontStyle = TextProperties.FontStyle.normal
        fontWeight = TextProperties.FontWeight.Normal
        absoluteFontWeight = AbsoluteFontWeight.normal
        fontFeatureSettings = ""
        fontVariationSettings = ""
        fontVariantLigatures = FontVariantLigatures.normal
        textAnchor = TextAnchor.start
        textDecoration = TextDecoration.None
        manualKerning = false
        kerning = DEFAULT_KERNING
        fontSize = DEFAULT_FONT_SIZE
        wordSpacing = DEFAULT_WORD_SPACING
        letterSpacing = DEFAULT_LETTER_SPACING
    }

    private fun toAbsolute(
            font: ReadableMap, prop: String, scale: Double, fontSize: Double, relative: Double): Double {
        val propType = font.getType(prop)
        return if (propType == ReadableType.Number) {
            font.getDouble(prop)
        } else {
            val string = font.getString(prop)
            PropHelper.fromRelative(string, relative, scale, fontSize)
        }
    }

    private fun setInheritedWeight(parent: FontData?) {
        absoluteFontWeight = parent!!.absoluteFontWeight
        fontWeight = parent.fontWeight
    }

    private fun handleNumericWeight(parent: FontData?, number: Double) {
        val weight = Math.round(number)
        if (weight >= 1 && weight <= 1000) {
            absoluteFontWeight = weight.toInt()
            fontWeight = AbsoluteFontWeight.nearestFontWeight(absoluteFontWeight)
        } else {
            setInheritedWeight(parent)
        }
    }

    constructor(font: ReadableMap, parent: FontData?, scale: Double) {
        val parentFontSize = parent!!.fontSize
        fontSize = if (font.hasKey(ViewProps.FONT_SIZE)) {
            toAbsolute(font, ViewProps.FONT_SIZE, 1.0, parentFontSize, parentFontSize)
        } else {
            parentFontSize
        }
        if (font.hasKey(ViewProps.FONT_WEIGHT)) {
            val fontWeightType = font.getType(ViewProps.FONT_WEIGHT)
            if (fontWeightType == ReadableType.Number) {
                handleNumericWeight(parent, font.getDouble(ViewProps.FONT_WEIGHT))
            } else {
                val string = font.getString(ViewProps.FONT_WEIGHT)
                if (TextProperties.FontWeight.Companion.hasEnum(string)) {
                    absoluteFontWeight = AbsoluteFontWeight.from(TextProperties.FontWeight.Companion.get(string), parent)
                    fontWeight = AbsoluteFontWeight.nearestFontWeight(absoluteFontWeight)
                } else if (string != null) {
                    handleNumericWeight(parent, string.toDouble())
                } else {
                    setInheritedWeight(parent)
                }
            }
        } else {
            setInheritedWeight(parent)
        }
        fontData = if (font.hasKey(FONT_DATA)) font.getMap(FONT_DATA) else parent.fontData
        fontFamily = if (font.hasKey(ViewProps.FONT_FAMILY)) font.getString(ViewProps.FONT_FAMILY) else parent.fontFamily
        fontStyle = if (font.hasKey(ViewProps.FONT_STYLE)) TextProperties.FontStyle.valueOf(font.getString(ViewProps.FONT_STYLE)!!) else parent.fontStyle
        fontFeatureSettings = if (font.hasKey(FONT_FEATURE_SETTINGS)) font.getString(FONT_FEATURE_SETTINGS) else parent.fontFeatureSettings
        fontVariationSettings = if (font.hasKey(FONT_VARIATION_SETTINGS)) font.getString(FONT_VARIATION_SETTINGS) else parent.fontVariationSettings
        fontVariantLigatures = if (font.hasKey(FONT_VARIANT_LIGATURES)) FontVariantLigatures.valueOf(font.getString(FONT_VARIANT_LIGATURES)!!) else parent.fontVariantLigatures
        textAnchor = if (font.hasKey(TEXT_ANCHOR)) TextAnchor.valueOf(font.getString(TEXT_ANCHOR)!!) else parent.textAnchor
        textDecoration = if (font.hasKey(TEXT_DECORATION)) TextDecoration.Companion.getEnum(font.getString(TEXT_DECORATION)) else parent.textDecoration
        val hasKerning = font.hasKey(KERNING)
        manualKerning = hasKerning || parent.manualKerning

        // https://www.w3.org/TR/SVG11/text.html#SpacingProperties
        // https://drafts.csswg.org/css-text-3/#spacing
        // calculated values for units in: kerning, word-spacing, and, letter-spacing.
        kerning = if (hasKerning) toAbsolute(font, KERNING, scale, fontSize, 0.0) else parent.kerning
        wordSpacing = if (font.hasKey(WORD_SPACING)) toAbsolute(font, WORD_SPACING, scale, fontSize, 0.0) else parent.wordSpacing
        letterSpacing = if (font.hasKey(LETTER_SPACING)) toAbsolute(font, LETTER_SPACING, scale, fontSize, 0.0) else parent.letterSpacing
    }

    companion object {
        const val DEFAULT_FONT_SIZE = 12.0
        private const val DEFAULT_KERNING = 0.0
        private const val DEFAULT_WORD_SPACING = 0.0
        private const val DEFAULT_LETTER_SPACING = 0.0
        private const val KERNING = "kerning"
        private const val FONT_DATA = "fontData"
        private const val TEXT_ANCHOR = "textAnchor"
        private const val WORD_SPACING = "wordSpacing"
        private const val LETTER_SPACING = "letterSpacing"
        private const val TEXT_DECORATION = "textDecoration"
        private const val FONT_FEATURE_SETTINGS = "fontFeatureSettings"
        private const val FONT_VARIATION_SETTINGS = "fontVariationSettings"
        private const val FONT_VARIANT_LIGATURES = "fontVariantLigatures"
        val Defaults = FontData()
    }
}
