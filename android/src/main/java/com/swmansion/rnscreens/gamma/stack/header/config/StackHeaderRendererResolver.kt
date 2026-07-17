package com.swmansion.rnscreens.gamma.stack.header.config

import android.util.Log
import com.facebook.react.bridge.JSApplicationIllegalArgumentException
import com.swmansion.rnscreens.BuildConfig
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderComposeActionPlanner

internal data class StackHeaderRendererCapabilities(
    val type: StackHeaderType,
    val isTransparent: Boolean = false,
    val hasBackgroundColor: Boolean = false,
    val hasBackgroundSubview: Boolean = false,
    val hasCenterSubview: Boolean = false,
    val hasTrailingSubview: Boolean = false,
    val hasToolbarMenuGroupDividers: Boolean = false,
    val hasCustomBackIcon: Boolean = false,
    val hasCustomBackTint: Boolean = false,
    val scrollFlags: StackHeaderScrollFlags = StackHeaderScrollFlags(),
)

internal data class StackHeaderRendererResolution(
    val requested: StackHeaderRenderer,
    val actual: StackHeaderRenderer,
    val fallbackReason: String? = null,
)

/** The only scroll profile Compose controls in v1 is the official medium enter-always pattern. */
internal data class StackHeaderScrollFlags(
    val scroll: Boolean = false,
    val enterAlways: Boolean = false,
    val enterAlwaysCollapsed: Boolean = false,
    val exitUntilCollapsed: Boolean = false,
    val snap: Boolean = false,
) {
    fun areSupportedFor(type: StackHeaderType): Boolean =
        when (type) {
            StackHeaderType.SMALL -> !hasAny
            StackHeaderType.MEDIUM ->
                scroll &&
                    enterAlways &&
                    !enterAlwaysCollapsed &&
                    !exitUntilCollapsed &&
                    !snap
            StackHeaderType.LARGE -> false
        }

    private val hasAny: Boolean
        get() = scroll || enterAlways || enterAlwaysCollapsed || exitUntilCollapsed || snap
}

internal object StackHeaderRendererResolver {
    fun resolve(
        requested: StackHeaderRenderer,
        capabilities: StackHeaderRendererCapabilities,
        actionMenuUnsupportedReason: String? = null,
    ): StackHeaderRenderer = resolution(requested, capabilities, actionMenuUnsupportedReason).actual

    fun resolution(
        requested: StackHeaderRenderer,
        capabilities: StackHeaderRendererCapabilities,
        actionMenuUnsupportedReason: String? = null,
    ): StackHeaderRendererResolution {
        if (requested != StackHeaderRenderer.COMPOSE) {
            return StackHeaderRendererResolution(requested, StackHeaderRenderer.VIEW)
        }

        val fallbackReason = unsupportedReason(capabilities) ?: actionMenuUnsupportedReason
        return if (fallbackReason == null) {
            StackHeaderRendererResolution(requested, StackHeaderRenderer.COMPOSE)
        } else {
            StackHeaderRendererResolution(requested, StackHeaderRenderer.VIEW, fallbackReason)
        }
    }

    fun unsupportedReason(capabilities: StackHeaderRendererCapabilities): String? =
        when {
            capabilities.type == StackHeaderType.LARGE -> "large app bars are not supported"
            capabilities.isTransparent -> "transparent headers are not supported"
            capabilities.hasBackgroundColor -> "custom background colors are not supported"
            capabilities.hasBackgroundSubview -> "custom background views are not supported"
            capabilities.hasCenterSubview -> "custom center views are not supported"
            capabilities.hasTrailingSubview -> "custom trailing views are not supported"
            capabilities.hasToolbarMenuGroupDividers -> "toolbar menu group dividers are not supported"
            capabilities.hasCustomBackIcon -> "custom back icons are not supported"
            capabilities.hasCustomBackTint -> "custom back icon tints are not supported"
            !capabilities.scrollFlags.areSupportedFor(capabilities.type) ->
                "only medium enterAlways scroll flags are supported"
            else -> null
        }
}

internal fun StackHeaderConfigurationProviding.resolvedRenderer(): StackHeaderRenderer {
    val capabilities =
        StackHeaderRendererCapabilities(
            type = type,
            isTransparent = transparent,
            hasBackgroundColor = headerBackgroundColor != null,
            hasBackgroundSubview = backgroundSubview != null,
            hasCenterSubview = centerSubview != null,
            hasTrailingSubview = trailingSubview != null,
            hasToolbarMenuGroupDividers = toolbarMenuGroupDividerEnabled,
            hasCustomBackIcon = backButtonIcon != null,
            hasCustomBackTint =
                backButtonTintColorNormal != null ||
                    backButtonTintColorPressed != null ||
                    backButtonTintColorFocused != null,
            scrollFlags =
                StackHeaderScrollFlags(
                    scroll = scrollFlagScroll,
                    enterAlways = scrollFlagEnterAlways,
                    enterAlwaysCollapsed = scrollFlagEnterAlwaysCollapsed,
                    exitUntilCollapsed = scrollFlagExitUntilCollapsed,
                    snap = scrollFlagSnap,
                ),
        )
    val fallbackReason =
        StackHeaderRendererResolver.unsupportedReason(capabilities)
            ?: StackHeaderComposeActionPlanner.unsupportedReason(toolbarMenu)
    val resolution = StackHeaderRendererResolver.resolution(renderer, capabilities, fallbackReason)

    if (renderer == StackHeaderRenderer.COMPOSE && fallbackReason != null) {
        val message = "[RNScreens] Cannot use the Compose Stack header renderer: $fallbackReason."
        if (BuildConfig.DEBUG) {
            throw JSApplicationIllegalArgumentException(message)
        }
        if ((this as? StackHeaderConfig)?.recordRendererResolution(resolution) != false) {
            Log.w("RNScreens", "$message Falling back to the View renderer.")
        }
    } else {
        (this as? StackHeaderConfig)?.recordRendererResolution(resolution)
    }

    return resolution.actual
}
