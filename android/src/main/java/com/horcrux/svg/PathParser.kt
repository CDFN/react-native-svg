package com.horcrux.svg

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class PathElement(var type: ElementType, var points: Array<Point?>)
internal object PathParser {
    var mScale: Float = 0f
    private var i: Int = 0
    private var l: Int = 0
    private var s: String? = null
    private var mPath: Path? = null
    var elements: ArrayList<PathElement>? = null
    private var mPenX: Float = 0f
    private var mPenY: Float = 0f
    private var mPivotX: Float = 0f
    private var mPivotY: Float = 0f
    private var mPenDownX: Float = 0f
    private var mPenDownY: Float = 0f
    private var mPenDown: Boolean = false
    fun parse(d: String?): Path {
        elements = ArrayList()
        mPath = Path()
        if (d == null) {
            return mPath as Path
        }
        var prev_cmd: Char = ' '
        l = d.length
        s = d
        i = 0
        mPenX = 0f
        mPenY = 0f
        mPivotX = 0f
        mPivotY = 0f
        mPenDownX = 0f
        mPenDownY = 0f
        mPenDown = false
        while (i < l) {
            skip_spaces()
            if (i >= l) {
                break
            }
            val has_prev_cmd: Boolean = prev_cmd != ' '
            val first_char: Char = s!!.get(i)
            if (!has_prev_cmd && (first_char != 'M') && (first_char != 'm')) {
                // The first segment must be a MoveTo.
                throw Error(String.format("Unexpected character '%c' (i=%d, s=%s)", first_char, i, s))
            }

            // TODO: simplify
            var is_implicit_move_to: Boolean
            var cmd: Char
            if (is_cmd(first_char)) {
                is_implicit_move_to = false
                cmd = first_char
                i += 1
            } else if (is_number_start(first_char) && has_prev_cmd) {
                if (prev_cmd == 'Z' || prev_cmd == 'z') {
                    // ClosePath cannot be followed by a number.
                    throw Error(String.format("Unexpected number after 'z' (s=%s)", s))
                }
                if (prev_cmd == 'M' || prev_cmd == 'm') {
                    // 'If a moveto is followed by multiple pairs of coordinates,
                    // the subsequent pairs are treated as implicit lineto commands.'
                    // So we parse them as LineTo.
                    is_implicit_move_to = true
                    if (is_absolute(prev_cmd)) {
                        cmd = 'L'
                    } else {
                        cmd = 'l'
                    }
                } else {
                    is_implicit_move_to = false
                    cmd = prev_cmd
                }
            } else {
                throw Error(String.format("Unexpected character '%c' (i=%d, s=%s)", first_char, i, s))
            }
            val absolute: Boolean = is_absolute(cmd)
            when (cmd) {
                'm' -> {
                    move(parse_list_number(), parse_list_number())
                }

                'M' -> {
                    moveTo(parse_list_number(), parse_list_number())
                }

                'l' -> {
                    line(parse_list_number(), parse_list_number())
                }

                'L' -> {
                    lineTo(parse_list_number(), parse_list_number())
                }

                'h' -> {
                    line(parse_list_number(), 0f)
                }

                'H' -> {
                    lineTo(parse_list_number(), mPenY)
                }

                'v' -> {
                    line(0f, parse_list_number())
                }

                'V' -> {
                    lineTo(mPenX, parse_list_number())
                }

                'c' -> {
                    curve(
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number())
                }

                'C' -> {
                    curveTo(
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number())
                }

                's' -> {
                    smoothCurve(
                            parse_list_number(), parse_list_number(), parse_list_number(), parse_list_number())
                }

                'S' -> {
                    smoothCurveTo(
                            parse_list_number(), parse_list_number(), parse_list_number(), parse_list_number())
                }

                'q' -> {
                    quadraticBezierCurve(
                            parse_list_number(), parse_list_number(), parse_list_number(), parse_list_number())
                }

                'Q' -> {
                    quadraticBezierCurveTo(
                            parse_list_number(), parse_list_number(), parse_list_number(), parse_list_number())
                }

                't' -> {
                    smoothQuadraticBezierCurve(parse_list_number(), parse_list_number())
                }

                'T' -> {
                    smoothQuadraticBezierCurveTo(parse_list_number(), parse_list_number())
                }

                'a' -> {
                    arc(
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_flag(),
                            parse_flag(),
                            parse_list_number(),
                            parse_list_number())
                }

                'A' -> {
                    arcTo(
                            parse_list_number(),
                            parse_list_number(),
                            parse_list_number(),
                            parse_flag(),
                            parse_flag(),
                            parse_list_number(),
                            parse_list_number())
                }

                'z', 'Z' -> {
                    close()
                }

                else -> {
                    throw Error(String.format("Unexpected comand '%c' (s=%s)", cmd, s))
                }
            }
            if (is_implicit_move_to) {
                if (absolute) {
                    prev_cmd = 'M'
                } else {
                    prev_cmd = 'm'
                }
            } else {
                prev_cmd = cmd
            }
        }
        return mPath as Path
    }

    private fun move(x: Float, y: Float) {
        moveTo(x + mPenX, y + mPenY)
    }

    private fun moveTo(x: Float, y: Float) {
        // FLog.w(ReactConstants.TAG, "move x: " + x + " y: " + y);
        mPenX = x
        mPivotX = mPenX
        mPenDownX = mPivotX
        mPenY = y
        mPivotY = mPenY
        mPenDownY = mPivotY
        mPath!!.moveTo(x * mScale, y * mScale)
        elements!!.add(
                PathElement(ElementType.kCGPathElementMoveToPoint, arrayOf(Point(x.toDouble(), y.toDouble()))))
    }

    private fun line(x: Float, y: Float) {
        lineTo(x + mPenX, y + mPenY)
    }

    private fun lineTo(x: Float, y: Float) {
        // FLog.w(ReactConstants.TAG, "line x: " + x + " y: " + y);
        setPenDown()
        mPenX = x
        mPivotX = mPenX
        mPenY = y
        mPivotY = mPenY
        mPath!!.lineTo(x * mScale, y * mScale)
        elements!!.add(
                PathElement(ElementType.kCGPathElementAddLineToPoint, arrayOf(Point(x.toDouble(), y.toDouble()))))
    }

    private fun curve(c1x: Float, c1y: Float, c2x: Float, c2y: Float, ex: Float, ey: Float) {
        curveTo(c1x + mPenX, c1y + mPenY, c2x + mPenX, c2y + mPenY, ex + mPenX, ey + mPenY)
    }

    private fun curveTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, ex: Float, ey: Float) {
        // FLog.w(ReactConstants.TAG, "curve c1x: " + c1x + " c1y: " + c1y + "ex: " + ex + " ey: " +
        // ey);
        mPivotX = c2x
        mPivotY = c2y
        cubicTo(c1x, c1y, c2x, c2y, ex, ey)
    }

    private fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, ex: Float, ey: Float) {
        setPenDown()
        mPenX = ex
        mPenY = ey
        mPath!!.cubicTo(c1x * mScale, c1y * mScale, c2x * mScale, c2y * mScale, ex * mScale, ey * mScale)
        elements!!.add(
                PathElement(
                        ElementType.kCGPathElementAddCurveToPoint, arrayOf(Point(c1x.toDouble(), c1y.toDouble()), Point(c2x.toDouble(), c2y.toDouble()), Point(ex.toDouble(), ey.toDouble()))))
    }

    private fun smoothCurve(c1x: Float, c1y: Float, ex: Float, ey: Float) {
        smoothCurveTo(c1x + mPenX, c1y + mPenY, ex + mPenX, ey + mPenY)
    }

    private fun smoothCurveTo(c1x: Float, c1y: Float, ex: Float, ey: Float) {
        // FLog.w(ReactConstants.TAG, "smoothcurve c1x: " + c1x + " c1y: " + c1y + "ex: " + ex + " ey: "
        // + ey);
        var c1x: Float = c1x
        var c1y: Float = c1y
        val c2x: Float = c1x
        val c2y: Float = c1y
        c1x = (mPenX * 2) - mPivotX
        c1y = (mPenY * 2) - mPivotY
        mPivotX = c2x
        mPivotY = c2y
        cubicTo(c1x, c1y, c2x, c2y, ex, ey)
    }

    private fun quadraticBezierCurve(c1x: Float, c1y: Float, c2x: Float, c2y: Float) {
        quadraticBezierCurveTo(c1x + mPenX, c1y + mPenY, c2x + mPenX, c2y + mPenY)
    }

    private fun quadraticBezierCurveTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float) {
        // FLog.w(ReactConstants.TAG, "quad c1x: " + c1x + " c1y: " + c1y + "c2x: " + c2x + " c2y: " +
        // c2y);
        var c1x: Float = c1x
        var c1y: Float = c1y
        var c2x: Float = c2x
        var c2y: Float = c2y
        mPivotX = c1x
        mPivotY = c1y
        val ex: Float = c2x
        val ey: Float = c2y
        c2x = (ex + c1x * 2) / 3
        c2y = (ey + c1y * 2) / 3
        c1x = (mPenX + c1x * 2) / 3
        c1y = (mPenY + c1y * 2) / 3
        cubicTo(c1x, c1y, c2x, c2y, ex, ey)
    }

    private fun smoothQuadraticBezierCurve(c1x: Float, c1y: Float) {
        smoothQuadraticBezierCurveTo(c1x + mPenX, c1y + mPenY)
    }

    private fun smoothQuadraticBezierCurveTo(c1x: Float, c1y: Float) {
        // FLog.w(ReactConstants.TAG, "smoothquad c1x: " + c1x + " c1y: " + c1y);
        var c1x: Float = c1x
        var c1y: Float = c1y
        val c2x: Float = c1x
        val c2y: Float = c1y
        c1x = (mPenX * 2) - mPivotX
        c1y = (mPenY * 2) - mPivotY
        quadraticBezierCurveTo(c1x, c1y, c2x, c2y)
    }

    private fun arc(
            rx: Float, ry: Float, rotation: Float, outer: Boolean, clockwise: Boolean, x: Float, y: Float) {
        arcTo(rx, ry, rotation, outer, clockwise, x + mPenX, y + mPenY)
    }

    private fun arcTo(
            rx: Float, ry: Float, rotation: Float, outer: Boolean, clockwise: Boolean, x: Float, y: Float) {
        // FLog.w(ReactConstants.TAG, "arc rx: " + rx + " ry: " + ry + " rotation: " + rotation + "
        // outer: " + outer + " clockwise: " + clockwise + " x: " + x + " y: " + y);
        var rx: Float = rx
        var ry: Float = ry
        var x: Float = x
        var y: Float = y
        val tX: Float = mPenX
        val tY: Float = mPenY
        ry = abs((if (ry == 0f) (if (rx == 0f) (y - tY) else rx) else ry).toDouble()).toFloat()
        rx = abs((if (rx == 0f) (x - tX) else rx).toDouble()).toFloat()
        if ((rx == 0f) || (ry == 0f) || (x == tX && y == tY)) {
            lineTo(x, y)
            return
        }
        val rad: Float = Math.toRadians(rotation.toDouble()).toFloat()
        val cos: Float = cos(rad.toDouble()).toFloat()
        val sin: Float = sin(rad.toDouble()).toFloat()
        x -= tX
        y -= tY

        // Ellipse Center
        var cx: Float = cos * x / 2 + sin * y / 2
        var cy: Float = -sin * x / 2 + cos * y / 2
        val rxry: Float = (rx * rx * ry * ry).toFloat()
        val rycx: Float = (ry * ry * cx * cx).toFloat()
        val rxcy: Float = (rx * rx * cy * cy).toFloat()
        var a: Float = rxry - rxcy - rycx
        if (a < 0) {
            a = sqrt((1 - a / rxry).toDouble()).toFloat()
            rx *= a
            ry *= a
            cx = x / 2
            cy = y / 2
        } else {
            a = sqrt((a / (rxcy + rycx)).toDouble()).toFloat()
            if (outer == clockwise) {
                a = -a
            }
            val cxd: Float = (-a * cy * rx / ry).toFloat()
            val cyd: Float = (a * cx * ry / rx).toFloat()
            cx = cos * cxd - sin * cyd + x / 2
            cy = (sin * cxd) + (cos * cyd) + (y / 2)
        }

        // Rotation + Scale Transform
        val xx: Float = (cos / rx).toFloat()
        val yx: Float = (sin / rx).toFloat()
        val xy: Float = (-sin / ry).toFloat()
        val yy: Float = (cos / ry).toFloat()

        // Start and End Angle
        val sa: Float = atan2((xy * -cx + yy * -cy).toDouble(), (xx * -cx + yx * -cy).toDouble()).toFloat()
        val ea: Float = atan2((xy * (x - cx) + yy * (y - cy)).toDouble(), (xx * (x - cx) + yx * (y - cy)).toDouble()).toFloat()
        cx += tX
        cy += tY
        x += tX
        y += tY
        setPenDown()
        mPivotX = x
        mPenX = mPivotX
        mPivotY = y
        mPenY = mPivotY
        if (rx != ry || rad != 0f) {
            arcToBezier(cx, cy, rx, ry, sa, ea, clockwise, rad)
        } else {
            val start: Float = Math.toDegrees(sa.toDouble()).toFloat()
            val end: Float = Math.toDegrees(ea.toDouble()).toFloat()
            var sweep: Float = abs(((start - end) % 360).toDouble()).toFloat()
            if (outer) {
                if (sweep < 180) {
                    sweep = 360 - sweep
                }
            } else {
                if (sweep > 180) {
                    sweep = 360 - sweep
                }
            }
            if (!clockwise) {
                sweep = -sweep
            }
            val oval: RectF = RectF(((cx - rx) * mScale).toFloat(), ((cy - rx) * mScale).toFloat(), ((cx + rx) * mScale).toFloat(), ((cy + rx) * mScale).toFloat())
            mPath!!.arcTo(oval, start, sweep)
            elements!!.add(
                    PathElement(
                            ElementType.kCGPathElementAddCurveToPoint, arrayOf(Point(x.toDouble(), y.toDouble()))))
        }
    }

    private fun close() {
        if (mPenDown) {
            mPenX = mPenDownX
            mPenY = mPenDownY
            mPenDown = false
            mPath!!.close()
            elements!!.add(
                    PathElement(
                            ElementType.kCGPathElementCloseSubpath, arrayOf(Point(mPenX.toDouble(), mPenY.toDouble()))))
        }
    }

    private fun arcToBezier(
            cx: Float, cy: Float, rx: Float, ry: Float, sa: Float, ea: Float, clockwise: Boolean, rad: Float) {
        // Inverse Rotation + Scale Transform
        var sa: Float = sa
        val cos: Float = cos(rad.toDouble()).toFloat()
        val sin: Float = sin(rad.toDouble()).toFloat()
        val xx: Float = cos * rx
        val yx: Float = -sin * ry
        val xy: Float = sin * rx
        val yy: Float = cos * ry

        // Bezier Curve Approximation
        var arc: Float = ea - sa
        if (arc < 0 && clockwise) {
            arc += (Math.PI * 2).toFloat()
        } else if (arc > 0 && !clockwise) {
            arc -= (Math.PI * 2).toFloat()
        }
        val n: Int = ceil(abs(round(arc / (Math.PI / 2)))).toInt()
        val step: Float = arc / n
        val k: Float = ((4 / 3.0) * tan((step / 4).toDouble())).toFloat()
        var x: Float = cos(sa.toDouble()).toFloat()
        var y: Float = sin(sa.toDouble()).toFloat()
        for (i in 0 until n) {
            val cp1x: Float = x - k * y
            val cp1y: Float = y + k * x
            sa += step
            x = cos(sa.toDouble()).toFloat()
            y = sin(sa.toDouble()).toFloat()
            val cp2x: Float = x + k * y
            val cp2y: Float = y - k * x
            val c1x: Float = (cx + (xx * cp1x) + (yx * cp1y))
            val c1y: Float = (cy + (xy * cp1x) + (yy * cp1y))
            val c2x: Float = (cx + (xx * cp2x) + (yx * cp2y))
            val c2y: Float = (cy + (xy * cp2x) + (yy * cp2y))
            val ex: Float = (cx + (xx * x) + (yx * y))
            val ey: Float = (cy + (xy * x) + (yy * y))
            mPath!!.cubicTo(
                    c1x * mScale, c1y * mScale, c2x * mScale, c2y * mScale, ex * mScale, ey * mScale)
            elements!!.add(
                    PathElement(
                            ElementType.kCGPathElementAddCurveToPoint, arrayOf(Point(c1x.toDouble(), c1y.toDouble()), Point(c2x.toDouble(), c2y.toDouble()), Point(ex.toDouble(), ey.toDouble()))))
        }
    }

    private fun setPenDown() {
        if (!mPenDown) {
            mPenDownX = mPenX
            mPenDownY = mPenY
            mPenDown = true
        }
    }

    private fun round(`val`: Double): Double {
        val multiplier: Double = 10.0.pow(4.0)
        return Math.round(`val` * multiplier) / multiplier
    }

    private fun skip_spaces() {
        while (i < l && Character.isWhitespace(s!!.get(i))) i++
    }

    private fun is_cmd(c: Char): Boolean {
        when (c) {
            'M', 'm', 'Z', 'z', 'L', 'l', 'H', 'h', 'V', 'v', 'C', 'c', 'S', 's', 'Q', 'q', 'T', 't', 'A', 'a' -> return true
        }
        return false
    }

    private fun is_number_start(c: Char): Boolean {
        return (c >= '0' && c <= '9') || (c == '.') || (c == '-') || (c == '+')
    }

    private fun is_absolute(c: Char): Boolean {
        return Character.isUpperCase(c)
    }

    // By the SVG spec 'large-arc' and 'sweep' must contain only one char
    // and can be written without any separators, e.g.: 10 20 30 01 10 20.
    private fun parse_flag(): Boolean {
        skip_spaces()
        val c: Char = s!!.get(i)
        when (c) {
            '0', '1' -> {
                i += 1
                if (i < l && s!!.get(i) == ',') {
                    i += 1
                }
                skip_spaces()
            }

            else -> throw Error(String.format("Unexpected flag '%c' (i=%d, s=%s)", c, i, s))
        }
        return c == '1'
    }

    private fun parse_list_number(): Float {
        if (i == l) {
            throw Error(String.format("Unexpected end (s=%s)", s))
        }
        val n: Float = parse_number()
        skip_spaces()
        parse_list_separator()
        return n
    }

    private fun parse_number(): Float {
        // Strip off leading whitespaces.
        skip_spaces()
        if (i == l) {
            throw Error(String.format("Unexpected end (s=%s)", s))
        }
        val start: Int = i
        var c: Char = s!!.get(i)

        // Consume sign.
        if (c == '-' || c == '+') {
            i += 1
            c = s!!.get(i)
        }

        // Consume integer.
        if (c >= '0' && c <= '9') {
            skip_digits()
            if (i < l) {
                c = s!!.get(i)
            }
        } else if (c != '.') {
            throw Error(String.format("Invalid number formating character '%c' (i=%d, s=%s)", c, i, s))
        }

        // Consume fraction.
        if (c == '.') {
            i += 1
            skip_digits()
            if (i < l) {
                c = s!!.get(i)
            }
        }
        if ((c == 'e' || c == 'E') && i + 1 < l) {
            val c2: Char = s!!.get(i + 1)
            // Check for `em`/`ex`.
            if (c2 != 'm' && c2 != 'x') {
                i += 1
                c = s!!.get(i)
                if (c == '+' || c == '-') {
                    i += 1
                    skip_digits()
                } else if (c >= '0' && c <= '9') {
                    skip_digits()
                } else {
                    throw Error(String.format("Invalid number formating character '%c' (i=%d, s=%s)", c, i, s))
                }
            }
        }
        val num: String = s!!.substring(start, i)
        val n: Float = num.toFloat()

        // inf, nan, etc. are an error.
        if (java.lang.Float.isInfinite(n) || java.lang.Float.isNaN(n)) {
            throw Error(String.format("Invalid number '%s' (start=%d, i=%d, s=%s)", num, start, i, s))
        }
        return n
    }

    private fun parse_list_separator() {
        if (i < l && s!!.get(i) == ',') {
            i += 1
        }
    }

    private fun skip_digits() {
        while (i < l && Character.isDigit(s!!.get(i))) i++
    }
}
