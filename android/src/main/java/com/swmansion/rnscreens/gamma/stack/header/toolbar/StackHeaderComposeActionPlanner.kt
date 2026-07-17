package com.swmansion.rnscreens.gamma.stack.header.toolbar

/**
 * The deliberately small action vocabulary supported by the Compose app-bar renderer.
 *
 * Keeping this separate from the Compose view makes validation deterministic before a
 * screen is mounted and lets action updates retain their identity by the JS-provided id.
 */
internal object StackHeaderComposeActionPlanner {
    const val MAX_TRAILING_ACTIONS = 2

    data class Action(
        val item: StackHeaderToolbarMenuItemConfig,
    ) {
        val id get() = item.id
        val disabled get() = item.disabled
        val selected get() = item.initialToggleState
    }

    data class Plan(
        val direct: List<Action>,
        val overflow: List<Action>,
    )

    fun unsupportedReason(menu: StackHeaderToolbarMenuConfig): String? {
        if (menu.groups.isNotEmpty()) return "action groups are not supported"

        val ids = mutableSetOf<String>()
        menu.children.forEach { element ->
            if (element !is StackHeaderToolbarMenuElementConfig.MenuItem) {
                return "nested action menus are not supported"
            }
            val item = element.item
            if (!ids.add(item.id)) return "duplicate action id '${item.id}'"
            if (item.itemType == StackHeaderToolbarMenuItemType.TOGGLE) {
                return "toggle actions are not supported"
            }
            if (item.iconTintColorNormal != null ||
                item.iconTintColorPressed != null ||
                item.iconTintColorFocused != null ||
                item.iconTintColorDisabled != null
            ) {
                return "custom action icon tints are not supported"
            }
            if (item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.ALWAYS_WITH_TEXT ||
                item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.IF_ROOM_WITH_TEXT
            ) {
                return "direct action '${item.id}' must be icon-only"
            }
            if (!item.hidden &&
                (
                    item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.ALWAYS ||
                        item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.IF_ROOM
                )
            ) {
                if (item.icon == null) return "direct action '${item.id}' requires an icon"
                if (item.tooltipText.isNullOrBlank()) {
                    return "direct action '${item.id}' requires an accessibility label"
                }
            }
            if (!item.hidden && item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.NEVER && item.title.isNullOrBlank()) {
                return "overflow action '${item.id}' requires a title"
            }
        }

        val visibleAlways =
            menu.children
                .map { it.item }
                .count { !it.hidden && it.showAsAction == StackHeaderToolbarMenuItemShowAsAction.ALWAYS }
        if (visibleAlways > MAX_TRAILING_ACTIONS) {
            return "at most $MAX_TRAILING_ACTIONS always-visible actions are supported"
        }
        val hasAnotherVisibleAction =
            menu.children
                .map { it.item }
                .any { !it.hidden && it.showAsAction != StackHeaderToolbarMenuItemShowAsAction.ALWAYS }
        if (visibleAlways == MAX_TRAILING_ACTIONS && hasAnotherVisibleAction) {
            return "always-visible actions leave no room for the overflow action"
        }
        return null
    }

    fun plan(menu: StackHeaderToolbarMenuConfig): Plan {
        check(unsupportedReason(menu) == null) { "Cannot plan an unsupported Compose action menu." }

        val actions = menu.children.map { Action(it.item) }.filterNot { it.item.hidden }
        val direct = actions.filter { it.item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.ALWAYS }.toMutableList()
        val candidates = actions.filter { it.item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.IF_ROOM }
        val forcedOverflow = actions.filter { it.item.showAsAction == StackHeaderToolbarMenuItemShowAsAction.NEVER }

        // Reserve a trailing slot before accepting IF_ROOM actions when an IF_ROOM action will
        // overflow as well as when an action explicitly requests overflow. This means the same
        // ordered descriptor list always gives the same row and never exceeds the capacity.
        val willOverflow = forcedOverflow.isNotEmpty() || candidates.size > MAX_TRAILING_ACTIONS - direct.size
        val available =
            (MAX_TRAILING_ACTIONS - direct.size - if (willOverflow) 1 else 0).coerceAtLeast(0)
        direct += candidates.take(available)
        val overflow = candidates.drop(available) + forcedOverflow

        return Plan(direct = direct, overflow = overflow)
    }
}
