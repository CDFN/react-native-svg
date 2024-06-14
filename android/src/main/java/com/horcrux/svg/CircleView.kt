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
internal class CircleView(reactContext: ReactContext?) : RenderableView(reactContext) {
    private var mCx: SVGLength? = null
    private var mCy: SVGLength? = null
    private var mR: SVGLength? = null
    fun setCx(cx: Dynamic) {
        mCx = SVGLength.Companion.from(cx)
        invalidate()
    }

    fun setCy(cy: Dynamic) {
        mCy = SVGLength.Companion.from(cy)
        invalidate()
    }

    fun setR(r: Dynamic) {
        mR = SVGLength.Companion.from(r)
        invalidate()
    }

     override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        val path: Path = Path()
        val cx: Double = relativeOnWidth((mCx)!!)
        val cy: Double = relativeOnHeight((mCy)!!)
        val r: Double = relativeOnOther((mR)!!)
        path.addCircle(cx.toFloat(), cy.toFloat(), r.toFloat(), Path.Direction.CW)
        elements = ArrayList()
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementMoveToPoint, arrayOf<Point?>(Point(cx, cy - r))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx, cy - r), Point(cx + r, cy))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx + r, cy), Point(cx, cy + r))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx, cy + r), Point(cx - r, cy))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx - r, cy), Point(cx, cy - r))))
        return path
    }
}
