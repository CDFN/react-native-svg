package com.horcrux.svg

import kotlin.math.abs
import kotlin.math.atan2

internal enum class RNSVGMarkerType {
    kStartMarker,
    kMidMarker,
    kEndMarker
}

enum class ElementType {
    kCGPathElementAddCurveToPoint,
    kCGPathElementAddQuadCurveToPoint,
    kCGPathElementMoveToPoint,
    kCGPathElementAddLineToPoint,
    kCGPathElementCloseSubpath
}

class Point(var x: Double, var y: Double)
internal class SegmentData() {
    var start_tangent: Point? = null // Tangent in the start point of the segment.
    var end_tangent: Point? = null // Tangent in the end point of the segment.
    var position: Point? = null // The end point of the segment.
}

internal class RNSVGMarkerPosition private constructor(var type: RNSVGMarkerType, var origin: Point?, var angle: Double) {
    companion object {
        private var positions_: ArrayList<RNSVGMarkerPosition>? = null
        private var element_index_: Int = 0
        private var origin_: Point? = null
        private var subpath_start_: Point? = null
        private var in_slope_: Point? = null
        private var out_slope_: Point? = null

        @Suppress("unused")
        private val auto_start_reverse_: Boolean = false // TODO
        fun fromPath(elements: ArrayList<PathElement>): ArrayList<RNSVGMarkerPosition>? {
            positions_ = ArrayList()
            element_index_ = 0
            origin_ = Point(0.0, 0.0)
            subpath_start_ = Point(0.0, 0.0)
            for (e: PathElement in elements) {
                UpdateFromPathElement(e)
            }
            PathIsDone()
            return positions_
        }

        private fun PathIsDone() {
            val angle: Double = CurrentAngle(RNSVGMarkerType.kEndMarker)
            positions_!!.add(RNSVGMarkerPosition(RNSVGMarkerType.kEndMarker, origin_, angle))
        }

        private fun BisectingAngle(in_angle: Double, out_angle: Double): Double {
            // WK193015: Prevent bugs due to angles being non-continuous.
            var in_angle: Double = in_angle
            if (abs(in_angle - out_angle) > 180) in_angle += 360.0
            return (in_angle + out_angle) / 2
        }

        private fun rad2deg(rad: Double): Double {
            val RNSVG_radToDeg: Double = 180 / Math.PI
            return rad * RNSVG_radToDeg
        }

        private fun SlopeAngleRadians(p: Point?): Double {
            return atan2(p!!.y, p.x)
        }

        private fun CurrentAngle(type: RNSVGMarkerType): Double {
            // For details of this calculation, see:
            // http://www.w3.org/TR/SVG/single-page.html#painting-MarkerElement
            val in_angle: Double = rad2deg(SlopeAngleRadians(in_slope_))
            var out_angle: Double = rad2deg(SlopeAngleRadians(out_slope_))
            when (type) {
                RNSVGMarkerType.kStartMarker -> {
                    if (auto_start_reverse_) out_angle += 180.0
                    return out_angle
                }

                RNSVGMarkerType.kMidMarker -> return BisectingAngle(in_angle, out_angle)
                RNSVGMarkerType.kEndMarker -> return in_angle
            }
            return 0.0
        }

        private fun subtract(p1: Point?, p2: Point?): Point {
            return Point(p2!!.x - p1!!.x, p2.y - p1.y)
        }

        private fun isZero(p: Point?): Boolean {
            return p!!.x == 0.0 && p.y == 0.0
        }

        private fun ComputeQuadTangents(data: SegmentData, start: Point?, control: Point?, end: Point?) {
            data.start_tangent = subtract(control, start)
            data.end_tangent = subtract(end, control)
            if (isZero(data.start_tangent)) data.start_tangent = data.end_tangent else if (isZero(data.end_tangent)) data.end_tangent = data.start_tangent
        }

        private fun ExtractPathElementFeatures(element: PathElement): SegmentData {
            val data: SegmentData = SegmentData()
            val points: Array<Point?>? = element.points
            when (element.type) {
                ElementType.kCGPathElementAddCurveToPoint -> {
                    data.position = points!!.get(2)
                    data.start_tangent = subtract(points.get(0), origin_)
                    data.end_tangent = subtract(points.get(2), points.get(1))
                    if (isZero(data.start_tangent)) ComputeQuadTangents(data, points.get(0), points.get(1), points.get(2)) else if (isZero(data.end_tangent)) ComputeQuadTangents(data, origin_, points.get(0), points.get(1))
                }

                ElementType.kCGPathElementAddQuadCurveToPoint -> {
                    data.position = points!!.get(1)
                    ComputeQuadTangents(data, origin_, points.get(0), points.get(1))
                }

                ElementType.kCGPathElementMoveToPoint, ElementType.kCGPathElementAddLineToPoint -> {
                    data.position = points!!.get(0)
                    data.start_tangent = subtract(data.position, origin_)
                    data.end_tangent = subtract(data.position, origin_)
                }

                ElementType.kCGPathElementCloseSubpath -> {
                    data.position = subpath_start_
                    data.start_tangent = subtract(data.position, origin_)
                    data.end_tangent = subtract(data.position, origin_)
                }
            }
            return data
        }

        private fun UpdateFromPathElement(element: PathElement) {
            val segment_data: SegmentData = ExtractPathElementFeatures(element)
            // First update the outgoing slope for the previous element.
            out_slope_ = segment_data.start_tangent
            // Record the marker for the previous element.
            if (element_index_ > 0) {
                val marker_type: RNSVGMarkerType = if (element_index_ == 1) RNSVGMarkerType.kStartMarker else RNSVGMarkerType.kMidMarker
                val angle: Double = CurrentAngle(marker_type)
                positions_!!.add(RNSVGMarkerPosition(marker_type, origin_, angle))
            }
            // Update the incoming slope for this marker position.
            in_slope_ = segment_data.end_tangent
            // Update marker position.
            origin_ = segment_data.position
            // If this is a 'move to' segment, save the point for use with 'close'.
            if (element.type == ElementType.kCGPathElementMoveToPoint) subpath_start_ = element.points.get(0) else if (element.type == ElementType.kCGPathElementCloseSubpath) subpath_start_ = Point(0.0, 0.0)
            ++element_index_
        }
    }
}
