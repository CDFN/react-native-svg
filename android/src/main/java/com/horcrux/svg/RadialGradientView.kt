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
internal class RadialGradientView(reactContext: ReactContext) : DefinitionView(reactContext) {
    private var mFx: SVGLength? = null
    private var mFy: SVGLength? = null
    private var mRx: SVGLength? = null
    private var mRy: SVGLength? = null
    private var mCx: SVGLength? = null
    private var mCy: SVGLength? = null
    private var mGradient: ReadableArray? = null
    private var mGradientUnits: BrushUnits? = null
    private var mRadialGradientMatrix: Matrix? = null
    fun setFx(fx: Dynamic) {
        mFx = SVGLength.Companion.from(fx)
        invalidate()
    }

    fun setFy(fy: Dynamic) {
        mFy = SVGLength.Companion.from(fy)
        invalidate()
    }

    fun setRx(rx: Dynamic) {
        mRx = SVGLength.Companion.from(rx)
        invalidate()
    }

    fun setRy(ry: Dynamic) {
        mRy = SVGLength.Companion.from(ry)
        invalidate()
    }

    fun setCx(cx: Dynamic) {
        mCx = SVGLength.Companion.from(cx)
        invalidate()
    }

    fun setCy(cy: Dynamic) {
        mCy = SVGLength.Companion.from(cy)
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
                if (mRadialGradientMatrix == null) {
                    mRadialGradientMatrix = Matrix()
                }
                mRadialGradientMatrix!!.setValues(sRawMatrix)
            } else if (matrixSize != -1) {
                FLog.w(ReactConstants.TAG, "RNSVG: Transform matrices must be of size 6")
            }
        } else {
            mRadialGradientMatrix = null
        }
        invalidate()
    }

    public override fun saveDefinition() {
        if (mName != null) {
            val points: Array<SVGLength?> = arrayOf(mFx, mFy, mRx, mRy, mCx, mCy)
            val brush: Brush = Brush(BrushType.RADIAL_GRADIENT, points, mGradientUnits)
            brush.setGradientColors(mGradient)
            if (mRadialGradientMatrix != null) {
                brush.setGradientTransform(mRadialGradientMatrix)
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
