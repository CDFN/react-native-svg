/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.module.annotations.ReactModule
import com.horcrux.svg.SvgViewModule
import javax.annotation.Nonnull
const val SVG_VIEW_MODULE_NAME: String = "RNSVGSvgViewModule"

@ReactModule(name = SVG_VIEW_MODULE_NAME)
internal class SvgViewModule(reactContext: ReactApplicationContext?) : NativeSvgViewModuleSpec(reactContext) {
    @Nonnull
    public override fun getName(): String {
        return SVG_VIEW_MODULE_NAME
    }

    @Suppress("unused")
    @ReactMethod
    public override fun toDataURL(tag: Double?, options: ReadableMap?, successCallback: Callback?) {
        toDataURL(tag!!.toInt(), options, successCallback, 0)
    }

    companion object {
        private fun toDataURL(
                tag: Int, options: ReadableMap?, successCallback: Callback?, attempt: Int) {
            UiThreadUtil.runOnUiThread(
                    object : Runnable {
                        public override fun run() {
                            val svg: SvgView? = SvgViewManager.getSvgViewByTag(tag)
                            if (svg == null) {
                                SvgViewManager.runWhenViewIsAvailable(
                                        tag,
                                        object : Runnable {
                                            public override fun run() {
                                                val svg: SvgView? = SvgViewManager.getSvgViewByTag(tag)
                                                if (svg == null) { // Should never happen
                                                    return
                                                }
                                                svg.setToDataUrlTask(
                                                        object : Runnable {
                                                            public override fun run() {
                                                                toDataURL(tag, options, successCallback, attempt + 1)
                                                            }
                                                        })
                                            }
                                        })
                            } else if (svg.notRendered()) {
                                svg.setToDataUrlTask(
                                        object : Runnable {
                                            public override fun run() {
                                                toDataURL(tag, options, successCallback, attempt + 1)
                                            }
                                        })
                            } else {
                                if (options != null) {
                                    successCallback!!.invoke(
                                            svg.toDataURL(options.getInt("width"), options.getInt("height")))
                                } else {
                                    successCallback!!.invoke(svg.toDataURL())
                                }
                            }
                        }
                    })
        }
    }
}
