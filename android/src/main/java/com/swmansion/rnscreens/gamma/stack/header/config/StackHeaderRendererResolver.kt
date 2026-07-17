package com.swmansion.rnscreens.gamma.stack.header.config

import android.util.Log
import com.facebook.react.bridge.JSApplicationIllegalArgumentException
import com.swmansion.rnscreens.BuildConfig
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderComposeActionPlanner
import java.util.concurrent.atomic.AtomicBoolean

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
    ): StackHeaderRenderer {
        if (requested != StackHeaderRenderer.COMPOSE) {
            return StackHeaderRenderer.VIEW
        }

        return if (unsupportedReason(capabilities) == null && actionMenuUnsupportedReason == null) {
            StackHeaderRenderer.COMPOSE
        } else {
            StackHeaderRenderer.VIEW
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

private val didReportComposeFallback = AtomicBoolean(false)

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
    val reason =
        StackHeaderRendererResolver.unsupportedReason(capabilities)
            ?: StackHeaderComposeActionPlanner.unsupportedReason(toolbarMenu)

    if (renderer == StackHeaderRenderer.COMPOSE && reason != null) {
        val message = "[RNScreens] Cannot use the Compose Stack header renderer: $reason."
        if (BuildConfig.DEBUG) {
            throw JSApplicationIllegalArgumentException(message)
        }
        if (didReportComposeFallback.compareAndSet(false, true)) {
            Log.w("RNScreens", "$message Falling back to the View renderer.")
        }
    }

    return StackHeaderRendererResolver.resolve(renderer, capabilities, reason)
}
