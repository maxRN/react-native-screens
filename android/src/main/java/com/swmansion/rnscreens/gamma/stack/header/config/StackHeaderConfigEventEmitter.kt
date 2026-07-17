package com.swmansion.rnscreens.gamma.stack.header.config

import com.facebook.react.bridge.ReactContext
import com.swmansion.rnscreens.gamma.common.event.BaseEventEmitter
import com.swmansion.rnscreens.gamma.stack.header.config.event.StackHeaderRendererResolvedEvent
import com.swmansion.rnscreens.gamma.stack.header.toolbar.event.StackHeaderToolbarMenuGroupSelectionChangeEvent
import com.swmansion.rnscreens.gamma.stack.header.toolbar.event.StackHeaderToolbarMenuItemPressEvent

internal class StackHeaderConfigEventEmitter(
    reactContext: ReactContext,
    viewTag: Int,
) : BaseEventEmitter(reactContext, viewTag) {
    internal fun emitOnToolbarMenuItemPress(id: String) {
        reactEventDispatcher.dispatchEvent(
            StackHeaderToolbarMenuItemPressEvent(surfaceId, viewTag, id),
        )
    }

    internal fun emitOnToolbarMenuGroupSelectionChange(
        groupId: String,
        selectedIds: List<String>,
    ) {
        reactEventDispatcher.dispatchEvent(
            StackHeaderToolbarMenuGroupSelectionChangeEvent(surfaceId, viewTag, groupId, selectedIds),
        )
    }

    internal fun emitOnRendererResolved(resolution: StackHeaderRendererResolution) {
        reactEventDispatcher.dispatchEvent(
            StackHeaderRendererResolvedEvent(
                surfaceId,
                viewTag,
                requestedRenderer = resolution.requested,
                actualRenderer = resolution.actual,
                fallbackReason = resolution.fallbackReason,
            ),
        )
    }
}
