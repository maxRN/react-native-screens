package com.swmansion.rnscreens.gamma.stack.header

import android.view.View
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        val inactiveTargets = StackHeaderScreenAccessibilityTargets.resolve(isActive = false)

        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            inactiveTargets.coordinatorImportance,
        )
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            inactiveTargets.stackScreenImportance,
        )
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            StackHeaderScreenActivity.resolve(isActive = true).accessibilityImportance,
        )
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            inactiveTargets.wrapperImportance,
        )
    }

    @Test
    fun `inactive Compose providers hide their virtual semantics without changing visibility`() {
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            StackHeaderComposeProviderActivity.INACTIVE.accessibilityImportance,
        )
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            StackHeaderComposeProviderActivity.ACTIVE.accessibilityImportance,
        )
    }

    @Test
    fun `only AndroidComposeView owns Compose virtual semantics`() {
        assertTrue(
            StackHeaderComposeSemanticsProvider.isProviderClassName(
                "androidx.compose.ui.platform.AndroidComposeView",
            ),
        )
        assertFalse(
            StackHeaderComposeSemanticsProvider.isProviderClassName(
                "androidx.compose.ui.platform.ComposeView",
            ),
        )
    }

    @Test
    fun `inactive Compose semantics provider clears its delegate`() {
        assertTrue(StackHeaderComposeSemanticsProvider.shouldClearDelegate(isActive = false))
        assertFalse(StackHeaderComposeSemanticsProvider.shouldClearDelegate(isActive = true))
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
