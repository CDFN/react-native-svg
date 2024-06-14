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
import android.os.Build
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class RectView(reactContext: ReactContext?) : RenderableView(reactContext) {
    private var mX: SVGLength? = null
    private var mY: SVGLength? = null
    private var mW: SVGLength? = null
    private var mH: SVGLength? = null
    private var mRx: SVGLength? = null
    private var mRy: SVGLength? = null
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

    fun setRx(rx: Dynamic) {
        mRx = SVGLength.Companion.from(rx)
        invalidate()
    }

    fun setRy(ry: Dynamic) {
        mRy = SVGLength.Companion.from(ry)
        invalidate()
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        val path = Path()
        val x = relativeOnWidth(mX!!)
        val y = relativeOnHeight(mY!!)
        val w = relativeOnWidth(mW!!)
        val h = relativeOnHeight(mH!!)
        if (mRx != null || mRy != null) {
            var rx = 0.0
            var ry = 0.0
            if (mRx == null) {
                ry = relativeOnHeight(mRy!!)
                rx = ry
            } else if (mRy == null) {
                rx = relativeOnWidth(mRx!!)
                ry = rx
            } else {
                rx = relativeOnWidth(mRx!!)
                ry = relativeOnHeight(mRy!!)
            }
            if (rx > w / 2) {
                rx = w / 2
            }
            if (ry > h / 2) {
                ry = h / 2
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                path.addRoundRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), rx.toFloat(), ry.toFloat(),
                        Path.Direction.CW)
            } else {
                path.addRoundRect(
                        RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat()), rx.toFloat(), ry.toFloat(),
                        Path.Direction.CW)
            }
        } else {
            path.addRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), Path.Direction.CW)
            path.close() // Ensure isSimplePath = false such that rect doesn't become represented using
            // integers
        }
        elements = ArrayList()
        elements!!.add(
                PathElement(ElementType.kCGPathElementMoveToPoint, arrayOf<Point?>(Point(x, y))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(x + w, y))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(x + w, y + h))))
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(x, y + h))))
        elements!!.add(
                PathElement(ElementType.kCGPathElementAddLineToPoint, arrayOf<Point?>(Point(x, y))))
        return path
    }
}
