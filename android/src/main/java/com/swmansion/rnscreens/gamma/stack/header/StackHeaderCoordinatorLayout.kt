package com.swmansion.rnscreens.gamma.stack.header

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import com.facebook.react.bridge.JSApplicationIllegalArgumentException
import com.google.android.material.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.swmansion.rnscreens.BuildConfig
import com.swmansion.rnscreens.gamma.stack.header.config.OnHeaderConfigurationAttachListener
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderConfigurationObserver
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderConfigurationProviding
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderDelegate
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderInvalidationFlags
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer
import com.swmansion.rnscreens.gamma.stack.header.config.resolvedRenderer
import com.swmansion.rnscreens.gamma.stack.header.subview.StackHeaderSubviewProviding
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuElementOptions
import com.swmansion.rnscreens.gamma.stack.header.toolbar.StackHeaderToolbarMenuGroupMetadata
import com.swmansion.rnscreens.gamma.stack.screen.StackScreen
import java.util.WeakHashMap

@SuppressLint("ViewConstructor")
internal class StackHeaderCoordinatorLayout(
    context: Context,
    internal val stackScreen: StackScreen,
    private val canNavigateBack: Boolean,
) : CoordinatorLayout(context) {
    // region Config attach / detach

    private var currentProvider: StackHeaderConfigurationProviding? = null
    private var currentDelegate: StackHeaderDelegate? = null

    // This callback is used to detect when header config is attached.
    // This allows us to configure the delegate for header config interactions.
    private val onHeaderConfigAttached =
        OnHeaderConfigurationAttachListener { provider, delegate ->
            handleHeaderConfigAttach(provider, delegate)
        }

    private fun handleHeaderConfigAttach(
        provider: StackHeaderConfigurationProviding?,
        delegate: StackHeaderDelegate?,
    ) {
        // Disconnect old config to prevent spurious updates from a detached config.
        currentProvider?.setConfigurationObserver(null)

        currentProvider = provider
        currentDelegate = delegate

        if (provider != null) {
            provider.setConfigurationObserver(configObserver)
            processUpdate(provider)
        } else {
            removeHeader()
        }
    }

    // endregion

    // region Configuration observer

    private val configObserver =
        object : StackHeaderConfigurationObserver {
            override fun onConfigChanged(config: StackHeaderConfigurationProviding) = processUpdate(config)

            override fun onMenuElementUpdated(
                id: String,
                options: StackHeaderToolbarMenuElementOptions,
            ) {
                val appBar = appBarLayout ?: return
                if (appBar is StackHeaderComposeAppBarLayout) {
                    appBar.applyMenuElementUpdate(id, options)?.let { reason ->
                        val message = "[RNScreens] Cannot apply Compose Stack header action update: $reason."
                        if (BuildConfig.DEBUG) {
                            throw JSApplicationIllegalArgumentException(message)
                        }
                        Log.w(TAG, "$message Keeping the last supported action state.")
                    }
                    return
                }
                if (appBar.renderer != StackHeaderRenderer.VIEW) return
                val toolbar = appBar.toolbar
                applicator.updateToolbarMenuElement(toolbar, toolbarMenuForwardIdMap, id, options)
                if (options.checked != null) {
                    handleGroupItemStateChange(toolbar, id, options.checked)
                }
            }
        }

    // endregion

    // region Layout callbacks

    private val appBarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { appBar, offset ->
            (appBar as? StackHeaderComposeAppBarLayout)?.onCoordinatorOffsetChanged(offset)
            onMaybeHeaderLayoutChanged()
        }

    private val appBarLayoutChangeListener =
        OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            onMaybeHeaderLayoutChanged()
        }

    private fun attachAppBarListeners(appBar: StackHeaderAppBarLayout) {
        appBar.addOnOffsetChangedListener(appBarOffsetListener)
        appBar.addOnLayoutChangeListener(appBarLayoutChangeListener)
    }

    private fun detachAppBarListeners(appBar: StackHeaderAppBarLayout) {
        appBar.removeOnOffsetChangedListener(appBarOffsetListener)
        appBar.removeOnLayoutChangeListener(appBarLayoutChangeListener)
    }

    private fun onMaybeHeaderLayoutChanged() {
        val delegate = currentDelegate ?: return
        val provider = currentProvider ?: return
        val appBar = appBarLayout ?: return

        // When config is transparent, the StackScreen is static so we need to offset the header
        // config by the offset of the AppBarLayout (which is 0 or is negative). When config is
        // opaque, the Screen always moves with the config, that's why we need to offset the
        // header config by the negative value of AppBarLayout's height.
        val configOffset = if (provider.transparent) appBar.top else appBar.top - appBar.bottom

        delegate.onHeaderFrameChanged(
            appBar.width,
            appBar.height,
            configOffset,
        )

        updateSubviewOffsets(appBar, provider)
    }

    private fun updateSubviewOffsets(
        appBar: StackHeaderAppBarLayout,
        config: StackHeaderConfigurationProviding,
    ) {
        config.leadingSubview?.let { updateSubviewOffset(it, appBar) }
        config.centerSubview?.let { updateSubviewOffset(it, appBar) }
        config.trailingSubview?.let { updateSubviewOffset(it, appBar) }
        config.backgroundSubview?.let { updateSubviewOffset(it, appBar) }
    }

    private fun updateSubviewOffset(
        subview: StackHeaderSubviewProviding,
        appBar: StackHeaderAppBarLayout,
    ) {
        val view = subview.view
        if (view.width == 0 && view.height == 0) return

        val appBarPos = IntArray(2)
        val subviewPos = IntArray(2)
        appBar.getLocationInWindow(appBarPos)
        view.getLocationInWindow(subviewPos)

        currentDelegate?.onSubviewOriginChanged(
            subview.type,
            x = subviewPos[0] - appBarPos[0],
            y = subviewPos[1] - appBarPos[1],
        )
    }

    // endregion

    // region Header updates

    private val wrappedContext =
        ContextThemeWrapper(
            context,
            R.style.Theme_Material3_DayNight_NoActionBar,
        )

    private val applicator = StackHeaderApplicator(wrappedContext)

    private var appBarLayout: StackHeaderAppBarLayout? = null
    private var isScreenActive = false
    private val composeProviderAccessibilityState = WeakHashMap<View, StackHeaderComposeProviderAccessibilityState>()
    private var hasLoggedComposeProviderScan = false
    private var isComposeProviderListenerRegistered = false
    private var isComposeProviderLayoutListenerRegistered = false
    private val composeProviderPreDrawListener =
        ViewTreeObserver.OnPreDrawListener {
            if (!isScreenActive) {
                reconcileTrackedComposeProviderAccessibility()
            }
            true
        }
    private val composeProviderLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            if (!isScreenActive) {
                // First clear any delegate that Compose reinstalled without replacing the view,
                // then discover providers introduced by a later Fabric mount.
                reconcileTrackedComposeProviderAccessibility()
                applyComposeProviderAccessibility()
            }
        }

    private var toolbarMenuForwardIdMap = emptyMap<String, Int>()
    private var toolbarMenuGroupMetadata = StackHeaderToolbarMenuGroupMetadata.EMPTY

    private val onNavigationIconClick: () -> Unit = ::navigateUp

    private fun navigateUp() {
        when (
            StackHeaderUpNavigation.resolve(
                canNavigateBack = canNavigateBack,
                preventNativeDismiss = stackScreen.isPreventNativeDismissEnabled,
            )
        ) {
            StackHeaderUpNavigation.Action.NO_OP -> Unit
            StackHeaderUpNavigation.Action.DISPATCH_PREVENTED -> stackScreen.onNativeDismissPrevented()
            StackHeaderUpNavigation.Action.POP_NATIVE_STACK ->
                stackScreen.getAssociatedFragment()?.let { fragment ->
                    // Use this screen's keyed transaction rather than the activity dispatcher: the
                    // latter can close the host app, and an unqualified pop can target a nested host.
                    StackHeaderUpNavigation.dispatchNativePop(stackScreen.screenKey) { screenKey ->
                        fragment.parentFragmentManager.popBackStack(
                            screenKey,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
                        )
                    }
                }
        }
    }

    private fun processUpdate(provider: StackHeaderConfigurationProviding) {
        val renderer = provider.resolvedRenderer()
        val needsRebuild =
            provider.invalidationFlags.needsRebuild ||
                appBarLayout?.renderer?.let { it != renderer } == true
        if (needsRebuild) {
            resetHeader()
            if (provider.hidden) {
                removeContentBehavior()
                requestLayout()
                provider.clearInvalidationFlags(StackHeaderInvalidationFlags.ALL)
                return
            }

            val appBar =
                applicator.rebuild(
                    this,
                    provider,
                    renderer,
                    canNavigateBack,
                    onNavigationIconClick,
                    onMenuItemClick = { id -> currentDelegate?.onMenuItemClicked(id) },
                )
            appBarLayout = appBar
            applyScreenActivity(appBar)
            attachAppBarListeners(appBar)

            // If config needs to be rebuilt, all other flags must be invalidated as well.
            provider.clearInvalidationFlags(
                StackHeaderInvalidationFlags.STRUCTURE or StackHeaderInvalidationFlags.SUBVIEWS,
            )
        }

        val appBar = appBarLayout
        if (appBar != null) {
            if (needsRebuild || provider.invalidationFlags.containsAny(StackHeaderInvalidationFlags.TITLE)) {
                applicator.applyTitle(appBar, provider)
                provider.clearInvalidationFlags(StackHeaderInvalidationFlags.TITLE)
            }

            if (needsRebuild || provider.invalidationFlags.containsAny(StackHeaderInvalidationFlags.BACK_BUTTON)) {
                applicator.applyBackButton(appBar, provider, canNavigateBack, onNavigationIconClick)
                provider.clearInvalidationFlags(StackHeaderInvalidationFlags.BACK_BUTTON)
            }

            if (needsRebuild || provider.invalidationFlags.containsAny(StackHeaderInvalidationFlags.SCROLL_FLAGS)) {
                applicator.applyScrollFlags(appBar, provider)
                provider.clearInvalidationFlags(StackHeaderInvalidationFlags.SCROLL_FLAGS)
            }

            if (provider.invalidationFlags.containsAny(StackHeaderInvalidationFlags.TOOLBAR_MENU)) {
                if (appBar is StackHeaderComposeAppBarLayout) {
                    appBar.applyToolbarMenu(provider.toolbarMenu)
                    provider.clearInvalidationFlags(StackHeaderInvalidationFlags.TOOLBAR_MENU)
                } else if (appBar.renderer != StackHeaderRenderer.VIEW) {
                    provider.clearInvalidationFlags(StackHeaderInvalidationFlags.TOOLBAR_MENU)
                    onMaybeHeaderLayoutChanged()
                    return
                }
                val (forwardIdMap, reverseIdMap) =
                    applicator.generateToolbarMenuItemMappings(
                        provider.toolbarMenu,
                    )
                val forwardGroupIdMap =
                    applicator.generateToolbarMenuGroupMappings(
                        provider.toolbarMenu,
                    )
                val groupMetadata =
                    applicator.computeGroupMetadata(
                        provider.toolbarMenu,
                    )

                applicator.validateRadioInitialSelection(provider.toolbarMenu)

                toolbarMenuForwardIdMap = forwardIdMap
                toolbarMenuGroupMetadata = groupMetadata

                applicator.rebuildToolbarMenu(
                    appBar.toolbar,
                    provider.toolbarMenu,
                    forwardIdMap,
                    reverseIdMap,
                    forwardGroupIdMap,
                    groupDividerEnabled = provider.toolbarMenuGroupDividerEnabled,
                    onItemClicked = { id, menuItem ->
                        if (menuItem.isCheckable) {
                            handleGroupItemStateChange(appBar.toolbar, id)
                        } else {
                            currentDelegate?.onMenuItemClicked(id)
                        }
                    },
                )

                provider.clearInvalidationFlags(StackHeaderInvalidationFlags.TOOLBAR_MENU)
            }
        }

        onMaybeHeaderLayoutChanged()
    }

    // endregion

    /**
     * Stack fragments stay mounted for native transitions. An inactive screen keeps drawing for the
     * transition surface, but its header and content are removed from the accessibility tree.
     */
    internal fun setScreenActive(isActive: Boolean) {
        if (isActive) {
            // Do not let an inactive reconciliation clear a delegate while it is restored.
            removeComposeProviderPreDrawListener()
            removeComposeProviderLayoutListener()
        }
        isScreenActive = isActive
        val accessibilityTargets = StackHeaderScreenAccessibilityTargets.resolve(isActive)
        // CoordinatorLayout exposes itself as a ScrollView accessibility root. It is the retained
        // fragment boundary that owns Expo UI's Compose virtual tree, so make it inactive before
        // its descendants. This keeps the transition surface drawable while pruning that tree.
        importantForAccessibility = accessibilityTargets.coordinatorImportance
        // These roots still cover ordinary React Native content and any content mounted after a
        // fragment has resigned its top position.
        stackScreen.importantForAccessibility = accessibilityTargets.stackScreenImportance
        stackScreenWrapper.importantForAccessibility = accessibilityTargets.wrapperImportance
        applyComposeProviderAccessibility()
        updateComposeProviderPreDrawListener()
        updateComposeProviderLayoutListener()
        appBarLayout?.let(::applyScreenActivity)
    }

    /**
     * Compose exposes its semantics through virtual children of AndroidComposeView, the internal
     * child hosted by ComposeView. Android's ancestor `NO_HIDE_DESCENDANTS` flags do not prune
     * that provider, so the provider itself must receive the inactive state. Preserve its original
     * value so React/Expo ownership is restored when this fragment returns to the top.
     */
    private fun applyComposeProviderAccessibility(): Int {
        if (isScreenActive) {
            removeComposeProviderPreDrawListener()
            removeComposeProviderLayoutListener()
            var activeProviderCount = 0
            composeProviderAccessibilityState.entries.toList().forEach { (provider, state) ->
                provider.importantForAccessibility = state.importance
                ViewCompat.setAccessibilityDelegate(provider, state.delegate)
                provider.invalidate()
                provider.parent?.notifySubtreeAccessibilityStateChanged(
                    provider,
                    provider,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
                )
                val hasNodeProvider = ViewCompat.getAccessibilityNodeProvider(provider) != null
                if (hasNodeProvider) {
                    activeProviderCount += 1
                }
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "[RNScreens] Restored Compose semantics provider " +
                            "delegate=${StackHeaderComposeSemanticsProvider.describeDelegate(state.delegate)}, " +
                            "nodeProvider=$hasNodeProvider.",
                    )
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "[RNScreens] Restored $activeProviderCount/${composeProviderAccessibilityState.size} " +
                        "Compose semantics provider(s).",
                )
            }
            composeProviderAccessibilityState.clear()
            hasLoggedComposeProviderScan = false
            return 0
        }

        val inactiveImportance = StackHeaderComposeProviderActivity.INACTIVE.accessibilityImportance
        var newlyIsolatedProviderCount = 0
        var capturedDelegateCount = 0
        visitComposeSemanticsProviders(this) { provider ->
            val currentImportance = provider.importantForAccessibility
            val currentDelegate = ViewCompat.getAccessibilityDelegate(provider)
            val hasNodeProvider = ViewCompat.getAccessibilityNodeProvider(provider) != null
            val previousState = composeProviderAccessibilityState[provider]
            if (previousState == null && !composeProviderAccessibilityState.containsKey(provider)) {
                composeProviderAccessibilityState[provider] =
                    StackHeaderComposeProviderAccessibilityState(
                        importance = currentImportance,
                        delegate =
                            if (StackHeaderComposeSemanticsProvider.shouldCaptureDelegate(currentDelegate, hasNodeProvider)) {
                                capturedDelegateCount += 1
                                currentDelegate
                            } else {
                                null
                            },
                    )
                newlyIsolatedProviderCount += 1
            } else if (
                previousState?.delegate == null &&
                StackHeaderComposeSemanticsProvider.shouldCaptureDelegate(currentDelegate, hasNodeProvider)
            ) {
                // Keep the first delegate that actually exposes Compose's node provider. Later
                // inactive re-installations may be temporary wrappers and must not replace it.
                composeProviderAccessibilityState[provider] =
                    checkNotNull(previousState).copy(
                        delegate = currentDelegate,
                    )
                capturedDelegateCount += 1
            }
            provider.importantForAccessibility = inactiveImportance
            // AndroidComposeView's delegate owns the virtual node provider. Reapply this on every
            // reconciliation in case Compose installs it again while Fabric finishes mounting.
            ViewCompat.setAccessibilityDelegate(provider, null)
        }
        if (
            BuildConfig.DEBUG &&
            (!hasLoggedComposeProviderScan || newlyIsolatedProviderCount > 0 || capturedDelegateCount > 0)
        ) {
            Log.d(
                TAG,
                "[RNScreens] Compose semantics scan found " +
                    "${composeProviderAccessibilityState.size} provider(s); isolated " +
                    "$newlyIsolatedProviderCount new provider(s), captured $capturedDelegateCount valid delegate(s), " +
                    "and cleared their delegates.",
            )
            hasLoggedComposeProviderScan = true
        }
        return newlyIsolatedProviderCount
    }

    /**
     * Compose can reinstall a provider delegate without changing bounds. Checking the tracked
     * providers on pre-draw keeps the inactive boundary durable without repeatedly walking the
     * entire retained fragment on frames belonging to another screen.
     */
    private fun reconcileTrackedComposeProviderAccessibility() {
        val inactiveImportance = StackHeaderComposeProviderActivity.INACTIVE.accessibilityImportance
        var capturedDelegateCount = 0
        composeProviderAccessibilityState.entries.toList().forEach { (provider, previousState) ->
            val currentImportance = provider.importantForAccessibility
            val currentDelegate = ViewCompat.getAccessibilityDelegate(provider)
            val hasNodeProvider = ViewCompat.getAccessibilityNodeProvider(provider) != null
            if (
                previousState.delegate == null &&
                StackHeaderComposeSemanticsProvider.shouldCaptureDelegate(currentDelegate, hasNodeProvider)
            ) {
                composeProviderAccessibilityState[provider] =
                    previousState.copy(
                        importance =
                            if (currentImportance == inactiveImportance) {
                                previousState.importance
                            } else {
                                currentImportance
                            },
                        delegate = currentDelegate,
                    )
                capturedDelegateCount += 1
            }
            provider.importantForAccessibility = inactiveImportance
            ViewCompat.setAccessibilityDelegate(provider, null)
        }
        if (capturedDelegateCount > 0 && BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "[RNScreens] Captured $capturedDelegateCount inactive Compose semantics delegate(s).",
            )
        }
    }

    private fun updateComposeProviderPreDrawListener() {
        if (!isAttachedToWindow) return

        if (isScreenActive) {
            removeComposeProviderPreDrawListener()
        } else if (!isComposeProviderListenerRegistered) {
            viewTreeObserver.addOnPreDrawListener(composeProviderPreDrawListener)
            isComposeProviderListenerRegistered = true
        }
    }

    private fun removeComposeProviderPreDrawListener() {
        if (isComposeProviderListenerRegistered && viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(composeProviderPreDrawListener)
        }
        isComposeProviderListenerRegistered = false
    }

    private fun updateComposeProviderLayoutListener() {
        if (!isAttachedToWindow) return

        if (isScreenActive) {
            removeComposeProviderLayoutListener()
        } else if (!isComposeProviderLayoutListenerRegistered) {
            viewTreeObserver.addOnGlobalLayoutListener(composeProviderLayoutListener)
            isComposeProviderLayoutListenerRegistered = true
        }
    }

    private fun removeComposeProviderLayoutListener() {
        if (isComposeProviderLayoutListenerRegistered && viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalLayoutListener(composeProviderLayoutListener)
        }
        isComposeProviderLayoutListenerRegistered = false
    }

    private fun visitComposeSemanticsProviders(
        view: View,
        onComposeProvider: (View) -> Unit,
    ) {
        if (StackHeaderComposeSemanticsProvider.isProviderClassName(view.javaClass.name)) {
            onComposeProvider(view)
        }
        (view as? ViewGroup)?.let { viewGroup ->
            for (index in 0 until viewGroup.childCount) {
                visitComposeSemanticsProviders(viewGroup.getChildAt(index), onComposeProvider)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyComposeProviderAccessibility()
        updateComposeProviderPreDrawListener()
        updateComposeProviderLayoutListener()
    }

    override fun onDetachedFromWindow() {
        removeComposeProviderPreDrawListener()
        removeComposeProviderLayoutListener()
        super.onDetachedFromWindow()
    }

    private fun applyScreenActivity(appBar: StackHeaderAppBarLayout) {
        val activity = StackHeaderAppBarActivity.resolve(appBar.renderer, isScreenActive)
        appBar.importantForAccessibility = activity.accessibilityImportance
        if (appBar.renderer == StackHeaderRenderer.COMPOSE) {
            (appBar as StackHeaderComposeAppBarLayout).setScreenActive(isScreenActive)
            appBar.visibility = activity.viewVisibility
        }
    }

    // region Group selection

    private fun handleGroupItemStateChange(
        toolbar: MaterialToolbar,
        itemId: String,
        explicitCheckedValue: Boolean? = null,
    ) {
        val groupId = toolbarMenuGroupMetadata.itemGroupMap[itemId] ?: return
        val singleSelection = toolbarMenuGroupMetadata.groupSingleSelection[groupId] ?: return
        val intId = toolbarMenuForwardIdMap[itemId] ?: return
        val menuItem = toolbar.menu.findItem(intId) ?: return

        if (singleSelection && explicitCheckedValue == false) {
            Log.w(
                TAG,
                "[RNScreens] Cannot uncheck item '$itemId' in single-selection group '$groupId'. " +
                    "Check a different item instead.",
            )
            return
        }

        val newChecked =
            if (singleSelection) {
                true
            } else {
                explicitCheckedValue ?: !menuItem.isChecked
            }
        if (menuItem.isChecked == newChecked) return
        menuItem.isChecked = newChecked

        val selectedIds = collectSelectedIds(toolbar, groupId)
        currentDelegate?.onGroupSelectionChanged(groupId, selectedIds)
    }

    private fun collectSelectedIds(
        toolbar: MaterialToolbar,
        groupId: String,
    ): List<String> =
        toolbarMenuGroupMetadata
            .groupMemberItems[groupId]
            .orEmpty()
            .filter { memberId ->
                val intId = toolbarMenuForwardIdMap[memberId] ?: return@filter false
                toolbar.menu.findItem(intId)?.isChecked == true
            }

    // endregion

    // region Header lifecycle

    private fun resetHeader() {
        appBarLayout?.let {
            detachAppBarListeners(it)
            removeView(it)
        }
        appBarLayout = null
        toolbarMenuForwardIdMap = emptyMap()
        toolbarMenuGroupMetadata = StackHeaderToolbarMenuGroupMetadata.EMPTY
    }

    private fun removeHeader() {
        resetHeader()
        removeContentBehavior()
        requestLayout()
    }

    // endregion

    // region Content behavior

    internal fun setContentBehavior() {
        val params = stackScreenWrapper.layoutParams as LayoutParams
        if (params.behavior == null) {
            params.behavior =
                StackHeaderScrollingViewBehavior { contentTop, _ ->
                    stackScreen.onContentYOriginChanged(contentTop)
                }
            stackScreenWrapper.layoutParams = params
            stackScreenWrapper.requestLayout()
        }
    }

    internal fun removeContentBehavior() {
        val params = stackScreenWrapper.layoutParams as LayoutParams
        if (params.behavior != null) {
            params.behavior = null
            stackScreenWrapper.layoutParams = params
            stackScreen.onContentYOriginChanged(0)
            stackScreenWrapper.requestLayout()
        }
    }

    // endregion

    // region Init

    internal var stackScreenWrapper: FrameLayout

    init {
        // Needed when Transition API is in use to ensure that shadows do not disappear,
        // views do not jump around the screen and whole subtree is animated as a whole.
        isTransitionGroup = true

        // Due to how we're synchronizing native & Yoga layout (via contentOriginOffset on
        // StackScreen), we can't use StackScreen directly as a child of CoordinatorLayout
        // because SurfaceMountingManager will override Y offset (that depends on the header
        // height) with Y=0. If we wrap StackScreen in another view, as Y is relative to
        // parent view, value set by Yoga will be correct.
        stackScreenWrapper = FrameLayout(context).apply { addView(stackScreen) }
        addView(
            stackScreenWrapper,
            LayoutParams(MATCH_PARENT, MATCH_PARENT),
        )

        stackScreen.registerHeaderConfigAttachListener(onHeaderConfigAttached)
    }

    // endregion

    // region Teardown

    internal fun tearDown() {
        removeComposeProviderPreDrawListener()
        removeComposeProviderLayoutListener()
        composeProviderAccessibilityState.clear()
        removeHeader()

        stackScreenWrapper.removeView(stackScreen)

        currentProvider?.setConfigurationObserver(null)
        currentProvider = null
        currentDelegate = null

        stackScreen.clearHeaderConfigAttachListener()
    }

    // endregion

    companion object {
        private const val TAG = "StackHeaderCoordinatorLayout"
    }
}

internal object StackHeaderUpNavigation {
    enum class Action {
        NO_OP,
        DISPATCH_PREVENTED,
        POP_NATIVE_STACK,
    }

    fun resolve(
        canNavigateBack: Boolean,
        preventNativeDismiss: Boolean,
    ): Action =
        when {
            !canNavigateBack -> Action.NO_OP
            preventNativeDismiss -> Action.DISPATCH_PREVENTED
            else -> Action.POP_NATIVE_STACK
        }

    /** Dispatches only the current Stack v5 screen's named back-stack transaction. */
    fun dispatchNativePop(
        screenKey: String?,
        popBackStack: (String) -> Unit,
    ): Boolean {
        val key = screenKey ?: return false
        popBackStack(key)
        return true
    }
}

internal enum class StackHeaderComposeActivity(
    val viewVisibility: Int,
    val accessibilityImportance: Int,
) {
    VISIBLE(View.VISIBLE, View.IMPORTANT_FOR_ACCESSIBILITY_AUTO),
    HIDDEN(View.INVISIBLE, View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS),
    ;

    companion object {
        fun resolve(isActive: Boolean): StackHeaderComposeActivity = if (isActive) VISIBLE else HIDDEN
    }
}

internal enum class StackHeaderScreenActivity(
    val accessibilityImportance: Int,
) {
    ACTIVE(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO),
    INACTIVE(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS),
    ;

    companion object {
        fun resolve(isActive: Boolean): StackHeaderScreenActivity = if (isActive) ACTIVE else INACTIVE
    }
}

internal enum class StackHeaderComposeProviderActivity(
    val accessibilityImportance: Int,
) {
    ACTIVE(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO),
    INACTIVE(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS),
}

internal data class StackHeaderComposeProviderAccessibilityState(
    val importance: Int,
    val delegate: AccessibilityDelegateCompat?,
)

/** AndroidComposeView owns Jetpack Compose's virtual accessibility-node provider. */
internal object StackHeaderComposeSemanticsProvider {
    // Matches the AndroidX Compose 1.10.6 implementation pinned by this module's Gradle defaults.
    private const val ANDROID_COMPOSE_VIEW_CLASS_NAME = "androidx.compose.ui.platform.AndroidComposeView"

    fun isProviderClassName(className: String): Boolean = className == ANDROID_COMPOSE_VIEW_CLASS_NAME

    fun shouldClearDelegate(isActive: Boolean): Boolean = !isActive

    fun shouldCaptureDelegate(
        delegate: AccessibilityDelegateCompat?,
        hasNodeProvider: Boolean,
    ): Boolean = delegate != null && hasNodeProvider

    fun describeDelegate(delegate: AccessibilityDelegateCompat?): String =
        delegate?.let { "${it.javaClass.name}@${System.identityHashCode(it)}" } ?: "null"
}

internal data class StackHeaderScreenAccessibilityTargets(
    val coordinatorImportance: Int,
    val stackScreenImportance: Int,
    val wrapperImportance: Int,
) {
    companion object {
        fun resolve(isActive: Boolean): StackHeaderScreenAccessibilityTargets {
            val importance = StackHeaderScreenActivity.resolve(isActive).accessibilityImportance
            return StackHeaderScreenAccessibilityTargets(importance, importance, importance)
        }
    }
}

internal data class StackHeaderAppBarActivity(
    val viewVisibility: Int,
    val accessibilityImportance: Int,
) {
    companion object {
        fun resolve(
            renderer: StackHeaderRenderer,
            isActive: Boolean,
        ): StackHeaderAppBarActivity {
            val accessibilityImportance = StackHeaderScreenActivity.resolve(isActive).accessibilityImportance
            val viewVisibility =
                if (renderer == StackHeaderRenderer.COMPOSE) {
                    StackHeaderComposeActivity.resolve(isActive).viewVisibility
                } else {
                    // Retained View headers preserve their existing transition visuals.
                    View.VISIBLE
                }
            return StackHeaderAppBarActivity(viewVisibility, accessibilityImportance)
        }
    }
}
