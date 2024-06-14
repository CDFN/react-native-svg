/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.ReactConstants
import com.horcrux.svg.SVGLength.UnitType

class Brush(private val mType: BrushType, private val mPoints: Array<SVGLength?>, units: BrushUnits?) {
    private var mColors: ReadableArray? = null
    private val mUseObjectBoundingBox: Boolean

    // TODO implement pattern units
    @Suppress("unused")
    private var mUseContentObjectBoundingBoxUnits = false
    private var mMatrix: Matrix? = null
    private var mUserSpaceBoundingBox: Rect? = null
    private var mPattern: PatternView? = null

    init {
        mUseObjectBoundingBox = units == BrushUnits.OBJECT_BOUNDING_BOX
    }

    fun setContentUnits(units: BrushUnits?) {
        mUseContentObjectBoundingBoxUnits = units == BrushUnits.OBJECT_BOUNDING_BOX
    }

    fun setPattern(pattern: PatternView?) {
        mPattern = pattern
    }

    enum class BrushType {
        LINEAR_GRADIENT,
        RADIAL_GRADIENT,
        PATTERN
    }

    enum class BrushUnits {
        OBJECT_BOUNDING_BOX,
        USER_SPACE_ON_USE
    }

    fun setUserSpaceBoundingBox(userSpaceBoundingBox: Rect?) {
        mUserSpaceBoundingBox = userSpaceBoundingBox
    }

    fun setGradientColors(colors: ReadableArray?) {
        mColors = colors
    }

    fun setGradientTransform(matrix: Matrix?) {
        mMatrix = matrix
    }

    private fun getPaintRect(pathBoundingBox: RectF): RectF {
        val rect = if (mUseObjectBoundingBox) pathBoundingBox else RectF(mUserSpaceBoundingBox)
        val width = rect.width()
        val height = rect.height()
        var x = 0f
        var y = 0f
        if (mUseObjectBoundingBox) {
            x = rect.left
            y = rect.top
        }
        return RectF(x, y, x + width, y + height)
    }

    private fun getVal(length: SVGLength?, relative: Double, scale: Float, textSize: Float): Double {
        return PropHelper.fromRelative(
                length,
                relative,
                0.0,
                if (mUseObjectBoundingBox && length!!.unit == UnitType.NUMBER) relative else scale.toDouble(),
                textSize.toDouble())
    }

    fun setupPaint(paint: Paint, pathBoundingBox: RectF, scale: Float, opacity: Float) {
        val rect = getPaintRect(pathBoundingBox)
        val width = rect.width()
        val height = rect.height()
        val offsetX = rect.left
        val offsetY = rect.top
        val textSize = paint.textSize
        if (mType == BrushType.PATTERN) {
            val x = getVal(mPoints[0], width.toDouble(), scale, textSize)
            val y = getVal(mPoints[1], height.toDouble(), scale, textSize)
            val w = getVal(mPoints[2], width.toDouble(), scale, textSize)
            val h = getVal(mPoints[3], height.toDouble(), scale, textSize)
            if (!(w > 1 && h > 1)) {
                return
            }
            val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val vbRect = mPattern?.viewBox
            if (vbRect != null && vbRect.width() > 0 && vbRect.height() > 0) {
                val eRect = RectF(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
                val mViewBoxMatrix = ViewBox.getTransform(vbRect, eRect, mPattern!!.mAlign, mPattern!!.mMeetOrSlice)
                canvas.concat(mViewBoxMatrix)
            }
            if (mUseContentObjectBoundingBoxUnits) {
                canvas.scale(width / scale, height / scale)
            }
            mPattern!!.draw(canvas, Paint(), opacity)
            val patternMatrix = Matrix()
            if (mMatrix != null) {
                patternMatrix.preConcat(mMatrix)
            }
            val bitmapShader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            bitmapShader.setLocalMatrix(patternMatrix)
            paint.setShader(bitmapShader)
            return
        }
        val size = mColors!!.size()
        if (size == 0) {
            FLog.w(ReactConstants.TAG, "Gradient contains no stops")
            return
        }
        val stopsCount = size / 2
        var stopsColors = IntArray(stopsCount)
        var stops = FloatArray(stopsCount)
        parseGradientStops(mColors, stopsCount, stops, stopsColors, opacity)
        if (stops.size == 1) {
            // Gradient with only one stop will make LinearGradient/RadialGradient
            // throw. It may happen when source SVG contains only one stop or
            // two stops at the same spot (see lib/extract/extractGradient.js).
            // Although it's mistake SVGs like this can be produced by vector
            // editors or other tools, so let's handle that gracefully.
            stopsColors = intArrayOf(stopsColors[0], stopsColors[0])
            stops = floatArrayOf(stops[0], stops[0])
            FLog.w(ReactConstants.TAG, "Gradient contains only one stop")
        }
        if (mType == BrushType.LINEAR_GRADIENT) {
            val x1 = getVal(mPoints[0], width.toDouble(), scale, textSize) + offsetX
            val y1 = getVal(mPoints[1], height.toDouble(), scale, textSize) + offsetY
            val x2 = getVal(mPoints[2], width.toDouble(), scale, textSize) + offsetX
            val y2 = getVal(mPoints[3], height.toDouble(), scale, textSize) + offsetY
            val linearGradient: Shader = LinearGradient(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(),
                    stopsColors,
                    stops,
                    Shader.TileMode.CLAMP)
            if (mMatrix != null) {
                val m = Matrix()
                m.preConcat(mMatrix)
                linearGradient.setLocalMatrix(m)
            }
            paint.setShader(linearGradient)
        } else if (mType == BrushType.RADIAL_GRADIENT) {
            var rx = getVal(mPoints[2], width.toDouble(), scale, textSize)
            var ry = getVal(mPoints[3], height.toDouble(), scale, textSize)
            if (rx <= 0 || ry <= 0) {
                // Gradient with radius = 0 should be rendered as solid color of the last stop
                rx = width.toDouble()
                ry = height.toDouble()
                stops = floatArrayOf(stops[0], stops[stops.size - 1])
                stopsColors = intArrayOf(stopsColors[stopsColors.size - 1], stopsColors[stopsColors.size - 1])
            }
            val ratio = ry / rx
            val cx = getVal(mPoints[4], width.toDouble(), scale, textSize) + offsetX
            val cy = getVal(mPoints[5], height / ratio, scale, textSize) + offsetY / ratio

            // TODO: support focus point.
            // double fx = PropHelper.fromRelative(mPoints[0], width, offsetX, scale);
            // double fy = PropHelper.fromRelative(mPoints[1], height, offsetY, scale) / (ry / rx);
            val radialGradient: Shader = RadialGradient(cx.toFloat(), cy.toFloat(), rx.toFloat(), stopsColors, stops, Shader.TileMode.CLAMP)
            val radialMatrix = Matrix()
            radialMatrix.preScale(1f, ratio.toFloat())
            if (mMatrix != null) {
                radialMatrix.preConcat(mMatrix)
            }
            radialGradient.setLocalMatrix(radialMatrix)
            paint.setShader(radialGradient)
        }
    }

    companion object {
        private fun parseGradientStops(
                value: ReadableArray?, stopsCount: Int, stops: FloatArray, stopsColors: IntArray, opacity: Float) {
            for (i in 0 until stopsCount) {
                val stopIndex = i * 2
                stops[i] = value!!.getDouble(stopIndex).toFloat()
                val color = value.getInt(stopIndex + 1)
                val alpha = color ushr 24
                val combined = Math.round(alpha.toFloat() * opacity)
                stopsColors[i] = combined shl 24 or (color and 0x00ffffff)
            }
        }
    }
}
