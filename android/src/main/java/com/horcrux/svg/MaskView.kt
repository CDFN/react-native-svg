/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.horcrux.svg.Brush.BrushUnits

@SuppressLint("ViewConstructor")
internal class MaskView(reactContext: ReactContext) : GroupView(reactContext) {
    var mX: SVGLength? = null
    var mY: SVGLength? = null
    var mW: SVGLength? = null
    var mH: SVGLength? = null

    // TODO implement proper support for units
    @Suppress("unused")
    private var mMaskUnits: BrushUnits? = null

    @Suppress("unused")
    private var mMaskContentUnits: BrushUnits? = null
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

    fun setMaskUnits(maskUnits: Int) {
        when (maskUnits) {
            0 -> mMaskUnits = BrushUnits.OBJECT_BOUNDING_BOX
            1 -> mMaskUnits = BrushUnits.USER_SPACE_ON_USE
        }
        invalidate()
    }

    fun setMaskContentUnits(maskContentUnits: Int) {
        when (maskContentUnits) {
            0 -> mMaskContentUnits = BrushUnits.OBJECT_BOUNDING_BOX
            1 -> mMaskContentUnits = BrushUnits.USER_SPACE_ON_USE
        }
        invalidate()
    }

    public override fun saveDefinition() {
        if (mName != null) {
            val svg = svgView
            svg!!.defineMask(this, mName!!)
        }
    }
}
