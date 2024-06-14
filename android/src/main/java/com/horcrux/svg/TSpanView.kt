/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.ReactContext
import com.facebook.react.views.text.ReactFontManager
import com.horcrux.svg.TextProperties.AlignmentBaseline
import com.horcrux.svg.TextProperties.FontVariantLigatures
import com.horcrux.svg.TextProperties.TextAnchor
import com.horcrux.svg.TextProperties.TextLengthAdjust
import com.horcrux.svg.TextProperties.TextPathMidLine
import com.horcrux.svg.TextProperties.TextPathSide
import java.text.Bidi
import kotlin.math.atan2

@SuppressLint("ViewConstructor")
internal class TSpanView(reactContext: ReactContext) : TextView(reactContext) {
    private var mCachedPath: Path? = null
    var mContent: String? = null
    private var textPath: TextPathView? = null
    private val emoji = ArrayList<String>()
    private val emojiTransforms = ArrayList<Matrix>()
    private val assets: AssetManager
    fun setContent(content: String?) {
        mContent = content
        invalidate()
    }

    override fun invalidate() {
        mCachedPath = null
        super.invalidate()
    }

    public override fun clearCache() {
        mCachedPath = null
        super.clearCache()
    }

    public override fun draw(canvas: Canvas, paint: Paint, opacity: Float) {
        if (mContent != null) {
            if (mInlineSize != null && mInlineSize!!.value != 0.0) {
                if (setupFillPaint(paint, opacity * fillOpacity)) {
                    drawWrappedText(canvas, paint)
                }
                if (setupStrokePaint(paint, opacity * strokeOpacity)) {
                    drawWrappedText(canvas, paint)
                }
            } else {
                val numEmoji = emoji.size
                if (numEmoji > 0) {
                    val gc = textRootGlyphContext
                    val font = gc!!.font
                    applyTextPropertiesToPaint(paint, font)
                    for (i in 0 until numEmoji) {
                        val current = emoji[i]
                        val mid = emojiTransforms[i]
                        canvas.save()
                        canvas.concat(mid)
                        canvas.drawText(current, 0f, 0f, paint)
                        canvas.restore()
                    }
                }
                drawPath(canvas, paint, opacity)
            }
        } else {
            clip(canvas, paint)
            drawGroup(canvas, paint, opacity)
        }
    }

    private fun drawWrappedText(canvas: Canvas, paint: Paint) {
        val gc = textRootGlyphContext
        pushGlyphContext()
        val font = gc!!.font
        val tp = TextPaint(paint)
        applyTextPropertiesToPaint(tp, font)
        applySpacingAndFeatures(tp, font)
        val fontSize = gc.fontSize
        val align: Layout.Alignment
        align = when (font!!.textAnchor) {
            TextAnchor.start -> Layout.Alignment.ALIGN_NORMAL
            TextAnchor.middle -> Layout.Alignment.ALIGN_CENTER
            TextAnchor.end -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val includeFontPadding = true
        val text = SpannableString(mContent)
        val width = PropHelper.fromRelative(mInlineSize, canvas.width.toDouble(), 0.0, mScale.toDouble(), fontSize)
        val layout = getStaticLayout(tp, align, includeFontPadding, text, width.toInt())
        val lineAscent = layout.getLineAscent(0)
        val dx = gc!!.nextX(0.0).toFloat()
        val dy = (gc.nextY() + lineAscent).toFloat()
        popGlyphContext()
        canvas.save()
        canvas.translate(dx, dy)
        layout.draw(canvas)
        canvas.restore()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("deprecation")
    private fun getStaticLayout(
            tp: TextPaint,
            align: Layout.Alignment,
            includeFontPadding: Boolean,
            text: SpannableString,
            width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            StaticLayout(text, tp, width, align, 1f, 0f, includeFontPadding)
        } else {
            StaticLayout.Builder.obtain(text, 0, text.length, tp, width)
                    .setAlignment(align)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(includeFontPadding)
                    .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                    .build()
        }
    }

    public override fun getPath(canvas: Canvas?, paint: Paint?): Path? {
        if (mCachedPath != null) {
            return mCachedPath
        }
        if (mContent == null) {
            mCachedPath = getGroupPath(canvas, paint)
            return mCachedPath
        }
        setupTextPath()
        pushGlyphContext()
        mCachedPath = getLinePath(visualToLogical(mContent), paint, canvas)
        popGlyphContext()
        return mCachedPath
    }

    public override fun getSubtreeTextChunksTotalAdvance(paint: Paint?): Double {
        if (!java.lang.Double.isNaN(cachedAdvance)) {
            return cachedAdvance
        }
        var advance = 0.0
        if (mContent == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is TextView) {
                    advance += child.getSubtreeTextChunksTotalAdvance(paint)
                }
            }
            cachedAdvance = advance
            return advance
        }
        val line: String = mContent as String
        val length = line.length
        if (length == 0) {
            cachedAdvance = 0.0
            return advance
        }
        val gc = textRootGlyphContext
        val font = gc!!.font
        applyTextPropertiesToPaint(paint, font)
        applySpacingAndFeatures(paint, font)
        cachedAdvance = paint!!.measureText(line).toDouble()
        return cachedAdvance
    }

    init {
        assets = mContext!!.resources.assets
    }

    private fun applySpacingAndFeatures(paint: Paint?, font: FontData?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val letterSpacing = font!!.letterSpacing
            paint!!.letterSpacing = (letterSpacing / (font.fontSize * mScale)).toFloat()
            val allowOptionalLigatures = letterSpacing == 0.0 && font.fontVariantLigatures == FontVariantLigatures.normal
            if (allowOptionalLigatures) {
                paint.setFontFeatureSettings(
                        defaultFeatures + additionalLigatures + font.fontFeatureSettings)
            } else {
                paint.setFontFeatureSettings(
                        defaultFeatures + disableDiscretionaryLigatures + font.fontFeatureSettings)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                paint.setFontVariationSettings(
                        fontWeightTag + font.absoluteFontWeight + font.fontVariationSettings)
            }
        }
    }

    private fun getLinePath(line: String?, paint: Paint?, canvas: Canvas?): Path {
        val length = line!!.length
        val path = Path()
        emoji.clear()
        emojiTransforms.clear()
        if (length == 0) {
            return path
        }
        var pathLength = 0.0
        var pm: PathMeasure? = null
        var isClosed = false
        val hasTextPath = textPath != null
        if (hasTextPath) {
            pm = PathMeasure(textPath!!.getTextPath(canvas, paint), false)
            pathLength = pm.length.toDouble()
            isClosed = pm.isClosed
            if (pathLength == 0.0) {
                return path
            }
        }
        val gc = textRootGlyphContext!!
        val font = gc.font
        applyTextPropertiesToPaint(paint, font)
        val bag = GlyphPathBag(paint)
        val ligature = BooleanArray(length)
        val chars = line.toCharArray()

        /*
     *
     * Three properties affect the space between characters and words:
     *
     * ‘kerning’ indicates whether the user agent should adjust inter-glyph spacing
     * based on kerning tables that are included in the relevant font
     * (i.e., enable auto-kerning) or instead disable auto-kerning
     * and instead set inter-character spacing to a specific length (typically, zero).
     *
     * ‘letter-spacing’ indicates an amount of space that is to be added between text
     * characters supplemental to any spacing due to the ‘kerning’ property.
     *
     * ‘word-spacing’ indicates the spacing behavior between words.
     *
     *  Letter-spacing is applied after bidi reordering and is in addition to any word-spacing.
     *  Depending on the justification rules in effect, user agents may further increase
     *  or decrease the space between typographic character units in order to justify text.
     *
     * */
        var kerning = font!!.kerning
        val wordSpacing = font!!.wordSpacing
        var letterSpacing = font!!.letterSpacing
        val autoKerning = !font!!.manualKerning

        /*
    11.1.2. Fonts and glyphs

    A font consists of a collection of glyphs together with other information (collectively,
    the font tables) necessary to use those glyphs to present characters on some visual medium.

    The combination of the collection of glyphs and the font tables is called the font data.

    A font may supply substitution and positioning tables that can be used by a formatter
    (text shaper) to re-order, combine and position a sequence of glyphs to form one or more
    composite glyphs.

    The combining may be as simple as a ligature, or as complex as an indic syllable which
    combines, usually with some re-ordering, multiple consonants and vowel glyphs.

    The tables may be language dependent, allowing the use of language appropriate letter forms.

    When a glyph, simple or composite, represents an indivisible unit for typesetting purposes,
    it is know as a typographic character.

    Ligatures are an important feature of advance text layout.

    Some ligatures are discretionary while others (e.g. in Arabic) are required.

    The following explicit rules apply to ligature formation:

    Ligature formation should not be enabled when characters are in different DOM text nodes;
    thus, characters separated by markup should not use ligatures.

    Ligature formation should not be enabled when characters are in different text chunks.

    Discretionary ligatures should not be used when the spacing between two characters is not
    the same as the default space (e.g. when letter-spacing has a non-default value,
    or text-align has a value of justify and text-justify has a value of distribute).
    (See CSS Text Module Level 3, ([css-text-3]).

    SVG attributes such as ‘dx’, ‘textLength’, and ‘spacing’ (in ‘textPath’) that may reposition
    typographic characters do not break discretionary ligatures.

    If discretionary ligatures are not desired
    they can be turned off by using the font-variant-ligatures property.

    / *
        When the effective letter-spacing between two characters is not zero
        (due to either justification or non-zero computed ‘letter-spacing’),
        user agents should not apply optional ligatures.
        https://www.w3.org/TR/css-text-3/#letter-spacing-property
    */
        val allowOptionalLigatures = letterSpacing == 0.0 && font!!.fontVariantLigatures == FontVariantLigatures.normal

        /*
        For OpenType fonts, discretionary ligatures include those enabled by
        the liga, clig, dlig, hlig, and cala features;
        required ligatures are found in the rlig feature.
        https://svgwg.org/svg2-draft/text.html#FontsGlyphs

        http://dev.w3.org/csswg/css-fonts/#propdef-font-feature-settings

        https://www.microsoft.com/typography/otspec/featurelist.htm
        https://www.microsoft.com/typography/otspec/featuretags.htm
        https://www.microsoft.com/typography/otspec/features_pt.htm
        https://www.microsoft.com/typography/otfntdev/arabicot/features.aspx
        http://unifraktur.sourceforge.net/testcases/enable_opentype_features/
        https://en.wikipedia.org/wiki/List_of_typographic_features
        http://ilovetypography.com/OpenType/opentype-features.html
        https://www.typotheque.com/articles/opentype_features_in_css
        https://practice.typekit.com/lesson/caring-about-opentype-features/
        http://stateofwebtype.com/

        6.12. Low-level font feature settings control: the font-feature-settings property

        Name:	font-feature-settings
        Value:	normal | <feature-tag-value> #
        Initial:	normal
        Applies to:	all elements
        Inherited:	yes
        Percentages:	N/A
        Media:	visual
        Computed value:	as specified
        Animatable:	no

        https://drafts.csswg.org/css-fonts-3/#default-features

        7.1. Default features

        For OpenType fonts, user agents must enable the default features defined in the OpenType
        documentation for a given script and writing mode.

        Required ligatures, common ligatures and contextual forms must be enabled by default
        (OpenType features: rlig, liga, clig, calt),
        along with localized forms (OpenType feature: locl),
        and features required for proper display of composed characters and marks
        (OpenType features: ccmp, mark, mkmk).

        These features must always be enabled, even when the value of the ‘font-variant’ and
        ‘font-feature-settings’ properties is ‘normal’.

        Individual features are only disabled when explicitly overridden by the author,
        as when ‘font-variant-ligatures’ is set to ‘no-common-ligatures’.

        TODO For handling complex scripts such as Arabic, Mongolian or Devanagari additional features
        are required.

        TODO For upright text within vertical text runs,
        vertical alternates (OpenType feature: vert) must be enabled.
    */if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // String arabic = "'isol', 'fina', 'medi', 'init', 'rclt', 'mset', 'curs', ";
            if (allowOptionalLigatures) {
                paint!!.setFontFeatureSettings(
                        defaultFeatures + additionalLigatures + font!!.fontFeatureSettings)
            } else {
                paint!!.setFontFeatureSettings(
                        defaultFeatures + disableDiscretionaryLigatures + font!!.fontFeatureSettings)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                paint.setFontVariationSettings(
                        fontWeightTag + font!!.absoluteFontWeight + font!!.fontVariationSettings)
            }
        }
        // OpenType.js font data
        val fontData = font!!.fontData
        val advances = FloatArray(length)
        paint!!.getTextWidths(line, advances)

        /*
    This would give both advances and textMeasure in one call / looping over the text
    double textMeasure = paint.getTextRunAdvances(line, 0, length, 0, length, true, advances, 0);
    */
        /*
       Determine the startpoint-on-the-path for the first glyph using attribute ‘startOffset’
       and property text-anchor.

       For text-anchor:start, startpoint-on-the-path is the point
       on the path which represents the point on the path which is ‘startOffset’ distance
       along the path from the start of the path, calculated using the user agent's distance
       along the path algorithm.

       For text-anchor:middle, startpoint-on-the-path is the point
       on the path which represents the point on the path which is [ ‘startOffset’ minus half
       of the total advance values for all of the glyphs in the ‘textPath’ element ] distance
       along the path from the start of the path, calculated using the user agent's distance
       along the path algorithm.

       For text-anchor:end, startpoint-on-the-path is the point on
       the path which represents the point on the path which is [ ‘startOffset’ minus the
       total advance values for all of the glyphs in the ‘textPath’ element ].

       Before rendering the first glyph, the horizontal component of the startpoint-on-the-path
       is adjusted to take into account various horizontal alignment text properties and
       attributes, such as a ‘dx’ attribute value on a ‘tspan’ element.
    */
        val textAnchor = font.textAnchor
        val anchorRoot = textAnchorRoot
        val textMeasure = anchorRoot.getSubtreeTextChunksTotalAdvance(paint)
        var offset = getTextAnchorOffset(textAnchor, textMeasure)
        var side = 1
        var startOfRendering = 0.0
        var endOfRendering = pathLength
        val fontSize = gc.fontSize
        var sharpMidLine = false
        if (hasTextPath) {
            sharpMidLine = textPath!!.midLine == TextPathMidLine.sharp
            /*
          Name
          side
          Value
          left | right
          initial value
          left
          Animatable
          yes

          Determines the side of the path the text is placed on
          (relative to the path direction).

          Specifying a value of right effectively reverses the path.

          Added in SVG 2 to allow text either inside or outside closed subpaths
          and basic shapes (e.g. rectangles, circles, and ellipses).

          Adding 'side' was resolved at the Sydney (2015) meeting.
      */side = if (textPath!!.side == TextPathSide.right) -1 else 1
            /*
          Name
          startOffset
          Value
          <length> | <percentage> | <number>
          initial value
          0
          Animatable
          yes

          An offset from the start of the path for the initial current text position,
          calculated using the user agent's distance along the path algorithm,
          after converting the path to the ‘textPath’ element's coordinate system.

          If a <length> other than a percentage is given, then the ‘startOffset’
          represents a distance along the path measured in the current user coordinate
          system for the ‘textPath’ element.

          If a percentage is given, then the ‘startOffset’ represents a percentage
          distance along the entire path. Thus, startOffset="0%" indicates the start
          point of the path and startOffset="100%" indicates the end point of the path.

          Negative values and values larger than the path length (e.g. 150%) are allowed.

          Any typographic characters with mid-points that are not on the path are not rendered

          For paths consisting of a single closed subpath (including an equivalent path for a
          basic shape), typographic characters are rendered along one complete circuit of the
          path. The text is aligned as determined by the text-anchor property to a position
          along the path set by the ‘startOffset’ attribute.

          For the start (end) value, the text is rendered from the start (end) of the line
          until the initial position along the path is reached again.

          For the middle, the text is rendered from the middle point in both directions until
          a point on the path equal distance in both directions from the initial position on
          the path is reached.
      */
            val absoluteStartOffset = getAbsoluteStartOffset(textPath!!.startOffset, pathLength, fontSize)
            offset += absoluteStartOffset
            if (isClosed) {
                val halfPathDistance = pathLength / 2
                startOfRendering = absoluteStartOffset + if (textAnchor == TextAnchor.middle) -halfPathDistance else 0.0
                endOfRendering = startOfRendering + pathLength
            }
            /*
      TextPathSpacing spacing = textPath.getSpacing();
      if (spacing == TextPathSpacing.auto) {
          // Hmm, what to do here?
          // https://svgwg.org/svg2-draft/text.html#TextPathElementSpacingAttribute
      }
      */
        }

        /*
        Name
        method
        Value
        align | stretch
        initial value
        align
        Animatable
        yes
        Indicates the method by which text should be rendered along the path.

        A value of align indicates that the typographic character should be rendered using
        simple 2×3 matrix transformations such that there is no stretching/warping of the
        typographic characters. Typically, supplemental rotation, scaling and translation
        transformations are done for each typographic characters to be rendered.

        As a result, with align, in fonts where the typographic characters are designed to be
        connected (e.g., cursive fonts), the connections may not align properly when text is
        rendered along a path.

        A value of stretch indicates that the typographic character outlines will be converted
        into paths, and then all end points and control points will be adjusted to be along the
        perpendicular vectors from the path, thereby stretching and possibly warping the glyphs.

        With this approach, connected typographic characters, such as in cursive scripts,
        will maintain their connections. (Non-vertical straight path segments should be
        converted to Bézier curves in such a way that horizontal straight paths have an
        (approximately) constant offset from the path along which the typographic characters
        are rendered.)

        TODO implement stretch
    */

        /*
        Name	Value	Initial value	Animatable
        textLength	<length> | <percentage> | <number>	See below	yes

        The author's computation of the total sum of all of the advance values that correspond
        to character data within this element, including the advance value on the glyph
        (horizontal or vertical), the effect of properties letter-spacing and word-spacing and
        adjustments due to attributes ‘dx’ and ‘dy’ on this ‘text’ or ‘tspan’ element or any
        descendants. This value is used to calibrate the user agent's own calculations with
        that of the author.

        The purpose of this attribute is to allow the author to achieve exact alignment,
        in visual rendering order after any bidirectional reordering, for the first and
        last rendered glyphs that correspond to this element; thus, for the last rendered
        character (in visual rendering order after any bidirectional reordering),
        any supplemental inter-character spacing beyond normal glyph advances are ignored
        (in most cases) when the user agent determines the appropriate amount to expand/compress
        the text string to fit within a length of ‘textLength’.

        If attribute ‘textLength’ is specified on a given element and also specified on an
        ancestor, the adjustments on all character data within this element are controlled by
        the value of ‘textLength’ on this element exclusively, with the possible side-effect
        that the adjustment ratio for the contents of this element might be different than the
        adjustment ratio used for other content that shares the same ancestor. The user agent
        must assume that the total advance values for the other content within that ancestor is
        the difference between the advance value on that ancestor and the advance value for
        this element.

        This attribute is not intended for use to obtain effects such as shrinking or
        expanding text.

        A negative value is an error (see Error processing).

        The ‘textLength’ attribute is only applied when the wrapping area is not defined by the
    TODO shape-inside or the inline-size properties. It is also not applied for any ‘text’ or
    TODO ‘tspan’ element that has forced line breaks (due to a white-space value of pre or
        pre-line).

        If the attribute is not specified anywhere within a ‘text’ element, the effect is as if
        the author's computation exactly matched the value calculated by the user agent;
        thus, no advance adjustments are made.
    */
        var scaleSpacingAndGlyphs = 1.0
        if (mTextLength != null) {
            val author = PropHelper.fromRelative(mTextLength, canvas!!.width.toDouble(), 0.0, mScale.toDouble(), fontSize)
            require(!(author < 0)) { "Negative textLength value" }
            when (mLengthAdjust) {
                TextLengthAdjust.spacing -> letterSpacing += (author - textMeasure) / (length - 1)
                TextLengthAdjust.spacingAndGlyphs -> scaleSpacingAndGlyphs = author / textMeasure
                else -> letterSpacing += (author - textMeasure) / (length - 1)
            }
        }
        val scaledDirection = scaleSpacingAndGlyphs * side

        /*
        https://developer.mozilla.org/en/docs/Web/CSS/vertical-align
        https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6bsln.html
        https://www.microsoft.com/typography/otspec/base.htm
        http://apike.ca/prog_svg_text_style.html
        https://www.w3schools.com/tags/canvas_textbaseline.asp
        http://vanseodesign.com/web-design/svg-text-baseline-alignment/
        https://iamvdo.me/en/blog/css-font-metrics-line-height-and-vertical-align
        https://tympanus.net/codrops/css_reference/vertical-align/

        https://svgwg.org/svg2-draft/text.html#AlignmentBaselineProperty
        11.10.2.6. The ‘alignment-baseline’ property

        This property is defined in the CSS Line Layout Module 3 specification. See 'alignment-baseline'. [css-inline-3]
        https://drafts.csswg.org/css-inline/#propdef-alignment-baseline

        The vertical-align property shorthand should be preferred in new content.

        SVG 2 introduces some changes to the definition of this property.
        In particular: the values 'auto', 'before-edge', and 'after-edge' have been removed.
        For backwards compatibility, 'text-before-edge' should be mapped to 'text-top' and
        'text-after-edge' should be mapped to 'text-bottom'.

        Neither 'text-before-edge' nor 'text-after-edge' should be used with the vertical-align property.
    */
        val fm = paint.getFontMetrics()
        val descenderDepth = fm.descent.toDouble()
        val bottom = descenderDepth + fm.leading
        val ascenderHeight = (-fm.ascent + fm.leading).toDouble()
        val top = -fm.top.toDouble()
        val totalHeight = top + bottom
        var baselineShift = 0.0
        val baselineShiftString = this.baselineShift
        val baseline = alignmentBaseline
        baselineShift = when (baseline) {
            AlignmentBaseline.baseline ->           // Use the dominant baseline choice of the parent.
                // Match the box’s corresponding baseline to that of its parent.
                0.0

            AlignmentBaseline.textBottom, AlignmentBaseline.afterEdge, AlignmentBaseline.textAfterEdge ->           // Match the bottom of the box to the bottom of the parent’s content area.
                // text-after-edge = text-bottom
                // text-after-edge = descender depth
                -descenderDepth

            AlignmentBaseline.alphabetic ->           // Match the box’s alphabetic baseline to that of its parent.
                // alphabetic = 0
                0.0

            AlignmentBaseline.ideographic ->           // Match the box’s ideographic character face under-side baseline to that of its parent.
                // ideographic = descender depth
                -descenderDepth

            AlignmentBaseline.middle -> {
                // Align the vertical midpoint of the box with the baseline of the parent box plus half
                // the x-height of the parent.
                // middle = x height / 2
                val bounds = Rect()
                // this will just retrieve the bounding rect for 'x'
                paint.getTextBounds("x", 0, 1, bounds)
                val xHeight = bounds.height()
                xHeight / 2.0
            }

            AlignmentBaseline.central ->           // Match the box’s central baseline to the central baseline of its parent.
                // central = (ascender height - descender depth) / 2
                (ascenderHeight - descenderDepth) / 2

            AlignmentBaseline.mathematical ->           // Match the box’s mathematical baseline to that of its parent.
                // Hanging and mathematical baselines
                // There are no obvious formulas to calculate the position of these baselines.
                // At the time of writing FOP puts the hanging baseline at 80% of the ascender
                // height and the mathematical baseline at 50%.
                0.5 * ascenderHeight

            AlignmentBaseline.hanging -> 0.8 * ascenderHeight
            AlignmentBaseline.textTop, AlignmentBaseline.beforeEdge, AlignmentBaseline.textBeforeEdge ->           // Match the top of the box to the top of the parent’s content area.
                // text-before-edge = text-top
                // text-before-edge = ascender height
                ascenderHeight

            AlignmentBaseline.bottom ->           // Align the top of the aligned subtree with the top of the line box.
                bottom

            AlignmentBaseline.center ->           // Align the center of the aligned subtree with the center of the line box.
                totalHeight / 2

            AlignmentBaseline.top ->           // Align the bottom of the aligned subtree with the bottom of the line box.
                top

            else -> 0.0
        }
        /*
    2.2.2. Alignment Shift: baseline-shift longhand

    This property specifies by how much the box is shifted up from its alignment point.
    It does not apply when alignment-baseline is top or bottom.

    Authors should use the vertical-align shorthand instead of this property.

    Values have the following meanings:

    <length>
    Raise (positive value) or lower (negative value) by the specified length.
    <percentage>
    Raise (positive value) or lower (negative value) by the specified percentage of the line-height.
    TODO sub
    Lower by the offset appropriate for subscripts of the parent’s box.
    (The UA should use the parent’s font data to find this offset whenever possible.)
    TODO super
    Raise by the offset appropriate for superscripts of the parent’s box.
    (The UA should use the parent’s font data to find this offset whenever possible.)

    User agents may additionally support the keyword baseline as computing to 0
    if is necessary for them to support legacy SVG content.
    Issue: We would prefer to remove this,
    and are looking for feedback from SVG user agents as to whether it’s necessary.

    https://www.w3.org/TR/css-inline-3/#propdef-baseline-shift
    */if (!baselineShiftString.isNullOrEmpty()) {
            when (baseline) {
                AlignmentBaseline.top, AlignmentBaseline.bottom -> {}
                else -> when (baselineShiftString) {
                    "sub" ->               // TODO
                        if (fontData != null && fontData.hasKey("tables") && fontData.hasKey("unitsPerEm")) {
                            val unitsPerEm = fontData.getInt("unitsPerEm")
                            val tables = fontData.getMap("tables")
                            if (tables!!.hasKey("os2")) {
                                val os2 = tables!!.getMap("os2")
                                if (os2!!.hasKey("ySubscriptYOffset")) {
                                    val subOffset = os2!!.getDouble("ySubscriptYOffset")
                                    baselineShift += mScale * fontSize * subOffset / unitsPerEm
                                }
                            }
                        }

                    "super" ->               // TODO
                        if (fontData != null && fontData.hasKey("tables") && fontData.hasKey("unitsPerEm")) {
                            val unitsPerEm = fontData.getInt("unitsPerEm")
                            val tables = fontData.getMap("tables")
                            if (tables!!.hasKey("os2")) {
                                val os2 = tables!!.getMap("os2")
                                if (os2!!.hasKey("ySuperscriptYOffset")) {
                                    val superOffset = os2!!.getDouble("ySuperscriptYOffset")
                                    baselineShift -= mScale * fontSize * superOffset / unitsPerEm
                                }
                            }
                        }

                    "baseline" -> {}
                    else -> baselineShift -= PropHelper.fromRelative(baselineShiftString, mScale * fontSize, mScale.toDouble(), fontSize)
                }
            }
        }
        val start = Matrix()
        val mid = Matrix()
        val end = Matrix()
        val startPointMatrixData = FloatArray(9)
        val endPointMatrixData = FloatArray(9)
        for (index in 0 until length) {
            val currentChar = chars[index]
            var current = currentChar.toString()
            val alreadyRenderedGraphemeCluster = ligature[index]

            /*
          Determine the glyph's charwidth (i.e., the amount which the current text position
          advances horizontally when the glyph is drawn using horizontal text layout).
      */
            var hasLigature = false
            if (alreadyRenderedGraphemeCluster) {
                current = ""
            } else {
                var nextIndex = index
                while (++nextIndex < length) {
                    val nextWidth = advances[nextIndex]
                    if (nextWidth > 0) {
                        break
                    }
                    val nextLigature = current + chars[nextIndex]
                    ligature[nextIndex] = true
                    current = nextLigature
                    hasLigature = true
                }
            }
            var charWidth = paint.measureText(current) * scaleSpacingAndGlyphs

            /*
          For each subsequent glyph, set a new startpoint-on-the-path as the previous
          endpoint-on-the-path, but with appropriate adjustments taking into account
          horizontal kerning tables in the font and current values of various attributes
          and properties, including spacing properties (e.g. letter-spacing and word-spacing)
          and ‘tspan’ elements with values provided for attributes ‘dx’ and ‘dy’. All
          adjustments are calculated as distance adjustments along the path, calculated
          using the user agent's distance along the path algorithm.
      */if (autoKerning) {
                val kerned = advances[index] * scaleSpacingAndGlyphs
                kerning = kerned - charWidth
            }
            val isWordSeparator = currentChar == ' '
            val wordSpace: Double = if (isWordSeparator) wordSpacing else 0.0
            val spacing = wordSpace + letterSpacing
            var advance = charWidth + spacing
            val x = gc.nextX(if (alreadyRenderedGraphemeCluster) 0.0 else kerning + advance)
            val y = gc.nextY()
            val dx = gc.nextDeltaX()
            val dy = gc.nextDeltaY()
            val r = gc.nextRotation()
            if (alreadyRenderedGraphemeCluster || isWordSeparator) {
                // Skip rendering other grapheme clusters of ligatures (already rendered),
                // But, make sure to increment index positions by making gc.next() calls.
                continue
            }
            advance *= side.toDouble()
            charWidth *= side.toDouble()
            val cursor = offset + (x + dx) * side
            val startPoint = cursor - advance
            if (hasTextPath) {
                /*
           Determine the point on the curve which is charwidth distance along the path from
           the startpoint-on-the-path for this glyph, calculated using the user agent's
           distance along the path algorithm. This point is the endpoint-on-the-path for
           the glyph.
        */
                val endPoint = startPoint + charWidth

                /*
            Determine the midpoint-on-the-path, which is the point on the path which is
            "halfway" (user agents can choose either a distance calculation or a parametric
            calculation) between the startpoint-on-the-path and the endpoint-on-the-path.
        */
                val halfWay = charWidth / 2
                val midPoint = startPoint + halfWay

                //  Glyphs whose midpoint-on-the-path are off the path are not rendered.
                if (midPoint > endOfRendering) {
                    continue
                } else if (midPoint < startOfRendering) {
                    continue
                }

                /*
            Determine the glyph-midline, which is the vertical line in the glyph's
            coordinate system that goes through the glyph's x-axis midpoint.

            Position the glyph such that the glyph-midline passes through
            the midpoint-on-the-path and is perpendicular to the line
            through the startpoint-on-the-path and the endpoint-on-the-path.

            TODO suggest adding a compatibility mid-line rendering attribute to textPath,
            for a chrome/firefox/opera/safari compatible sharp text path rendering,
            which doesn't bend text smoothly along a right angle curve, (like Edge does)
            but keeps the mid-line orthogonal to the mid-point tangent at all times instead.
            https://github.com/w3c/svgwg/issues/337
        */
                val posAndTanFlags = PathMeasure.POSITION_MATRIX_FLAG or PathMeasure.TANGENT_MATRIX_FLAG
                if (sharpMidLine) {
                    pm!!.getMatrix(midPoint.toFloat(), mid, posAndTanFlags)
                } else {
                    /*
              In the calculation above, if either the startpoint-on-the-path
              or the endpoint-on-the-path is off the end of the path,
              then extend the path beyond its end points with a straight line
              that is parallel to the tangent at the path at its end point
              so that the midpoint-on-the-path can still be calculated.

              TODO suggest change in wording of svg spec:
              so that the midpoint-on-the-path can still be calculated.
              to
              so that the angle of the glyph-midline to the x-axis can still be calculated.
              or
              so that the line through the startpoint-on-the-path and the
              endpoint-on-the-path can still be calculated.
              https://github.com/w3c/svgwg/issues/337#issuecomment-318056199
          */
                    if (startPoint < 0) {
                        pm!!.getMatrix(0f, start, posAndTanFlags)
                        start.preTranslate(startPoint.toFloat(), 0f)
                    } else {
                        pm!!.getMatrix(startPoint.toFloat(), start, PathMeasure.POSITION_MATRIX_FLAG)
                    }
                    pm.getMatrix(midPoint.toFloat(), mid, PathMeasure.POSITION_MATRIX_FLAG)
                    if (endPoint > pathLength) {
                        pm.getMatrix(pathLength.toFloat(), end, posAndTanFlags)
                        end.preTranslate((endPoint - pathLength).toFloat(), 0f)
                    } else {
                        pm.getMatrix(endPoint.toFloat(), end, PathMeasure.POSITION_MATRIX_FLAG)
                    }
                    start.getValues(startPointMatrixData)
                    end.getValues(endPointMatrixData)
                    val startX = startPointMatrixData[Matrix.MTRANS_X].toDouble()
                    val startY = startPointMatrixData[Matrix.MTRANS_Y].toDouble()
                    val endX = endPointMatrixData[Matrix.MTRANS_X].toDouble()
                    val endY = endPointMatrixData[Matrix.MTRANS_Y].toDouble()

                    // line through the startpoint-on-the-path and the endpoint-on-the-path
                    val lineX = endX - startX
                    val lineY = endY - startY
                    val glyphMidlineAngle = atan2(lineY, lineX)
                    mid.preRotate((glyphMidlineAngle * radToDeg * side).toFloat())
                }

                /*
            Align the glyph vertically relative to the midpoint-on-the-path based on property
            alignment-baseline and any specified values for attribute ‘dy’ on a ‘tspan’ element.
        */mid.preTranslate(-halfWay.toFloat(), (dy + baselineShift).toFloat())
                mid.preScale(scaledDirection.toFloat(), side.toFloat())
                mid.postTranslate(0f, y.toFloat())
            } else {
                mid.setTranslate(startPoint.toFloat(), (y + dy + baselineShift).toFloat())
            }
            mid.preRotate(r.toFloat())
            var glyph: Path?
            if (hasLigature) {
                glyph = Path()
                paint.getTextPath(current, 0, current.length, 0f, 0f, glyph)
            } else {
                glyph = bag.getOrCreateAndCache(currentChar, current)
            }
            val bounds = RectF()
            glyph.computeBounds(bounds, true)
            val width = bounds.width()
            if (width == 0f) { // Render unicode emoji
                canvas!!.save()
                canvas.concat(mid)
                emoji.add(current)
                emojiTransforms.add(Matrix(mid))
                canvas.drawText(current, 0f, 0f, paint)
                canvas.restore()
            } else {
                glyph.transform(mid)
                path.addPath(glyph)
            }
        }
        return path
    }

    private fun getAbsoluteStartOffset(startOffset: SVGLength?, distance: Double, fontSize: Double): Double {
        return PropHelper.fromRelative(startOffset, distance, 0.0, mScale.toDouble(), fontSize)
    }

    private fun getTextAnchorOffset(textAnchor: TextAnchor?, textMeasure: Double): Double {
        return when (textAnchor) {
            TextAnchor.start -> 0.0
            TextAnchor.middle -> -textMeasure / 2
            TextAnchor.end -> -textMeasure
            else -> 0.0
        }
    }

    private fun applyTextPropertiesToPaint(paint: Paint?, font: FontData?) {
        val isBold = font!!.fontWeight == TextProperties.FontWeight.Bold || font.absoluteFontWeight >= 550
        val isItalic = font.fontStyle == TextProperties.FontStyle.italic
        val style: Int = if (isBold && isItalic) {
            Typeface.BOLD_ITALIC
        } else if (isBold) {
            Typeface.BOLD
        } else if (isItalic) {
            Typeface.ITALIC
        } else {
            Typeface.NORMAL
        }
        var typeface: Typeface? = null
        val weight = font.absoluteFontWeight
        val fontFamily = font.fontFamily
        if (!fontFamily.isNullOrEmpty()) {
            val otfpath = FONTS + fontFamily + OTF
            val ttfpath = FONTS + fontFamily + TTF
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var builder = Typeface.Builder(assets, otfpath)
                builder.setFontVariationSettings("'wght' " + weight + font.fontVariationSettings)
                builder.setWeight(weight)
                builder.setItalic(isItalic)
                typeface = builder.build()
                if (typeface == null) {
                    builder = Typeface.Builder(assets, ttfpath)
                    builder.setFontVariationSettings("'wght' " + weight + font.fontVariationSettings)
                    builder.setWeight(weight)
                    builder.setItalic(isItalic)
                    typeface = builder.build()
                }
            } else {
                try {
                    typeface = Typeface.createFromAsset(assets, otfpath)
                    typeface = Typeface.create(typeface, style)
                } catch (ignored: Exception) {
                    try {
                        typeface = Typeface.createFromAsset(assets, ttfpath)
                        typeface = Typeface.create(typeface, style)
                    } catch (ignored2: Exception) {
                    }
                }
            }
        }
        if (typeface == null) {
            try {
                typeface = ReactFontManager.getInstance().getTypeface(fontFamily!!, style, assets)
            } catch (ignored: Exception) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            typeface = Typeface.create(typeface, weight, isItalic)
        }
        paint!!.isLinearText = true
        paint.isSubpixelText = true
        paint.setTypeface(typeface)
        paint.textSize = (font.fontSize * mScale).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            paint.letterSpacing = 0f
        }
    }

    private fun setupTextPath() {
        var parent = parent
        while (parent != null) {
            if (parent.javaClass == TextPathView::class.java) {
                textPath = parent as TextPathView?
                break
            } else if (parent !is TextView) {
                break
            }
            parent = parent.getParent()
        }
    }

    public override fun hitTest(src: FloatArray?): Int {
        if (mContent == null) {
            return super.hitTest(src)
        }
        if (mPath == null || !mInvertible || !mTransformInvertible) {
            return -1
        }
        val dst = FloatArray(2)
        mInvMatrix.mapPoints(dst, src)
        mInvTransform.mapPoints(dst)
        val x = Math.round(dst[0])
        val y = Math.round(dst[1])
        initBounds()
        if ((mRegion == null || !mRegion!!.contains(x, y))
                && (mStrokeRegion == null || !mStrokeRegion!!.contains(x, y))) {
            return -1
        }
        val clipPath = clipPath
        if (clipPath != null) {
            if (!mClipRegion!!.contains(x, y)) {
                return -1
            }
        }
        return id
    }

    companion object {
        private const val tau = 2 * Math.PI
        private val radToDeg = 360 / tau
        private const val FONTS = "fonts/"
        private const val OTF = ".otf"
        private const val TTF = ".ttf"

        /**
         * Implements visual to logical order converter.
         *
         * @author [Nesterovsky bros](http://www.nesterovsky-bros.com)
         * @param text an input text in visual order to convert.
         * @return a String value in logical order.
         */
        fun visualToLogical(text: String?): String? {
            if (text == null || text.length == 0) {
                return text
            }
            val bidi = Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
            if (bidi.isLeftToRight) {
                return text
            }
            val count = bidi.getRunCount()
            val levels = ByteArray(count)
            val runs = IntArray(count).toTypedArray()
            for (i in 0 until count) {
                levels[i] = bidi.getRunLevel(i).toByte()
                runs[i] = i
            }
            Bidi.reorderVisually(levels, 0, runs, 0, count)
            val result = StringBuilder()
            for (i in 0 until count) {
                val index = runs[i]
                val start = bidi.getRunStart(index)
                var end = bidi.getRunLimit(index)
                val level = levels[index].toInt()
                if (level and 1 != 0) {
                    while (--end >= start) {
                        result.append(text[end])
                    }
                } else {
                    result.append(text, start, end)
                }
            }
            return result.toString()
        }

        const val requiredFontFeatures = "'rlig', 'liga', 'clig', 'calt', 'locl', 'ccmp', 'mark', 'mkmk',"
        const val disableDiscretionaryLigatures = "'liga' 0, 'clig' 0, 'dlig' 0, 'hlig' 0, 'cala' 0, "
        val defaultFeatures = requiredFontFeatures + "'kern', "
        const val additionalLigatures = "'hlig', 'cala', "
        const val fontWeightTag = "'wght' "
    }
}
