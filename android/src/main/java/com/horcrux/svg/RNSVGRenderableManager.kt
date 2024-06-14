/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Region
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.horcrux.svg.RNSVGRenderableManager
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.annotation.Nonnull
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

const val SVG_RENDERABLE_MODULE_NAME: String = "RNSVGRenderableModule"

@ReactModule(name = SVG_RENDERABLE_MODULE_NAME)
internal class RNSVGRenderableManager(reactContext: ReactApplicationContext?) : NativeSvgRenderableModuleSpec(reactContext) {
    @Nonnull
    public override fun getName(): String {
        return SVG_RENDERABLE_MODULE_NAME
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun isPointInFill(tag: Double?, options: ReadableMap?): Boolean {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return false
        }
        val scale: Float = svg.mScale
        val x: Float = options!!.getDouble("x").toFloat() * scale
        val y: Float = options.getDouble("y").toFloat() * scale
        val i: Int = svg.hitTest(floatArrayOf(x, y))
        return i != -1
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun isPointInStroke(tag: Double?, options: ReadableMap?): Boolean {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return false
        }
        try {
            svg.getPath(null, null)
        } catch (e: NullPointerException) {
            svg.invalidate()
            return false
        }
        svg.initBounds()
        val scale: Float = svg.mScale
        val x: Int = (options!!.getDouble("x") * scale).toInt()
        val y: Int = (options.getDouble("y") * scale).toInt()
        val strokeRegion: Region? = svg.mStrokeRegion
        return strokeRegion != null && strokeRegion.contains(x, y)
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun getTotalLength(tag: Double?): Double {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return 0.0
        }
        val path: Path?
        try {
            path = svg.getPath(null, null)
        } catch (e: NullPointerException) {
            svg.invalidate()
            return (-1).toDouble()
        }
        val pm: PathMeasure = PathMeasure(path, false)
        return (pm.getLength() / svg.mScale).toDouble()
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun getPointAtLength(tag: Double?, options: ReadableMap?): WritableMap {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return Arguments.createMap()
        }
        val path: Path?
        try {
            path = svg.getPath(null, null)
        } catch (e: NullPointerException) {
            svg.invalidate()
            return Arguments.createMap()
        }
        val pm: PathMeasure = PathMeasure(path, false)
        val length: Float = options!!.getDouble("length").toFloat()
        val scale: Float = svg.mScale
        val pos: FloatArray = FloatArray(2)
        val tan: FloatArray = FloatArray(2)
        val distance: Float = max(0.toFloat(), min(length * scale, pm.length))
        pm.getPosTan(distance, pos, tan)
        val angle: Double = atan2(tan.get(1).toDouble(), tan.get(0).toDouble())
        val result: WritableMap = Arguments.createMap()
        result.putDouble("x", (pos.get(0) / scale).toDouble())
        result.putDouble("y", (pos.get(1) / scale).toDouble())
        result.putDouble("angle", angle)
        return result
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun getBBox(tag: Double?, options: ReadableMap?): WritableMap {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return Arguments.createMap()
        }
        val fill: Boolean = options!!.getBoolean("fill")
        val stroke: Boolean = options.getBoolean("stroke")
        val markers: Boolean = options.getBoolean("markers")
        val clipped: Boolean = options.getBoolean("clipped")
        try {
            svg.getPath(null, null)
        } catch (e: NullPointerException) {
            svg.invalidate()
            return Arguments.createMap()
        }
        val scale: Float = svg.mScale
        svg.initBounds()
        val bounds: RectF = RectF()
        val fillBounds: RectF? = svg.mFillBounds
        val strokeBounds: RectF? = svg.mStrokeBounds
        val markerBounds: RectF? = svg.mMarkerBounds
        val clipBounds: RectF? = svg.mClipBounds
        if (fill && fillBounds != null) {
            bounds.union(fillBounds)
        }
        if (stroke && strokeBounds != null) {
            bounds.union(strokeBounds)
        }
        if (markers && markerBounds != null) {
            bounds.union(markerBounds)
        }
        if (clipped && clipBounds != null) {
            bounds.intersect(clipBounds)
        }
        val result: WritableMap = Arguments.createMap()
        result.putDouble("x", (bounds.left / scale).toDouble())
        result.putDouble("y", (bounds.top / scale).toDouble())
        result.putDouble("width", (bounds.width() / scale).toDouble())
        result.putDouble("height", (bounds.height() / scale).toDouble())
        return result
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun getCTM(tag: Double?): WritableMap {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return Arguments.createMap()
        }
        val scale: Float = svg.mScale
        val ctm: Matrix = Matrix(svg.mCTM)
        val svgView: SvgView = svg.svgView
                ?: throw RuntimeException("Did not find parent SvgView for view with tag: " + tag)
        val invViewBoxMatrix: Matrix? = svgView.mInvViewBoxMatrix
        ctm.preConcat(invViewBoxMatrix)
        val values: FloatArray = FloatArray(9)
        ctm.getValues(values)
        val result: WritableMap = Arguments.createMap()
        result.putDouble("a", values.get(Matrix.MSCALE_X).toDouble())
        result.putDouble("b", values.get(Matrix.MSKEW_Y).toDouble())
        result.putDouble("c", values.get(Matrix.MSKEW_X).toDouble())
        result.putDouble("d", values.get(Matrix.MSCALE_Y).toDouble())
        result.putDouble("e", (values.get(Matrix.MTRANS_X) / scale).toDouble())
        result.putDouble("f", (values.get(Matrix.MTRANS_Y) / scale).toDouble())
        return result
    }

    @Suppress("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public override fun getScreenCTM(tag: Double?): WritableMap {
        val svg: RenderableView? = VirtualViewManager.Companion.getRenderableViewByTag(tag!!.toInt())
        if (svg == null) {
            return Arguments.createMap()
        }
        val values: FloatArray = FloatArray(9)
        svg.mCTM.getValues(values)
        val scale: Float = svg.mScale
        val result: WritableMap = Arguments.createMap()
        result.putDouble("a", values.get(Matrix.MSCALE_X).toDouble())
        result.putDouble("b", values.get(Matrix.MSKEW_Y).toDouble())
        result.putDouble("c", values.get(Matrix.MSKEW_X).toDouble())
        result.putDouble("d", values.get(Matrix.MSCALE_Y).toDouble())
        result.putDouble("e", (values.get(Matrix.MTRANS_X) / scale).toDouble())
        result.putDouble("f", (values.get(Matrix.MTRANS_Y) / scale).toDouble())
        return result
    }

    @ReactMethod
    public override fun getRawResource(name: String, promise: Promise) {
        try {
            val context: ReactApplicationContext = getReactApplicationContext()
            val resources: Resources = context.getResources()
            val packageName: String = context.getPackageName()
            val id: Int = resources.getIdentifier(name, "raw", packageName)
            val stream: InputStream = resources.openRawResource(id)
            try {
                val reader: InputStreamReader = InputStreamReader(stream, StandardCharsets.UTF_8)
                val buffer: CharArray = CharArray(DEFAULT_BUFFER_SIZE)
                val builder: StringBuilder = StringBuilder()
                var n: Int
                while ((reader.read(buffer, 0, DEFAULT_BUFFER_SIZE).also({ n = it })) != EOF) {
                    builder.append(buffer, 0, n)
                }
                val result: String = builder.toString()
                promise.resolve(result)
            } finally {
                try {
                    stream.close()
                } catch (ioe: IOException) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject(e)
        }
    }

    companion object {
        private val EOF: Int = -1
        private val DEFAULT_BUFFER_SIZE: Int = 1024 * 4
    }
}
