package com.swmansion.rnscreens.gamma.stack.header.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StackHeaderRendererResolverTest {
    @Test
    fun `selects Compose for a supported small app bar`() {
        val result =
            StackHeaderRendererResolver.resolve(
                requested = StackHeaderRenderer.COMPOSE,
                capabilities = StackHeaderRendererCapabilities(type = StackHeaderType.SMALL),
            )

        assertEquals(StackHeaderRenderer.COMPOSE, result)
    }

    @Test
    fun `selects Compose for the medium enterAlways scroll profile`() {
        val result =
            StackHeaderRendererResolver.resolve(
                requested = StackHeaderRenderer.COMPOSE,
                capabilities =
                    StackHeaderRendererCapabilities(
                        type = StackHeaderType.MEDIUM,
                        scrollFlags =
                            StackHeaderScrollFlags(
                                scroll = true,
                                enterAlways = true,
                            ),
                    ),
            )

        assertEquals(StackHeaderRenderer.COMPOSE, result)
    }

    @Test
    fun `retains View when it is explicitly requested`() {
        val result =
            StackHeaderRendererResolver.resolve(
                requested = StackHeaderRenderer.VIEW,
                capabilities = StackHeaderRendererCapabilities(type = StackHeaderType.SMALL),
            )

        assertEquals(StackHeaderRenderer.VIEW, result)
    }

    @Test
    fun `falls back to View for unsupported Compose configuration`() {
        val unsupported =
            listOf(
                StackHeaderRendererCapabilities(type = StackHeaderType.LARGE),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, isTransparent = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasBackgroundColor = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasBackgroundSubview = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasCenterSubview = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasTrailingSubview = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasToolbarMenuGroupDividers = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasCustomBackIcon = true),
                StackHeaderRendererCapabilities(type = StackHeaderType.SMALL, hasCustomBackTint = true),
                StackHeaderRendererCapabilities(
                    type = StackHeaderType.SMALL,
                    scrollFlags = StackHeaderScrollFlags(scroll = true),
                ),
                StackHeaderRendererCapabilities(
                    type = StackHeaderType.MEDIUM,
                    scrollFlags =
                        StackHeaderScrollFlags(
                            scroll = true,
                            exitUntilCollapsed = true,
                            snap = true,
                        ),
                ),
                StackHeaderRendererCapabilities(
                    type = StackHeaderType.MEDIUM,
                    scrollFlags = StackHeaderScrollFlags(enterAlways = true),
                ),
            )

        unsupported.forEach { capabilities ->
            assertEquals(
                StackHeaderRenderer.VIEW,
                StackHeaderRendererResolver.resolve(StackHeaderRenderer.COMPOSE, capabilities),
            )
        }
    }

    @Test
    fun `falls back to View for an unsupported Compose action descriptor`() {
        assertEquals(
            StackHeaderRenderer.VIEW,
            StackHeaderRendererResolver.resolve(
                requested = StackHeaderRenderer.COMPOSE,
                capabilities =
                    StackHeaderRendererCapabilities(
                        type = StackHeaderType.MEDIUM,
                        scrollFlags = StackHeaderScrollFlags(scroll = true, enterAlways = true),
                    ),
                actionMenuUnsupportedReason = "duplicate action id 'bookmark'",
            ),
        )
    }

    @Test
    fun `describes the first unsupported Compose option precisely`() {
        assertEquals(
            "custom background colors are not supported",
            StackHeaderRendererResolver.unsupportedReason(
                StackHeaderRendererCapabilities(
                    type = StackHeaderType.SMALL,
                    hasBackgroundColor = true,
                ),
            ),
        )
    }

    @Test
    fun `exposes the actual View fallback and rejected reason`() {
        val resolution =
            StackHeaderRendererResolver.resolution(
                requested = StackHeaderRenderer.COMPOSE,
                capabilities = StackHeaderRendererCapabilities(type = StackHeaderType.LARGE),
            )

        assertEquals(StackHeaderRenderer.COMPOSE, resolution.requested)
        assertEquals(StackHeaderRenderer.VIEW, resolution.actual)
        assertEquals("large app bars are not supported", resolution.fallbackReason)
    }

    @Test
    fun `supported Compose resolution has no fallback reason`() {
        val resolution =
            StackHeaderRendererResolver.resolution(
                requested = StackHeaderRenderer.COMPOSE,
                capabilities = StackHeaderRendererCapabilities(type = StackHeaderType.SMALL),
            )

        assertEquals(StackHeaderRenderer.COMPOSE, resolution.actual)
        assertNull(resolution.fallbackReason)
    }
}
