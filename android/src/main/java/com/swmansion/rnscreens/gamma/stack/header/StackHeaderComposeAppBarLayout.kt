@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.swmansion.rnscreens.gamma.stack.header

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.swmansion.rnscreens.ext.detachFromCurrentParent
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderConfigurationProviding
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderType
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderComposeActionPlanner
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuConfig
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuElementConfig
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuElementOptions
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuItemConfig
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarUpdate
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A navigation-owned Compose app bar. CoordinatorLayout remains the nested-scroll authority:
 * it changes this AppBarLayout's offset from the RN scrolling child, and that offset is copied
 * as a normalized collapse fraction to Material3's [TopAppBarState] on the Android UI thread.
 * No scroll progress crosses the JS bridge and recomposition never replaces this view or its
 * per-screen state.
 */
internal class StackHeaderComposeAppBarLayout(
    context: Context,
    private val type: StackHeaderType,
) : StackHeaderAppBarLayout(context) {
    override val renderer = StackHeaderRenderer.COMPOSE

    // Stack v5 still accesses a Toolbar for its View path. Compose owns the rendered header,
    // so this placeholder is intentionally never attached.
    override val toolbar = MaterialToolbar(context)

    private var title by mutableStateOf("")
    private var leadingView by mutableStateOf<View?>(null)
    private var showUpButton by mutableStateOf(false)
    private var onNavigationIconClick by mutableStateOf<() -> Unit>({})
    private var toolbarMenu by mutableStateOf(StackHeaderToolbarMenuConfig(emptyList(), emptyList()))
    private var onMenuItemClick by mutableStateOf<(String) -> Unit>({})
    private var mediumTopAppBarState: TopAppBarState? = null
    private var coordinatorOffsetPx = 0
    private var topInsetPx by mutableStateOf(0)

    private val composeView =
        ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val dark = isSystemInDarkTheme()
                val colorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    } else {
                        if (dark) darkColorScheme() else lightColorScheme()
                    }
                val mediumScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                val mediumWindowInsets =
                    WindowInsets(
                        top = StackHeaderMediumAppBarMetrics.collapsedRowTopPx(topInsetPx),
                    )

                LaunchedEffect(mediumScrollBehavior.state.heightOffsetLimit) {
                    if (type == StackHeaderType.MEDIUM) {
                        mediumTopAppBarState = mediumScrollBehavior.state
                        synchronizeMediumTopAppBarOffset()
                    }
                }

                MaterialExpressiveTheme(colorScheme = colorScheme) {
                    when (type) {
                        StackHeaderType.SMALL ->
                            TopAppBar(
                                title = { titleContent() },
                                navigationIcon = { navigationIconContent() },
                                actions = { actionsContent() },
                            )

                        StackHeaderType.MEDIUM ->
                            MediumTopAppBar(
                                title = { titleContent() },
                                navigationIcon = { navigationIconContent() },
                                actions = { actionsContent() },
                                windowInsets = mediumWindowInsets,
                                scrollBehavior = mediumScrollBehavior,
                            )

                        StackHeaderType.LARGE -> error("Compose does not support large app bars.")
                    }
                }
            }
        }

    private val appBarContent: View =
        if (type == StackHeaderType.MEDIUM) {
            StackHeaderMediumAppBarContainer(context) { topInsetPx = it }.apply {
                addView(composeView, FrameLayout.LayoutParams(MATCH_PARENT, LayoutParams.WRAP_CONTENT))
                layoutParams =
                    AppBarLayout.LayoutParams(MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        scrollFlags =
                            StackHeaderMediumAppBarContract.scrollingFlags
                    }
            }
        } else {
            composeView.apply {
                layoutParams = AppBarLayout.LayoutParams(MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
        }

    init {
        fitsSystemWindows = false
        addView(appBarContent)
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            synchronizeMediumTopAppBarOffset()
        }
    }

    fun applyConfiguration(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
        onMenuItemClick: (String) -> Unit,
    ) {
        title = config.title
        toolbarMenu = config.toolbarMenu
        this.onMenuItemClick = onMenuItemClick
        updateNavigation(config, canNavigateBack, onNavigationIconClick)
    }

    fun applyTitle(title: String) {
        this.title = title
    }

    fun applyBackButton(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
    ) {
        updateNavigation(config, canNavigateBack, onNavigationIconClick)
    }

    fun applyToolbarMenu(menu: StackHeaderToolbarMenuConfig) {
        toolbarMenu = menu
    }

    /** Returns a validation reason without mutating the mounted action row when rejected. */
    fun applyMenuElementUpdate(
        id: String,
        options: StackHeaderToolbarMenuElementOptions,
    ): String? {
        val updated = toolbarMenu.updatingItem(id, options)
        val reason = StackHeaderComposeActionPlanner.unsupportedReason(updated)
        if (reason == null) {
            toolbarMenu = updated
        }
        return reason
    }

    fun onCoordinatorOffsetChanged(offset: Int) {
        coordinatorOffsetPx = offset
        if (type == StackHeaderType.MEDIUM) {
            // AppBarLayout translates the entire child while collapsing. Counter-translate the
            // Compose content so its inset-safe small row stays in screen coordinates; the
            // parent continues to clip away the expanded portion as its bottom moves upward.
            composeView.translationY =
                StackHeaderMediumAppBarMetrics.composeContentTranslationYPx(offset).toFloat()
        }
        synchronizeMediumTopAppBarOffset()
    }

    private fun synchronizeMediumTopAppBarOffset() {
        val state = mediumTopAppBarState ?: return
        // AppBarLayout and Material3 reserve different collapsed heights. Drive Material3 by
        // the native collapse fraction so its expanded and collapsed states land together.
        state.heightOffset =
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = coordinatorOffsetPx,
                appBarTotalScrollRangePx = totalScrollRange,
                composeHeightOffsetLimitPx = state.heightOffsetLimit,
            )
    }

    @androidx.compose.runtime.Composable
    private fun titleContent() {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @androidx.compose.runtime.Composable
    private fun navigationIconContent() {
        val customLeadingView = leadingView
        when {
            customLeadingView != null ->
                AndroidView(
                    factory = {
                        customLeadingView.detachFromCurrentParent()
                        customLeadingView
                    },
                )

            showUpButton ->
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    )
                }
        }
    }

    @androidx.compose.runtime.Composable
    private fun actionsContent() {
        val plan = StackHeaderComposeActionPlanner.plan(toolbarMenu)
        plan.direct.forEach { action ->
            key(action.id) { composeHeaderAction(action.item, onMenuItemClick) }
        }
        if (plan.overflow.isNotEmpty()) {
            composeHeaderOverflow(plan.overflow.map { it.item }, onMenuItemClick)
        }
    }

    private fun updateNavigation(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
    ) {
        leadingView = config.leadingSubview?.view
        showUpButton = leadingView == null && canNavigateBack && !config.backButtonHidden
        this.onNavigationIconClick = onNavigationIconClick
    }
}

/**
 * AppBarLayout calculates its total scroll range from its direct child's minimum height.
 * ComposeView may replace its framework minimum during measurement, so use a View container
 * with an explicit collapsed Material small-bar height plus the consumed top inset.
 */
private class StackHeaderMediumAppBarContainer(
    context: Context,
    private val onTopInsetChanged: (Int) -> Unit,
) : FrameLayout(context) {
    private var topInsetPx = 0

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val insetTypes = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            val dispatchedTopInset =
                insets
                    .getInsetsIgnoringVisibility(insetTypes)
                    .top
            val rootTopInset =
                ViewCompat
                    .getRootWindowInsets(this)
                    ?.getInsetsIgnoringVisibility(insetTypes)
                    ?.top ?: 0
            val nextTopInset =
                StackHeaderMediumAppBarMetrics.retainedTopInsetPx(
                    previousTopInsetPx = topInsetPx,
                    dispatchedTopInsetPx = dispatchedTopInset,
                    rootTopInsetPx = rootTopInset,
                )
            if (topInsetPx != nextTopInset) {
                topInsetPx = nextTopInset
                onTopInsetChanged(topInsetPx)
                requestLayout()
            }
            insets
        }
    }

    override fun getMinimumHeight(): Int = StackHeaderMediumAppBarMetrics.collapsedHeightPx(resources.displayMetrics.density, topInsetPx)
}

internal object StackHeaderMediumAppBarMetrics {
    private const val COLLAPSED_HEIGHT_DP = 64

    fun collapsedHeightPx(
        density: Float,
        topInsetPx: Int,
    ): Int = (COLLAPSED_HEIGHT_DP * density).roundToInt() + collapsedRowTopPx(topInsetPx)

    /** The collapsed row must begin below the system status/cutout area in screen coordinates. */
    fun collapsedRowTopPx(topInsetPx: Int): Int = max(0, topInsetPx)

    /**
     * AppBarLayout can consume its descendant's insets while it translates. The root inset stays
     * in screen coordinates, so preserve whichever source still reports the system-safe top.
     */
    fun stableTopInsetPx(
        dispatchedTopInsetPx: Int,
        rootTopInsetPx: Int,
    ): Int = max(collapsedRowTopPx(dispatchedTopInsetPx), collapsedRowTopPx(rootTopInsetPx))

    /**
     * AppBarLayout can report zero from every descendant inset source after collapsing. Keep the
     * last safe top for this header instance; rebuilding the header starts a fresh inset lifetime.
     */
    fun retainedTopInsetPx(
        previousTopInsetPx: Int,
        dispatchedTopInsetPx: Int,
        rootTopInsetPx: Int,
    ): Int = max(collapsedRowTopPx(previousTopInsetPx), stableTopInsetPx(dispatchedTopInsetPx, rootTopInsetPx))

    /** Keeps the Compose small row pinned while AppBarLayout translates its direct child. */
    fun composeContentTranslationYPx(coordinatorOffsetPx: Int): Int = max(0, -coordinatorOffsetPx)

    fun totalScrollRangePx(
        expandedHeightPx: Int,
        density: Float,
        topInsetPx: Int,
    ): Int = max(0, expandedHeightPx - collapsedHeightPx(density, topInsetPx))

    fun composeHeightOffset(
        coordinatorOffsetPx: Int,
        appBarTotalScrollRangePx: Int,
        composeHeightOffsetLimitPx: Float,
    ): Float {
        if (appBarTotalScrollRangePx <= 0) {
            return 0f
        }
        val collapsedFraction =
            (-coordinatorOffsetPx.toFloat() / appBarTotalScrollRangePx).coerceIn(0f, 1f)
        return composeHeightOffsetLimitPx * collapsedFraction
    }
}

/**
 * EXIT_UNTIL_COLLAPSED is a View-system implementation detail: AppBarLayout only subtracts
 * its direct child's minimum height from totalScrollRange when this flag is set. Compose still
 * exposes the public semantic profile as enter-always and deliberately does not request snap.
 */
internal object StackHeaderMediumAppBarContract {
    val scrollingFlags: Int =
        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
            AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
}

@androidx.compose.runtime.Composable
private fun composeHeaderAction(
    item: StackHeaderToolbarMenuItemConfig,
    onClick: (String) -> Unit,
) {
    val contentColor = LocalContentColor.current
    IconButton(
        onClick = { onClick(item.id) },
        enabled = !item.disabled,
        modifier =
            Modifier.semantics {
                contentDescription = item.tooltipText.orEmpty()
                selected = item.initialToggleState
            },
    ) {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = {
                it.setImageDrawable(item.icon)
                ImageViewCompat.setImageTintList(
                    it,
                    ColorStateList.valueOf(contentColor.toArgb()),
                )
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun composeHeaderOverflow(
    items: List<StackHeaderToolbarMenuItemConfig>,
    onClick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        items.forEach { item ->
            key(item.id) {
                DropdownMenuItem(
                    text = { Text(item.title.orEmpty()) },
                    onClick = {
                        expanded = false
                        onClick(item.id)
                    },
                    enabled = !item.disabled,
                )
            }
        }
    }
}

private fun StackHeaderToolbarMenuConfig.updatingItem(
    id: String,
    options: StackHeaderToolbarMenuElementOptions,
): StackHeaderToolbarMenuConfig {
    var changed = false
    val updatedChildren =
        children.map { element ->
            if (element !is StackHeaderToolbarMenuElementConfig.MenuItem || element.item.id != id) {
                element
            } else {
                changed = true
                StackHeaderToolbarMenuElementConfig.MenuItem(element.item.with(options))
            }
        }
    return if (changed) copy(children = updatedChildren) else this
}

private fun StackHeaderToolbarMenuItemConfig.with(options: StackHeaderToolbarMenuElementOptions): StackHeaderToolbarMenuItemConfig =
    copy(
        title = options.title.update(title),
        titleCondensed = options.titleCondensed.update(titleCondensed),
        tooltipText = options.tooltipText.update(tooltipText),
        hidden = options.hidden ?: hidden,
        disabled = options.disabled ?: disabled,
        showAsAction = options.showAsAction ?: showAsAction,
        icon = options.icon.update(icon),
        iconTintColorNormal = options.iconTintColorNormal.update(iconTintColorNormal),
        iconTintColorPressed = options.iconTintColorPressed.update(iconTintColorPressed),
        iconTintColorFocused = options.iconTintColorFocused.update(iconTintColorFocused),
        iconTintColorDisabled = options.iconTintColorDisabled.update(iconTintColorDisabled),
        initialToggleState = options.checked ?: initialToggleState,
    )

private fun <T> StackHeaderToolbarUpdate<T>?.update(current: T?): T? =
    when (this) {
        null -> current
        StackHeaderToolbarUpdate.Reset -> null
        is StackHeaderToolbarUpdate.Set -> value
    }
