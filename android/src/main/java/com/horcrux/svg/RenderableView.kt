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
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.Path.FillType
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import com.facebook.react.bridge.ColorPropConverter
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.JSApplicationIllegalArgumentException
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.touch.ReactHitSlopView
import com.facebook.react.uimanager.PointerEvents
import java.lang.reflect.Method
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor

const val JOIN_ROUND: Int = 1
const val CAP_ROUND: Int = 1
const val FILL_RULE_NONZERO: Int = 1

abstract class RenderableView internal constructor(reactContext: ReactContext) : VirtualView(reactContext), ReactHitSlopView {
    // static final int VECTOR_EFFECT_INHERIT = 2;
    // static final int VECTOR_EFFECT_URI = 3;
    /*
  Used in mergeProperties, keep public
  */
    var vectorEffect: Int = VECTOR_EFFECT_DEFAULT
        set(value) {
            field = value
            invalidate()
        }
    var stroke: ReadableArray? = null
    var strokeDasharray: Array<SVGLength?>? = null
    var strokeWidth: SVGLength? = SVGLength(1.0)
    var strokeOpacity: Float = 1f
        set(value) {
            field = value
            invalidate()
        }
    var strokeMiterlimit: Float = 4f
        set(value) {
            field = value
            invalidate()
        }
    var strokeDashoffset: Float = 0f
        set(value) {
            field = value * mScale
            invalidate()
        }
    var strokeLinecap: Cap = Cap.BUTT
    var strokeLinejoin: Join = Join.MITER
    var fill: ReadableArray? = null
    var fillOpacity: Float = 1f
        set(value) {
            field = value
            invalidate()
        }
    var fillRule: FillType = FillType.WINDING

    /*
  End merged properties
  */
    private var mLastMergedList: ArrayList<String> = ArrayList()
    private var mOriginProperties: ArrayList<Any?> = ArrayList()
    private var mPropList: ArrayList<String> = ArrayList()
    private var attributeList: ArrayList<String> = ArrayList()


    override fun getHitSlopRect(): Rect? {
        return if (mPointerEvents == PointerEvents.BOX_NONE) {
            Rect(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        } else null
    }

    init {
        setPivotX(0f)
        setPivotY(0f)
    }

    public override fun setId(id: Int) {
        super.setId(id)
        VirtualViewManager.setRenderableView(id, this)
    }

    fun setFill(fill: Dynamic?) {
        if (fill == null || fill.isNull) {
            this.fill = null
            invalidate()
            return
        }
        val fillType: ReadableType = fill.type
        if ((fillType == ReadableType.Map)) {
            val fillMap: ReadableMap = fill.asMap()
            setFill(fillMap)
            return
        }

        // This code will probably never be reached with current changes
        if ((fillType == ReadableType.Number)) {
            this.fill = JavaOnlyArray.of(0, fill.asInt())
        } else if ((fillType == ReadableType.Array)) {
            this.fill = fill.asArray()
        } else {
            val arr: JavaOnlyArray = JavaOnlyArray()
            arr.pushInt(0)
            val m: Matcher = regex.matcher(fill.asString())
            var i: Int = 0
            while (m.find()) {
                val parsed: Double = m.group().toDouble()
                arr.pushDouble(if (i++ < 3) parsed / 255 else parsed)
            }
            this.fill = arr
        }
        invalidate()
    }

    fun setFill(fill: ReadableMap?) {
        if (fill == null) {
            this.fill = null
            invalidate()
            return
        }
        val type: Int = fill.getInt("type")
        if (type == 0) {
            val valueType: ReadableType = fill.getType("payload")
            if ((valueType == ReadableType.Number)) {
                this.fill = JavaOnlyArray.of(0, fill.getInt("payload"))
            } else if ((valueType == ReadableType.Map)) {
                this.fill = JavaOnlyArray.of(0, fill.getMap("payload"))
            }
        } else if (type == 1) {
            this.fill = JavaOnlyArray.of(1, fill.getString("brushRef"))
        } else {
            this.fill = JavaOnlyArray.of(type)
        }
        invalidate()
    }

    fun setFillRule(fillRule: Int) {
        when (fillRule) {
            FILL_RULE_EVENODD -> this.fillRule = FillType.EVEN_ODD
            FILL_RULE_NONZERO -> {}
            else -> throw JSApplicationIllegalArgumentException("fillRule " + fillRule + " unrecognized")
        }
        invalidate()
    }

    fun setStroke(strokeColors: Dynamic?) {
        if (strokeColors == null || strokeColors.isNull()) {
            stroke = null
            invalidate()
            return
        }
        val strokeType: ReadableType = strokeColors.getType()
        if ((strokeType == ReadableType.Map)) {
            val strokeMap: ReadableMap = strokeColors.asMap()
            setStroke(strokeMap)
            return
        }

        // This code will probably never be reached with current changes
        val type: ReadableType = strokeColors.getType()
        if ((type == ReadableType.Number)) {
            stroke = JavaOnlyArray.of(0, strokeColors.asInt())
        } else if ((type == ReadableType.Array)) {
            stroke = strokeColors.asArray()
        } else {
            val arr: JavaOnlyArray = JavaOnlyArray()
            arr.pushInt(0)
            val m: Matcher = regex.matcher(strokeColors.asString())
            var i: Int = 0
            while (m.find()) {
                val parsed: Double = m.group().toDouble()
                arr.pushDouble(if (i++ < 3) parsed / 255 else parsed)
            }
            stroke = arr
        }
        invalidate()
    }

    fun setStroke(stroke: ReadableMap?) {
        if (stroke == null) {
            this.stroke = null
            invalidate()
            return
        }
        val type: Int = stroke.getInt("type")
        if (type == 0) {
            val payloadType: ReadableType = stroke.getType("payload")
            if ((payloadType == ReadableType.Number)) {
                this.stroke = JavaOnlyArray.of(0, stroke.getInt("payload"))
            } else if ((payloadType == ReadableType.Map)) {
                this.stroke = JavaOnlyArray.of(0, stroke.getMap("payload"))
            }
        } else if (type == 1) {
            this.stroke = JavaOnlyArray.of(1, stroke.getString("brushRef"))
        } else {
            this.stroke = JavaOnlyArray.of(type)
        }
        invalidate()
    }

    fun setStrokeDasharray(dynamicStrokeDasharray: Dynamic) {
        if (!dynamicStrokeDasharray.isNull()) {
            val arrayList = SVGLength.arrayFrom(dynamicStrokeDasharray)!!
            strokeDasharray = arrayList.toArray(arrayOf(SVGLength(0.0)))
        } else {
            strokeDasharray = null
        }
        invalidate()
    }

    fun setStrokeWidth(strokeWidth: Dynamic) {
        this.strokeWidth = if (strokeWidth.isNull()) SVGLength(1.0) else SVGLength.Companion.from(strokeWidth)
        invalidate()
    }

    fun setStrokeLinecap(strokeLinecap: Int) {
        when (strokeLinecap) {
            CAP_BUTT -> this.strokeLinecap = Cap.BUTT
            CAP_SQUARE -> this.strokeLinecap = Cap.SQUARE
            CAP_ROUND -> this.strokeLinecap = Cap.ROUND
            else -> throw JSApplicationIllegalArgumentException("strokeLinecap " + strokeLinecap + " unrecognized")
        }
        invalidate()
    }

    fun setStrokeLinejoin(strokeLinejoin: Int) {
        when (strokeLinejoin) {
            JOIN_MITER -> this.strokeLinejoin = Join.MITER
            JOIN_BEVEL -> this.strokeLinejoin = Join.BEVEL
            JOIN_ROUND -> this.strokeLinejoin = Join.ROUND
            else -> throw JSApplicationIllegalArgumentException("strokeLinejoin " + strokeLinejoin + " unrecognized")
        }
        invalidate()
    }

    fun setPropList(propList: ReadableArray?) {
        if (propList != null) {
            attributeList = ArrayList()
            mPropList = attributeList
            for (i in 0 until propList.size()) {
                mPropList.add(propList.getString(i))
            }
        }
        invalidate()
    }

    public override fun render(canvas: Canvas, paint: Paint, opacity: Float) {
        var mask: MaskView? = null
        if (mMask != null) {
            val root: SvgView? = svgView
            mask = root!!.getDefinedMask(mMask!!) as MaskView?
        }
        if (mask != null) {
            val clipBounds: Rect = canvas.getClipBounds()
            val height: Int = clipBounds.height()
            val width: Int = clipBounds.width()
            val maskBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val original: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val result: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val originalCanvas: Canvas = Canvas(original)
            val maskCanvas: Canvas = Canvas(maskBitmap)
            val resultCanvas: Canvas = Canvas(result)

            // Clip to mask bounds and render the mask
            val maskX: Float = relativeOnWidth((mask.mX)!!).toFloat()
            val maskY: Float = relativeOnHeight((mask.mY)!!).toFloat()
            val maskWidth: Float = relativeOnWidth((mask.mW)!!).toFloat()
            val maskHeight: Float = relativeOnHeight((mask.mH)!!).toFloat()
            maskCanvas.clipRect(maskX, maskY, maskWidth + maskX, maskHeight + maskY)
            val maskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
            mask.draw(maskCanvas, maskPaint, 1f)

            // Apply luminanceToAlpha filter primitive
            // https://www.w3.org/TR/SVG11/filters.html#feColorMatrixElement
            val nPixels: Int = width * height
            val pixels: IntArray = IntArray(nPixels)
            maskBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in 0 until nPixels) {
                val color: Int = pixels.get(i)
                val r: Int = (color shr 16) and 0xFF
                val g: Int = (color shr 8) and 0xFF
                val b: Int = color and 0xFF
                val a: Int = color ushr 24
                val luminance: Double = saturate(((0.299 * r) + (0.587 * g) + (0.144 * b)) / 255)
                val alpha: Int = (a * luminance).toInt()
                val pixel: Int = (alpha shl 24)
                pixels[i] = pixel
            }
            maskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            // Render content of current SVG Renderable to image
            draw(originalCanvas, paint, opacity)

            // Blend current element and mask
            maskPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
            resultCanvas.drawBitmap(original, 0f, 0f, null)
            resultCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)

            // Render composited result into current render context
            canvas.drawBitmap(result, 0f, 0f, paint)
        } else {
            draw(canvas, paint, opacity)
        }
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        var opacity: Float = opacity
        opacity *= mOpacity
        val computePaths: Boolean = mPath == null
        if (computePaths) {
            mPath = getPath(canvas, paint)
            mPath!!.fillType = fillRule
        }
        val nonScalingStroke: Boolean = vectorEffect == VECTOR_EFFECT_NON_SCALING_STROKE
        var path: Path = mPath!!
        if (nonScalingStroke) {
            val scaled = Path()
            mPath!!.transform(mCTM, scaled)
            canvas.setMatrix(null)
            path = scaled
        }
        if (computePaths || path !== mPath) {
            mBox = RectF()
            path.computeBounds(mBox!!, true)
        }
        val clientRect: RectF = RectF(mBox)
        mCTM.mapRect(clientRect)
        setClientRect(clientRect)
        clip(canvas, paint)
        if (setupFillPaint(paint, opacity * fillOpacity)) {
            if (computePaths) {
                mFillPath = Path()
                paint.getFillPath(path, mFillPath)
            }
            canvas.drawPath(path, paint)
        }
        if (setupStrokePaint(paint, opacity * strokeOpacity)) {
            if (computePaths) {
                mStrokePath = Path()
                paint.getFillPath(path, mStrokePath)
            }
            canvas.drawPath(path, paint)
        }
        renderMarkers(canvas, paint, opacity)
    }

    fun renderMarkers(canvas: Canvas, paint: Paint?, opacity: Float) {
        val markerStart: MarkerView? = svgView?.getDefinedMarker(mMarkerStart) as MarkerView?
        val markerMid: MarkerView? = svgView?.getDefinedMarker(mMarkerMid) as MarkerView?
        val markerEnd: MarkerView? = svgView?.getDefinedMarker(mMarkerEnd) as MarkerView?
        if (elements != null && ((markerStart != null) || (markerMid != null) || (markerEnd != null))) {
            contextElement = this
            val positions: ArrayList<RNSVGMarkerPosition>? = RNSVGMarkerPosition.Companion.fromPath(elements!!)
            val width: Float = (if (strokeWidth != null) relativeOnOther(strokeWidth!!) else 1).toFloat()
            mMarkerPath = Path()
            for (position: RNSVGMarkerPosition in positions!!) {
                val type: RNSVGMarkerType? = position.type
                val marker: MarkerView? = when (type) {
                    RNSVGMarkerType.kStartMarker -> markerStart
                    RNSVGMarkerType.kMidMarker -> markerMid
                    RNSVGMarkerType.kEndMarker -> markerEnd
                    else -> null
                } ?: continue
                marker!!.renderMarker(canvas, paint, opacity, position, width)
                val transform: Matrix = marker.markerTransform
                mMarkerPath!!.addPath((marker.getPath(canvas, paint))!!, transform)
            }
            contextElement = null
        }
    }

    /**
     * Sets up paint according to the props set on a view. Returns `true` if the fill should be
     * drawn, `false` if not.
     */
    @SuppressLint("WrongConstant")
    fun setupFillPaint(paint: Paint, opacity: Float): Boolean {
        if (fill != null && fill!!.size() > 0) {
            paint.reset()
            paint.setFlags(Paint.ANTI_ALIAS_FLAG or Paint.DEV_KERN_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
            paint.setStyle(Paint.Style.FILL)
            setupPaint(paint, opacity, fill)
            return true
        }
        return false
    }

    /**
     * Sets up paint according to the props set on a view. Returns `true` if the stroke should
     * be drawn, `false` if not.
     */
    @SuppressLint("WrongConstant")
    fun setupStrokePaint(paint: Paint, opacity: Float): Boolean {
        paint.reset()
        val strokeWidth: Double = relativeOnOther((strokeWidth)!!)
        if ((strokeWidth == 0.0) || (stroke == null) || (stroke!!.size() == 0)) {
            return false
        }
        paint.setFlags(Paint.ANTI_ALIAS_FLAG or Paint.DEV_KERN_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        paint.setStyle(Paint.Style.STROKE)
        paint.setStrokeCap(strokeLinecap)
        paint.setStrokeJoin(strokeLinejoin)
        paint.setStrokeMiter(strokeMiterlimit * mScale)
        paint.setStrokeWidth(strokeWidth.toFloat())
        setupPaint(paint, opacity, stroke)
        if (strokeDasharray != null) {
            val length: Int = strokeDasharray!!.size
            val intervals: FloatArray = FloatArray(length)
            for (i in 0 until length) {
                intervals[i] = relativeOnOther((strokeDasharray!!.get(i))!!).toFloat()
            }
            paint.setPathEffect(DashPathEffect(intervals, strokeDashoffset))
        }
        return true
    }

    private fun setupPaint(paint: Paint, opacity: Float, colors: ReadableArray?) {
        val colorType: Int = colors!!.getInt(0)
        when (colorType) {
            0 -> if (colors.size() == 2) {
                val color: Int
                if (colors.getType(1) == ReadableType.Map) {
                    color = ColorPropConverter.getColor(colors.getMap(1), getContext())
                } else {
                    color = colors.getInt(1)
                }
                val alpha: Int = color ushr 24
                val combined: Int = Math.round(alpha.toFloat() * opacity)
                paint.setColor((combined shl 24) or (color and 0x00ffffff))
            } else {
                // solid color
                paint.setARGB((if (colors.size() > 4) colors.getDouble(4) * opacity * 255 else opacity * 255).toInt(), (colors.getDouble(1) * 255).toInt(), (colors.getDouble(2) * 255).toInt(), (colors.getDouble(3) * 255).toInt())
            }

            1 -> {
                val brush: Brush? = svgView?.getDefinedBrush(colors.getString(1))
                if (brush != null) {
                    brush.setupPaint(paint, (mBox)!!, mScale, opacity)
                }
            }

            2 -> {
                val color: Int = svgView!!.mTintColor
                var alpha: Int = color ushr 24
                alpha = Math.round(alpha.toFloat() * opacity)
                paint.setColor((alpha shl 24) or (color and 0x00ffffff))
            }

            3 -> {
                if (contextElement != null && contextElement!!.fill != null) {
                    setupPaint(paint, opacity, contextElement!!.fill)
                }
            }

            4 -> {
                if (contextElement != null && contextElement!!.stroke != null) {
                    setupPaint(paint, opacity, contextElement!!.stroke)
                }
            }
        }
    }

    abstract override fun getPath(canvas: Canvas?, paint: Paint?): Path?
    public override fun hitTest(src: FloatArray?): Int {
        if ((mPath == null) || !mInvertible || !mTransformInvertible) {
            return -1
        }
        if (mPointerEvents == PointerEvents.NONE) {
            return -1
        }
        val dst: FloatArray = FloatArray(2)
        mInvMatrix.mapPoints(dst, src)
        mInvTransform.mapPoints(dst)
        val x: Int = Math.round(dst.get(0))
        val y: Int = Math.round(dst.get(1))
        initBounds()
        if (((mRegion == null || !mRegion!!.contains(x, y)) && (mStrokeRegion == null || !mStrokeRegion!!.contains(x, y) && (mMarkerRegion == null || !mMarkerRegion!!.contains(x, y))))) {
            return -1
        }
        val clipPath: Path? = super.clipPath
        if (clipPath != null) {
            if (!mClipRegion!!.contains(x, y)) {
                return -1
            }
        }
        return getId()
    }

    fun initBounds() {
        if (mRegion == null && mFillPath != null) {
            mFillBounds = RectF()
            mFillPath!!.computeBounds(mFillBounds!!, true)
            mRegion = getRegion(mFillPath, mFillBounds!!)
        }
        if (mRegion == null && mPath != null) {
            mFillBounds = RectF()
            mPath!!.computeBounds(mFillBounds!!, true)
            mRegion = getRegion(mPath, mFillBounds!!)
        }
        if (mStrokeRegion == null && mStrokePath != null) {
            mStrokeBounds = RectF()
            mStrokePath!!.computeBounds(mStrokeBounds!!, true)
            mStrokeRegion = getRegion(mStrokePath, mStrokeBounds!!)
        }
        if (mMarkerRegion == null && mMarkerPath != null) {
            mMarkerBounds = RectF()
            mMarkerPath!!.computeBounds(mMarkerBounds!!, true)
            mMarkerRegion = getRegion(mMarkerPath, mMarkerBounds!!)
        }
        val clipPath: Path? = super.clipPath
        if (clipPath != null) {
            if (mClipRegionPath !== clipPath) {
                mClipRegionPath = clipPath
                mClipBounds = RectF()
                clipPath.computeBounds(mClipBounds!!, true)
                mClipRegion = getRegion(clipPath, mClipBounds!!)
            }
        }
    }

    fun getRegion(path: Path?, rectF: RectF): Region {
        val region: Region = Region()
        region.setPath((path)!!, Region(floor(rectF.left.toDouble()).toInt(), floor(rectF.top.toDouble()).toInt(), ceil(rectF.right.toDouble()).toInt(), ceil(rectF.bottom.toDouble()).toInt()))
        return region
    }

    open fun mergeProperties(target: RenderableView) {
        val targetAttributeList: ArrayList<String>? = target.attributeList
        if (targetAttributeList == null || targetAttributeList.size == 0) {
            return
        }
        mOriginProperties = ArrayList()
        attributeList = ArrayList(mPropList)
        var i: Int = 0
        val size: Int = targetAttributeList.size
        while (i < size) {
            try {
                val fieldName: String = targetAttributeList[i]
                val getter: Method = javaClass.getMethod("get${fieldName.replaceFirstChar { it.titlecaseChar() }}")
                        ?: throw IllegalStateException("no getter found in ${javaClass.name} for property $fieldName. Make sure it has public access")
                val value: Any? = getter.invoke(target)
                mOriginProperties.add(getter.invoke(this))
                if (!hasOwnProperty(fieldName)) {
                    attributeList.add(fieldName)
                    val setters = javaClass.methods.filter { it.name == "set${fieldName.replaceFirstChar { ch -> ch.titlecaseChar() }}" }
                    val setterWithSpecificType = setters.find { it.parameterTypes.first().isAssignableFrom(getter.returnType) }
                            ?: throw IllegalStateException("no setter with ${getter.returnType.name} value type found in ${javaClass.name} for property $fieldName. Make sure it is var, not val")
                    setterWithSpecificType.invoke(this, value)
                }
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            i++
        }
        mLastMergedList = targetAttributeList
    }

    open fun resetProperties() {
        try {
            for (i in mLastMergedList.indices.reversed()) {
                val fieldName = mLastMergedList[i]
                val setters = javaClass.methods.filter { it.name == "set${fieldName.replaceFirstChar { ch -> ch.titlecaseChar() }}" }
                val originValue = mOriginProperties[i] ?: continue
                val setterWithSpecificType = setters.find { it.parameterTypes.first().simpleName.lowercase() == originValue.javaClass.simpleName.lowercase() || it.parameterTypes.first().isAssignableFrom(originValue.javaClass) }
                        ?: throw IllegalStateException("no setter with ${originValue.javaClass.name} value type found in ${javaClass.name} for property $fieldName. Make sure it is var, not val")
                setterWithSpecificType.invoke(this, originValue)
            }
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
        mLastMergedList = ArrayList()
        mOriginProperties = ArrayList()
        attributeList = mPropList
    }

    private fun hasOwnProperty(propName: String): Boolean {
        return attributeList.contains(propName)
    }

    companion object {
        var contextElement: RenderableView? = null

        // strokeLinecap
        private val CAP_BUTT: Int = 0
        private val CAP_SQUARE: Int = 2

        // strokeLinejoin
        private val JOIN_BEVEL: Int = 2
        private val JOIN_MITER: Int = 0


        // fillRule
        private val FILL_RULE_EVENODD: Int = 0

        // vectorEffect
        private val VECTOR_EFFECT_DEFAULT: Int = 0
        private val VECTOR_EFFECT_NON_SCALING_STROKE: Int = 1
        private val regex: Pattern = Pattern.compile("[0-9.-]+")
        private fun saturate(v: Double): Double {
            return if (v <= 0) 0.0 else (if (v >= 1) 1.0 else v)
        }
    }
}
