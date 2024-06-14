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
import android.graphics.Region
import android.os.Build
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType

@SuppressLint("ViewConstructor")
open class GroupView(reactContext: ReactContext) : RenderableView(reactContext) {
    var mFont: ReadableMap? = null
    override var glyphContext: GlyphContext? = null

    fun setFont(dynamic: Dynamic) {
        mFont = if (dynamic.type == ReadableType.Map) {
            dynamic.asMap()
        } else {
            null
        }
        invalidate()
    }

    fun setFont(font: ReadableMap?) {
        mFont = font
        invalidate()
    }

    fun setupGlyphContext(canvas: Canvas) {
        val clipBounds = RectF(canvas.getClipBounds())
        if (mMatrix != null) {
            mMatrix!!.mapRect(clipBounds)
        }
        mTransform.mapRect(clipBounds)
        this.glyphContext = GlyphContext(mScale, clipBounds.width(), clipBounds.height())
    }

    val textRootGlyphContext: GlyphContext
        get() = requireNonNull(textRoot).glyphContext!!

    open fun pushGlyphContext() {
        textRootGlyphContext.pushContext(this, mFont)
    }

    open fun popGlyphContext() {
        textRootGlyphContext.popContext()
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        setupGlyphContext(canvas)
        clip(canvas, paint)
        drawGroup(canvas, paint, opacity)
        renderMarkers(canvas, paint, opacity)
    }

    open fun drawGroup(canvas: Canvas, paint: Paint, opacity: Float) {
        pushGlyphContext()
        val svg = svgView
        val self = this
        val groupRect = RectF()
        elements = ArrayList()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is MaskView) {
                continue
            }
            if (child is VirtualView) {
                val node = child
                if ("none" == node.mDisplay) {
                    continue
                }
                if (node is RenderableView) {
                    node.mergeProperties(self)
                }
                val count = node.saveAndSetupCanvas(canvas, mCTM)
                node.render(canvas, paint, opacity * mOpacity)
                val r = node.clientRect
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
                if (node.elements != null) {
                    elements!!.addAll(node.elements!!)
                }
            } else if (child is SvgView) {
                val svgView = child
                svgView.drawChildren(canvas)
                if (svgView.isResponsible) {
                    svg!!.enableTouchEvents()
                }
            }
        }
        setClientRect(groupRect)
        popGlyphContext()
    }

    fun drawPath(canvas: Canvas, paint: Paint, opacity: Float) {
        super.draw(canvas, paint, opacity)
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        if (mPath != null) {
            return mPath
        }
        mPath = Path()
        for (i in 0 until childCount) {
            val node = getChildAt(i)
            if (node is MaskView) {
                continue
            }
            if (node is VirtualView) {
                val transform = node.mMatrix
                mPath!!.addPath(node.getPath(canvas, paint)!!, transform!!)
            }
        }
        return mPath
    }

    open fun getPath(canvas: Canvas, paint: Paint?, op: Region.Op): Path? {
        val path = Path()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val pop = Path.Op.valueOf(op.name)
            for (i in 0 until childCount) {
                val node = getChildAt(i)
                if (node is MaskView) {
                    continue
                }
                if (node is VirtualView) {
                    val n = node
                    val transform = n.mMatrix
                    var p2: Path?
                    p2 = if (n is GroupView) {
                        n.getPath(canvas, paint, op)
                    } else {
                        n.getPath(canvas, paint)
                    }
                    p2!!.transform(transform!!)
                    path.op(p2, pop)
                }
            }
        } else {
            val clipBounds = canvas.getClipBounds()
            val bounds = Region(clipBounds)
            val r = Region()
            for (i in 0 until childCount) {
                val node = getChildAt(i)
                if (node is MaskView) {
                    continue
                }
                if (node is VirtualView) {
                    val n = node
                    val transform = n.mMatrix
                    var p2: Path?
                    p2 = if (n is GroupView) {
                        n.getPath(canvas, paint, op)
                    } else {
                        n.getPath(canvas, paint)
                    }
                    if (transform != null) {
                        p2!!.transform(transform)
                    }
                    val r2 = Region()
                    r2.setPath(p2!!, bounds)
                    r.op(r2, op)
                }
            }
            path.addPath(r.getBoundaryPath())
        }
        return path
    }

    public override fun hitTest(src: FloatArray?): Int {
        if (!mInvertible || !mTransformInvertible) {
            return -1
        }
        val dst = FloatArray(2)
        mInvMatrix.mapPoints(dst, src)
        mInvTransform.mapPoints(dst)
        val x = Math.round(dst[0])
        val y = Math.round(dst[1])
        val clipPath = clipPath
        if (clipPath != null) {
            if (mClipRegionPath !== clipPath) {
                mClipRegionPath = clipPath
                mClipBounds = RectF()
                clipPath.computeBounds(mClipBounds!!, true)
                mClipRegion = getRegion(clipPath, mClipBounds!!)
            }
            if (!mClipRegion!!.contains(x, y)) {
                return -1
            }
        }
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is VirtualView) {
                if (child is MaskView) {
                    continue
                }
                val node = child
                val hitChild = node.hitTest(dst)
                if (hitChild != -1) {
                    return if (node.isResponsible || hitChild != child.getId()) hitChild else id
                }
            } else if (child is SvgView) {
                val hitChild = child.reactTagForTouch(dst[0], dst[1])
                if (hitChild != child.getId()) {
                    return hitChild
                }
            }
        }
        return -1
    }

    public override fun saveDefinition() {
        if (mName != null) {
            svgView?.defineTemplate(this, mName)
        }
        for (i in 0 until childCount) {
            val node = getChildAt(i)
            if (node is VirtualView) {
                node.saveDefinition()
            }
        }
    }

    public override fun resetProperties() {
        for (i in 0 until childCount) {
            val node = getChildAt(i)
            if (node is RenderableView) {
                node.resetProperties()
            }
        }
    }

    companion object {
        private fun <T> requireNonNull(obj: T?): T {
            if (obj == null) throw NullPointerException()
            return obj
        }
    }
}
