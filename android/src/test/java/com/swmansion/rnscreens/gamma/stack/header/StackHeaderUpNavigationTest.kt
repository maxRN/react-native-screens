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
}
