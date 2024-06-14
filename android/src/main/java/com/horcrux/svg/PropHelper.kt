/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import com.facebook.react.bridge.ReadableArray
import com.horcrux.svg.SVGLength.UnitType

/** Contains static helper methods for accessing props.  */
internal object PropHelper {
    private val inputMatrixDataSize: Int = 6

    /**
     * Converts given [ReadableArray] to a matrix data array, `float[6]`. Writes result to
     * the array passed in {@param into}. This method will write exactly six items to the output array
     * from the input array.
     *
     *
     * If the input array has a different size, then only the size is returned; Does not check
     * output array size. Ensure space for at least six elements.
     *
     * @param value input array
     * @param sRawMatrix output matrix
     * @param mScale current resolution scaling
     * @return size of input array
     */
    fun toMatrixData(value: ReadableArray, sRawMatrix: FloatArray, mScale: Float): Int {
        val fromSize: Int = value.size()
        if (fromSize != inputMatrixDataSize) {
            return fromSize
        }
        sRawMatrix[0] = value.getDouble(0).toFloat()
        sRawMatrix[1] = value.getDouble(2).toFloat()
        sRawMatrix[2] = value.getDouble(4).toFloat() * mScale
        sRawMatrix[3] = value.getDouble(1).toFloat()
        sRawMatrix[4] = value.getDouble(3).toFloat()
        sRawMatrix[5] = value.getDouble(5).toFloat() * mScale
        return inputMatrixDataSize
    }

    /**
     * Converts length string into px / user units in the current user coordinate system
     *
     * @param length length string
     * @param relative relative size for percentages
     * @param scale scaling parameter
     * @param fontSize current font size
     * @return value in the current user coordinate system
     */
    fun fromRelative(length: String?, relative: Double, scale: Double, fontSize: Double): Double {
        /*
        TODO list

        unit  relative to
        em    font size of the element
        ex    x-height of the element’s font
        ch    width of the "0" (ZERO, U+0030) glyph in the element’s font
        rem   font size of the root element
        vw    1% of viewport’s width
        vh    1% of viewport’s height
        vmin  1% of viewport’s smaller dimension
        vmax  1% of viewport’s larger dimension

        relative-size [ larger | smaller ]
        absolute-size: [ xx-small | x-small | small | medium | large | x-large | xx-large ]

        https://www.w3.org/TR/css3-values/#relative-lengths
        https://www.w3.org/TR/css3-values/#absolute-lengths
        https://drafts.csswg.org/css-cascade-4/#computed-value
        https://drafts.csswg.org/css-fonts-3/#propdef-font-size
        https://drafts.csswg.org/css2/fonts.html#propdef-font-size
    */
        var length: String? = length
        length = length!!.trim({ it <= ' ' })
        val stringLength: Int = length.length
        val percentIndex: Int = stringLength - 1
        if (stringLength == 0 || (length == "normal")) {
            return 0.0
        } else if (length.codePointAt(percentIndex) == '%'.code) {
            return length.substring(0, percentIndex).toDouble() / 100 * relative
        } else {
            val twoLetterUnitIndex: Int = stringLength - 2
            if (twoLetterUnitIndex > 0) {
                val lastTwo: String = length.substring(twoLetterUnitIndex)
                var end: Int = twoLetterUnitIndex
                var unit: Double = 1.0
                when (lastTwo) {
                    "px" -> {}
                    "em" -> unit = fontSize
                    "pt" -> unit = 1.25
                    "pc" -> unit = 15.0
                    "mm" -> unit = 3.543307
                    "cm" -> unit = 35.43307
                    "in" -> unit = 90.0
                    else -> end = stringLength
                }
                return length.substring(0, end).toDouble() * unit * scale
            } else {
                return length.toDouble() * scale
            }
        }
    }

    /**
     * Converts SVGLength into px / user units in the current user coordinate system
     *
     * @param length length string
     * @param relative relative size for percentages
     * @param offset offset for all units
     * @param scale scaling parameter
     * @param fontSize current font size
     * @return value in the current user coordinate system
     */
    fun fromRelative(
            length: SVGLength?, relative: Double, offset: Double, scale: Double, fontSize: Double): Double {
        /*
        TODO list

        unit  relative to
        em    font size of the element
        ex    x-height of the element’s font
        ch    width of the "0" (ZERO, U+0030) glyph in the element’s font
        rem   font size of the root element
        vw    1% of viewport’s width
        vh    1% of viewport’s height
        vmin  1% of viewport’s smaller dimension
        vmax  1% of viewport’s larger dimension

        relative-size [ larger | smaller ]
        absolute-size: [ xx-small | x-small | small | medium | large | x-large | xx-large ]

        https://www.w3.org/TR/css3-values/#relative-lengths
        https://www.w3.org/TR/css3-values/#absolute-lengths
        https://drafts.csswg.org/css-cascade-4/#computed-value
        https://drafts.csswg.org/css-fonts-3/#propdef-font-size
        https://drafts.csswg.org/css2/fonts.html#propdef-font-size
    */
        if (length == null) {
            return offset
        }
        val unitType: UnitType = length.unit
        val value: Double = length.value
        var unit: Double = 1.0
        when (unitType) {
            UnitType.NUMBER, UnitType.PX -> {}
            UnitType.PERCENTAGE -> return value / 100 * relative + offset
            UnitType.EMS -> unit = fontSize
            UnitType.EXS -> unit = fontSize / 2
            UnitType.CM -> unit = 35.43307
            UnitType.MM -> unit = 3.543307
            UnitType.IN -> unit = 90.0
            UnitType.PT -> unit = 1.25
            UnitType.PC -> unit = 15.0
            UnitType.UNKNOWN -> return value * scale + offset
            else -> return value * scale + offset
        }
        return value * unit * scale + offset
    }
}
