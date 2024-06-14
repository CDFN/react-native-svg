/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class ForeignObjectView(reactContext: ReactContext) : GroupView(reactContext) {
    var mX: SVGLength? = null
    var mY: SVGLength? = null
    var mW: SVGLength? = null
    var mH: SVGLength? = null
    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        val x: Float = relativeOnWidth((mX)!!).toFloat()
        val y: Float = relativeOnHeight((mY)!!).toFloat()
        val w: Float = relativeOnWidth((mW)!!).toFloat()
        val h: Float = relativeOnHeight((mH)!!).toFloat()
        canvas.translate(x, y)
        canvas.clipRect(0f, 0f, w, h)
        super.draw(canvas, paint, opacity)
    }

    public override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
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

    public override fun drawGroup(canvas: Canvas, paint: Paint, opacity: Float) {
        pushGlyphContext()
        val svg: SvgView? = this.svgView
        val self: GroupView = this
        val groupRect: RectF = RectF()
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            if (child is MaskView) {
                continue
            }
            if (child is VirtualView) {
                val node: VirtualView = child
                if (("none" == node.mDisplay)) {
                    continue
                }
                if (node is RenderableView) {
                    node.mergeProperties(self)
                }
                val count: Int = node.saveAndSetupCanvas(canvas, mCTM)
                node.render(canvas, paint, opacity * mOpacity)
                val r: RectF? = node.clientRect
                if (r != null) {
                    groupRect.union(r)
                }
                node.restoreCanvas(canvas, count)
                if (node is RenderableView) {
                    node.resetProperties()
                }
                if (node.isResponsible) {
                    svg!!.enableTouchEvents()
                }
            } else if (child is SvgView) {
                val svgView: SvgView = child
                svgView.drawChildren(canvas)
                if (svgView.isResponsible) {
                    svg!!.enableTouchEvents()
                }
            } else {
                // Enable rendering other native ancestor views in e.g. masks
                child.draw(canvas)
            }
        }
        setClientRect(groupRect)
        popGlyphContext()
    }

    // Enable rendering other native ancestor views in e.g. masks, but don't render them another time
    var fakeBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var fake: Canvas = Canvas(fakeBitmap)
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(fake)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return super.drawChild(fake, child, drawingTime)
    }
}
