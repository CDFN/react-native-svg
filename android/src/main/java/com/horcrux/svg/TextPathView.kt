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
import com.horcrux.svg.TextProperties.TextPathMethod
import com.horcrux.svg.TextProperties.TextPathMidLine
import com.horcrux.svg.TextProperties.TextPathSide
import com.horcrux.svg.TextProperties.TextPathSpacing

@SuppressLint("ViewConstructor")
internal class TextPathView(reactContext: ReactContext) : TextView(reactContext) {
    private var mHref: String? = null
    var side: TextPathSide? = null
        private set
    var midLine: TextPathMidLine? = null
        private set
    var startOffset: SVGLength? = null
        private set

    @get:Suppress("unused")
    var method: TextPathMethod = TextPathMethod.align
        private set

    @get:Suppress("unused")
    var spacing: TextPathSpacing = TextPathSpacing.exact
        private set

    fun setHref(href: String?) {
        mHref = href
        invalidate()
    }

    fun setStartOffset(startOffset: Dynamic) {
        this.startOffset = SVGLength.Companion.from(startOffset)
        invalidate()
    }

    public override fun setMethod(method: String?) {
        this.method = TextPathMethod.valueOf((method)!!)
        invalidate()
    }

    fun setSpacing(spacing: String?) {
        this.spacing = TextPathSpacing.valueOf((spacing)!!)
        invalidate()
    }

    fun setSide(side: String?) {
        this.side = TextPathSide.valueOf((side)!!)
        invalidate()
    }

    fun setSharp(midLine: String?) {
        this.midLine = TextPathMidLine.valueOf((midLine)!!)
        invalidate()
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        drawGroup(canvas, paint, opacity)
    }

    fun getTextPath(canvas: Canvas?, paint: Paint?): Path? {
        val svg: SvgView? = svgView
        val template: VirtualView? = svg!!.getDefinedTemplate(mHref)
        if (template !is RenderableView) {
            // warning about this.
            return null
        }
        return template.getPath(canvas, paint)
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        return getGroupPath(canvas, paint)
    }

    public override fun pushGlyphContext() {
        // do nothing
    }

    public override fun popGlyphContext() {
        // do nothing
    }
}
