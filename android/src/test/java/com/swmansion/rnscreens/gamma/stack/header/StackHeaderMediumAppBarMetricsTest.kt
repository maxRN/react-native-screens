package com.swmansion.rnscreens.gamma.stack.header

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackHeaderMediumAppBarMetricsTest {
    @Test
    fun `collapsed height retains the small bar and status inset`() {
        assertEquals(
            216,
            StackHeaderMediumAppBarMetrics.collapsedHeightPx(
                density = 3f,
                topInsetPx = 24,
            ),
        )
    }

    @Test
    fun `scroll range cannot collapse below the collapsed height`() {
        assertEquals(
            168,
            StackHeaderMediumAppBarMetrics.totalScrollRangePx(
                expandedHeightPx = 384,
                density = 3f,
                topInsetPx = 24,
            ),
        )
    }

    @Test
    fun `short app bar content has no negative scroll range`() {
        assertEquals(
            0,
            StackHeaderMediumAppBarMetrics.totalScrollRangePx(
                expandedHeightPx = 180,
                density = 3f,
                topInsetPx = 24,
            ),
        )
    }

    @Test
    fun `Compose app bar reaches its own collapsed limit with the native app bar`() {
        assertEquals(
            -126f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -168,
                appBarTotalScrollRangePx = 168,
                composeHeightOffsetLimitPx = -126f,
            ),
        )
    }

    @Test
    fun `a restored native collapse is applied after Compose initializes its limit`() {
        assertEquals(
            0f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -168,
                appBarTotalScrollRangePx = 168,
                composeHeightOffsetLimitPx = 0f,
            ),
        )
        assertEquals(
            -126f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -168,
                appBarTotalScrollRangePx = 168,
                composeHeightOffsetLimitPx = -126f,
            ),
        )
    }

    @Test
    fun `Compose app bar tracks the native collapse fraction and clamps overscroll`() {
        assertEquals(
            -63f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -84,
                appBarTotalScrollRangePx = 168,
                composeHeightOffsetLimitPx = -126f,
            ),
        )
        assertEquals(
            -126f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -200,
                appBarTotalScrollRangePx = 168,
                composeHeightOffsetLimitPx = -126f,
            ),
        )
        assertEquals(
            0f,
            StackHeaderMediumAppBarMetrics.composeHeightOffset(
                coordinatorOffsetPx = -168,
                appBarTotalScrollRangePx = 0,
                composeHeightOffsetLimitPx = -126f,
            ),
        )
    }

    @Test
    fun `internal AppBar flags retain the collapsed height without snapping`() {
        val flags = StackHeaderMediumAppBarContract.scrollingFlags

        assertTrue(flags and com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL != 0)
        assertTrue(flags and com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS != 0)
        assertTrue(
            flags and
                com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED !=
                0,
        )
        assertFalse(flags and com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP != 0)
    }
}
