package com.horcrux.svg

import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableType

class SVGLength {
    // https://www.w3.org/TR/SVG/types.html#InterfaceSVGLength
    enum class UnitType {
        UNKNOWN,
        NUMBER,
        PERCENTAGE,
        EMS,
        EXS,
        PX,
        CM,
        MM,
        IN,
        PT,
        PC
    }

    val value: Double
    val unit: UnitType

    private constructor() {
        value = 0.0
        unit = UnitType.UNKNOWN
    }

    constructor(number: Double) {
        value = number
        unit = UnitType.NUMBER
    }

    constructor(length: String) {
        var length = length
        length = length.trim { it <= ' ' }
        val stringLength = length.length
        val percentIndex = stringLength - 1
        if (stringLength == 0 || length == "normal") {
            unit = UnitType.UNKNOWN
            value = 0.0
        } else if (length.codePointAt(percentIndex) == '%'.code) {
            unit = UnitType.PERCENTAGE
            value = length.substring(0, percentIndex).toDouble()
        } else {
            val twoLetterUnitIndex = stringLength - 2
            if (twoLetterUnitIndex > 0) {
                val lastTwo = length.substring(twoLetterUnitIndex)
                var end = twoLetterUnitIndex
                when (lastTwo) {
                    "px" -> unit = UnitType.NUMBER
                    "em" -> unit = UnitType.EMS
                    "ex" -> unit = UnitType.EXS
                    "pt" -> unit = UnitType.PT
                    "pc" -> unit = UnitType.PC
                    "mm" -> unit = UnitType.MM
                    "cm" -> unit = UnitType.CM
                    "in" -> unit = UnitType.IN
                    else -> {
                        unit = UnitType.NUMBER
                        end = stringLength
                    }
                }
                value = length.substring(0, end).toDouble()
            } else {
                unit = UnitType.NUMBER
                value = length.toDouble()
            }
        }
    }

    companion object {
        fun from(dynamic: Dynamic): SVGLength {
            return when (dynamic.type) {
                ReadableType.Number -> SVGLength(dynamic.asDouble())
                ReadableType.String -> SVGLength(dynamic.asString())
                else -> SVGLength()
            }
        }

        fun from(string: String?): SVGLength {
            return string?.let { SVGLength(it) } ?: SVGLength()
        }

        fun from(value: Double?): SVGLength {
            return value?.let { SVGLength(it) } ?: SVGLength()
        }

        fun toString(dynamic: Dynamic?): String? {
            return when (dynamic!!.type) {
                ReadableType.Number -> dynamic.asDouble().toString()
                ReadableType.String -> dynamic.asString()
                else -> null
            }
        }

        fun arrayFrom(dynamic: Dynamic): ArrayList<SVGLength>? {
            return when (dynamic.type) {
                ReadableType.Number -> {
                    val list = ArrayList<SVGLength>(1)
                    list.add(SVGLength(dynamic.asDouble()))
                    list
                }

                ReadableType.Array -> {
                    val arr = dynamic.asArray()
                    val size = arr.size()
                    val list = ArrayList<SVGLength>(size)
                    var i = 0
                    while (i < size) {
                        val `val` = arr.getDynamic(i)
                        list.add(from(`val`))
                        i++
                    }
                    list
                }

                ReadableType.String -> {
                    var stringValue = dynamic.asString().trim { it <= ' ' }
                    stringValue = stringValue.replace(",".toRegex(), " ")
                    val strings = stringValue.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val list = ArrayList<SVGLength>(strings.size)
                    for (length in strings) {
                        list.add(SVGLength(length))
                    }
                    list
                }

                else -> null
            }
        }

        fun arrayFrom(arr: ReadableArray): ArrayList<SVGLength> {
            val size = arr.size()
            val list = ArrayList<SVGLength>(size)
            for (i in 0 until size) {
                val `val` = arr.getDynamic(i)
                list.add(from(`val`))
            }
            return list
        }
    }
}
