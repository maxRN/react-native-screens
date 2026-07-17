package com.swmansion.rnscreens.gamma.stack.header.config

/** Tracks the actual renderer for one native header config, not the whole process. */
internal class StackHeaderRendererDiagnostics {
    var current: StackHeaderRendererResolution? = null
        private set

    private var didReportFallback = false

    /** Returns resolution/event and warning changes separately for each header config. */
    fun record(resolution: StackHeaderRendererResolution): StackHeaderRendererDiagnosticUpdate {
        val resolutionChanged = current != resolution
        current = resolution
        val shouldReportFallback =
            resolution.fallbackReason != null && !didReportFallback
        if (shouldReportFallback) {
            didReportFallback = true
        }
        return StackHeaderRendererDiagnosticUpdate(resolutionChanged, shouldReportFallback)
    }
}

internal data class StackHeaderRendererDiagnosticUpdate(
    val resolutionChanged: Boolean,
    val shouldReportFallback: Boolean,
)
