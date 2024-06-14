/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import com.facebook.react.bridge.ReadableMap
import kotlin.math.min

// https://www.w3.org/TR/SVG/text.html#TSpanElement
class GlyphContext(// Constructor parameters
        private val mScale: Float, val width: Float, val height: Float) {
    // Current stack (one per node push/pop)
    val mFontContext: ArrayList<FontData?> = ArrayList()

    // Unique input attribute lists (only added if node sets a value)
    private val mXsContext: ArrayList<Array<SVGLength?>> = ArrayList()
    private val mYsContext: ArrayList<Array<SVGLength?>> = ArrayList()
    private val mDXsContext: ArrayList<Array<SVGLength?>> = ArrayList()
    private val mDYsContext: ArrayList<Array<SVGLength?>> = ArrayList()
    private val mRsContext: ArrayList<DoubleArray> = ArrayList()

    // Unique index into attribute list (one per unique list)
    private val mXIndices: ArrayList<Int> = ArrayList()
    private val mYIndices: ArrayList<Int> = ArrayList()
    private val mDXIndices: ArrayList<Int> = ArrayList()
    private val mDYIndices: ArrayList<Int> = ArrayList()
    private val mRIndices: ArrayList<Int> = ArrayList()

    // Index of unique context used (one per node push/pop)
    private val mXsIndices: ArrayList<Int> = ArrayList()
    private val mYsIndices: ArrayList<Int> = ArrayList()
    private val mDXsIndices: ArrayList<Int> = ArrayList()
    private val mDYsIndices: ArrayList<Int> = ArrayList()
    private val mRsIndices: ArrayList<Int> = ArrayList()

    /**
     * Get font size from context.
     *
     *
     * ‘font-size’ Value: < absolute-size > | < relative-size > | < length > | < percentage > |
     * inherit Initial: medium Applies to: text content elements Inherited: yes, the computed value is
     * inherited Percentages: refer to parent element's font size Media: visual Animatable: yes
     *
     *
     * This property refers to the size of the font from baseline to baseline when multiple lines
     * of text are set solid in a multiline layout environment.
     *
     *
     * For SVG, if a < length > is provided without a unit identifier (e.g., an unqualified number
     * such as 128), the SVG user agent processes the < length > as a height value in the current user
     * coordinate system.
     *
     *
     * If a < length > is provided with one of the unit identifiers (e.g., 12pt or 10%), then the
     * SVG user agent converts the < length > into a corresponding value in the current user
     * coordinate system by applying the rules described in Units.
     *
     *
     * Except for any additional information provided in this specification, the normative
     * definition of the property is in CSS2 ([CSS2], section 15.2.4).
     */
    // Calculated on push context, percentage and em length depends on parent font size
    var fontSize: Double = FontData.Companion.DEFAULT_FONT_SIZE
        private set
    var font: FontData? = FontData.Companion.Defaults
        private set

    // Current accumulated values
    // https://www.w3.org/TR/SVG/types.html#DataTypeCoordinate
    // <coordinate> syntax is the same as that for <length>
    private var mX: Double = 0.0
    private var mY: Double = 0.0

    // https://www.w3.org/TR/SVG/types.html#Length
    private var mDX: Double = 0.0
    private var mDY: Double = 0.0

    // Current <list-of-coordinates> SVGLengthList
    // https://www.w3.org/TR/SVG/types.html#InterfaceSVGLengthList
    // https://www.w3.org/TR/SVG/types.html#DataTypeCoordinates
    // https://www.w3.org/TR/SVG/text.html#TSpanElementXAttribute
    private var mXs: Array<SVGLength?> = arrayOf()

    // https://www.w3.org/TR/SVG/text.html#TSpanElementYAttribute
    private var mYs: Array<SVGLength?> = arrayOf()

    // Current <list-of-lengths> SVGLengthList
    // https://www.w3.org/TR/SVG/types.html#DataTypeLengths
    // https://www.w3.org/TR/SVG/text.html#TSpanElementDXAttribute
    private var mDXs: Array<SVGLength?> = arrayOf()

    // https://www.w3.org/TR/SVG/text.html#TSpanElementDYAttribute
    private var mDYs: Array<SVGLength?> = arrayOf()

    // Current <list-of-numbers> SVGLengthList
    // https://www.w3.org/TR/SVG/types.html#DataTypeNumbers
    // https://www.w3.org/TR/SVG/text.html#TSpanElementRotateAttribute
    private var mRs: DoubleArray = doubleArrayOf(0.0)

    // Current attribute list index
    private var mXsIndex: Int = 0
    private var mYsIndex: Int = 0
    private var mDXsIndex: Int = 0
    private var mDYsIndex: Int = 0
    private var mRsIndex: Int = 0

    // Current value index in current attribute list
    private var mXIndex: Int = -1
    private var mYIndex: Int = -1
    private var mDXIndex: Int = -1
    private var mDYIndex: Int = -1
    private var mRIndex: Int = -1

    // Top index of stack
    private var mTop: Int = 0
    private fun pushIndices() {
        mXsIndices.add(mXsIndex)
        mYsIndices.add(mYsIndex)
        mDXsIndices.add(mDXsIndex)
        mDYsIndices.add(mDYsIndex)
        mRsIndices.add(mRsIndex)
    }

    init {
        mXsContext.add(mXs)
        mYsContext.add(mYs)
        mDXsContext.add(mDXs)
        mDYsContext.add(mDYs)
        mRsContext.add(mRs)
        mXIndices.add(mXIndex)
        mYIndices.add(mYIndex)
        mDXIndices.add(mDXIndex)
        mDYIndices.add(mDYIndex)
        mRIndices.add(mRIndex)
        mFontContext.add(font)
        pushIndices()
    }

    private fun reset() {
        mRsIndex = 0
        mDYsIndex = mRsIndex
        mDXsIndex = mDYsIndex
        mYsIndex = mDXsIndex
        mXsIndex = mYsIndex
        mRIndex = -1
        mDYIndex = mRIndex
        mDXIndex = mDYIndex
        mYIndex = mDXIndex
        mXIndex = mYIndex
        mDY = 0.0
        mDX = mDY
        mY = mDX
        mX = mY
    }

    private fun getTopOrParentFont(child: GroupView): FontData? {
        if (mTop > 0) {
            return font
        } else {
            var parentRoot: GroupView? = child.parentTextRoot
            while (parentRoot != null) {
                val map: FontData? = parentRoot.glyphContext?.font
                if (map !== FontData.Defaults) {
                    return map
                }
                parentRoot = parentRoot.parentTextRoot
            }
            return FontData.Defaults
        }
    }

    private fun pushNodeAndFont(node: GroupView, font: ReadableMap?) {
        val parent: FontData? = getTopOrParentFont(node)
        mTop++
        if (font == null) {
            mFontContext.add(parent)
            return
        }
        val data: FontData = FontData(font, parent, mScale.toDouble())
        fontSize = data.fontSize
        mFontContext.add(data)
        this.font = data
    }

    fun pushContext(node: GroupView, font: ReadableMap?) {
        pushNodeAndFont(node, font)
        pushIndices()
    }

    private fun getStringArrayFromReadableArray(readableArray: ArrayList<SVGLength>): Array<SVGLength?> {
        val size: Int = readableArray.size
        val strings: Array<SVGLength?> = arrayOfNulls(size)
        for (i in 0 until size) {
            strings[i] = readableArray[i]
        }
        return strings
    }

    private fun getDoubleArrayFromReadableArray(readableArray: ArrayList<SVGLength>): DoubleArray {
        val size: Int = readableArray.size
        val doubles: DoubleArray = DoubleArray(size)
        for (i in 0 until size) {
            val length: SVGLength = readableArray[i]
            doubles[i] = length.value
        }
        return doubles
    }

    fun pushContext(
            reset: Boolean,
            node: TextView,
            font: ReadableMap?,
            x: ArrayList<SVGLength>?,
            y: ArrayList<SVGLength>?,
            deltaX: ArrayList<SVGLength>?,
            deltaY: ArrayList<SVGLength>?,
            rotate: ArrayList<SVGLength>?) {
        if (reset) {
            reset()
        }
        pushNodeAndFont(node, font)
        if (x != null && x.size != 0) {
            mXsIndex++
            mXIndex = -1
            mXIndices.add(mXIndex)
            mXs = getStringArrayFromReadableArray(x)
            mXsContext.add(mXs)
        }
        if (y != null && y.size != 0) {
            mYsIndex++
            mYIndex = -1
            mYIndices.add(mYIndex)
            mYs = getStringArrayFromReadableArray(y)
            mYsContext.add(mYs)
        }
        if (deltaX != null && deltaX.size != 0) {
            mDXsIndex++
            mDXIndex = -1
            mDXIndices.add(mDXIndex)
            mDXs = getStringArrayFromReadableArray(deltaX)
            mDXsContext.add(mDXs)
        }
        if (deltaY != null && deltaY.size != 0) {
            mDYsIndex++
            mDYIndex = -1
            mDYIndices.add(mDYIndex)
            mDYs = getStringArrayFromReadableArray(deltaY)
            mDYsContext.add(mDYs)
        }
        if (rotate != null && rotate.size != 0) {
            mRsIndex++
            mRIndex = -1
            mRIndices.add(mRIndex)
            mRs = getDoubleArrayFromReadableArray(rotate)
            mRsContext.add(mRs)
        }
        pushIndices()
    }

    fun popContext() {
        mFontContext.removeAt(mTop)
        mXsIndices.removeAt(mTop)
        mYsIndices.removeAt(mTop)
        mDXsIndices.removeAt(mTop)
        mDYsIndices.removeAt(mTop)
        mRsIndices.removeAt(mTop)
        mTop--
        val x: Int = mXsIndex
        val y: Int = mYsIndex
        val dx: Int = mDXsIndex
        val dy: Int = mDYsIndex
        val r: Int = mRsIndex
        font = mFontContext.get(mTop)
        mXsIndex = mXsIndices.get(mTop)
        mYsIndex = mYsIndices.get(mTop)
        mDXsIndex = mDXsIndices.get(mTop)
        mDYsIndex = mDYsIndices.get(mTop)
        mRsIndex = mRsIndices.get(mTop)
        if (x != mXsIndex) {
            mXsContext.removeAt(x)
            mXs = mXsContext.get(mXsIndex)
            mXIndex = mXIndices.get(mXsIndex)
        }
        if (y != mYsIndex) {
            mYsContext.removeAt(y)
            mYs = mYsContext.get(mYsIndex)
            mYIndex = mYIndices.get(mYsIndex)
        }
        if (dx != mDXsIndex) {
            mDXsContext.removeAt(dx)
            mDXs = mDXsContext.get(mDXsIndex)
            mDXIndex = mDXIndices.get(mDXsIndex)
        }
        if (dy != mDYsIndex) {
            mDYsContext.removeAt(dy)
            mDYs = mDYsContext.get(mDYsIndex)
            mDYIndex = mDYIndices.get(mDYsIndex)
        }
        if (r != mRsIndex) {
            mRsContext.removeAt(r)
            mRs = mRsContext.get(mRsIndex)
            mRIndex = mRIndices.get(mRsIndex)
        }
    }

    // https://www.w3.org/TR/SVG11/text.html#FontSizeProperty
    fun nextX(advance: Double): Double {
        incrementIndices(mXIndices, mXsIndex)
        val nextIndex: Int = mXIndex + 1
        if (nextIndex < mXs.size) {
            mDX = 0.0
            mXIndex = nextIndex
            val string: SVGLength? = mXs.get(nextIndex)
            mX = PropHelper.fromRelative(string, width.toDouble(), 0.0, mScale.toDouble(), fontSize)
        }
        mX += advance
        return mX
    }

    fun nextY(): Double {
        incrementIndices(mYIndices, mYsIndex)
        val nextIndex: Int = mYIndex + 1
        if (nextIndex < mYs.size) {
            mDY = 0.0
            mYIndex = nextIndex
            val string: SVGLength? = mYs.get(nextIndex)
            mY = PropHelper.fromRelative(string, height.toDouble(), 0.0, mScale.toDouble(), fontSize)
        }
        return mY
    }

    fun nextDeltaX(): Double {
        incrementIndices(mDXIndices, mDXsIndex)
        val nextIndex: Int = mDXIndex + 1
        if (nextIndex < mDXs.size) {
            mDXIndex = nextIndex
            val string: SVGLength? = mDXs.get(nextIndex)
            val `val`: Double = PropHelper.fromRelative(string, width.toDouble(), 0.0, mScale.toDouble(), fontSize)
            mDX += `val`
        }
        return mDX
    }

    fun nextDeltaY(): Double {
        incrementIndices(mDYIndices, mDYsIndex)
        val nextIndex: Int = mDYIndex + 1
        if (nextIndex < mDYs.size) {
            mDYIndex = nextIndex
            val string: SVGLength? = mDYs.get(nextIndex)
            val `val`: Double = PropHelper.fromRelative(string, height.toDouble(), 0.0, mScale.toDouble(), fontSize)
            mDY += `val`
        }
        return mDY
    }

    fun nextRotation(): Double {
        incrementIndices(mRIndices, mRsIndex)
        mRIndex = min(mRIndex + 1, mRs.size - 1)
        return mRs.get(mRIndex)
    }

    companion object {
        private fun incrementIndices(indices: ArrayList<Int>, topIndex: Int) {
            for (index in topIndex downTo 0) {
                val xIndex: Int = indices.get(index)
                indices.set(index, xIndex + 1)
            }
        }
    }
}
