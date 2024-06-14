package com.horcrux.svg

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.view.View
import com.horcrux.svg.TextProperties.TextAnchor
import com.horcrux.svg.TextProperties.TextPathSide
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// TODO implement https://www.w3.org/TR/SVG2/text.html#TextLayoutAlgorithm
internal class TextLayoutAlgorithm() {
    internal inner class CharacterInformation(var index: Int, var character: Char) {
        var x: Double = 0.0
        var y: Double = 0.0
        var advance: Double = 0.0
        var rotate: Double = 0.0
        var element: TextView? = null
        var hidden: Boolean = false
        var middle: Boolean = false
        var resolved: Boolean = false
        var xSpecified: Boolean = false
        var ySpecified: Boolean = false
        var addressable: Boolean = true
        var anchoredChunk: Boolean = false
        var rotateSpecified: Boolean = false
        var firstCharacterInResolvedDescendant: Boolean = false
    }

    internal inner class LayoutInput() {
        var text: TextView? = null
        var horizontal: Boolean = false
    }

    private fun getSubTreeTypographicCharacterPositions(
            inTextPath: ArrayList<TextPathView?>,
            subtree: ArrayList<TextView>,
            line: StringBuilder,
            node: View?,
            textPath: TextPathView?) {
        var textPath: TextPathView? = textPath
        if (node is TSpanView) {
            val tSpanView: TSpanView = node
            val content: String? = tSpanView.mContent
            if (content == null) {
                for (i in 0 until tSpanView.getChildCount()) {
                    getSubTreeTypographicCharacterPositions(
                            inTextPath, subtree, line, tSpanView.getChildAt(i), textPath)
                }
            } else {
                for (i in 0 until content.length) {
                    subtree.add(tSpanView)
                    inTextPath.add(textPath)
                }
                line.append(content)
            }
        } else {
            textPath = if (node is TextPathView) node else textPath
            for (i in 0 until textPath!!.getChildCount()) {
                getSubTreeTypographicCharacterPositions(
                        inTextPath, subtree, line, textPath.getChildAt(i), textPath)
            }
        }
    }

    fun layoutText(layoutInput: LayoutInput): Array<CharacterInformation?> {
        /*
          Setup

          Let root be the result of generating
          typographic character positions for the
          ‘text’ element and its subtree, laid out as if it
          were an absolutely positioned element.

            This will be a single line of text unless the
            white-space property causes line breaks.
    */
        val text: TextView? = layoutInput.text
        val line: StringBuilder = StringBuilder()
        val subtree: ArrayList<TextView> = ArrayList()
        val inTextPath: ArrayList<TextPathView?> = ArrayList()
        getSubTreeTypographicCharacterPositions(inTextPath, subtree, line, text, null)
        val root: CharArray = line.toString().toCharArray()
        /*
          Let count be the number of DOM characters
          within the ‘text’ element's subtree.
    */
        val count: Int = root.size
        /*

          Let result be an array of length count
          whose entries contain the per-character information described
          above.  Each entry is initialized as follows:

            its global index number equal to its position in the array,
            its "x" coordinate set to "unspecified",
            its "y" coordinate set to "unspecified",
            its "rotate" coordinate set to "unspecified",
            its "hidden" flag is false,
            its "addressable" flag is true,
            its "middle" flag is false,
            its "anchored chunk" flag is false.
    */
        val result: Array<CharacterInformation?> = arrayOfNulls(count)
        for (i in 0 until count) {
            result[i] = CharacterInformation(i, root[i])
        }
        /*
          If result is empty, then return result.
    */if (count == 0) {
            return result
        }
        /*

          Let CSS_positions be an array of length
          count whose entries will be filled with the
          x and y positions of the corresponding
          typographic character in root. The array
          entries are initialized to (0, 0).
    */
        val CSS_positions: Array<PointF?> = arrayOfNulls(count)
        for (i in 0 until count) {
            CSS_positions[i] = PointF(0f, 0f)
        }
        /*
          Let "horizontal" be a flag, true if the writing mode of ‘text’
          is horizontal, false otherwise.
    */
        val horizontal: Boolean = true
        /*
          Set flags and assign initial positions

        For each array element with index i in
        result:
    */for (i in 0 until count) {
            /*
            TODO Set addressable to false if the character at index i was:

                part of the text content of a non-rendered element

                discarded during layout due to being a
                collapsed
              white space character, a soft hyphen character, or a
                bidi control character; or


                discarded during layout due to being a
                collapsed
              segment break; or


                trimmed
                from the start or end of a line.

                Since there is collapsible white space not addressable by glyph
                positioning attributes in the following ‘text’ element
                (with a standard font), the "B" glyph will be placed at x=300.

              <text x="100 200 300">
                A
                B
                </text>

                This is because the white space before the "A", and all but one white space
                character between the "A" and "B", is collapsed away or trimmed.

      */
            result.get(i)!!.addressable = true
            /*

            Set middle to true if the character at index i
            TODO is the second or later character that corresponds to a typographic character.
      */result.get(i)!!.middle = false
            /*

            TODO If the character at index i corresponds to a typographic character at the beginning of a line, then set the "anchored
            chunk" flag of result[i] to true.

              This ensures chunks shifted by text-anchor do not
              span multiple lines.
      */result.get(i)!!.anchoredChunk = i == 0
            /*

            If addressable is true and middle is false then
            set CSS_positions[i] to the position of the
            TODO corresponding typographic character as determined by the CSS
            renderer. Otherwise, if i > 0, then set
            CSS_positions[i] =
            CSS_positions[i − 1]

      */if (result.get(i)!!.addressable && !result.get(i)!!.middle) {
                CSS_positions.get(i)!!.set(0f, 0f)
            } else if (i > 0) {
                CSS_positions.get(i)!!.set((CSS_positions.get(i - 1))!!)
            }
        }
        /*

          Resolve character positioning

        Position adjustments (e.g values in a ‘x’ attribute)
        specified by a node apply to all characters in that node including
        characters in the node's descendants. Adjustments specified in
        descendant nodes, however, override adjustments from ancestor
        nodes. This section resolves which adjustments are to be applied to
        which characters. It also directly sets the rotate coordinate
        of result.

          Set up:

              Let resolve_x, resolve_y,
              resolve_dx, and resolve_dy be arrays of
              length count whose entries are all initialized
              to "unspecified".
    */
        val resolve_x: Array<String?> = arrayOfNulls(count)
        val resolve_y: Array<String?> = arrayOfNulls(count)
        val resolve_dx: Array<String?> = arrayOfNulls(count)
        val resolve_dy: Array<String?> = arrayOfNulls(count)
        /*

              Set "in_text_path" flag false.

            This flag will allow ‘y’ (‘x’)
            attribute values to be ignored for horizontal (vertical)
            text inside ‘textPath’ elements.
    */
        var in_text_path: Boolean = false

        /*
              Call the following procedure with the ‘text’ element node.

          Procedure: resolve character
              positioning:

            A recursive procedure that takes as input a node and
            whose steps are as follows:
    */ class CharacterPositioningResolver(
                private val result: Array<CharacterInformation?>,
                private val resolve_x: Array<String?>,
                private val resolve_y: Array<String?>,
                private val resolve_dx: Array<String?>,
                private val resolve_dy: Array<String?>) {
            private val global: Int = 0
            private val horizontal: Boolean = true
            private val in_text_path: Boolean = false
            private fun resolveCharacterPositioning(node: TextView) {
                /*
                  If node is a ‘text’ or ‘tspan’ node:
        */
                if (node.javaClass == TextView::class.java || node.javaClass == TSpanView::class.java) {
                    /*
                    Let index equal the "global index number" of the
                    first character in the node.
          */
                    val index: Int = global
                    /*
                    Let x, y, dx, dy
                    and rotate be the lists of values from the
                    TODO corresponding attributes on node, or empty
                    lists if the corresponding attribute was not specified
                    or was invalid.
          */
                    // https://www.w3.org/TR/SVG/text.html#TSpanElementXAttribute
                    val x: Array<String> = arrayOf()

                    // https://www.w3.org/TR/SVG/text.html#TSpanElementYAttribute
                    val y: Array<String> = arrayOf()

                    // Current <list-of-lengths> SVGLengthList
                    // https://www.w3.org/TR/SVG/types.html#DataTypeLengths

                    // https://www.w3.org/TR/SVG/text.html#TSpanElementDXAttribute
                    val dx: Array<String> = arrayOf()

                    // https://www.w3.org/TR/SVG/text.html#TSpanElementDYAttribute
                    val dy: Array<String> = arrayOf()

                    // Current <list-of-numbers> SVGLengthList
                    // https://www.w3.org/TR/SVG/types.html#DataTypeNumbers

                    // https://www.w3.org/TR/SVG/text.html#TSpanElementRotateAttribute
                    val rotate: DoubleArray = doubleArrayOf()
                    /*

                    If "in_text_path" flag is false:
                        Let new_chunk_count
                        = max(length of x, length of y).
          */
                    val new_chunk_count: Int
                    if (!in_text_path) {
                        new_chunk_count = max(x.size, y.size)
                        /*

                      Else:
            */
                    } else {
                        /*
                          If the "horizontal" flag is true:

                          Let new_chunk_count = length of x.
            */
                        if (horizontal) {
                            new_chunk_count = x.size
                            /*

                            Else:

                            Let new_chunk_count = length of y.
              */
                        } else {
                            new_chunk_count = y.size
                        }
                    }
                    /*

                    Let length be the number of DOM characters in the
                    subtree rooted at node.
          */
                    val content: String? = (node as TSpanView).mContent
                    val length: Int = if (content == null) 0 else content.length
                    /*
                    Let i = 0 and j = 0.

                      i is an index of addressable characters in the node;
                      j is an index of all characters in the node.
          */
                    var i: Int = 0
                    var j: Int = 0
                    /*
                    While j < length, do:
          */while (j < length) {
                        /*
                        This loop applies the ‘x’, ‘y’,
                        ‘dx’, ‘dy’ and ‘rotate’
                        attributes to the content inside node.
                          If the "addressable" flag of result[index +
                          j] is true, then:
            */
                        if (result.get(index + j)!!.addressable) {
                            /*
                            If i < TODO new_check_count, then (typo)
                            set the "anchored chunk" flag of
                            result[index + j] to
                            true. Else set the flag to false.

                              Setting the flag to false ensures that ‘x’
                              and ‘y’ attributes set in a ‘text’
                              element don't create anchored chunk in a ‘textPath’
                              element when they should not.
              */
                            result.get(index + j)!!.anchoredChunk = i < new_chunk_count
                            /*

                            If i < length of x,
                            then set resolve_x[index
                            + j] to x[i].
              */if (i < x.size) {
                                resolve_x[index + j] = x[i]
                            }
                            /*

                            If "in_text_path" flag is true and the "horizontal"
                            flag is false, unset
                            resolve_x[index].

                              The ‘x’ attribute is ignored for
                              vertical text on a path.
              */if (in_text_path && !horizontal) {
                                resolve_x[index] = ""
                            }
                            /*

                            If i < length of y,
                            then set resolve_y[index
                            + j] to y[i].
              */if (i < y.size) {
                                resolve_y[index + j] = y[i]
                            }
                            /*
                            If "in_text_path" flag is true and the "horizontal"
                            flag is true, unset
                            resolve_y[index].

                              The ‘y’ attribute is ignored for
                              horizontal text on a path.
              */if (in_text_path && horizontal) {
                                resolve_y[index] = ""
                            }
                            /*
                            If i < length of dx,
                            then set resolve_dx[index
                            + j] to TODO dy[i]. (typo)
              */if (i < dx.size) {
                                resolve_dx[index + j] = dx[i]
                            }
                            /*
                            If i < length of dy,
                            then set resolve_dy[index
                            + j] to dy[i].
              */if (i < dy.size) {
                                resolve_dy[index + j] = dy[i]
                            }
                            /*
                            If i < length of rotate,
                            then set the angle value of result[index
                            + j] to rotate[i].
                            Otherwise, if rotate is not empty, then
                            set result[index + j]
                            to result[index + j − 1].
              */if (i < rotate.size) {
                                result.get(index + j)!!.rotate = rotate.get(i)
                            } else if (rotate.size != 0) {
                                result.get(index + j)!!.rotate = result.get(index + j - 1)!!.rotate
                            }
                            /*
                            Set i = i + 1.
                            Set j = j + 1.
              */
                        }
                        i++
                        j++
                    }
                    /*
                    If node is a ‘textPath’ node:

                    Let index equal the global index number of the
                    first character in the node (including descendant nodes).
          */
                } else if (node.javaClass == TextPathView::class.java) {
                    val index: Int = global
                    /*
                    Set the "anchored chunk" flag of result[index]
                    to true.

                      A ‘textPath’ element always creates an anchored chunk.
          */result.get(index)!!.anchoredChunk = true
                    /*
                    Set in_text_path flag true.
          */in_text_path = true
                    /*
                    For each child node child of node:
                    Resolve glyph
                      positioning of child.
          */for (child in 0 until node.getChildCount()) {
                        resolveCharacterPositioning(node.getChildAt(child) as TextView)
                    }
                    /*
                    If node is a ‘textPath’ node:

                    Set "in_text_path" flag false.

          */if (node is TextPathView) {
                        in_text_path = false
                    }
                }
            }
        }

        val resolver: CharacterPositioningResolver = CharacterPositioningResolver(result, resolve_x, resolve_y, resolve_dx, resolve_dy)
        /*
          Adjust positions: dx, dy

        The ‘dx’ and ‘dy’ adjustments are applied
        before adjustments due to the ‘textLength’ attribute while
        the ‘x’, ‘y’ and ‘rotate’
        adjustments are applied after.

          Let shift be the cumulative x and
          y shifts due to ‘x’ and ‘y’
          attributes, initialized to (0,0).
    */
        val shift: PointF = PointF(0f, 0f)
        /*
          For each array element with index i in result:
    */for (i in 0 until count) {
            /*
                If resolve_x[i] is unspecified, set it to 0.
                If resolve_y[i] is unspecified, set it to 0.
      */
            if ((resolve_x.get(i) == "")) {
                resolve_x[i] = "0"
            }
            if ((resolve_y.get(i) == "")) {
                resolve_y[i] = "0"
            }
            /*
                Let shift.x = shift.x + resolve_x[i]
                and shift.y = shift.y + resolve_y[i].
      */shift.x = shift.x + resolve_x.get(i)!!.toFloat()
            shift.y = shift.y + resolve_y.get(i)!!.toFloat()
            /*
                Let result[i].x = CSS_positions[i].x + shift.x
                and result[i].y = CSS_positions[i].y + shift.y.
      */result.get(i)!!.x = (CSS_positions.get(i)!!.x + shift.x).toDouble()
            result.get(i)!!.y = (CSS_positions.get(i)!!.y + shift.y).toDouble()
        }
        /*
          TODO Apply ‘textLength’ attribute

          Set up:

              Define resolved descendant node as a
              descendant of node with a valid ‘textLength’
              attribute that is not itself a descendant node of a
              descendant node that has a valid ‘textLength’
              attribute.

              Call the following procedure with the ‘text’ element
              node.

          Procedure: resolve text length:

            A recursive procedure that takes as input
            a node and whose steps are as follows:
              For each child node child of node:

              Resolve text length of child.

                Child nodes are adjusted before parent nodes.
    */ class TextLengthResolver() {
            var global: Int = 0
            fun resolveTextLength(node: TextView?) {
                /*

                  If node is a ‘text’ or ‘tspan’ node
                  and if the node has a valid ‘textLength’ attribute value:
        */
                val nodeClass: Class<out TextView> = node!!.javaClass
                val validTextLength: Boolean = node.mTextLength != null
                if ((nodeClass == TSpanView::class.java) && validTextLength) {
                    /*
                    Let a = +∞ and b = −∞.
          */
                    var a: Double = Double.POSITIVE_INFINITY
                    var b: Double = Double.NEGATIVE_INFINITY
                    /*


                    Let i and j be the global
                    index of the first character and last characters
                    in node, respectively.
          */
                    val content: String? = (node as TSpanView?)!!.mContent
                    val i: Int = global
                    val j: Int = i + (if (content == null) 0 else content.length)
                    /*
                    For each index k in the range
                    [i, j] where the "addressable" flag
                    of result[k] is true:

                      This loop finds the left-(top-) most and
                      right-(bottom-) most extents of the typographic characters within the node and checks for
                      forced line breaks.
          */for (k in i..j) {
                        if (!result.get(i)!!.addressable) {
                            continue
                        }
                        when (result.get(i)!!.character) {
                            '\n', '\r' -> return
                        }
                        /*
                          Let pos = the x coordinate of the position
                          in result[k], if the "horizontal"
                          flag is true, and the y coordinate otherwise.
            */
                        val pos: Double = if (horizontal) result.get(k)!!.x else result.get(k)!!.y
                        /*
                          Let advance = the advance of
                          the typographic character corresponding to
                          character k. [NOTE: This advance will be
                          negative for RTL horizontal text.]
            */
                        val advance: Double = result.get(k)!!.advance
                        /*
                          Set a =
                          min(a, pos, pos
                          + advance).


                          Set b =
                          max(b, pos, pos
                          + advance).
            */a = min(a, min(pos, pos + advance))
                        b = max(b, max(pos, pos + advance))
                    }
                    /*

                    If a ≠ +∞ then:

          */if (a != Double.POSITIVE_INFINITY) {
                        /*

                          Find the distance delta = ‘textLength’
                          computed value − (b − a).
            */
                        val delta: Double = node.mTextLength!!.value - (b - a)
                        /*

                        User agents are required to shift the last
                        typographic character in the node by
                        delta, in the positive x direction
                        if the "horizontal" flag is true and if
                        direction is
                        lrt, in the
                        negative x direction if the "horizontal" flag
                        is true and direction is
                        rtl, or in the
                        positive y direction otherwise.  User agents
                        are free to adjust intermediate
                        typographic characters for optimal
                        typography. The next steps indicate one way to
                        adjust typographic characters when
                        the value of ‘lengthAdjust’ is
                        spacing.

                          Find n, the total number of
                          typographic characters in this node
                          TODO including any descendant nodes that are not resolved
                          descendant nodes or within a resolved descendant
                          node.
            */
                        var n: Int = 0
                        var resolvedDescendantNodes: Int = 0
                        for (c in 0 until node.getChildCount()) {
                            if ((node.getChildAt(c) as TextPathView).mTextLength == null) {
                                val ccontent: String? = (node as TSpanView?)!!.mContent
                                n += if (ccontent == null) 0 else ccontent.length
                            } else {
                                result.get(n)!!.firstCharacterInResolvedDescendant = true
                                resolvedDescendantNodes++
                            }
                        }
                        /*
                          Let n = n + number of
                          resolved descendant nodes − 1.
            */n += resolvedDescendantNodes - 1
                        /*
                        Each resolved descendant node is treated as if it
                        were a single
                        typographic character in this
                        context.

                          Find the per-character adjustment δ
                          = delta/n.

                          Let shift = 0.
            */
                        val perCharacterAdjustment: Double = delta / n
                        var shift: Double = 0.0
                        /*
                          For each index k in the range [i,j]:
            */for (k in i..j) {
                            /*
                            Add shift to the x coordinate of the
                            position in result[k], if the "horizontal"
                            flag is true, and to the y coordinate
                            otherwise.
              */
                            if (horizontal) {
                                result.get(k)!!.x += shift
                            } else {
                                result.get(k)!!.y += shift
                            }
                            /*
              If the "middle" flag for result[k]
              is not true and k is not a character in
              a resolved descendant node other than the first
              character then shift = shift
              + δ.
              */if ((!result.get(k)!!.middle
                                            && (!result.get(k)!!.resolved || result.get(k)!!.firstCharacterInResolvedDescendant))) {
                                shift += perCharacterAdjustment
                            }
                        }
                    }
                }
            }
        }

        val lengthResolver: TextLengthResolver = TextLengthResolver()
        lengthResolver.resolveTextLength(text)
        /*

          Adjust positions: x, y

        This loop applies ‘x’ and ‘y’ values,
        and ensures that text-anchor chunks do not start in
        the middle of a typographic character.

          Let shift be the current adjustment due to
          the ‘x’ and ‘y’ attributes,
          initialized to (0,0).

          Set index = 1.
    */shift.set(0f, 0f)
        var index: Int = 1
        /*
          While index < count:
    */while (index < count) {
            /*
                TODO If resolved_x[index] is set, then let (typo)
                shift.x =
                resolved_x[index] −
                result.x[index].
      */
            if (resolve_x.get(index) != null) {
                shift.x = (resolve_x.get(index)!!.toDouble() - result.get(index)!!.x).toFloat()
            }
            /*
                TODO If resolved_y[index] is set, then let (typo)
                shift.y =
                resolved_y[index] −
                result.y[index].
      */if (resolve_y.get(index) != null) {
                shift.y = (resolve_y.get(index)!!.toDouble() - result.get(index)!!.y).toFloat()
            }
            /*
                Let result.x[index] =
                  result.x[index] + shift.x
                and result.y[index] =
              result.y[index] + shift.y.
      */result.get(index)!!.x += shift.x.toDouble()
            result.get(index)!!.y += shift.y.toDouble()
            /*
                If the "middle" and "anchored chunk" flags
                of result[index] are both true, then:
      */if (result.get(index)!!.middle && result.get(index)!!.anchoredChunk) {
                /*
                  Set the "anchored chunk" flag
                  of result[index] to false.
        */
                result.get(index)!!.anchoredChunk = false
            }
            /*

                If index + 1 < count, then set
                the "anchored chunk" flag
                of result[index + 1] to true.
      */if (index + 1 < count) {
                result.get(index + 1)!!.anchoredChunk = true
            }
            /*
                Set index to index + 1.
      */index++
        }
        /*

          Apply anchoring

         TODO For each slice result[i..j]
          (inclusive of both i and j), where:

              the "anchored chunk" flag of result[i]
              is true,

              the "anchored chunk" flags
              of result[k] where i
              < k ≤ j are false, and

              j = count − 1 or the "anchored
              chunk" flag of result[j + 1] is
              true;
          do:

            This loops over each anchored chunk.

              Let a = +∞ and b = −∞.

              For each index k in the range
              [i, j] where the "addressable" flag
              of result[k] is true:

            This loop finds the left-(top-) most and
            right-(bottom-) most extents of the typographic character within the anchored chunk.
    */
        var i: Int = 0
        var a: Double = Double.POSITIVE_INFINITY
        var b: Double = Double.NEGATIVE_INFINITY
        var prevA: Double = Double.POSITIVE_INFINITY
        var prevB: Double = Double.NEGATIVE_INFINITY
        for (k in 0 until count) {
            if (!result.get(k)!!.addressable) {
                continue
            }
            if (result.get(k)!!.anchoredChunk) {
                prevA = a
                prevB = b
                a = Double.POSITIVE_INFINITY
                b = Double.NEGATIVE_INFINITY
            }
            /*
                Let pos = the x coordinate of the position
                in result[k], if the "horizontal" flag
                is true, and the y coordinate otherwise.

                Let advance = the advance of
                the typographic character corresponding to
                character k. [NOTE: This advance will be
                negative for RTL horizontal text.]

                Set a =
                min(a, pos, pos
                + advance).

                Set b =
                max(b, pos, pos
                + advance).
      */
            val pos: Double = if (horizontal) result.get(k)!!.x else result.get(k)!!.y
            val advance: Double = result.get(k)!!.advance
            a = min(a, min(pos, pos + advance))
            b = max(b, max(pos, pos + advance))
            /*
                If a ≠ +∞, then:

              Here we perform the text anchoring.

                Let shift be the x coordinate of
                result[i], if the "horizontal" flag
                is true, and the y coordinate otherwise.

                TODO Adjust shift based on the value of text-anchor
                TODO and direction of the element the character at
                index i is in:

                  (start, ltr) or (end, rtl)
                  Set shift = shift − a.
                  (start, rtl) or (end, ltr)
                  Set shift = shift − b.
                  (middle, ltr) or (middle, rtl)
                  Set shift = shift − (a + b) / 2.
      */if ((((k > 0) && result.get(k)!!.anchoredChunk && (prevA != Double.POSITIVE_INFINITY))
                            || k == count - 1)) {
                val anchor: TextAnchor = TextAnchor.start
                val direction: TextProperties.Direction = TextProperties.Direction.ltr
                if (k == count - 1) {
                    prevA = a
                    prevB = b
                }
                var anchorShift: Double = if (horizontal) result.get(i)!!.x else result.get(i)!!.y
                when (anchor) {
                    TextAnchor.start -> if (direction == TextProperties.Direction.ltr) {
                        anchorShift = anchorShift - prevA
                    } else {
                        anchorShift = anchorShift - prevB
                    }

                    TextAnchor.middle -> if (direction == TextProperties.Direction.ltr) {
                        anchorShift = anchorShift - (prevA + prevB) / 2
                    } else {
                        anchorShift = anchorShift - (prevA + prevB) / 2
                    }

                    TextAnchor.end -> if (direction == TextProperties.Direction.ltr) {
                        anchorShift = anchorShift - prevB
                    } else {
                        anchorShift = anchorShift - prevA
                    }
                }
                /*
                  For each index k in the range [i, j]:

                      Add shift to the x coordinate of the position
                      in result[k], if the "horizontal"
                      flag is true, and to the y coordinate otherwise.
        */
                val j: Int = if (k == count - 1) k else k - 1
                for (r in i..j) {
                    if (horizontal) {
                        result.get(r)!!.x += anchorShift
                    } else {
                        result.get(r)!!.y += anchorShift
                    }
                }
                i = k
            }
        }
        /*

          Position on path

          Set index = 0.

          Set the "in path" flag to false.

          Set the "after path" flag to false.

          Let path_end be an offset for characters that follow
          a ‘textPath’ element. Set path_end to (0,0).

          While index < count:
    */index = 0
        var inPath: Boolean = false
        var afterPath: Boolean = false
        val path_end: PointF = PointF(0f, 0f)
        var textPath: Path? = null
        val pm: PathMeasure = PathMeasure()
        while (index < count) {
            /*
                If the character at index i is within a
                ‘textPath’ element and corresponds to a typographic character, then:

                Set "in path" flag to true.
      */
            val textPathView: TextPathView? = inTextPath.get(index)
            if (textPathView != null && result.get(index)!!.addressable) {
                textPath = textPathView.getTextPath(null, null)
                inPath = true
                /*

                  If the "middle" flag of
                  result[index] is false, then:
        */if (!result.get(index)!!.middle) {
                    /*
                      Here we apply ‘textPath’ positioning.

                        Let path be the equivalent path of
                        the basic shape element referenced by
                        the ‘textPath’ element, or an empty path if
                        the reference is invalid.

                        If the ‘side’ attribute of
                        the ‘textPath’ element is
                        'right', then
                       TODO reverse path.
          */
                    val path: Path? = textPath
                    if (textPathView.side == TextPathSide.right) {
                    }

                    /*
                        Let length be the length
                        of path.
          */pm.setPath(path, false)
                    val length: Double = pm.getLength().toDouble()
                    /*
                        Let offset be the value of the
                        ‘textPath’ element's
                        ‘startOffset’ attribute, adjusted
                        due to any ‘pathLength’ attribute on the
                        referenced element (if the referenced element is
                        a ‘path’ element).
          */
                    val offset: Double = textPathView.startOffset!!.value
                    /*
                        Let advance = the advance of
                        the typographic character corresponding
                        to character TODO k. (typo) [NOTE: This advance will
                        be negative for RTL horizontal text.]
          */
                    val advance: Double = result.get(index)!!.advance
                    /*
                        Let (x, y)
                        and angle be the position and angle
                        in result[index].
          */
                    val x: Double = result.get(index)!!.x
                    val y: Double = result.get(index)!!.y
                    val angle: Double = result.get(index)!!.rotate
                    /*

                        Let mid be a coordinate value depending
                        on the value of the "horizontal" flag:

                      true
                      mid is x + advance / 2
                        + offset
                      false
                      mid is y + advance / 2
                        + offset
          */
                    var mid: Double = (if (horizontal) x else y) + (advance / 2) + offset
                    /*

                      The user agent is free to make any additional adjustments to
                      mid necessary to ensure high quality typesetting
                     TODO due to a ‘spacing’ value of
                      'auto' or a
                      ‘method’ value of
                      'stretch'.

                        If path is not a closed subpath and
                        mid < 0 or mid > length,
                        set the "hidden" flag of result[index] to true.
          */if (!pm.isClosed() && (mid < 0 || mid > length)) {
                        result.get(index)!!.hidden = true
                    }
                    /*
                        If path is a closed subpath depending on
                        the values of text-anchor and direction of
                        the element the character at index is in:
          */if (pm.isClosed()) {
                        /*
                        This implements the special wrapping criteria for single
                        closed subpaths.

                        (start, ltr) or (end, rtl)

                          If mid−offset < 0
                          or mid−offset > length,
                          set the "hidden" flag of result[index] to true.

                        (middle, ltr) or (middle, rtl)

                          If
                          If mid−offset < −length/2
                          or mid−offset >  length/2,
                          set the "hidden" flag of result[index] to true.

                        (start, rtl) or (end, ltr)

                          If mid−offset < −length
                          or mid−offset > 0,
                          set the "hidden" flag of result[index] to true.
            */
                        val anchor: TextAnchor = TextAnchor.start
                        val direction: TextProperties.Direction = TextProperties.Direction.ltr
                        val anchorShift: Double = if (horizontal) result.get(i)!!.x else result.get(i)!!.y
                        when (anchor) {
                            TextAnchor.start -> if (direction == TextProperties.Direction.ltr) {
                                if (mid < 0 || mid > length) {
                                    result.get(index)!!.hidden = true
                                }
                            } else {
                                if (mid < -length || mid > 0) {
                                    result.get(index)!!.hidden = true
                                }
                            }

                            TextAnchor.middle -> if (mid < -length / 2 || mid > length / 2) {
                                result.get(index)!!.hidden = true
                            }

                            TextAnchor.end -> if (direction == TextProperties.Direction.ltr) {
                                if (mid < -length || mid > 0) {
                                    result.get(index)!!.hidden = true
                                }
                            } else {
                                if (mid < 0 || mid > length) {
                                    result.get(index)!!.hidden = true
                                }
                            }
                        }
                    }
                    /*
                      Set mid = mid mod length.
          */mid %= length
                    /*
                      If the hidden flag is false:
          */if (!result.get(index)!!.hidden) {
                        /*
                          Let point be the position and
                          t be the unit vector tangent to
                          the point mid distance
                          along path.
            */
                        val point: FloatArray = FloatArray(2)
                        val t: FloatArray = FloatArray(2)
                        pm.getPosTan(mid.toFloat(), point, t)
                        val tau: Double = 2 * Math.PI
                        val radToDeg: Double = 360 / tau
                        val r: Double = atan2(t.get(1).toDouble(), t.get(0).toDouble()) * radToDeg
                        /*
                          If the "horizontal" flag is
            */if (horizontal) {
                            /*
                              true

                                Let n be the normal unit vector
                                pointing in the direction t + 90°.
              */
                            val normAngle: Double = r + 90
                            val n: DoubleArray = doubleArrayOf(cos(normAngle), sin(normAngle))
                            /*
                                Let o be the horizontal distance from the
                                TODO vertical center line of the glyph to the alignment point.
              */
                            val o: Double = 0.0
                            /*
                                Then set the position in
                                result[index] to
                                point -
                                o×t +
                                y×n.

                                Let r be the angle from
                                the positive x-axis to the tangent.

                                Set the angle value
                                in result[index]
                                to angle + r.
              */result.get(index)!!.rotate += r
                        } else {
                            /*
                              false

                                Let n be the normal unit vector
                                pointing in the direction t - 90°.
              */
                            val normAngle: Double = r - 90
                            val n: DoubleArray = doubleArrayOf(cos(normAngle), sin(normAngle))
                            /*
                                Let o be the vertical distance from the
                                TODO horizontal center line of the glyph to the alignment point.
              */
                            val o: Double = 0.0
                            /*

                                Then set the position in
                                result[index] to
                                point -
                                o×t +
                                x×n.

                                Let r be the angle from
                                the positive y-axis to the tangent.

                                Set the angle value
                                in result[index]
                                to angle + r.
              */result.get(index)!!.rotate += r
                        }
                    }
                    /*

                    Otherwise, the "middle" flag
                    of result[index] is true:

                        Set the position and angle values
                        of result[index] to those
                        in result[index − 1].
          */
                } else {
                    result.get(index)!!.x = result.get(index - 1)!!.x
                    result.get(index)!!.y = result.get(index - 1)!!.y
                    result.get(index)!!.rotate = result.get(index - 1)!!.rotate
                }
            }
            /*
                If the character at index i is not within a
                ‘textPath’ element and corresponds to a typographic character, then:

              This sets the starting point for rendering any characters that
              occur after a ‘textPath’ element to the end of the path.
      */if (textPathView == null && result.get(index)!!.addressable) {
                /*
                If the "in path" flag is true:

                      Set the "in path" flag to false.

                      Set the "after path" flag to true.

                      Set path_end equal to the end point of the path
                      referenced by ‘textPath’ − the position of
                      result[index].
        */
                if (inPath) {
                    inPath = false
                    afterPath = true
                    pm.setPath(textPath, false)
                    val pos: FloatArray = FloatArray(2)
                    pm.getPosTan(pm.getLength(), pos, null)
                    path_end.set(pos.get(0), pos.get(1))
                }
                /*

                  If the "after path" is true.

                      If anchored chunk of
                      result[index] is true, set the
                      "after path" flag to false.

                      Else,
                      let result.x[index] =
                      result.x[index] + path_end.x
                      and result.y[index] =
                      result.y[index] + path_end.y.
        */if (afterPath) {
                    if (result.get(index)!!.anchoredChunk) {
                        afterPath = false
                    } else {
                        result.get(index)!!.x += path_end.x.toDouble()
                        result.get(index)!!.y += path_end.y.toDouble()
                    }
                }
            }
            /*

                Set index = index + 1.
      */index++
        }
        /*
          Return result
    */return result
    }
}
