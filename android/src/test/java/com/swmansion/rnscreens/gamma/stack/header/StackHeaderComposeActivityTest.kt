package com.swmansion.rnscreens.gamma.stack.header

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

class StackHeaderComposeActivityTest {
    @Test
    fun `only an active Compose header is visible and accessibility-important`() {
        assertEquals(
            StackHeaderComposeActivity.VISIBLE,
            StackHeaderComposeActivity.resolve(isActive = true),
        )
        assertEquals(
            StackHeaderComposeActivity.HIDDEN,
            StackHeaderComposeActivity.resolve(isActive = false),
        )
        assertEquals(View.VISIBLE, StackHeaderComposeActivity.VISIBLE.viewVisibility)
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            StackHeaderComposeActivity.HIDDEN.accessibilityImportance,
        )
    }
}
