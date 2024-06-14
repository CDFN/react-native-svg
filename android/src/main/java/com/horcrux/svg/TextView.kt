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
import android.graphics.Region
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.horcrux.svg.TextProperties.AlignmentBaseline
import com.horcrux.svg.TextProperties.TextAnchor
import com.horcrux.svg.TextProperties.TextLengthAdjust

@SuppressLint("ViewConstructor")
open class TextView(reactContext: ReactContext) : GroupView(reactContext) {
    var mInlineSize: SVGLength? = null
    var mTextLength: SVGLength? = null
    private var mBaselineShift: String? = null
    var mLengthAdjust = TextLengthAdjust.spacing
    private var mAlignmentBaseline: AlignmentBaseline? = null
    private var mPositionX: ArrayList<SVGLength>? = null
    private var mPositionY: ArrayList<SVGLength>? = null
    private var mRotate: ArrayList<SVGLength>? = null
    private var mDeltaX: ArrayList<SVGLength>? = null
    private var mDeltaY: ArrayList<SVGLength>? = null
    var cachedAdvance = Double.NaN
    override fun invalidate() {
        if (mPath == null) {
            return
        }
        super.invalidate()
        textContainer.clearChildCache()
    }

    public override fun clearCache() {
        cachedAdvance = Double.NaN
        super.clearCache()
    }

    fun setInlineSize(inlineSize: Dynamic) {
        mInlineSize = SVGLength.Companion.from(inlineSize)
        invalidate()
    }

    fun setTextLength(length: Dynamic) {
        mTextLength = SVGLength.Companion.from(length)
        invalidate()
    }

    fun setLengthAdjust(adjustment: String?) {
        mLengthAdjust = TextLengthAdjust.valueOf(adjustment!!)
        invalidate()
    }

    open fun setMethod(alignment: String?) {
        mAlignmentBaseline = AlignmentBaseline.Companion.getEnum(alignment)
        invalidate()
    }

    fun setBaselineShift(baselineShift: Dynamic?) {
        mBaselineShift = SVGLength.Companion.toString(baselineShift)
        invalidate()
    }

    fun setVerticalAlign(dynamicVerticalAlign: Dynamic?) {
        var verticalAlign: String? = SVGLength.toString(dynamicVerticalAlign)
        if (verticalAlign != null) {
            verticalAlign = verticalAlign.trim { it <= ' ' }
            val i = verticalAlign.lastIndexOf(' ')
            mAlignmentBaseline = try {
                AlignmentBaseline.Companion.getEnum(verticalAlign.substring(i))
            } catch (e: IllegalArgumentException) {
                AlignmentBaseline.baseline
            }
            mBaselineShift = try {
                verticalAlign.substring(0, i)
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        } else {
            mAlignmentBaseline = AlignmentBaseline.baseline
            mBaselineShift = null
        }
        invalidate()
    }

    fun setRotate(rotate: Dynamic) {
        mRotate = SVGLength.Companion.arrayFrom(rotate)
        invalidate()
    }

    fun setDeltaX(deltaX: Dynamic) {
        mDeltaX = SVGLength.Companion.arrayFrom(deltaX)
        invalidate()
    }

    fun setDeltaY(deltaY: Dynamic) {
        mDeltaY = SVGLength.Companion.arrayFrom(deltaY)
        invalidate()
    }

    fun setPositionX(positionX: Dynamic) {
        mPositionX = SVGLength.Companion.arrayFrom(positionX)
        invalidate()
    }

    fun setPositionY(positionY: Dynamic) {
        mPositionY = SVGLength.Companion.arrayFrom(positionY)
        invalidate()
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        setupGlyphContext(canvas)
        clip(canvas, paint)
        getGroupPath(canvas, paint)
        pushGlyphContext()
        drawGroup(canvas, paint, opacity)
        popGlyphContext()
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        if (mPath != null) {
            return mPath
        }
        setupGlyphContext(canvas!!)
        return getGroupPath(canvas, paint)
    }

    public override fun getPath(canvas: Canvas, paint: Paint?, op: Region.Op): Path? {
        return getPath(canvas, paint)
    }

    val alignmentBaseline: AlignmentBaseline
        get() {
            if (mAlignmentBaseline == null) {
                var parent = this.parent
                while (parent != null) {
                    if (parent is TextView) {
                        val baseline = parent.mAlignmentBaseline
                        if (baseline != null) {
                            mAlignmentBaseline = baseline
                            return baseline
                        }
                    }
                    parent = parent.parent
                }
            }
            if (mAlignmentBaseline == null) {
                mAlignmentBaseline = AlignmentBaseline.baseline
            }
            return mAlignmentBaseline!!
        }
    val baselineShift: String?
        get() {
            if (mBaselineShift == null) {
                var parent = this.parent
                while (parent != null) {
                    if (parent is TextView) {
                        val baselineShift = parent.mBaselineShift
                        if (baselineShift != null) {
                            mBaselineShift = baselineShift
                            return baselineShift
                        }
                    }
                    parent = parent.parent
                }
            }
            return mBaselineShift
        }

    fun getGroupPath(canvas: Canvas?, paint: Paint?): Path {
        if (mPath != null) {
            return mPath!!
        }
        pushGlyphContext()
        mPath = super.getPath(canvas, paint)
        popGlyphContext()
        return mPath!!
    }

    public override fun pushGlyphContext() {
        val isTextNode = this !is TextPathView && this !is TSpanView
        textRootGlyphContext?.pushContext(isTextNode, this, mFont, mPositionX, mPositionY, mDeltaX, mDeltaY, mRotate)
    }

    val textAnchorRoot: TextView
        get() {
            val gc = textRootGlyphContext
            val font = gc!!.mFontContext
            var node = this
            var parent = this.parent
            for (i in font!!.indices.reversed()) {
                if (parent !is TextView || font[i]!!.textAnchor == TextAnchor.start || node.mPositionX != null) {
                    return node
                }
                node = parent
                parent = node.parent
            }
            return node
        }

    open fun getSubtreeTextChunksTotalAdvance(paint: Paint?): Double {
        if (!java.lang.Double.isNaN(cachedAdvance)) {
            return cachedAdvance
        }
        var advance = 0.0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                advance += child.getSubtreeTextChunksTotalAdvance(paint)
            }
        }
        cachedAdvance = advance
        return advance
    }

    val textContainer: TextView
        get() {
            var node = this
            var parent = this.parent
            while (parent is TextView) {
                node = parent
                parent = node.parent
            }
            return node
        }
}
