package com.horcrux.svg

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.FillType
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.view.View
import android.view.ViewParent
import android.view.accessibility.AccessibilityNodeInfo
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableType
import com.facebook.react.common.ReactConstants
import com.facebook.react.uimanager.DisplayMetricsHolder
import com.facebook.react.uimanager.OnLayoutEvent
import com.facebook.react.uimanager.PointerEvents
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.view.ReactViewGroup
import com.horcrux.svg.SVGLength.UnitType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
abstract class VirtualView internal constructor(val mContext: ReactContext) : ReactViewGroup(mContext) {
    var mOpacity: Float = 1f
    var mCTM: Matrix = Matrix()
    var mMatrix: Matrix? = Matrix()
    var mTransform: Matrix = Matrix()
    var mInvCTM: Matrix = Matrix()
    var mInvMatrix: Matrix = Matrix()
    val mInvTransform: Matrix = Matrix()
    var mInvertible: Boolean = true
    var mCTMInvertible: Boolean = true
    var mTransformInvertible: Boolean = true
    var clientRect: RectF? = null
        private set
    var mClipRule: Int = 0
    private var mClipPath: String? = null
    var mMask: String? = null
    var mMarkerStart: String? = null
    var mMarkerMid: String? = null
    var mMarkerEnd: String? = null
    val mScale: Float
    private var mResponsible: Boolean = false
    var mDisplay: String? = null
    var mName: String? = null
    var svgView: SvgView? = null
        get() {
            if (field != null) {
                return field
            }
            val parent: ViewParent? = getParent()
            if (parent == null) {
                return null
            } else if (parent is SvgView) {
                field = parent
            } else if (parent is VirtualView) {
                field = parent.svgView
            } else {
                FLog.e(
                        ReactConstants.TAG,
                        "RNSVG: " + javaClass.getName() + " should be descendant of a SvgView.")
            }
            return field
        }
        private set
    var clipPath: Path? = null
        private set
    private var mTextRoot: GroupView? = null
    private var fontSize: Double = -1.0
    private var canvasDiagonal: Double = -1.0
    private var canvasHeight: Float = -1f
    private var canvasWidth: Float = -1f
    internal open var glyphContext: GlyphContext? = null
    open var mPath: Path? = null
    var mFillPath: Path? = null
    var mStrokePath: Path? = null
    var mMarkerPath: Path? = null
    var mClipRegionPath: Path? = null
    var mBox: RectF? = null
    var mFillBounds: RectF? = null
    var mStrokeBounds: RectF? = null
    var mMarkerBounds: RectF? = null
    var mClipBounds: RectF? = null
    var mRegion: Region? = null
    var mMarkerRegion: Region? = null
    var mStrokeRegion: Region? = null
    var mClipRegion: Region? = null
    var elements: ArrayList<PathElement>? = null
    var mPointerEvents: PointerEvents? = null

    init {
        mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density
    }

    fun setPointerEvents(pointerEvents: PointerEvents?) {
        mPointerEvents = pointerEvents
    }

    public override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (clientRect != null) {
            val root: SvgView? = svgView
            val rootPositionOnScreen: IntArray = IntArray(2)
            svgView!!.getLocationOnScreen(rootPositionOnScreen)
            val infoBoundsInScreen: Rect = Rect()
            infoBoundsInScreen.left = rootPositionOnScreen.get(0) + floor(clientRect!!.left.toDouble()).toInt()
            infoBoundsInScreen.top = rootPositionOnScreen.get(1) + floor(clientRect!!.top.toDouble()).toInt()
            infoBoundsInScreen.right = infoBoundsInScreen.left + ceil(clientRect!!.width().toDouble()).toInt()
            infoBoundsInScreen.bottom = infoBoundsInScreen.top + ceil(clientRect!!.height().toDouble()).toInt()
            val rootVisibleRect: Rect = Rect()
            val isRootVisible: Boolean = root!!.getGlobalVisibleRect(rootVisibleRect)
            val infoIsVisibleToUser: Boolean = isRootVisible && infoBoundsInScreen.intersect(rootVisibleRect)
            val infoClassName: String = this.javaClass.getCanonicalName()
            info.setBoundsInScreen(infoBoundsInScreen)
            info.setClassName(infoClassName)
            info.setVisibleToUser(infoIsVisibleToUser)
        }
    }

    public override fun invalidate() {
        if (this is RenderableView && mPath == null) {
            return
        }
        clearCache()
        clearParentCache()
        super.invalidate()
    }

    open fun clearCache() {
        canvasDiagonal = -1.0
        canvasHeight = -1f
        canvasWidth = -1f
        fontSize = -1.0
        mStrokeRegion = null
        mMarkerRegion = null
        mRegion = null
        mPath = null
    }

    fun clearChildCache() {
        clearCache()
        for (i in 0 until getChildCount()) {
            val node: View = getChildAt(i)
            if (node is VirtualView) {
                node.clearChildCache()
            }
        }
    }

    private fun clearParentCache() {
        var node: VirtualView = this
        while (true) {
            val parent: ViewParent? = node.parent
            if (parent !is VirtualView) {
                return
            }
            node = parent
            if (node.mPath == null) {
                return
            }
            node.clearCache()
        }
    }

    val textRoot: GroupView?
        get() {
            var node: VirtualView? = this
            if (mTextRoot == null) {
                while (node != null) {
                    if (node is GroupView && (node as GroupView).glyphContext != null) {
                        mTextRoot = node
                        break
                    }
                    val parent: ViewParent = node.parent
                    node = if (parent !is VirtualView) {
                        null
                    } else {
                        parent
                    }
                }
            }
            return mTextRoot
        }
    val parentTextRoot: GroupView?
        get() {
            val parent: ViewParent = getParent()
            if (!(parent is VirtualView)) {
                return null
            } else {
                return parent.textRoot
            }
        }
    private val fontSizeFromContext: Double
        private get() {
            if (fontSize != -1.0) {
                return fontSize
            }
            val root: GroupView? = textRoot
            if (root == null) {
                return FontData.Companion.DEFAULT_FONT_SIZE
            }
            if (glyphContext == null) {
                glyphContext = root.glyphContext
            }
            fontSize = glyphContext!!.fontSize
            return fontSize
        }

    abstract fun draw(canvas: Canvas, paint: Paint, opacity: Float)
    open fun render(canvas: Canvas, paint: Paint, opacity: Float) {
        draw(canvas, paint, opacity)
    }

    /**
     * Sets up the transform matrix on the canvas before an element is drawn.
     *
     *
     * NB: for perf reasons this does not apply opacity, as that would mean creating a new canvas
     * layer (which allocates an offscreen bitmap) and having it composited afterwards. Instead, the
     * drawing code should apply opacity recursively.
     *
     * @param canvas the canvas to set up
     * @param ctm current transformation matrix
     */
    fun saveAndSetupCanvas(canvas: Canvas, ctm: Matrix?): Int {
        val count: Int = canvas.save()
        mCTM.setConcat(mMatrix, mTransform)
        canvas.concat(mCTM)
        mCTM.preConcat(ctm)
        mCTMInvertible = mCTM.invert(mInvCTM)
        return count
    }

    /**
     * Restore the canvas after an element was drawn. This is always called in mirror with [ ][.saveAndSetupCanvas].
     *
     * @param canvas the canvas to restore
     */
    fun restoreCanvas(canvas: Canvas, count: Int) {
        canvas.restoreToCount(count)
    }

    fun setName(name: String?) {
        mName = name
        invalidate()
    }

    fun setDisplay(display: String?) {
        mDisplay = display
        invalidate()
    }

    fun setMask(mask: String?) {
        mMask = mask
        invalidate()
    }

    fun setMarkerStart(markerStart: String?) {
        mMarkerStart = markerStart
        invalidate()
    }

    fun setMarkerMid(markerMid: String?) {
        mMarkerMid = markerMid
        invalidate()
    }

    fun setMarkerEnd(markerEnd: String?) {
        mMarkerEnd = markerEnd
        invalidate()
    }

    fun setClipPath(clipPath: String?) {
        this.clipPath = null
        mClipPath = clipPath
        invalidate()
    }

    fun setClipRule(clipRule: Int) {
        mClipRule = clipRule
        invalidate()
    }

    fun setOpacity(opacity: Float) {
        mOpacity = opacity
        invalidate()
    }

    fun setMatrix(matrixArray: Dynamic) {
        val isArrayType: Boolean = !matrixArray.isNull() && (matrixArray.getType() == ReadableType.Array)
        setMatrix(if (isArrayType) matrixArray.asArray() else null)
    }

    fun setMatrix(matrixArray: ReadableArray?) {
        if (matrixArray != null) {
            val matrixSize: Int = PropHelper.toMatrixData(matrixArray, sRawMatrix, mScale)
            if (matrixSize == 6) {
                if (mMatrix == null) {
                    mMatrix = Matrix()
                    mInvMatrix = Matrix()
                }
                mMatrix!!.setValues(sRawMatrix)
                mInvertible = mMatrix!!.invert(mInvMatrix)
            } else if (matrixSize != -1) {
                FLog.w(ReactConstants.TAG, "RNSVG: Transform matrices must be of size 6")
            }
        } else {
            mMatrix!!.reset()
            mInvMatrix.reset()
            mInvertible = true
        }
        super.invalidate()
        clearParentCache()
    }

    fun getClipPath(canvas: Canvas?, paint: Paint?): Path? {
        if (mClipPath != null) {
            val mClipNode: ClipPathView? = svgView!!.getDefinedClipPath(mClipPath!!) as ClipPathView?
            if (mClipNode != null) {
                val clipPath: Path? = if (mClipRule == CLIP_RULE_EVENODD) mClipNode.getPath(canvas, paint) else mClipNode.getPath((canvas)!!, paint, Region.Op.UNION)
                clipPath!!.transform((mClipNode.mMatrix)!!)
                clipPath.transform(mClipNode.mTransform)
                when (mClipRule) {
                    CLIP_RULE_EVENODD -> clipPath.setFillType(FillType.EVEN_ODD)
                    CLIP_RULE_NONZERO -> {}
                    else -> FLog.w(ReactConstants.TAG, "RNSVG: clipRule: " + mClipRule + " unrecognized")
                }
                this.clipPath = clipPath
            } else {
                FLog.w(ReactConstants.TAG, "RNSVG: Undefined clipPath: " + mClipPath)
            }
        }
        return clipPath
    }

    fun clip(canvas: Canvas, paint: Paint?) {
        val clip: Path? = getClipPath(canvas, paint)
        if (clip != null) {
            canvas.clipPath(clip)
        }
    }

    abstract fun hitTest(point: FloatArray?): Int
    open var isResponsible: Boolean
        get() {
            return mResponsible
        }
        set(responsible) {
            mResponsible = responsible
            invalidate()
        }

    abstract fun getPath(canvas: Canvas?, paint: Paint?): Path?
    fun relativeOnWidth(length: SVGLength): Double {
        val unit: UnitType? = length.unit
        if (unit == UnitType.NUMBER) {
            return length.value * mScale
        } else if (unit == UnitType.PERCENTAGE) {
            return length.value / 100 * getCanvasWidth()
        }
        return fromRelativeFast(length)
    }

    fun relativeOnHeight(length: SVGLength): Double {
        val unit: UnitType? = length.unit
        if (unit == UnitType.NUMBER) {
            return length.value * mScale
        } else if (unit == UnitType.PERCENTAGE) {
            return length.value / 100 * getCanvasHeight()
        }
        return fromRelativeFast(length)
    }

    fun relativeOnOther(length: SVGLength): Double {
        val unit: UnitType? = length.unit
        if (unit == UnitType.NUMBER) {
            return length.value * mScale
        } else if (unit == UnitType.PERCENTAGE) {
            return length.value / 100 * getCanvasDiagonal()
        }
        return fromRelativeFast(length)
    }

    /**
     * Converts SVGLength into px / user units in the current user coordinate system
     *
     * @param length length string
     * @return value in the current user coordinate system
     */
    private fun fromRelativeFast(length: SVGLength): Double {
        val unit: Double
        when (length.unit) {
            UnitType.EMS -> unit = fontSizeFromContext
            UnitType.EXS -> unit = fontSizeFromContext / 2
            UnitType.CM -> unit = 35.43307
            UnitType.MM -> unit = 3.543307
            UnitType.IN -> unit = 90.0
            UnitType.PT -> unit = 1.25
            UnitType.PC -> unit = 15.0
            else -> unit = 1.0
        }
        return length.value * unit * mScale
    }

    private fun getCanvasWidth(): Float {
        if (canvasWidth != -1f) {
            return canvasWidth
        }
        val root: GroupView? = textRoot
        if (root == null) {
            canvasWidth = svgView!!.canvasBounds.width().toFloat()
        } else {
            canvasWidth = root.glyphContext!!.width
        }
        return canvasWidth
    }

    private fun getCanvasHeight(): Float {
        if (canvasHeight != -1f) {
            return canvasHeight
        }
        val root: GroupView? = textRoot
        canvasHeight = if (root == null) {
            svgView!!.canvasBounds.height().toFloat()
        } else {
            root.glyphContext!!.height
        }
        return canvasHeight
    }

    private fun getCanvasDiagonal(): Double {
        if (canvasDiagonal != -1.0) {
            return canvasDiagonal
        }
        val powX: Double = getCanvasWidth().pow(2).toDouble()
        val powY: Double = getCanvasHeight().pow(2).toDouble()
        canvasDiagonal = sqrt(powX + powY) * M_SQRT1_2l
        return canvasDiagonal
    }

    open fun saveDefinition() {
        if (mName != null) {
            svgView!!.defineTemplate(this, mName)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = if (clientRect != null) ceil(clientRect!!.width().toDouble()).toInt() else getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec)
        val height: Int = if (clientRect != null) ceil(clientRect!!.height().toDouble()).toInt() else getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    /**
     * Called from layout when this view should assign a size and position to each of its children.
     *
     *
     * Derived classes with children should override this method and call layout on each of their
     * children.
     *
     * @param changed This is a new size or position for this view
     * @param pleft Left position, relative to parent
     * @param ptop Top position, relative to parent
     * @param pright Right position, relative to parent
     * @param pbottom Bottom position, relative to parent
     */
    override fun onLayout(changed: Boolean, pleft: Int, ptop: Int, pright: Int, pbottom: Int) {
        if (clientRect == null) {
            return
        }
        if (!(this is GroupView)) {
            val left: Int = floor(clientRect!!.left.toDouble()).toInt()
            val top: Int = floor(clientRect!!.top.toDouble()).toInt()
            val right: Int = ceil(clientRect!!.right.toDouble()).toInt()
            val bottom: Int = ceil(clientRect!!.bottom.toDouble()).toInt()
            setLeft(left)
            setTop(top)
            setRight(right)
            setBottom(bottom)
        }
        val width: Int = ceil(clientRect!!.width().toDouble()).toInt()
        val height: Int = ceil(clientRect!!.height().toDouble()).toInt()
        setMeasuredDimension(width, height)
    }

    fun setClientRect(rect: RectF) {
        if (clientRect != null && (clientRect == rect)) {
            return
        }
        clientRect = rect
        if (clientRect == null) {
            return
        }
        val width: Int = ceil(clientRect!!.width().toDouble()).toInt()
        val height: Int = ceil(clientRect!!.height().toDouble()).toInt()
        val left: Int = floor(clientRect!!.left.toDouble()).toInt()
        val top: Int = floor(clientRect!!.top.toDouble()).toInt()
        val right: Int = ceil(clientRect!!.right.toDouble()).toInt()
        val bottom: Int = ceil(clientRect!!.bottom.toDouble()).toInt()
        setMeasuredDimension(width, height)
        if (!(this is GroupView)) {
            setLeft(left)
            setTop(top)
            setRight(right)
            setBottom(bottom)
        }
        val eventDispatcher: EventDispatcher? = UIManagerHelper.getEventDispatcherForReactTag(mContext, getId())
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(OnLayoutEvent.obtain(getId(), left, top, width, height))
        }
    }

    companion object {
        /*
      N[1/Sqrt[2], 36]
      The inverse of the square root of 2.
      Provide enough digits for the 128-bit IEEE quad (36 significant digits).
  */
        private val M_SQRT1_2l: Double = 0.707106781186547524400844362104849039
        private val sRawMatrix: FloatArray = floatArrayOf(
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f
        )
        private val CLIP_RULE_EVENODD: Int = 0
        val CLIP_RULE_NONZERO: Int = 1
    }
}
