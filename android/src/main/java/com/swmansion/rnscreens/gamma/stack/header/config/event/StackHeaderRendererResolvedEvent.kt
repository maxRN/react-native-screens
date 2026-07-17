package com.swmansion.rnscreens.gamma.stack.header.config.event

import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.events.Event
import com.swmansion.rnscreens.gamma.common.event.NamingAwareEventType
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer

internal class StackHeaderRendererResolvedEvent(
    surfaceId: Int,
    viewTag: Int,
    private val requestedRenderer: StackHeaderRenderer,
    private val actualRenderer: StackHeaderRenderer,
    private val fallbackReason: String?,
) : Event<StackHeaderRendererResolvedEvent>(surfaceId, viewTag),
    NamingAwareEventType {
    override fun getEventName() = EVENT_NAME

    override fun getEventRegistrationName() = EVENT_REGISTRATION_NAME

    override fun canCoalesce(): Boolean = false

    override fun getEventData() =
        Arguments.createMap().apply {
            putString(EK_REQUESTED_RENDERER, requestedRenderer.jsValue)
            putString(EK_ACTUAL_RENDERER, actualRenderer.jsValue)
            putString(EK_FALLBACK_REASON, fallbackReason)
        }

    companion object : NamingAwareEventType {
        const val EVENT_NAME = "topRendererResolved"
        const val EVENT_REGISTRATION_NAME = "onRendererResolved"

        private const val EK_REQUESTED_RENDERER = "requestedRenderer"
        private const val EK_ACTUAL_RENDERER = "actualRenderer"
        private const val EK_FALLBACK_REASON = "fallbackReason"

        override fun getEventName() = EVENT_NAME

        override fun getEventRegistrationName() = EVENT_REGISTRATION_NAME
    }
}

private val StackHeaderRenderer.jsValue: String
    get() = name.lowercase()
