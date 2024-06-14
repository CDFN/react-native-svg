/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import android.graphics.Matrix
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.ReactConstants
import com.horcrux.svg.Brush.BrushType
import com.horcrux.svg.Brush.BrushUnits

@SuppressLint("ViewConstructor")
internal class LinearGradientView(reactContext: ReactContext?) : DefinitionView(reactContext) {
    private var mX1: SVGLength? = null
    private var mY1: SVGLength? = null
    private var mX2: SVGLength? = null
    private var mY2: SVGLength? = null
    private var mGradient: ReadableArray? = null
    private var mGradientUnits: BrushUnits? = null
    override var mMatrix: Matrix? = null
    fun setX1(x1: Dynamic) {
        mX1 = SVGLength.Companion.from(x1)
        invalidate()
    }

    fun setY1(y1: Dynamic) {
        mY1 = SVGLength.Companion.from(y1)
        invalidate()
    }

    fun setX2(x2: Dynamic) {
        mX2 = SVGLength.Companion.from(x2)
        invalidate()
    }

    fun setY2(y2: Dynamic) {
        mY2 = SVGLength.Companion.from(y2)
        invalidate()
    }

    fun setGradient(gradient: ReadableArray?) {
        mGradient = gradient
        invalidate()
    }

    fun setGradientUnits(gradientUnits: Int) {
        when (gradientUnits) {
            0 -> mGradientUnits = BrushUnits.OBJECT_BOUNDING_BOX
            1 -> mGradientUnits = BrushUnits.USER_SPACE_ON_USE
        }
        invalidate()
    }

    fun setGradientTransform(matrixArray: ReadableArray?) {
        if (matrixArray != null) {
            val matrixSize: Int = PropHelper.toMatrixData(matrixArray, sRawMatrix, mScale)
            if (matrixSize == 6) {
                if (mMatrix == null) {
                    mMatrix = Matrix()
                }
                mMatrix!!.setValues(sRawMatrix)
            } else if (matrixSize != -1) {
                FLog.w(ReactConstants.TAG, "RNSVG: Transform matrices must be of size 6")
            }
        } else {
            mMatrix = null
        }
        invalidate()
    }

    public override fun saveDefinition() {
        if (mName != null) {
            val points: Array<SVGLength?> = arrayOf(mX1, mY1, mX2, mY2)
            val brush: Brush = Brush(BrushType.LINEAR_GRADIENT, points, mGradientUnits)
            brush.setGradientColors(mGradient)
            if (mMatrix != null) {
                brush.setGradientTransform(mMatrix)
            }
            val svg: SvgView? = svgView
            if (mGradientUnits == BrushUnits.USER_SPACE_ON_USE) {
                brush.setUserSpaceBoundingBox(svg?.canvasBounds)
            }
            svg!!.defineBrush(brush, mName!!)
        }
    }

    companion object {
        private val sRawMatrix: FloatArray = floatArrayOf(
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f
        )
    }
}
