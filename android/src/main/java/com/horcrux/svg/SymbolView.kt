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
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class SymbolView(reactContext: ReactContext) : GroupView(reactContext) {
    private var mMinX: Float = 0f
    private var mMinY: Float = 0f
    private var mVbWidth: Float = 0f
    private var mVbHeight: Float = 0f
    private var mAlign: String? = null
    private var mMeetOrSlice: Int = 0
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

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        saveDefinition()
    }

    fun drawSymbol(canvas: Canvas, paint: Paint, opacity: Float, width: Float, height: Float) {
        if (mAlign != null) {
            val vbRect: RectF = RectF(
                    mMinX * mScale,
                    mMinY * mScale,
                    (mMinX + mVbWidth) * mScale,
                    (mMinY + mVbHeight) * mScale)
            val eRect: RectF = RectF(0f, 0f, width, height)
            val viewBoxMatrix: Matrix? = ViewBox.getTransform(vbRect, eRect, mAlign, mMeetOrSlice)
            canvas.concat(viewBoxMatrix)
            super.draw(canvas, paint, opacity)
        }
    }
}
