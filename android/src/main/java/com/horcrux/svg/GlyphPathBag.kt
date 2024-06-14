package com.horcrux.svg

import android.graphics.Paint
import android.graphics.Path

internal class GlyphPathBag(private val paint: Paint?) {
    private val paths: ArrayList<Path> = ArrayList()
    private val data: Array<IntArray?> = arrayOfNulls(256)

    init {
        // Make indexed-by-one, to allow zero to represent non-cached
        paths.add(Path())
    }

    fun getOrCreateAndCache(ch: Char, current: String?): Path {
        val index: Int = getIndex(ch)
        val cached: Path
        if (index != 0) {
            cached = paths.get(index)
        } else {
            cached = Path()
            paint!!.getTextPath(current, 0, 1, 0f, 0f, cached)
            var bin: IntArray? = data.get(ch.code shr 8)
            if (bin == null) {
                data[ch.code shr 8] = IntArray(256)
                bin = data.get(ch.code shr 8)
            }
            bin!![ch.code and 0xFF] = paths.size
            paths.add(cached)
        }
        val glyph: Path = Path()
        glyph.addPath(cached)
        return glyph
    }

    private fun getIndex(ch: Char): Int {
        val bin: IntArray = data.get(ch.code shr 8) ?: return 0
        return bin[ch.code and 0xFF]
    }
}
