package com.horcrux.svg

import android.graphics.Rect
import com.facebook.react.bridge.ReactContext
import com.facebook.react.touch.ReactHitSlopView
import com.facebook.react.uimanager.PointerEvents

abstract class HitSlopVirtualView(context: ReactContext) : VirtualView(context), ReactHitSlopView {
    override val hitSlopRect: Rect?
        get() = if (mPointerEvents == PointerEvents.BOX_NONE) {
            Rect(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        } else null
}