package com.swmansion.rnscreens.gamma.stack.header

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
    }
}
