/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.graphics.Matrix
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup.OnHierarchyChangeListener
import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.uimanager.DisplayMetricsHolder
import com.facebook.react.uimanager.LayoutShadowNode
import com.facebook.react.uimanager.MatrixMathHelper
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.uimanager.PointerEvents
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.TransformHelper
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.ViewProps
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.annotations.ReactPropGroup
import com.facebook.react.viewmanagers.RNSVGCircleManagerDelegate
import com.facebook.react.viewmanagers.RNSVGCircleManagerInterface
import com.facebook.react.viewmanagers.RNSVGClipPathManagerDelegate
import com.facebook.react.viewmanagers.RNSVGClipPathManagerInterface
import com.facebook.react.viewmanagers.RNSVGDefsManagerDelegate
import com.facebook.react.viewmanagers.RNSVGDefsManagerInterface
import com.facebook.react.viewmanagers.RNSVGEllipseManagerDelegate
import com.facebook.react.viewmanagers.RNSVGEllipseManagerInterface
import com.facebook.react.viewmanagers.RNSVGForeignObjectManagerDelegate
import com.facebook.react.viewmanagers.RNSVGForeignObjectManagerInterface
import com.facebook.react.viewmanagers.RNSVGGroupManagerDelegate
import com.facebook.react.viewmanagers.RNSVGGroupManagerInterface
import com.facebook.react.viewmanagers.RNSVGImageManagerDelegate
import com.facebook.react.viewmanagers.RNSVGImageManagerInterface
import com.facebook.react.viewmanagers.RNSVGLineManagerDelegate
import com.facebook.react.viewmanagers.RNSVGLineManagerInterface
import com.facebook.react.viewmanagers.RNSVGLinearGradientManagerDelegate
import com.facebook.react.viewmanagers.RNSVGLinearGradientManagerInterface
import com.facebook.react.viewmanagers.RNSVGMarkerManagerDelegate
import com.facebook.react.viewmanagers.RNSVGMarkerManagerInterface
import com.facebook.react.viewmanagers.RNSVGMaskManagerDelegate
import com.facebook.react.viewmanagers.RNSVGMaskManagerInterface
import com.facebook.react.viewmanagers.RNSVGPathManagerDelegate
import com.facebook.react.viewmanagers.RNSVGPathManagerInterface
import com.facebook.react.viewmanagers.RNSVGPatternManagerDelegate
import com.facebook.react.viewmanagers.RNSVGPatternManagerInterface
import com.facebook.react.viewmanagers.RNSVGRadialGradientManagerDelegate
import com.facebook.react.viewmanagers.RNSVGRadialGradientManagerInterface
import com.facebook.react.viewmanagers.RNSVGRectManagerDelegate
import com.facebook.react.viewmanagers.RNSVGRectManagerInterface
import com.facebook.react.viewmanagers.RNSVGSymbolManagerDelegate
import com.facebook.react.viewmanagers.RNSVGSymbolManagerInterface
import com.facebook.react.viewmanagers.RNSVGTSpanManagerDelegate
import com.facebook.react.viewmanagers.RNSVGTSpanManagerInterface
import com.facebook.react.viewmanagers.RNSVGTextManagerDelegate
import com.facebook.react.viewmanagers.RNSVGTextManagerInterface
import com.facebook.react.viewmanagers.RNSVGTextPathManagerDelegate
import com.facebook.react.viewmanagers.RNSVGTextPathManagerInterface
import com.facebook.react.viewmanagers.RNSVGUseManagerDelegate
import com.facebook.react.viewmanagers.RNSVGUseManagerInterface
import javax.annotation.Nonnull
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** ViewManager for DefinitionView RNSVG views  */
internal open class VirtualViewManager<V : VirtualView> protected constructor(protected val svgClass: SVGClass) : ViewGroupManager<V>() {
    private val mClassName: String = svgClass.toString()
    protected lateinit var mDelegate: ViewManagerDelegate<V>
    override fun getDelegate(): ViewManagerDelegate<V>? {
        return mDelegate
    }

    internal class RenderableShadowNode() : LayoutShadowNode() {
        @Suppress("unused")
        @ReactPropGroup(names = [ViewProps.ALIGN_SELF, ViewProps.ALIGN_ITEMS, ViewProps.COLLAPSABLE, ViewProps.FLEX, ViewProps.FLEX_BASIS, ViewProps.FLEX_DIRECTION, ViewProps.FLEX_GROW, ViewProps.FLEX_SHRINK, ViewProps.FLEX_WRAP, ViewProps.JUSTIFY_CONTENT, ViewProps.OVERFLOW, ViewProps.ALIGN_CONTENT, ViewProps.DISPLAY,  /* position */ViewProps.POSITION, ViewProps.RIGHT, ViewProps.TOP, ViewProps.BOTTOM, ViewProps.LEFT, ViewProps.START, ViewProps.END,  /* dimensions */ViewProps.WIDTH, ViewProps.HEIGHT, ViewProps.MIN_WIDTH, ViewProps.MAX_WIDTH, ViewProps.MIN_HEIGHT, ViewProps.MAX_HEIGHT,  /* margins */ViewProps.MARGIN, ViewProps.MARGIN_VERTICAL, ViewProps.MARGIN_HORIZONTAL, ViewProps.MARGIN_LEFT, ViewProps.MARGIN_RIGHT, ViewProps.MARGIN_TOP, ViewProps.MARGIN_BOTTOM, ViewProps.MARGIN_START, ViewProps.MARGIN_END,  /* paddings */ViewProps.PADDING, ViewProps.PADDING_VERTICAL, ViewProps.PADDING_HORIZONTAL, ViewProps.PADDING_LEFT, ViewProps.PADDING_RIGHT, ViewProps.PADDING_TOP, ViewProps.PADDING_BOTTOM, ViewProps.PADDING_START, ViewProps.PADDING_END, ViewProps.BORDER_WIDTH, ViewProps.BORDER_START_WIDTH, ViewProps.BORDER_END_WIDTH, ViewProps.BORDER_TOP_WIDTH, ViewProps.BORDER_BOTTOM_WIDTH, ViewProps.BORDER_LEFT_WIDTH, ViewProps.BORDER_RIGHT_WIDTH])
        fun ignoreLayoutProps(index: Int, value: Dynamic?) {
        }
    }

    public override fun createShadowNodeInstance(): LayoutShadowNode {
        return RenderableShadowNode()
    }

    public override fun getShadowNodeClass(): Class<out LayoutShadowNode> {
        return RenderableShadowNode::class.java
    }

    internal class MatrixDecompositionContext() : MatrixMathHelper.MatrixDecompositionContext() {
        val perspective: DoubleArray = DoubleArray(4)
        val scale: DoubleArray = DoubleArray(3)
        val skew: DoubleArray = DoubleArray(3)
        val translation: DoubleArray = DoubleArray(3)
        val rotationDegrees: DoubleArray = DoubleArray(3)
    }

    @Nonnull
    public override fun getName(): String {
        return mClassName
    }

    @ReactProp(name = "mask")
    fun setMask(node: V, mask: String?) {
        node.setMask(mask)
    }

    @ReactProp(name = "markerStart")
    fun setMarkerStart(node: V, markerStart: String?) {
        node.setMarkerStart(markerStart)
    }

    @ReactProp(name = "markerMid")
    fun setMarkerMid(node: V, markerMid: String?) {
        node.setMarkerMid(markerMid)
    }

    @ReactProp(name = "markerEnd")
    fun setMarkerEnd(node: V, markerEnd: String?) {
        node.setMarkerEnd(markerEnd)
    }

    @ReactProp(name = "clipPath")
    fun setClipPath(node: V, clipPath: String?) {
        node.setClipPath(clipPath)
    }

    @ReactProp(name = "clipRule")
    fun setClipRule(node: V, clipRule: Int) {
        node.setClipRule(clipRule)
    }

    @ReactProp(name = "opacity", defaultFloat = 1f)
    override fun setOpacity(@Nonnull node: V, opacity: Float) {
        node.setOpacity(opacity)
    }

    @ReactProp(name = "responsible")
    fun setResponsible(node: V, responsible: Boolean) {
        node.isResponsible = responsible
    }

    @ReactProp(name = ViewProps.POINTER_EVENTS)
    fun setPointerEvents(view: V, pointerEventsStr: String?) {
        if (pointerEventsStr == null) {
            view.setPointerEvents(PointerEvents.AUTO)
        } else {
            val pointerEvents: PointerEvents = PointerEvents.valueOf(pointerEventsStr.uppercase().replace("-", "_"))
            view.setPointerEvents(pointerEvents)
        }
    }

    @ReactProp(name = "name")
    fun setName(node: V, name: String?) {
        node.setName(name)
    }

    @ReactProp(name = "display")
    fun setDisplay(node: V, display: String?) {
        node.setDisplay(display)
    }

    @ReactProp(name = "matrix")
    fun setMatrix(node: V, matrixArray: Dynamic) {
        node.setMatrix(matrixArray)
    }

    fun setMatrix(view: V, value: ReadableArray?) {
        view.setMatrix(value)
    }


    override fun setTransform(node: V, matrix: ReadableArray?) {
        if (matrix == null) {
            resetTransformProperty(node)
        } else {
            setTransformProperty(node, matrix)
        }
        val m: Matrix = node.getMatrix()
        node.mTransform = m
        node.mTransformInvertible = m.invert(node.mInvTransform)
    }

    @ReactProp(name = "transform")
    fun setTransform(node: V, matrix: Dynamic) {
        if (matrix.type != ReadableType.Array) {
            return
        }
        val ma: ReadableArray = matrix.asArray()
        super.setTransform(node, ma)
    }

    private fun invalidateSvgView(node: V) {
        node?.svgView?.invalidate()
        if (node is TextView) {
            (node as TextView).textContainer.clearChildCache()
        }
    }

    override fun addEventEmitters(
            @Nonnull reactContext: ThemedReactContext, @Nonnull view: V) {
        super.addEventEmitters(reactContext, view)
        view.setOnHierarchyChangeListener(
                object : OnHierarchyChangeListener {
                    public override fun onChildViewAdded(view: View, view1: View) {
                        if (view is VirtualView) {
                            invalidateSvgView(view as V)
                        }
                    }

                    public override fun onChildViewRemoved(view: View, view1: View) {
                        if (view is VirtualView) {
                            invalidateSvgView(view as V)
                        }
                    }
                })
    }

    /**
     * Callback that will be triggered after all properties are updated in current update transaction
     * (all @ReactProp handlers for properties updated in current transaction have been called). If
     * you want to override this method you should call super.onAfterUpdateTransaction from it as the
     * parent class of the ViewManager may rely on callback being executed.
     */
    override fun onAfterUpdateTransaction(@Nonnull node: V) {
        super.onAfterUpdateTransaction(node)
        invalidateSvgView(node as V)
    }

    enum class SVGClass {
        RNSVGGroup,
        RNSVGPath,
        RNSVGText,
        RNSVGTSpan,
        RNSVGTextPath,
        RNSVGImage,
        RNSVGCircle,
        RNSVGEllipse,
        RNSVGLine,
        RNSVGRect,
        RNSVGClipPath,
        RNSVGDefs,
        RNSVGUse,
        RNSVGSymbol,
        RNSVGLinearGradient,
        RNSVGRadialGradient,
        RNSVGPattern,
        RNSVGMask,
        RNSVGMarker,
        RNSVGForeignObject
    }

    @Nonnull
    override fun createViewInstance(@Nonnull reactContext: ThemedReactContext): V {
        when (svgClass) {
            SVGClass.RNSVGGroup -> return GroupView(reactContext) as V
            SVGClass.RNSVGPath -> return PathView(reactContext) as V
            SVGClass.RNSVGCircle -> return CircleView(reactContext) as V
            SVGClass.RNSVGEllipse -> return EllipseView(reactContext) as V
            SVGClass.RNSVGLine -> return LineView(reactContext) as V
            SVGClass.RNSVGRect -> return RectView(reactContext) as V
            SVGClass.RNSVGText -> return TextView(reactContext) as V
            SVGClass.RNSVGTSpan -> return TSpanView(reactContext) as V
            SVGClass.RNSVGTextPath -> return TextPathView(reactContext) as V
            SVGClass.RNSVGImage -> return ImageView(reactContext) as V
            SVGClass.RNSVGClipPath -> return ClipPathView(reactContext) as V
            SVGClass.RNSVGDefs -> return DefsView(reactContext) as V
            SVGClass.RNSVGUse -> return UseView(reactContext) as V
            SVGClass.RNSVGSymbol -> return SymbolView(reactContext) as V
            SVGClass.RNSVGLinearGradient -> return LinearGradientView(reactContext) as V
            SVGClass.RNSVGRadialGradient -> return RadialGradientView(reactContext) as V
            SVGClass.RNSVGPattern -> return PatternView(reactContext) as V
            SVGClass.RNSVGMask -> return MaskView(reactContext) as V
            SVGClass.RNSVGMarker -> return MarkerView(reactContext) as V
            SVGClass.RNSVGForeignObject -> return ForeignObjectView(reactContext) as V
            else -> throw IllegalStateException("Unexpected type $svgClass")
        }
    }

    override fun onDropViewInstance(@Nonnull view: V) {
        super.onDropViewInstance(view)
        mTagToRenderableView.remove(view.id)
    }

    companion object {
        private val sMatrixDecompositionContext: MatrixDecompositionContext = MatrixDecompositionContext()
        private val sTransformDecompositionArray: DoubleArray = DoubleArray(16)
        private val PERSPECTIVE_ARRAY_INVERTED_CAMERA_DISTANCE_INDEX: Int = 2
        private val CAMERA_DISTANCE_NORMALIZATION_MULTIPLIER: Float = 5f
        private val EPSILON: Double = .00001
        private fun isZero(d: Double): Boolean {
            return !java.lang.Double.isNaN(d) && abs(d) < EPSILON
        }

        private fun decomposeMatrix() {

            // output values
            val perspective: DoubleArray = sMatrixDecompositionContext.perspective
            val scale: DoubleArray = sMatrixDecompositionContext.scale
            val skew: DoubleArray = sMatrixDecompositionContext.skew
            val translation: DoubleArray = sMatrixDecompositionContext.translation
            val rotationDegrees: DoubleArray = sMatrixDecompositionContext.rotationDegrees

            // create normalized, 2d array matrix
            // and normalized 1d array perspectiveMatrix with redefined 4th column
            if (isZero(sTransformDecompositionArray.get(15))) {
                return
            }
            val matrix: Array<DoubleArray> = Array(4, { DoubleArray(4) })
            val perspectiveMatrix: DoubleArray = DoubleArray(16)
            for (i in 0..3) {
                for (j in 0..3) {
                    val value: Double = sTransformDecompositionArray.get((i * 4) + j) / sTransformDecompositionArray.get(15)
                    matrix.get(i)[j] = value
                    perspectiveMatrix[(i * 4) + j] = if (j == 3) 0.0 else value
                }
            }
            perspectiveMatrix[15] = 1.0

            // test for singularity of upper 3x3 part of the perspective matrix
            if (isZero(MatrixMathHelper.determinant(perspectiveMatrix))) {
                return
            }

            // isolate perspective
            if (!isZero(matrix.get(0).get(3)) || !isZero(matrix.get(1).get(3)) || !isZero(matrix.get(2).get(3))) {
                // rightHandSide is the right hand side of the equation.
                // rightHandSide is a vector, or point in 3d space relative to the origin.
                val rightHandSide: DoubleArray = doubleArrayOf(matrix.get(0).get(3), matrix.get(1).get(3), matrix.get(2).get(3), matrix.get(3).get(3))

                // Solve the equation by inverting perspectiveMatrix and multiplying
                // rightHandSide by the inverse.
                val inversePerspectiveMatrix: DoubleArray = MatrixMathHelper.inverse(perspectiveMatrix)
                val transposedInversePerspectiveMatrix: DoubleArray = MatrixMathHelper.transpose(inversePerspectiveMatrix)
                MatrixMathHelper.multiplyVectorByMatrix(rightHandSide, transposedInversePerspectiveMatrix, perspective)
            } else {
                // no perspective
                perspective[2] = 0.0
                perspective[1] = perspective.get(2)
                perspective[0] = perspective.get(1)
                perspective[3] = 1.0
            }

            // translation is simple
            System.arraycopy(matrix.get(3), 0, translation, 0, 3)

            // Now get scale and shear.
            // 'row' is a 3 element array of 3 component vectors
            val row: Array<DoubleArray> = Array(3, { DoubleArray(3) })
            for (i in 0..2) {
                row.get(i)[0] = matrix.get(i).get(0)
                row.get(i)[1] = matrix.get(i).get(1)
                row.get(i)[2] = matrix.get(i).get(2)
            }

            // Compute X scale factor and normalize first row.
            scale[0] = MatrixMathHelper.v3Length(row.get(0))
            row[0] = MatrixMathHelper.v3Normalize(row.get(0), scale.get(0))

            // Compute XY shear factor and make 2nd row orthogonal to 1st.
            skew[0] = MatrixMathHelper.v3Dot(row.get(0), row.get(1))
            row[1] = MatrixMathHelper.v3Combine(row.get(1), row.get(0), 1.0, -skew.get(0))

            // Compute XY shear factor and make 2nd row orthogonal to 1st.
            skew[0] = MatrixMathHelper.v3Dot(row.get(0), row.get(1))
            row[1] = MatrixMathHelper.v3Combine(row.get(1), row.get(0), 1.0, -skew.get(0))

            // Now, compute Y scale and normalize 2nd row.
            scale[1] = MatrixMathHelper.v3Length(row.get(1))
            row[1] = MatrixMathHelper.v3Normalize(row.get(1), scale.get(1))
            skew[0] /= scale.get(1)

            // Compute XZ and YZ shears, orthogonalize 3rd row
            skew[1] = MatrixMathHelper.v3Dot(row.get(0), row.get(2))
            row[2] = MatrixMathHelper.v3Combine(row.get(2), row.get(0), 1.0, -skew.get(1))
            skew[2] = MatrixMathHelper.v3Dot(row.get(1), row.get(2))
            row[2] = MatrixMathHelper.v3Combine(row.get(2), row.get(1), 1.0, -skew.get(2))

            // Next, get Z scale and normalize 3rd row.
            scale[2] = MatrixMathHelper.v3Length(row.get(2))
            row[2] = MatrixMathHelper.v3Normalize(row.get(2), scale.get(2))
            skew[1] /= scale.get(2)
            skew[2] /= scale.get(2)

            // At this point, the matrix (in rows) is orthonormal.
            // Check for a coordinate system flip.  If the determinant
            // is -1, then negate the matrix and the scaling factors.
            val pdum3: DoubleArray = MatrixMathHelper.v3Cross(row.get(1), row.get(2))
            if (MatrixMathHelper.v3Dot(row.get(0), pdum3) < 0) {
                for (i in 0..2) {
                    scale[i] *= -1.0
                    row.get(i)[0] *= -1.0
                    row.get(i)[1] *= -1.0
                    row.get(i)[2] *= -1.0
                }
            }

            // Now, get the rotations out
            // Based on: http://nghiaho.com/?page_id=846
            val conv: Double = 180 / Math.PI
            rotationDegrees[0] = MatrixMathHelper.roundTo3Places(-atan2(row.get(2).get(1), row.get(2).get(2)) * conv)
            rotationDegrees[1] = MatrixMathHelper.roundTo3Places((
                    -atan2(-row.get(2).get(0), sqrt(row.get(2).get(1) * row.get(2).get(1) + row.get(2).get(2) * row.get(2).get(2)))
                            * conv))
            rotationDegrees[2] = MatrixMathHelper.roundTo3Places(-atan2(row.get(1).get(0), row.get(0).get(0)) * conv)
        }

        private fun setTransformProperty(view: View, transforms: ReadableArray) {
            TransformHelper.processTransform(transforms, sTransformDecompositionArray)
            decomposeMatrix()
            view.setTranslationX(
                    PixelUtil.toPixelFromDIP(sMatrixDecompositionContext.translation.get(0).toFloat()))
            view.setTranslationY(
                    PixelUtil.toPixelFromDIP(sMatrixDecompositionContext.translation.get(1).toFloat()))
            view.setRotation(sMatrixDecompositionContext.rotationDegrees.get(2).toFloat())
            view.setRotationX(sMatrixDecompositionContext.rotationDegrees.get(0).toFloat())
            view.setRotationY(sMatrixDecompositionContext.rotationDegrees.get(1).toFloat())
            view.setScaleX(sMatrixDecompositionContext.scale.get(0).toFloat())
            view.setScaleY(sMatrixDecompositionContext.scale.get(1).toFloat())
            val perspectiveArray: DoubleArray = sMatrixDecompositionContext.perspective
            if (perspectiveArray.size > PERSPECTIVE_ARRAY_INVERTED_CAMERA_DISTANCE_INDEX) {
                var invertedCameraDistance: Float = perspectiveArray.get(PERSPECTIVE_ARRAY_INVERTED_CAMERA_DISTANCE_INDEX).toFloat()
                if (invertedCameraDistance == 0f) {
                    // Default camera distance, before scale multiplier (1280)
                    invertedCameraDistance = 0.00078125f
                }
                val cameraDistance: Float = -1 / invertedCameraDistance
                val scale: Float = DisplayMetricsHolder.getScreenDisplayMetrics().density

                // The following converts the matrix's perspective to a camera distance
                // such that the camera perspective looks the same on Android and iOS.
                // The native Android implementation removed the screen density from the
                // calculation, so squaring and a normalization value of
                // sqrt(5) produces an exact replica with iOS.
                // For more information, see https://github.com/facebook/react-native/pull/18302
                val normalizedCameraDistance: Float = scale * scale * cameraDistance * CAMERA_DISTANCE_NORMALIZATION_MULTIPLIER
                view.setCameraDistance(normalizedCameraDistance)
            }
        }

        private fun resetTransformProperty(view: View) {
            view.setTranslationX(0f)
            view.setTranslationY(0f)
            view.setRotation(0f)
            view.setRotationX(0f)
            view.setRotationY(0f)
            view.setScaleX(1f)
            view.setScaleY(1f)
            view.setCameraDistance(0f)
        }

        private val mTagToRenderableView: SparseArray<RenderableView> = SparseArray()
        private val mTagToRunnable: SparseArray<Runnable> = SparseArray()
        fun setRenderableView(tag: Int, svg: RenderableView) {
            mTagToRenderableView.put(tag, svg)
            val task: Runnable? = mTagToRunnable.get(tag)
            if (task != null) {
                task.run()
                mTagToRunnable.delete(tag)
            }
        }

        fun runWhenViewIsAvailable(tag: Int, task: Runnable) {
            mTagToRunnable.put(tag, task)
        }

        fun getRenderableViewByTag(tag: Int): RenderableView? {
            return mTagToRenderableView.get(tag)
        }
    }
}

/** ViewManager for Renderable RNSVG views  */
internal open class RenderableViewManager<T : RenderableView>(svgclass: SVGClass) : VirtualViewManager<T>(svgclass) {
    internal open class GroupViewManagerAbstract<U : GroupView>(svgClass: SVGClass) : RenderableViewManager<U>(svgClass) {
        @ReactProp(name = "font")
        open fun setFont(node: U, font: Dynamic) {
            node.setFont(font)
        }

        @ReactProp(name = "fontSize")
        fun setFontSize(node: U, fontSize: Dynamic) {
            val map: JavaOnlyMap = JavaOnlyMap()
            when (fontSize.type) {
                ReadableType.Number -> map.putDouble("fontSize", fontSize.asDouble())
                ReadableType.String -> map.putString("fontSize", fontSize.asString())
                else -> return
            }
            node.setFont(map)
        }

        @ReactProp(name = "fontWeight")
        fun setFontWeight(node: U, fontWeight: Dynamic) {
            val map: JavaOnlyMap = JavaOnlyMap()
            when (fontWeight.type) {
                ReadableType.Number -> map.putDouble("fontWeight", fontWeight.asDouble())
                ReadableType.String -> map.putString("fontWeight", fontWeight.asString())
                else -> return
            }
            node.setFont(map)
        }
    }

    internal class GroupViewManager : GroupViewManagerAbstract<GroupView>(SVGClass.RNSVGGroup), RNSVGGroupManagerInterface<GroupView> {
        init {
            mDelegate = RNSVGGroupManagerDelegate(this)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGGroup"
        }
    }

    internal class PathViewManager() : RenderableViewManager<PathView>(SVGClass.RNSVGPath), RNSVGPathManagerInterface<PathView> {
        init {
            mDelegate = RNSVGPathManagerDelegate(this)
        }

        @ReactProp(name = "d")
        public override fun setD(node: PathView, d: String?) {
            node.setD(d)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGPath"
        }
    }

    internal open class TextViewManagerAbstract<K : TextView>(svgClass: SVGClass) : GroupViewManagerAbstract<K>(svgClass) {
        @ReactProp(name = "inlineSize")
        fun setInlineSize(node: K, inlineSize: Dynamic) {
            node.setInlineSize(inlineSize)
        }

        @ReactProp(name = "textLength")
        fun setTextLength(node: K, length: Dynamic) {
            node.setTextLength(length)
        }

        @ReactProp(name = "lengthAdjust")
        fun setLengthAdjust(node: K, adjustment: String?) {
            node.setLengthAdjust(adjustment)
        }

        @ReactProp(name = "alignmentBaseline")
        open fun setMethod(node: K, alignment: String?) {
            node.setMethod(alignment)
        }

        @ReactProp(name = "baselineShift")
        fun setBaselineShift(node: K, baselineShift: Dynamic?) {
            node.setBaselineShift(baselineShift)
        }

        @ReactProp(name = "verticalAlign")
        fun setVerticalAlign(node: K, verticalAlign: Dynamic?) {
            node.setVerticalAlign(verticalAlign)
        }

        @ReactProp(name = "rotate")
        fun setRotate(node: K, rotate: Dynamic) {
            node.setRotate(rotate)
        }

        @ReactProp(name = "dx")
        fun setDx(node: K, deltaX: Dynamic) {
            node.setDeltaX(deltaX)
        }

        @ReactProp(name = "dy")
        fun setDy(node: K, deltaY: Dynamic) {
            node.setDeltaY(deltaY)
        }

        @ReactProp(name = "x")
        fun setX(node: K, positionX: Dynamic) {
            node.setPositionX(positionX)
        }

        @ReactProp(name = "y")
        fun setY(node: K, positionY: Dynamic) {
            node.setPositionY(positionY)
        }

        @ReactProp(name = "font")
        public override fun setFont(node: K, font: Dynamic) {
            node.setFont(font)
        }

        fun setAlignmentBaseline(view: K, value: String?) {
            view.setMethod(value)
        }
    }

    internal class TextViewManager : TextViewManagerAbstract<TextView>, RNSVGTextManagerInterface<TextView> {
        constructor() : super(SVGClass.RNSVGText) {
            mDelegate = RNSVGTextManagerDelegate(this)
        }

        constructor(svgClass: SVGClass) : super(svgClass) {
            mDelegate = RNSVGTextManagerDelegate(this)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGText"
        }
    }

    internal class TSpanViewManager : TextViewManagerAbstract<TSpanView>, RNSVGTSpanManagerInterface<TSpanView> {
        constructor() : super(SVGClass.RNSVGTSpan) {
            mDelegate = RNSVGTSpanManagerDelegate(this)
        }

        constructor(svgClass: SVGClass) : super(svgClass) {
            mDelegate = RNSVGTSpanManagerDelegate(this)
        }

        @ReactProp(name = "content")
        public override fun setContent(node: TSpanView, content: String?) {
            node.setContent(content)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGTSpan"
        }
    }

    internal class TextPathViewManager : TextViewManagerAbstract<TextPathView>, RNSVGTextPathManagerInterface<TextPathView> {
        constructor() : super(SVGClass.RNSVGTextPath) {
            mDelegate = RNSVGTextPathManagerDelegate(this)
        }

        constructor(svgClass: SVGClass) : super(svgClass) {
            mDelegate = RNSVGTextPathManagerDelegate(this)
        }

        @ReactProp(name = "href")
        public override fun setHref(node: TextPathView, href: String?) {
            node.setHref(href)
        }

        @ReactProp(name = "startOffset")
        public override fun setStartOffset(node: TextPathView, startOffset: Dynamic) {
            node.setStartOffset(startOffset)
        }

        @ReactProp(name = "method")
        public override fun setMethod(node: TextPathView, method: String?) {
            node.setMethod(method)
        }

        public override fun setMidLine(view: TextPathView, value: String?) {
            view?.setSharp(value)
        }

        @ReactProp(name = "spacing")
        public override fun setSpacing(node: TextPathView, spacing: String?) {
            node.setSpacing(spacing)
        }

        @ReactProp(name = "side")
        public override fun setSide(node: TextPathView, side: String?) {
            node.setSide(side)
        }

        @ReactProp(name = "midLine")
        fun setSharp(node: TextPathView, midLine: String?) {
            node.setSharp(midLine)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGTextPath"
        }
    }

    internal class ImageViewManager() : RenderableViewManager<ImageView>(SVGClass.RNSVGImage), RNSVGImageManagerInterface<ImageView> {
        init {
            mDelegate = RNSVGImageManagerDelegate(this)
        }

        @ReactProp(name = "x")
        public override fun setX(node: ImageView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        public override fun setY(node: ImageView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        public override fun setWidth(node: ImageView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        public override fun setHeight(node: ImageView, height: Dynamic) {
            node.setHeight(height)
        }

        @ReactProp(name = "src", customType = "ImageSource")
        public override fun setSrc(node: ImageView, src: ReadableMap?) {
            node.setSrc(src)
        }

        @ReactProp(name = "align")
        public override fun setAlign(node: ImageView, align: String?) {
            node.setAlign(align)
        }

        @ReactProp(name = "meetOrSlice")
        public override fun setMeetOrSlice(node: ImageView, meetOrSlice: Int) {
            node.setMeetOrSlice(meetOrSlice)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGImage"
        }
    }

    internal class CircleViewManager() : RenderableViewManager<CircleView>(SVGClass.RNSVGCircle), RNSVGCircleManagerInterface<CircleView> {
        init {
            mDelegate = RNSVGCircleManagerDelegate(this)
        }

        @ReactProp(name = "cx")
        public override fun setCx(node: CircleView, cx: Dynamic) {
            node.setCx(cx)
        }

        @ReactProp(name = "cy")
        public override fun setCy(node: CircleView, cy: Dynamic) {
            node.setCy(cy)
        }

        @ReactProp(name = "r")
        public override fun setR(node: CircleView, r: Dynamic) {
            node.setR(r)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGCircle"
        }
    }

    internal class EllipseViewManager() : RenderableViewManager<EllipseView>(SVGClass.RNSVGEllipse), RNSVGEllipseManagerInterface<EllipseView> {
        init {
            mDelegate = RNSVGEllipseManagerDelegate(this)
        }

        @ReactProp(name = "cx")
        public override fun setCx(node: EllipseView, cx: Dynamic) {
            node.setCx(cx)
        }

        @ReactProp(name = "cy")
        public override fun setCy(node: EllipseView, cy: Dynamic) {
            node.setCy(cy)
        }

        @ReactProp(name = "rx")
        public override fun setRx(node: EllipseView, rx: Dynamic) {
            node.setRx(rx)
        }

        @ReactProp(name = "ry")
        public override fun setRy(node: EllipseView, ry: Dynamic) {
            node.setRy(ry)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGEllipse"
        }
    }

    internal class LineViewManager() : RenderableViewManager<LineView>(SVGClass.RNSVGLine), RNSVGLineManagerInterface<LineView> {
        init {
            mDelegate = RNSVGLineManagerDelegate(this)
        }

        @ReactProp(name = "x1")
        public override fun setX1(node: LineView, x1: Dynamic) {
            node.setX1(x1)
        }

        @ReactProp(name = "y1")
        public override fun setY1(node: LineView, y1: Dynamic) {
            node.setY1(y1)
        }

        @ReactProp(name = "x2")
        public override fun setX2(node: LineView, x2: Dynamic) {
            node.setX2(x2)
        }

        @ReactProp(name = "y2")
        public override fun setY2(node: LineView, y2: Dynamic) {
            node.setY2(y2)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGLine"
        }
    }

    class RectViewManager() : RenderableViewManager<RectView>(SVGClass.RNSVGRect), RNSVGRectManagerInterface<RectView> {
        init {
            mDelegate = RNSVGRectManagerDelegate(this)
        }

        @ReactProp(name = "x")
        override fun setX(node: RectView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        override fun setY(node: RectView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        override fun setWidth(node: RectView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        override fun setHeight(node: RectView, height: Dynamic) {
            node.setHeight(height)
        }

        @ReactProp(name = "rx")
        override fun setRx(node: RectView, rx: Dynamic) {
            node.setRx(rx)
        }

        @ReactProp(name = "ry")
        override fun setRy(node: RectView, ry: Dynamic) {
            node.setRy(ry)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGRect"
        }
    }

    internal class ClipPathViewManager() : GroupViewManagerAbstract<ClipPathView>(SVGClass.RNSVGClipPath), RNSVGClipPathManagerInterface<ClipPathView> {
        init {
            mDelegate = RNSVGClipPathManagerDelegate(this)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGClipPath"
        }
    }

    internal class DefsViewManager() : VirtualViewManager<DefsView>(SVGClass.RNSVGDefs), RNSVGDefsManagerInterface<DefsView> {
        init {
            mDelegate = RNSVGDefsManagerDelegate(this)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGDefs"
        }
    }

    internal class UseViewManager() : RenderableViewManager<UseView>(SVGClass.RNSVGUse), RNSVGUseManagerInterface<UseView> {
        init {
            mDelegate = RNSVGUseManagerDelegate(this)
        }

        @ReactProp(name = "href")
        public override fun setHref(node: UseView, href: String?) {
            node.setHref(href)
        }

        @ReactProp(name = "x")
        public override fun setX(node: UseView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        public override fun setY(node: UseView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        public override fun setWidth(node: UseView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        public override fun setHeight(node: UseView, height: Dynamic) {
            node.setHeight(height)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGUse"
        }
    }

    internal class SymbolManager() : GroupViewManagerAbstract<SymbolView>(SVGClass.RNSVGSymbol), RNSVGSymbolManagerInterface<SymbolView> {
        init {
            mDelegate = RNSVGSymbolManagerDelegate(this)
        }

        @ReactProp(name = "minX")
        public override fun setMinX(node: SymbolView, minX: Float) {
            node.setMinX(minX)
        }

        @ReactProp(name = "minY")
        public override fun setMinY(node: SymbolView, minY: Float) {
            node.setMinY(minY)
        }

        @ReactProp(name = "vbWidth")
        public override fun setVbWidth(node: SymbolView, vbWidth: Float) {
            node.setVbWidth(vbWidth)
        }

        @ReactProp(name = "vbHeight")
        public override fun setVbHeight(node: SymbolView, vbHeight: Float) {
            node.setVbHeight(vbHeight)
        }

        @ReactProp(name = "align")
        public override fun setAlign(node: SymbolView, align: String?) {
            node.setAlign(align)
        }

        @ReactProp(name = "meetOrSlice")
        public override fun setMeetOrSlice(node: SymbolView, meetOrSlice: Int) {
            node.setMeetOrSlice(meetOrSlice)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGSymbol"
        }
    }

    internal class PatternManager() : GroupViewManagerAbstract<PatternView>(SVGClass.RNSVGPattern), RNSVGPatternManagerInterface<PatternView> {
        init {
            mDelegate = RNSVGPatternManagerDelegate(this)
        }

        @ReactProp(name = "x")
        public override fun setX(node: PatternView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        public override fun setY(node: PatternView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        public override fun setWidth(node: PatternView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        public override fun setHeight(node: PatternView, height: Dynamic) {
            node.setHeight(height)
        }

        @ReactProp(name = "patternUnits")
        public override fun setPatternUnits(node: PatternView, patternUnits: Int) {
            node.setPatternUnits(patternUnits)
        }

        @ReactProp(name = "patternContentUnits")
        public override fun setPatternContentUnits(node: PatternView, patternContentUnits: Int) {
            node.setPatternContentUnits(patternContentUnits)
        }

        @ReactProp(name = "patternTransform")
        public override fun setPatternTransform(node: PatternView, matrixArray: ReadableArray?) {
            node.setPatternTransform(matrixArray)
        }

        @ReactProp(name = "minX")
        public override fun setMinX(node: PatternView, minX: Float) {
            node.setMinX(minX)
        }

        @ReactProp(name = "minY")
        public override fun setMinY(node: PatternView, minY: Float) {
            node.setMinY(minY)
        }

        @ReactProp(name = "vbWidth")
        public override fun setVbWidth(node: PatternView, vbWidth: Float) {
            node.setVbWidth(vbWidth)
        }

        @ReactProp(name = "vbHeight")
        public override fun setVbHeight(node: PatternView, vbHeight: Float) {
            node.setVbHeight(vbHeight)
        }

        @ReactProp(name = "align")
        public override fun setAlign(node: PatternView, align: String?) {
            node.setAlign(align)
        }

        @ReactProp(name = "meetOrSlice")
        public override fun setMeetOrSlice(node: PatternView, meetOrSlice: Int) {
            node.setMeetOrSlice(meetOrSlice)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGPattern"
        }
    }

    internal class MaskManager() : GroupViewManagerAbstract<MaskView>(SVGClass.RNSVGMask), RNSVGMaskManagerInterface<MaskView> {
        init {
            mDelegate = RNSVGMaskManagerDelegate(this)
        }

        @ReactProp(name = "x")
        public override fun setX(node: MaskView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        public override fun setY(node: MaskView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        public override fun setWidth(node: MaskView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        public override fun setHeight(node: MaskView, height: Dynamic) {
            node.setHeight(height)
        }

        @ReactProp(name = "maskUnits")
        public override fun setMaskUnits(node: MaskView, maskUnits: Int) {
            node.setMaskUnits(maskUnits)
        }

        @ReactProp(name = "maskContentUnits")
        public override fun setMaskContentUnits(node: MaskView, maskContentUnits: Int) {
            node.setMaskContentUnits(maskContentUnits)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGMask"
        }
    }

    internal class ForeignObjectManager() : GroupViewManagerAbstract<ForeignObjectView>(SVGClass.RNSVGForeignObject), RNSVGForeignObjectManagerInterface<ForeignObjectView> {
        init {
            mDelegate = RNSVGForeignObjectManagerDelegate(this)
        }

        @ReactProp(name = "x")
        public override fun setX(node: ForeignObjectView, x: Dynamic) {
            node.setX(x)
        }

        @ReactProp(name = "y")
        public override fun setY(node: ForeignObjectView, y: Dynamic) {
            node.setY(y)
        }

        @ReactProp(name = "width")
        public override fun setWidth(node: ForeignObjectView, width: Dynamic) {
            node.setWidth(width)
        }

        @ReactProp(name = "height")
        public override fun setHeight(node: ForeignObjectView, height: Dynamic) {
            node.setHeight(height)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGForeignObject"
        }
    }

    internal class MarkerManager() : GroupViewManagerAbstract<MarkerView>(SVGClass.RNSVGMarker), RNSVGMarkerManagerInterface<MarkerView> {
        init {
            mDelegate = RNSVGMarkerManagerDelegate(this)
        }

        @ReactProp(name = "refX")
        public override fun setRefX(node: MarkerView, refX: Dynamic) {
            node.setRefX(refX)
        }

        @ReactProp(name = "refY")
        public override fun setRefY(node: MarkerView, refY: Dynamic) {
            node.setRefY(refY)
        }

        @ReactProp(name = "markerWidth")
        public override fun setMarkerWidth(node: MarkerView, markerWidth: Dynamic) {
            node.setMarkerWidth(markerWidth)
        }

        @ReactProp(name = "markerHeight")
        public override fun setMarkerHeight(node: MarkerView, markerHeight: Dynamic) {
            node.setMarkerHeight(markerHeight)
        }

        @ReactProp(name = "markerUnits")
        public override fun setMarkerUnits(node: MarkerView, markerUnits: String?) {
            node.setMarkerUnits(markerUnits)
        }

        @ReactProp(name = "orient")
        public override fun setOrient(node: MarkerView, orient: String?) {
            node.setOrient(orient)
        }

        @ReactProp(name = "minX")
        public override fun setMinX(node: MarkerView, minX: Float) {
            node.setMinX(minX)
        }

        @ReactProp(name = "minY")
        public override fun setMinY(node: MarkerView, minY: Float) {
            node.setMinY(minY)
        }

        @ReactProp(name = "vbWidth")
        public override fun setVbWidth(node: MarkerView, vbWidth: Float) {
            node.setVbWidth(vbWidth)
        }

        @ReactProp(name = "vbHeight")
        public override fun setVbHeight(node: MarkerView, vbHeight: Float) {
            node.setVbHeight(vbHeight)
        }

        @ReactProp(name = "align")
        public override fun setAlign(node: MarkerView, align: String?) {
            node.setAlign(align)
        }

        @ReactProp(name = "meetOrSlice")
        public override fun setMeetOrSlice(node: MarkerView, meetOrSlice: Int) {
            node.setMeetOrSlice(meetOrSlice)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGMarker"
        }
    }

    internal class LinearGradientManager() : VirtualViewManager<LinearGradientView>(SVGClass.RNSVGLinearGradient), RNSVGLinearGradientManagerInterface<LinearGradientView> {
        init {
            mDelegate = RNSVGLinearGradientManagerDelegate(this)
        }

        @ReactProp(name = "x1")
        public override fun setX1(node: LinearGradientView, x1: Dynamic) {
            node.setX1(x1)
        }

        @ReactProp(name = "y1")
        public override fun setY1(node: LinearGradientView, y1: Dynamic) {
            node.setY1(y1)
        }

        @ReactProp(name = "x2")
        public override fun setX2(node: LinearGradientView, x2: Dynamic) {
            node.setX2(x2)
        }

        @ReactProp(name = "y2")
        public override fun setY2(node: LinearGradientView, y2: Dynamic) {
            node.setY2(y2)
        }

        @ReactProp(name = "gradient")
        public override fun setGradient(node: LinearGradientView, gradient: ReadableArray?) {
            node.setGradient(gradient)
        }

        @ReactProp(name = "gradientUnits")
        public override fun setGradientUnits(node: LinearGradientView, gradientUnits: Int) {
            node.setGradientUnits(gradientUnits)
        }

        @ReactProp(name = "gradientTransform")
        public override fun setGradientTransform(node: LinearGradientView, matrixArray: ReadableArray?) {
            node.setGradientTransform(matrixArray)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGLinearGradient"
        }
    }

    internal class RadialGradientManager() : VirtualViewManager<RadialGradientView>(SVGClass.RNSVGRadialGradient), RNSVGRadialGradientManagerInterface<RadialGradientView> {
        init {
            mDelegate = RNSVGRadialGradientManagerDelegate(this)
        }

        @ReactProp(name = "fx")
        public override fun setFx(node: RadialGradientView, fx: Dynamic) {
            node.setFx(fx)
        }

        @ReactProp(name = "fy")
        public override fun setFy(node: RadialGradientView, fy: Dynamic) {
            node.setFy(fy)
        }

        @ReactProp(name = "rx")
        public override fun setRx(node: RadialGradientView, rx: Dynamic) {
            node.setRx(rx)
        }

        @ReactProp(name = "ry")
        public override fun setRy(node: RadialGradientView, ry: Dynamic) {
            node.setRy(ry)
        }

        @ReactProp(name = "cx")
        public override fun setCx(node: RadialGradientView, cx: Dynamic) {
            node.setCx(cx)
        }

        @ReactProp(name = "cy")
        public override fun setCy(node: RadialGradientView, cy: Dynamic) {
            node.setCy(cy)
        }

        @ReactProp(name = "gradient")
        public override fun setGradient(node: RadialGradientView, gradient: ReadableArray?) {
            node.setGradient(gradient)
        }

        @ReactProp(name = "gradientUnits")
        public override fun setGradientUnits(node: RadialGradientView, gradientUnits: Int) {
            node.setGradientUnits(gradientUnits)
        }

        @ReactProp(name = "gradientTransform")
        public override fun setGradientTransform(node: RadialGradientView, matrixArray: ReadableArray?) {
            node.setGradientTransform(matrixArray)
        }

        companion object {
            val REACT_CLASS: String = "RNSVGRadialGradient"
        }
    }

    @ReactProp(name = "fill")
    fun setFill(node: T, fill: Dynamic?) {
        node.setFill(fill)
    }

    fun setFill(view: T, value: ReadableMap?) {
        view.setFill(value)
    }

    @ReactProp(name = "fillOpacity", defaultFloat = 1f)
    fun setFillOpacity(node: T, fillOpacity: Float) {
        node.fillOpacity = fillOpacity
    }

    @ReactProp(name = "fillRule", defaultInt = FILL_RULE_NONZERO)
    fun setFillRule(node: T, fillRule: Int) {
        node.setFillRule(fillRule)
    }

    @ReactProp(name = "stroke")
    fun setStroke(node: T, strokeColors: Dynamic?) {
        node.setStroke(strokeColors)
    }

    fun setStroke(view: T, value: ReadableMap?) {
        view.setStroke(value)
    }

    @ReactProp(name = "strokeOpacity", defaultFloat = 1f)
    fun setStrokeOpacity(node: T, strokeOpacity: Float) {
        node.strokeOpacity = strokeOpacity
    }

    @ReactProp(name = "strokeDasharray")
    fun setStrokeDasharray(node: T, strokeDasharray: Dynamic) {
        node.setStrokeDasharray(strokeDasharray)
    }

    @ReactProp(name = "strokeDashoffset")
    fun setStrokeDashoffset(node: T, strokeDashoffset: Float) {
        node.strokeDashoffset = strokeDashoffset
    }

    @ReactProp(name = "strokeWidth")
    fun setStrokeWidth(node: T, strokeWidth: Dynamic) {
        node.setStrokeWidth(strokeWidth)
    }

    @ReactProp(name = "strokeMiterlimit", defaultFloat = 4f)
    fun setStrokeMiterlimit(node: T, strokeMiterlimit: Float) {
        node.strokeMiterlimit = strokeMiterlimit
    }

    @ReactProp(name = "strokeLinecap", defaultInt = CAP_ROUND)
    fun setStrokeLinecap(node: T, strokeLinecap: Int) {
        node.setStrokeLinecap(strokeLinecap)
    }

    @ReactProp(name = "strokeLinejoin", defaultInt = JOIN_ROUND)
    fun setStrokeLinejoin(node: T, strokeLinejoin: Int) {
        node.setStrokeLinejoin(strokeLinejoin)
    }

    @ReactProp(name = "vectorEffect")
    fun setVectorEffect(node: T, vectorEffect: Int) {
        node.vectorEffect = vectorEffect
    }

    @ReactProp(name = "propList")
    fun setPropList(node: T, propList: ReadableArray?) {
        node.setPropList(propList)
    }
}
