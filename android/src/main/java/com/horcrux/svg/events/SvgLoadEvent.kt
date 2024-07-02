package com.horcrux.svg.events

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.facebook.react.views.imagehelper.ImageSource

class SvgLoadEvent(surfaceId: Int, viewId: Int, mContext: ReactContext?, uriString: String?, val width: Float, val height: Float) : Event<SvgLoadEvent>(surfaceId, viewId) {
    private val uri: String = ImageSource(mContext, uriString).source

    override fun getEventName(): String {
        return EVENT_NAME
    }

    override fun getCoalescingKey(): Short {
        return 0
    }

    override fun dispatch(rctEventEmitter: RCTEventEmitter) {
        rctEventEmitter.receiveEvent(viewTag, eventName, getEventData())
    }

    override fun getEventData(): WritableMap? {
        val eventData = Arguments.createMap()
        eventData.putDouble("width", width.toDouble())
        eventData.putDouble("height", height.toDouble())
        eventData.putString("uri", uri)

        val event = Arguments.createMap()
        event.putMap("source", eventData)

        return event
    }

    companion object {
        const val EVENT_NAME = "topLoad"
    }
}
