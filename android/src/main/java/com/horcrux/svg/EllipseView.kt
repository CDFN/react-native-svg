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
import android.graphics.RectF
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class EllipseView(reactContext: ReactContext) : RenderableView(reactContext) {
    private var mCx: SVGLength? = null
    private var mCy: SVGLength? = null
    private var mRx: SVGLength? = null
    private var mRy: SVGLength? = null
    fun setCx(cx: Dynamic) {
        mCx = SVGLength.Companion.from(cx)
        invalidate()
    }

    fun setCy(cy: Dynamic) {
        mCy = SVGLength.Companion.from(cy)
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

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        val path: Path = Path()
        val cx: Double = relativeOnWidth((mCx)!!)
        val cy: Double = relativeOnHeight((mCy)!!)
        val rx: Double = relativeOnWidth((mRx)!!)
        val ry: Double = relativeOnHeight((mRy)!!)
        val oval: RectF = RectF((cx - rx).toFloat(), (cy - ry).toFloat(), (cx + rx).toFloat(), (cy + ry).toFloat())
        path.addOval(oval, Path.Direction.CW)
        elements = ArrayList()
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementMoveToPoint, arrayOf<Point?>(Point(cx, cy - ry))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx, cy - ry), Point(cx + rx, cy))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx + rx, cy), Point(cx, cy + ry))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx, cy + ry), Point(cx - rx, cy))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(cx - rx, cy), Point(cx, cy - ry))))
        return path
    }
}
