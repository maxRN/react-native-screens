package com.swmansion.rnscreens.gamma.stack.header.toolbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackHeaderComposeActionPlannerTest {
    @Test
    fun `keeps stable ids while action properties change`() {
        val initial = action(id = "bookmark", showAsAction = StackHeaderToolbarMenuItemShowAsAction.ALWAYS)
        val updated = initial.copy(disabled = true, initialToggleState = true)

        val plan = StackHeaderComposeActionPlanner.plan(menu(initial))
        val changedPlan = StackHeaderComposeActionPlanner.plan(menu(updated))

        assertEquals(listOf("bookmark"), plan.direct.map { it.id })
        assertEquals(listOf("bookmark"), changedPlan.direct.map { it.id })
        assertTrue(changedPlan.direct.single().disabled)
        assertTrue(changedPlan.direct.single().selected)
    }

    @Test
    fun `reserves one trailing slot for overflow and places ifRoom actions deterministically`() {
        val plan =
            StackHeaderComposeActionPlanner.plan(
                menu(
                    action("always", StackHeaderToolbarMenuItemShowAsAction.ALWAYS),
                    action("first", StackHeaderToolbarMenuItemShowAsAction.IF_ROOM),
                    action("second", StackHeaderToolbarMenuItemShowAsAction.IF_ROOM),
                    action("overflow", StackHeaderToolbarMenuItemShowAsAction.NEVER, icon = false),
                ),
            )

        assertEquals(listOf("always"), plan.direct.map { it.id })
        assertEquals(listOf("first", "second", "overflow"), plan.overflow.map { it.id })
    }

    @Test
    fun `reserves a slot when an ifRoom action overflows`() {
        val plan =
            StackHeaderComposeActionPlanner.plan(
                menu(
                    action("first", StackHeaderToolbarMenuItemShowAsAction.IF_ROOM),
                    action("second", StackHeaderToolbarMenuItemShowAsAction.IF_ROOM),
                    action("third", StackHeaderToolbarMenuItemShowAsAction.IF_ROOM),
                ),
            )

        assertEquals(listOf("first"), plan.direct.map { it.id })
        assertEquals(listOf("second", "third"), plan.overflow.map { it.id })
    }

    @Test
    fun `rejects duplicate ids and invalid direct descriptors precisely`() {
        assertEquals(
            "duplicate action id 'bookmark'",
            StackHeaderComposeActionPlanner.unsupportedReason(menu(action("bookmark"), action("bookmark"))),
        )
        assertEquals(
            "custom action icon tints are not supported",
            StackHeaderComposeActionPlanner.unsupportedReason(
                menu(action("bookmark").copy(iconTintColorNormal = 0xFF000000.toInt())),
            ),
        )
        assertEquals(
            "always-visible actions leave no room for the overflow action",
            StackHeaderComposeActionPlanner.unsupportedReason(
                menu(
                    action("first", StackHeaderToolbarMenuItemShowAsAction.ALWAYS),
                    action("second", StackHeaderToolbarMenuItemShowAsAction.ALWAYS),
                    action("overflow", StackHeaderToolbarMenuItemShowAsAction.NEVER, icon = false),
                ),
            ),
        )
        assertEquals(
            "direct action 'bookmark' requires an icon",
            StackHeaderComposeActionPlanner.unsupportedReason(
                menu(action("bookmark", StackHeaderToolbarMenuItemShowAsAction.ALWAYS, icon = false)),
            ),
        )
        assertEquals(
            "direct action 'bookmark' requires an accessibility label",
            StackHeaderComposeActionPlanner.unsupportedReason(
                menu(action("bookmark", StackHeaderToolbarMenuItemShowAsAction.ALWAYS, label = null)),
            ),
        )
    }

    private fun menu(vararg items: StackHeaderToolbarMenuItemConfig) =
        StackHeaderToolbarMenuConfig(
            groups = emptyList(),
            children = items.map(StackHeaderToolbarMenuElementConfig::MenuItem),
        )

    private fun action(
        id: String,
        showAsAction: StackHeaderToolbarMenuItemShowAsAction = StackHeaderToolbarMenuItemShowAsAction.NEVER,
        icon: Boolean = true,
        label: String? = "Label",
    ) = StackHeaderToolbarMenuItemConfig(
        id = id,
        title = "Title",
        titleCondensed = null,
        tooltipText = label,
        hidden = false,
        disabled = false,
        showAsAction = showAsAction,
        icon = if (icon) android.graphics.drawable.ColorDrawable() else null,
        iconTintColorNormal = null,
        iconTintColorPressed = null,
        iconTintColorFocused = null,
        iconTintColorDisabled = null,
        groupId = null,
        itemType = StackHeaderToolbarMenuItemType.ACTION,
        initialToggleState = false,
    )
}
