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
import android.graphics.Paint
import android.graphics.Path
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class LineView(reactContext: ReactContext) : RenderableView(reactContext) {
    private var mX1: SVGLength? = null
    private var mY1: SVGLength? = null
    private var mX2: SVGLength? = null
    private var mY2: SVGLength? = null
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

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        val path = Path()
        val x1 = relativeOnWidth(mX1!!)
        val y1 = relativeOnHeight(mY1!!)
        val x2 = relativeOnWidth(mX2!!)
        val y2 = relativeOnHeight(mY2!!)
        path.moveTo(x1.toFloat(), y1.toFloat())
        path.lineTo(x2.toFloat(), y2.toFloat())
        elements = ArrayList()
        elements!!.add(
                PathElement(ElementType.kCGPathElementMoveToPoint, arrayOf<Point?>(Point(x1, y1))))
        elements!!.add(
                PathElement(ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(x2, y2))))
        return path
    }
}
