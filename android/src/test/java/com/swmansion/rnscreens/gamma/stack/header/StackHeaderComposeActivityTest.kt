package com.swmansion.rnscreens.gamma.stack.header

import android.view.View
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer
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

    @Test
    fun `inactive screen content remains drawable but is hidden from accessibility`() {
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            StackHeaderScreenActivity.resolve(isActive = false).accessibilityImportance,
        )
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            StackHeaderScreenActivity.resolve(isActive = true).accessibilityImportance,
        )
    }

    @Test
    fun `inactive View fallback app bar remains visible but is hidden from accessibility`() {
        val activity =
            StackHeaderAppBarActivity.resolve(
                renderer = StackHeaderRenderer.VIEW,
                isActive = false,
            )

        assertEquals(View.VISIBLE, activity.viewVisibility)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, activity.accessibilityImportance)
    }
}
