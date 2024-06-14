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
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal open class DefinitionView(reactContext: ReactContext?) : VirtualView(reactContext) {
    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {}
    override var isResponsible: Boolean
        get() {
            return false
        }
        set(isResponsible) {
            super.isResponsible = isResponsible
        }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        return null
    }

    public override fun hitTest(src: FloatArray?): Int {
        return -1
    }
}
