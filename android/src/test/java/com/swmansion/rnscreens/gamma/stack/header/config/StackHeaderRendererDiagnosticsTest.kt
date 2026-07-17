package com.swmansion.rnscreens.gamma.stack.header.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackHeaderRendererDiagnosticsTest {
    @Test
    fun `reports only the first fallback for one header config`() {
        val diagnostics = StackHeaderRendererDiagnostics()
        val firstFallback =
            StackHeaderRendererResolution(
                requested = StackHeaderRenderer.COMPOSE,
                actual = StackHeaderRenderer.VIEW,
                fallbackReason = "transparent headers are not supported",
            )
        val changedFallback =
            firstFallback.copy(fallbackReason = "custom back icons are not supported")

        assertTrue(diagnostics.record(firstFallback).shouldReportFallback)
        assertFalse(diagnostics.record(firstFallback).shouldReportFallback)
        assertFalse(diagnostics.record(changedFallback).shouldReportFallback)
        assertEquals(changedFallback, diagnostics.current)
    }

    @Test
    fun `records a supported renderer without emitting a fallback warning`() {
        val diagnostics = StackHeaderRendererDiagnostics()
        val supported =
            StackHeaderRendererResolution(
                requested = StackHeaderRenderer.COMPOSE,
                actual = StackHeaderRenderer.COMPOSE,
            )

        assertFalse(diagnostics.record(supported).shouldReportFallback)
        assertEquals(supported, diagnostics.current)
    }

    @Test
    fun `starts unresolved and only changes when the actual resolution changes`() {
        val diagnostics = StackHeaderRendererDiagnostics()
        val supported =
            StackHeaderRendererResolution(
                requested = StackHeaderRenderer.COMPOSE,
                actual = StackHeaderRenderer.COMPOSE,
            )

        assertNull(diagnostics.current)
        assertTrue(diagnostics.record(supported).resolutionChanged)
        assertFalse(diagnostics.record(supported).resolutionChanged)
    }
}
