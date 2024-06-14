/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Base64
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.DisplayMetricsHolder
import com.facebook.react.uimanager.ReactCompoundView
import com.facebook.react.uimanager.ReactCompoundViewGroup
import com.facebook.react.views.view.ReactViewGroup
import java.io.ByteArrayOutputStream
import javax.annotation.Nonnull
import kotlin.math.log10

/** Custom [View] implementation that draws an RNSVGSvg React view and its children.  */
@SuppressLint("ViewConstructor")
class SvgView(reactContext: ReactContext) : ReactViewGroup(reactContext), ReactCompoundView, ReactCompoundViewGroup {
    override fun interceptsTouchEvent(touchX: Float, touchY: Float): Boolean {
        return true
    }

    @Suppress("unused")
    enum class Events(private val mName: String) {
        EVENT_DATA_URL("onDataURL");

        @Nonnull
        override fun toString(): String {
            return mName
        }
    }

    private var mBitmap: Bitmap? = null
    private var mRemovalTransitionStarted = false
    override fun setId(id: Int) {
        super.setId(id)
        SvgViewManager.setSvgView(id, this)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        val r = Rect()
        val isVisible = this.getGlobalVisibleRect(r)
        info.isVisibleToUser = isVisible
        info.setClassName(this.javaClass.getCanonicalName())
    }

    override fun invalidate() {
        super.invalidate()
        val parent = parent
        if (parent is VirtualView) {
            if (!mRendered) {
                return
            }
            mRendered = false
            parent.svgView?.invalidate()
            return
        }
        if (!mRemovalTransitionStarted) {
            // when view is removed from the view hierarchy, we want to recycle the mBitmap when
            // the view is detached from window, in order to preserve it for during animation, see
            // https://github.com/react-native-svg/react-native-svg/pull/1542
            if (mBitmap != null) {
                mBitmap!!.recycle()
            }
            mBitmap = null
        }
    }

    override fun startViewTransition(view: View) {
        mRemovalTransitionStarted = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mBitmap != null) {
            mBitmap!!.recycle()
        }
        mBitmap = null
    }

    override fun onDraw(canvas: Canvas) {
        if (parent is VirtualView) {
            return
        }
        super.onDraw(canvas)
        if (mBitmap == null) {
            mBitmap = drawOutput()
        }
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap!!, 0f, 0f, null)
            if (toDataUrlTask != null) {
                toDataUrlTask!!.run()
                toDataUrlTask = null
            }
        }
    }

    private var toDataUrlTask: Runnable? = null
    fun setToDataUrlTask(task: Runnable?) {
        toDataUrlTask = task
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.invalidate()
    }

    override fun reactTagForTouch(touchX: Float, touchY: Float): Int {
        return hitTest(touchX, touchY)
    }

    var isResponsible = false
        private set
    private val mDefinedClipPaths: MutableMap<String, VirtualView> = HashMap()
    private val mDefinedTemplates: MutableMap<String?, VirtualView> = HashMap()
    private val mDefinedMarkers: MutableMap<String, VirtualView> = HashMap()
    private val mDefinedMasks: MutableMap<String, VirtualView> = HashMap()
    private val mDefinedBrushes: MutableMap<String, Brush> = HashMap()
    private var mCanvas: Canvas? = null
    private val mScale: Float
    private var mMinX = 0f
    private var mMinY = 0f
    private var mVbWidth = 0f
    private var mVbHeight = 0f
    private var mbbWidth: SVGLength? = null
    private var mbbHeight: SVGLength? = null
    private var mAlign: String? = null
    private var mMeetOrSlice = 0
    val mInvViewBoxMatrix = Matrix()
    private var mInvertible = true
    private var mRendered = false
    var mTintColor = 0

    init {
        mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density
        // for some reason on Fabric the `onDraw` won't be called without it
        setWillNotDraw(false)
    }

    fun notRendered(): Boolean {
        return !mRendered
    }

    private fun clearChildCache() {
        if (!mRendered) {
            return
        }
        mRendered = false
        for (i in 0 until childCount) {
            val node = getChildAt(i)
            if (node is VirtualView) {
                node.clearChildCache()
            }
        }
    }

    fun setTintColor(tintColor: Int?) {
        mTintColor = tintColor ?: 0
        invalidate()
        clearChildCache()
    }

    fun setMinX(minX: Float) {
        mMinX = minX
        invalidate()
        clearChildCache()
    }

    fun setMinY(minY: Float) {
        mMinY = minY
        invalidate()
        clearChildCache()
    }

    fun setVbWidth(vbWidth: Float) {
        mVbWidth = vbWidth
        invalidate()
        clearChildCache()
    }

    fun setVbHeight(vbHeight: Float) {
        mVbHeight = vbHeight
        invalidate()
        clearChildCache()
    }

    fun setBbWidth(bbWidth: Dynamic) {
        mbbWidth = SVGLength.Companion.from(bbWidth)
        invalidate()
        clearChildCache()
    }

    fun setBbHeight(bbHeight: Dynamic) {
        mbbHeight = SVGLength.Companion.from(bbHeight)
        invalidate()
        clearChildCache()
    }

    fun setAlign(align: String?) {
        mAlign = align
        invalidate()
        clearChildCache()
    }

    fun setMeetOrSlice(meetOrSlice: Int) {
        mMeetOrSlice = meetOrSlice
        invalidate()
        clearChildCache()
    }

    private fun drawOutput(): Bitmap? {
        mRendered = true
        val width = width.toFloat()
        val height = height.toFloat()
        val invalid = (java.lang.Float.isNaN(width)
                || java.lang.Float.isNaN(height) || width < 1) || height < 1 || log10(width.toDouble()) + log10(height.toDouble()) > 42
        if (invalid) {
            return null
        }
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        drawChildren(Canvas(bitmap))
        return bitmap
    }

    val canvasBounds: Rect
        get() = mCanvas!!.getClipBounds()

    @SuppressLint("WrongConstant")
    @Synchronized
    fun drawChildren(canvas: Canvas) {
        mRendered = true
        mCanvas = canvas
        var mViewBoxMatrix: Matrix? = Matrix()
        if (mAlign != null) {
            val vbRect = viewBox
            var width = canvas.width.toFloat()
            var height = canvas.height.toFloat()
            val nested = parent is VirtualView
            if (nested) {
                width = PropHelper.fromRelative(mbbWidth, width.toDouble(), 0.0, mScale.toDouble(), 12.0).toFloat()
                height = PropHelper.fromRelative(mbbHeight, height.toDouble(), 0.0, mScale.toDouble(), 12.0).toFloat()
            }
            val eRect = RectF(0f, 0f, width, height)
            if (nested) {
                canvas.clipRect(eRect)
            }
            mViewBoxMatrix = ViewBox.getTransform(vbRect, eRect, mAlign, mMeetOrSlice)
            mInvertible = mViewBoxMatrix.invert(mInvViewBoxMatrix)
            canvas.concat(mViewBoxMatrix)
        }
        val paint = Paint()
        paint.flags = Paint.ANTI_ALIAS_FLAG or Paint.DEV_KERN_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG
        paint.setTypeface(Typeface.DEFAULT)
        for (i in 0 until childCount) {
            val node = getChildAt(i)
            if (node is VirtualView) {
                node.saveDefinition()
            }
        }
        for (i in 0 until childCount) {
            val lNode = getChildAt(i)
            if (lNode is VirtualView) {
                val node = lNode
                val count = node.saveAndSetupCanvas(canvas, mViewBoxMatrix)
                node.render(canvas, paint, 1f)
                node.restoreCanvas(canvas, count)
                if (node.isResponsible && !isResponsible) {
                    isResponsible = true
                }
            }
        }
    }

    private val viewBox: RectF
        private get() = RectF(
                mMinX * mScale, mMinY * mScale, (mMinX + mVbWidth) * mScale, (mMinY + mVbHeight) * mScale)

    fun toDataURL(): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        clearChildCache()
        drawChildren(Canvas(bitmap))
        clearChildCache()
        this.invalidate()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        val bitmapBytes = stream.toByteArray()
        return Base64.encodeToString(bitmapBytes, Base64.NO_WRAP)
    }

    fun toDataURL(width: Int, height: Int): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        clearChildCache()
        drawChildren(Canvas(bitmap))
        clearChildCache()
        this.invalidate()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        val bitmapBytes = stream.toByteArray()
        return Base64.encodeToString(bitmapBytes, Base64.NO_WRAP)
    }

    fun enableTouchEvents() {
        if (!isResponsible) {
            isResponsible = true
        }
    }

    private fun hitTest(touchX: Float, touchY: Float): Int {
        if (!isResponsible || !mInvertible) {
            return id
        }
        val transformed = floatArrayOf(touchX, touchY)
        mInvViewBoxMatrix.mapPoints(transformed)
        val count = childCount
        var viewTag = -1
        for (i in count - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is VirtualView) {
                viewTag = child.hitTest(transformed)
            } else if (child is SvgView) {
                viewTag = child.hitTest(touchX, touchY)
            }
            if (viewTag != -1) {
                break
            }
        }
        return if (viewTag == -1) id else viewTag
    }

    fun defineClipPath(clipPath: VirtualView, clipPathRef: String) {
        mDefinedClipPaths[clipPathRef] = clipPath
    }

    fun getDefinedClipPath(clipPathRef: String): VirtualView? {
        return mDefinedClipPaths[clipPathRef]
    }

    fun defineTemplate(template: VirtualView, templateRef: String?) {
        mDefinedTemplates[templateRef] = template
    }

    fun getDefinedTemplate(templateRef: String?): VirtualView? {
        return mDefinedTemplates[templateRef]
    }

    fun defineBrush(brush: Brush, brushRef: String) {
        mDefinedBrushes[brushRef] = brush
    }

    fun getDefinedBrush(brushRef: String): Brush? {
        return mDefinedBrushes[brushRef]
    }

    fun defineMask(mask: VirtualView, maskRef: String) {
        mDefinedMasks[maskRef] = mask
    }

    fun getDefinedMask(maskRef: String): VirtualView? {
        return mDefinedMasks[maskRef]
    }

    fun defineMarker(marker: VirtualView, markerRef: String) {
        mDefinedMarkers[markerRef] = marker
    }

    fun getDefinedMarker(markerRef: String?): VirtualView? {
        return mDefinedMarkers[markerRef]
    }
}
