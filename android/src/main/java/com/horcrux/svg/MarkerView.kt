/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class MarkerView(reactContext: ReactContext) : GroupView(reactContext) {
    private var mRefX: SVGLength? = null
    private var mRefY: SVGLength? = null
    private var mMarkerWidth: SVGLength? = null
    private var mMarkerHeight: SVGLength? = null
    private var mMarkerUnits: String? = null
    private var mOrient: String? = null
    private var mMinX: Float = 0f
    private var mMinY: Float = 0f
    private var mVbWidth: Float = 0f
    private var mVbHeight: Float = 0f
    var mAlign: String? = null
    var mMeetOrSlice: Int = 0
    var markerTransform: Matrix = Matrix()
    fun setRefX(refX: Dynamic) {
        mRefX = SVGLength.Companion.from(refX)
        invalidate()
    }

    fun setRefY(refY: Dynamic) {
        mRefY = SVGLength.Companion.from(refY)
        invalidate()
    }

    fun setMarkerWidth(markerWidth: Dynamic) {
        mMarkerWidth = SVGLength.Companion.from(markerWidth)
        invalidate()
    }

    fun setMarkerHeight(markerHeight: Dynamic) {
        mMarkerHeight = SVGLength.Companion.from(markerHeight)
        invalidate()
    }

    fun setMarkerUnits(markerUnits: String?) {
        mMarkerUnits = markerUnits
        invalidate()
    }

    fun setOrient(orient: String?) {
        mOrient = orient
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

    public override fun saveDefinition() {
        if (mName != null) {
            val svg: SvgView? = svgView
            svg!!.defineMarker(this, mName!!)
            for (i in 0 until childCount) {
                val node: View = getChildAt(i)
                if (node is VirtualView) {
                    node.saveDefinition()
                }
            }
        }
    }

    fun renderMarker(
            canvas: Canvas, paint: Paint?, opacity: Float, position: RNSVGMarkerPosition, strokeWidth: Float) {
        val count: Int = saveAndSetupCanvas(canvas, mCTM)
        markerTransform.reset()
        val origin: Point? = position.origin
        markerTransform.setTranslate(origin!!.x.toFloat(), origin.y.toFloat())
        val markerAngle: Double = if (("auto" == mOrient)) -1.0 else mOrient!!.toDouble()
        val degrees: Float = 180 + (if (markerAngle == -1.0) position.angle else markerAngle).toFloat()
        markerTransform.preRotate(degrees)
        val useStrokeWidth: Boolean = ("strokeWidth" == mMarkerUnits)
        if (useStrokeWidth) {
            markerTransform.preScale(strokeWidth / mScale, strokeWidth / mScale)
        }
        val width: Double = relativeOnWidth((mMarkerWidth)!!) / mScale
        val height: Double = relativeOnHeight((mMarkerHeight)!!) / mScale
        val eRect: RectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (mAlign != null) {
            val vbRect: RectF = RectF(
                    mMinX * mScale,
                    mMinY * mScale,
                    (mMinX + mVbWidth) * mScale,
                    (mMinY + mVbHeight) * mScale)
            val viewBoxMatrix: Matrix? = ViewBox.getTransform(vbRect, eRect, mAlign, mMeetOrSlice)
            val values: FloatArray = FloatArray(9)
            viewBoxMatrix!!.getValues(values)
            markerTransform.preScale(values.get(Matrix.MSCALE_X), values.get(Matrix.MSCALE_Y))
        }
        val x: Double = relativeOnWidth((mRefX)!!)
        val y: Double = relativeOnHeight((mRefY)!!)
        markerTransform.preTranslate(-x.toFloat(), -y.toFloat())
        canvas.concat(markerTransform)
        drawGroup(canvas, (paint)!!, opacity)
        restoreCanvas(canvas, count)
    }
}
