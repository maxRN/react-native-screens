package com.swmansion.rnscreens.gamma.stack.header

import org.junit.Assert.assertEquals
import org.junit.Test

class StackHeaderUpNavigationTest {
    @Test
    fun `Up pops only a navigable native stack`() {
        assertEquals(
            StackHeaderUpNavigation.Action.POP_NATIVE_STACK,
            StackHeaderUpNavigation.resolve(canNavigateBack = true, preventNativeDismiss = false),
        )
        assertEquals(
            StackHeaderUpNavigation.Action.NO_OP,
            StackHeaderUpNavigation.resolve(canNavigateBack = false, preventNativeDismiss = false),
        )
    }

    @Test
    fun `Up preserves a prevented native dismiss`() {
        assertEquals(
            StackHeaderUpNavigation.Action.DISPATCH_PREVENTED,
            StackHeaderUpNavigation.resolve(canNavigateBack = true, preventNativeDismiss = true),
        )
    }

    @Test
    fun `Up targets only the associated named native back-stack entry`() {
        var poppedScreenKey: String? = null

        assertEquals(
            true,
            StackHeaderUpNavigation.dispatchNativePop("screen-2") { screenKey ->
                poppedScreenKey = screenKey
            },
        )
        assertEquals("screen-2", poppedScreenKey)
    }

    @Test
    fun `Up does not use an unnamed native back-stack transaction`() {
        var didPop = false

        assertEquals(
            false,
            StackHeaderUpNavigation.dispatchNativePop(null) { didPop = true },
        )
        assertEquals(false, didPop)
    }
}
