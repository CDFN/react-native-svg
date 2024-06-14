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
internal class PathView(reactContext: ReactContext) : RenderableView(reactContext) {
    private var parsedPath: Path? = Path()

    init {
        PathParser.mScale = mScale
    }

    fun setD(d: String?) {
        parsedPath = PathParser.parse(d)
        elements = PathParser.elements
        for (elem in elements!!) {
            for (point in elem.points) {
                point!!.x *= mScale.toDouble()
                point!!.y *= mScale.toDouble()
            }
        }
        invalidate()
    }

    override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        return parsedPath
    }
}
