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
import com.facebook.react.bridge.ReactContext

@SuppressLint("ViewConstructor")
internal class DefsView(reactContext: ReactContext?) : DefinitionView(reactContext) {
    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {}
    public override fun saveDefinition() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is VirtualView) {
                child.saveDefinition()
            }
        }
    }
}
