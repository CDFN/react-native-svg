package com.horcrux.svg

import javax.annotation.Nonnull

class TextProperties() {
    /*
      https://drafts.csswg.org/css-inline/#propdef-alignment-baseline
      2.2.1. Alignment Point: alignment-baseline longhand

      Name:	alignment-baseline
      Value:	baseline | text-bottom | alphabetic | ideographic | middle | central | mathematical | text-top | bottom | center | top
      Initial:	baseline
      Applies to:	inline-level boxes, flex items, grid items, table cells
      Inherited:	no
      Percentages:	N/A
      Media:	visual
      Computed value:	as specified
      Canonical order:	per grammar
      Animation type:	discrete
  */
    enum class AlignmentBaseline(private val alignment: String) {
        baseline("baseline"),
        textBottom("text-bottom"),
        alphabetic("alphabetic"),
        ideographic("ideographic"),
        middle("middle"),
        central("central"),
        mathematical("mathematical"),
        textTop("text-top"),
        bottom("bottom"),
        center("center"),
        top("top"),

        /*
        SVG implementations may support the following aliases in order to support legacy content:

        text-before-edge = text-top
        text-after-edge = text-bottom
    */
        textBeforeEdge("text-before-edge"),
        textAfterEdge("text-after-edge"),

        // SVG 1.1
        beforeEdge("before-edge"),
        afterEdge("after-edge"),
        hanging("hanging");

        @Nonnull
        public override fun toString(): String {
            return alignment
        }

        companion object {
            fun getEnum(strVal: String?): AlignmentBaseline? {
                if (!alignmentToEnum.containsKey(strVal)) {
                    throw IllegalArgumentException("Unknown String Value: " + strVal)
                }
                return alignmentToEnum.get(strVal)
            }

            private val alignmentToEnum: MutableMap<String?, AlignmentBaseline> = HashMap()

            init {
                for (en: AlignmentBaseline in values()) {
                    alignmentToEnum.put(en.alignment, en)
                }
            }
        }
    }

    // TODO implement rtl
    @Suppress("unused")
    internal enum class Direction {
        ltr,
        rtl
    }

    enum class FontVariantLigatures {
        normal,
        @Suppress("unused")
        none
    }

    enum class FontStyle {
        normal,
        italic,
        @Suppress("unused")
        oblique
    }

    enum class FontWeight(private val weight: String) {
        // Absolute
        Normal("normal"),
        Bold("bold"),
        w100("100"),
        w200("200"),
        w300("300"),
        w400("400"),
        w500("500"),
        w600("600"),
        w700("700"),
        w800("800"),
        w900("900"),

        // Relative
        Bolder("bolder"),
        Lighter("lighter");

        @Nonnull
        public override fun toString(): String {
            return weight
        }

        companion object {
            fun hasEnum(strVal: String?): Boolean {
                return weightToEnum.containsKey(strVal)
            }

            operator fun get(strVal: String?): FontWeight? {
                return weightToEnum.get(strVal)
            }

            private val weightToEnum: MutableMap<String?, FontWeight> = HashMap()

            init {
                for (en: FontWeight in values()) {
                    weightToEnum.put(en.weight, en)
                }
            }
        }
    }

    enum class TextAnchor {
        start,
        middle,
        end
    }

    internal enum class TextDecoration(private val decoration: String) {
        None("none"),
        Underline("underline"),
        Overline("overline"),
        LineThrough("line-through"),
        Blink("blink");

        @Nonnull
        public override fun toString(): String {
            return decoration
        }

        companion object {
            fun getEnum(strVal: String?): TextDecoration? {
                if (!decorationToEnum.containsKey(strVal)) {
                    throw IllegalArgumentException("Unknown String Value: " + strVal)
                }
                return decorationToEnum.get(strVal)
            }

            private val decorationToEnum: MutableMap<String?, TextDecoration> = HashMap()

            init {
                for (en: TextDecoration in values()) {
                    decorationToEnum.put(en.decoration, en)
                }
            }
        }
    }

    enum class TextLengthAdjust {
        spacing,
        spacingAndGlyphs
    }

    internal enum class TextPathMethod {
        align,
        @Suppress("unused")
        stretch
    }

    /*
      TODO suggest adding a compatibility mid-line rendering attribute to textPath,
      for a chrome/firefox/opera/safari compatible sharp text path rendering,
      which doesn't bend text smoothly along a right angle curve, (like Edge does)
      but keeps the mid-line orthogonal to the mid-point tangent at all times instead.
  */
    internal enum class TextPathMidLine {
        sharp,
        @Suppress("unused")
        smooth
    }

    internal enum class TextPathSide {
        @Suppress("unused")
        left,
        right
    }

    internal enum class TextPathSpacing {
        @Suppress("unused")
        auto,
        exact
    }
}
