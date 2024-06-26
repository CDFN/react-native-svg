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
import android.graphics.RectF
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.ReactConstants
import com.horcrux.svg.Brush.BrushType
import com.horcrux.svg.Brush.BrushUnits

@SuppressLint("ViewConstructor")
class PatternView(reactContext: ReactContext) : GroupView(reactContext) {
    private var mX: SVGLength? = null
    private var mY: SVGLength? = null
    private var mW: SVGLength? = null
    private var mH: SVGLength? = null
    private var mPatternUnits: BrushUnits? = null
    private var mPatternContentUnits: BrushUnits? = null
    private var mMinX: Float = 0f
    private var mMinY: Float = 0f
    private var mVbWidth: Float = 0f
    private var mVbHeight: Float = 0f
    var mAlign: String? = null
    var mMeetOrSlice: Int = 0
    var mPatternMatrix: Matrix? = null

    fun setX(x: Dynamic) {
        mX = SVGLength.Companion.from(x)
        invalidate()
    }

    fun setY(y: Dynamic) {
        mY = SVGLength.Companion.from(y)
        invalidate()
    }

    fun setWidth(width: Dynamic) {
        mW = SVGLength.Companion.from(width)
        invalidate()
    }

    fun setHeight(height: Dynamic) {
        mH = SVGLength.Companion.from(height)
        invalidate()
    }

    fun setPatternUnits(patternUnits: Int) {
        when (patternUnits) {
            0 -> mPatternUnits = BrushUnits.OBJECT_BOUNDING_BOX
            1 -> mPatternUnits = BrushUnits.USER_SPACE_ON_USE
        }
        invalidate()
    }

    fun setPatternContentUnits(patternContentUnits: Int) {
        when (patternContentUnits) {
            0 -> mPatternContentUnits = BrushUnits.OBJECT_BOUNDING_BOX
            1 -> mPatternContentUnits = BrushUnits.USER_SPACE_ON_USE
        }
        invalidate()
    }

    fun setPatternTransform(matrixArray: ReadableArray?) {
        if (matrixArray != null) {
            val matrixSize: Int = PropHelper.toMatrixData(matrixArray, sRawMatrix, mScale)
            if (matrixSize == 6) {
                if (mPatternMatrix == null) {
                    mPatternMatrix = Matrix()
                }
                mPatternMatrix!!.setValues(sRawMatrix)
            } else if (matrixSize != -1) {
                FLog.w(ReactConstants.TAG, "RNSVG: Transform matrices must be of size 6")
            }
        } else {
            mPatternMatrix = null
        }
        invalidate()
    }

    fun setMinX(minX: Float) {
        mMinX = minX
        invalidate()
    }

    fun setMinY(minY: Float) {
        mMinY = minY
        invalidate()
    }

    fun setVbWidth(vbWidth: Float) {
        mVbWidth = vbWidth
        invalidate()
    }

    fun setVbHeight(vbHeight: Float) {
        mVbHeight = vbHeight
        invalidate()
    }

    fun setAlign(align: String?) {
        mAlign = align
        invalidate()
    }

    fun setMeetOrSlice(meetOrSlice: Int) {
        mMeetOrSlice = meetOrSlice
        invalidate()
    }

    val viewBox: RectF
        get() {
            return RectF(
                    mMinX * mScale, mMinY * mScale, (mMinX + mVbWidth) * mScale, (mMinY + mVbHeight) * mScale)
        }

    public override fun saveDefinition() {
        if (mName != null) {
            val points: Array<SVGLength?> = arrayOf(mX, mY, mW, mH)
            val brush: Brush = Brush(BrushType.PATTERN, points, mPatternUnits)
            brush.setContentUnits(mPatternContentUnits)
            brush.setPattern(this)
            if (mPatternMatrix != null) {
                brush.setGradientTransform(mPatternMatrix)
            }
            val svg: SvgView? = svgView
            if ((mPatternUnits == BrushUnits.USER_SPACE_ON_USE
                            || mPatternContentUnits == BrushUnits.USER_SPACE_ON_USE)) {
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
