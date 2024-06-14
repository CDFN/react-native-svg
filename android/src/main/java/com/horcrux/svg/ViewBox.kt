/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

internal object ViewBox {
    private const val MOS_MEET = 0
    private const val MOS_SLICE = 1
    private const val MOS_NONE = 2
    fun getTransform(vbRect: RectF, eRect: RectF, align: String?, meetOrSlice: Int): Matrix {
        // based on https://svgwg.org/svg2-draft/coords.html#ComputingAViewportsTransform

        // Let vb-x, vb-y, vb-width, vb-height be the min-x, min-y, width and height values of the
        // viewBox attribute respectively.
        val vbX = vbRect.left.toDouble()
        val vbY = vbRect.top.toDouble()
        val vbWidth = vbRect.width().toDouble()
        val vbHeight = vbRect.height().toDouble()

        // Let e-x, e-y, e-width, e-height be the position and size of the element respectively.
        val eX = eRect.left.toDouble()
        val eY = eRect.top.toDouble()
        val eWidth = eRect.width().toDouble()
        val eHeight = eRect.height().toDouble()

        // Initialize scale-x to e-width/vb-width.
        var scaleX = eWidth / vbWidth

        // Initialize scale-y to e-height/vb-height.
        var scaleY = eHeight / vbHeight

        // Initialize translate-x to e-x - (vb-x * scale-x).
        // Initialize translate-y to e-y - (vb-y * scale-y).
        var translateX = eX - vbX * scaleX
        var translateY = eY - vbY * scaleY

        // If align is 'none'
        if (meetOrSlice == MOS_NONE) {
            // Let scale be set the smaller value of scale-x and scale-y.
            // Assign scale-x and scale-y to scale.
            scaleY = min(scaleX, scaleY)
            scaleX = scaleY
            val scale = scaleX

            // If scale is greater than 1
            if (scale > 1) {
                // Minus translateX by (eWidth / scale - vbWidth) / 2
                // Minus translateY by (eHeight / scale - vbHeight) / 2
                translateX -= (eWidth / scale - vbWidth) / 2
                translateY -= (eHeight / scale - vbHeight) / 2
            } else {
                translateX -= (eWidth - vbWidth * scale) / 2
                translateY -= (eHeight - vbHeight * scale) / 2
            }
        } else {
            // If align is not 'none' and meetOrSlice is 'meet', set the larger of scale-x and scale-y to
            // the smaller.
            // Otherwise, if align is not 'none' and meetOrSlice is 'slice', set the smaller of scale-x
            // and scale-y to the larger.
            if (align != "none" && meetOrSlice == MOS_MEET) {
                scaleY = min(scaleX, scaleY)
                scaleX = scaleY
            } else if (align != "none" && meetOrSlice == MOS_SLICE) {
                scaleY = max(scaleX, scaleY)
                scaleX = scaleY
            }

            // If align contains 'xMid', add (e-width - vb-width * scale-x) / 2 to translate-x.
            if (align!!.contains("xMid")) {
                translateX += (eWidth - vbWidth * scaleX) / 2.0
            }

            // If align contains 'xMax', add (e-width - vb-width * scale-x) to translate-x.
            if (align.contains("xMax")) {
                translateX += eWidth - vbWidth * scaleX
            }

            // If align contains 'yMid', add (e-height - vb-height * scale-y) / 2 to translate-y.
            if (align.contains("YMid")) {
                translateY += (eHeight - vbHeight * scaleY) / 2.0
            }

            // If align contains 'yMax', add (e-height - vb-height * scale-y) to translate-y.
            if (align.contains("YMax")) {
                translateY += eHeight - vbHeight * scaleY
            }
        }

        // The transform applied to content contained by the element is given by
        // translate(translate-x, translate-y) scale(scale-x, scale-y).
        val transform = Matrix()
        transform.postTranslate(translateX.toFloat(), translateY.toFloat())
        transform.preScale(scaleX.toFloat(), scaleY.toFloat())
        return transform
    }
}
