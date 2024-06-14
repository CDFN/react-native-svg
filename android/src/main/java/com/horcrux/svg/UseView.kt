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
import android.graphics.Path
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.common.ReactConstants

@SuppressLint("ViewConstructor")
internal class UseView(reactContext: ReactContext?) : RenderableView(reactContext) {
    private var mHref: String? = null
    private var mX: SVGLength? = null
    private var mY: SVGLength? = null
    private var mW: SVGLength? = null
    private var mH: SVGLength? = null
    fun setHref(href: String?) {
        mHref = href
        invalidate()
    }

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

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        val template = svgView?.getDefinedTemplate(mHref)
        if (template == null) {
            FLog.w(
                    ReactConstants.TAG,
                    "`Use` element expected a pre-defined svg template as `href` prop, "
                            + "template named: "
                            + mHref
                            + " is not defined.")
            return
        }
        template.clearCache()
        canvas.translate(relativeOnWidth(mX!!).toFloat(), relativeOnHeight(mY!!).toFloat())
        if (template is RenderableView) {
            (template as RenderableView).mergeProperties(this)
        }
        val count = template.saveAndSetupCanvas(canvas, mCTM)
        clip(canvas, paint)
        if (template is SymbolView) {
            val symbol = template as SymbolView
            symbol.drawSymbol(
                    canvas, paint, opacity, relativeOnWidth(mW!!).toFloat(), relativeOnHeight(mH!!).toFloat())
        } else {
            template.draw(canvas, paint, opacity * mOpacity)
        }
        setClientRect(template.clientRect!!)
        template.restoreCanvas(canvas, count)
        if (template is RenderableView) {
            (template as RenderableView).resetProperties()
        }
    }

    public override fun hitTest(src: FloatArray?): Int {
        if (!mInvertible || !mTransformInvertible) {
            return -1
        }
        val dst = FloatArray(2)
        mInvMatrix.mapPoints(dst, src)
        mInvTransform.mapPoints(dst)
        val template = svgView?.getDefinedTemplate(mHref)
        if (template == null) {
            FLog.w(
                    ReactConstants.TAG,
                    "`Use` element expected a pre-defined svg template as `href` prop, "
                            + "template named: "
                            + mHref
                            + " is not defined.")
            return -1
        }
        val hitChild = template.hitTest(dst)
        return if (hitChild != -1) {
            if (template.isResponsible || hitChild != template.id) hitChild else id
        } else -1
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        val template = svgView?.getDefinedTemplate(mHref)
        if (template == null) {
            FLog.w(
                    ReactConstants.TAG,
                    "`Use` element expected a pre-defined svg template as `href` prop, "
                            + "template named: "
                            + mHref
                            + " is not defined.")
            return null
        }
        val path = template.getPath(canvas, paint)
        val use = Path()
        val m = Matrix()
        m.setTranslate(relativeOnWidth(mX!!).toFloat(), relativeOnHeight(mY!!).toFloat())
        path!!.transform(m, use)
        return use
    }
}
