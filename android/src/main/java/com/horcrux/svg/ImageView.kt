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
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.ReactConstants
import com.facebook.react.views.imagehelper.ImageSource
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.Nonnull

@SuppressLint("ViewConstructor")
internal class ImageView(reactContext: ReactContext) : RenderableView(reactContext) {
    private var mX: SVGLength? = null
    private var mY: SVGLength? = null
    private var mW: SVGLength? = null
    private var mH: SVGLength? = null
    private var uriString: String? = null
    private var mImageWidth = 0
    private var mImageHeight = 0
    private var mAlign: String? = null
    private var mMeetOrSlice = 0
    private val mLoading = AtomicBoolean(false)
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

    fun setSrc(src: ReadableMap?) {
        if (src != null) {
            uriString = src.getString("uri")
            if (uriString == null || uriString!!.isEmpty()) {
                // TODO: give warning about this
                return
            }
            if (src.hasKey("width") && src.hasKey("height")) {
                mImageWidth = src.getInt("width")
                mImageHeight = src.getInt("height")
            } else {
                mImageWidth = 0
                mImageHeight = 0
            }
            val mUri = Uri.parse(uriString)
            if (mUri.scheme == null) {
                ResourceDrawableIdHelper.getInstance().getResourceDrawableUri(mContext!!, uriString)
            }
        }
    }

    fun setAlign(align: String?) {
        mAlign = align
        invalidate()
    }

    fun setMeetOrSlice(meetOrSlice: Int) {
        mMeetOrSlice = meetOrSlice
        invalidate()
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        if (!mLoading.get()) {
            val imagePipeline = Fresco.getImagePipeline()
            val imageSource = ImageSource(mContext, uriString)
            val request = ImageRequest.fromUri(imageSource.uri)
            val inMemoryCache = imagePipeline.isInBitmapMemoryCache(request!!)
            if (inMemoryCache) {
                tryRenderFromBitmapCache(imagePipeline, request, canvas, paint, opacity * mOpacity)
            } else {
                loadBitmap(imagePipeline, request)
            }
        }
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        mPath = Path()
        mPath!!.addRect(rect, Path.Direction.CW)
        return mPath
    }

    private fun loadBitmap(imagePipeline: ImagePipeline, request: ImageRequest?) {
        mLoading.set(true)
        val dataSource = imagePipeline.fetchDecodedImage(request, mContext)
        val subscriber: BaseBitmapDataSubscriber = object : BaseBitmapDataSubscriber() {
            public override fun onNewResultImpl(bitmap: Bitmap?) {
                mLoading.set(false)
                val view = svgView
                view?.invalidate()
            }

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                // No cleanup required here.
                // TODO: more details about this failure
                mLoading.set(false)
                FLog.w(
                        ReactConstants.TAG,
                        dataSource.failureCause!!,
                        "RNSVG: fetchDecodedImage failed!")
            }
        }
        dataSource.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance())
    }

    @get:Nonnull
    private val rect: RectF
        private get() {
            val x = relativeOnWidth(mX!!)
            val y = relativeOnHeight(mY!!)
            var w = relativeOnWidth(mW!!)
            var h = relativeOnHeight(mH!!)
            if (w == 0.0) {
                w = (mImageWidth * mScale).toDouble()
            }
            if (h == 0.0) {
                h = (mImageHeight * mScale).toDouble()
            }
            return RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
        }

    private fun doRender(canvas: Canvas, paint: Paint, bitmap: Bitmap, opacity: Float) {
        if (mImageWidth == 0 || mImageHeight == 0) {
            mImageWidth = bitmap.getWidth()
            mImageHeight = bitmap.getHeight()
        }
        val renderRect = rect
        val vbRect = RectF(0f, 0f, mImageWidth.toFloat(), mImageHeight.toFloat())
        val transform = ViewBox.getTransform(vbRect, renderRect, mAlign, mMeetOrSlice)
        transform!!.mapRect(vbRect)
        canvas.clipPath(getPath(canvas, paint)!!)
        val clipPath = getClipPath(canvas, paint)
        if (clipPath != null) {
            canvas.clipPath(clipPath)
        }
        val alphaPaint = Paint()
        alphaPaint.setAlpha((opacity * 255).toInt())
        canvas.drawBitmap(bitmap, null, vbRect, alphaPaint)
        mCTM.mapRect(vbRect)
        setClientRect(vbRect)
    }

    private fun tryRenderFromBitmapCache(
            imagePipeline: ImagePipeline,
            request: ImageRequest?,
            canvas: Canvas,
            paint: Paint,
            opacity: Float) {
        val dataSource = imagePipeline.fetchImageFromBitmapCache(request!!, mContext)
        try {
            val imageReference = dataSource.result ?: return
            try {
                val closeableImage = imageReference.get() as? CloseableBitmap ?: return
                val bitmap = closeableImage.underlyingBitmap ?: return
                doRender(canvas, paint, bitmap, opacity)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            } finally {
                CloseableReference.closeSafely(imageReference)
            }
        } catch (e: Exception) {
            throw IllegalStateException(e)
        } finally {
            dataSource.close()
        }
    }
}
