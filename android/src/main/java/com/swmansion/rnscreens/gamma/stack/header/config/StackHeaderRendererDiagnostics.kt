package com.swmansion.rnscreens.gamma.stack.header.config

/** Tracks the actual renderer for one native header config, not the whole process. */
internal class StackHeaderRendererDiagnostics {
    var current = StackHeaderRendererResolution(StackHeaderRenderer.VIEW, StackHeaderRenderer.VIEW)
        private set

    private var didReportFallback = false

    /** Returns true only for this config's first production fallback. */
    fun record(resolution: StackHeaderRendererResolution): Boolean {
        current = resolution
        if (resolution.fallbackReason == null || didReportFallback) {
            return false
        }
        didReportFallback = true
        return true
    }
}
