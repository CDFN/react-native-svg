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
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.ReactContext
import com.facebook.react.common.ReactConstants

@SuppressLint("ViewConstructor")
internal class ClipPathView(reactContext: ReactContext) : GroupView(reactContext) {
    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        FLog.w(
                ReactConstants.TAG,
                "RNSVG: ClipPath can't be drawn, it should be defined as a child component for `Defs` ")
    }

    public override fun saveDefinition() {
        svgView?.defineClipPath(this, mName!!)
    }

    override var isResponsible: Boolean
        get() {
            return false
        }
        set(isResponsible) {
            super.isResponsible = isResponsible
        }

    public override fun hitTest(src: FloatArray?): Int {
        return -1
    }

    public override fun mergeProperties(target: RenderableView) {}
    public override fun resetProperties() {}
}
