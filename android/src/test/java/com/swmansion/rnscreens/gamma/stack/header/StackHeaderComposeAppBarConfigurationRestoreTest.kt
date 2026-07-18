package com.swmansion.rnscreens.gamma.stack.header

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StackHeaderComposeAppBarConfigurationRestoreTest {
    @Test
    fun `matching rebuilt header consumes the retained collapse only once`() {
        val fingerprint = fingerprint()

        StackHeaderComposeAppBarConfigurationRestore.save(
            fingerprint = fingerprint,
            coordinatorOffsetPx = -168,
            targetUiMode = Configuration.UI_MODE_NIGHT_YES,
        )

        assertEquals(
            -168,
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint,
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint,
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
    }

    @Test
    fun `different active header discards a configuration collapse`() {
        StackHeaderComposeAppBarConfigurationRestore.save(
            fingerprint = fingerprint(title = "Stops"),
            coordinatorOffsetPx = -168,
            targetUiMode = Configuration.UI_MODE_NIGHT_YES,
        )

        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint(title = "Settings"),
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint(title = "Stops"),
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
    }

    @Test
    fun `a handoff for another configuration is discarded`() {
        val fingerprint = fingerprint()
        StackHeaderComposeAppBarConfigurationRestore.save(
            fingerprint = fingerprint,
            coordinatorOffsetPx = -168,
            targetUiMode = Configuration.UI_MODE_NIGHT_YES,
        )

        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint,
                targetUiMode = Configuration.UI_MODE_NIGHT_NO,
            ),
        )
        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint,
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
    }

    @Test
    fun `only one active configuration handoff is retained`() {
        StackHeaderComposeAppBarConfigurationRestore.save(
            fingerprint = fingerprint(title = "Stops"),
            coordinatorOffsetPx = -168,
            targetUiMode = Configuration.UI_MODE_NIGHT_YES,
        )
        StackHeaderComposeAppBarConfigurationRestore.save(
            fingerprint = fingerprint(title = "Settings"),
            coordinatorOffsetPx = -72,
            targetUiMode = Configuration.UI_MODE_NIGHT_YES,
        )

        assertEquals(
            -72,
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint(title = "Settings"),
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
        assertNull(
            StackHeaderComposeAppBarConfigurationRestore.consume(
                fingerprint(title = "Stops"),
                targetUiMode = Configuration.UI_MODE_NIGHT_YES,
            ),
        )
    }

    private fun fingerprint(title: String = "Stops") =
        StackHeaderComposeAppBarConfigurationRestore.Fingerprint(
            type = StackHeaderType.MEDIUM,
            title = title,
            hasLeadingView = false,
            showsUpButton = false,
            actionLayout = "item:refresh:ALWAYS:false",
        )
}
