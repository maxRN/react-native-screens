@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.swmansion.rnscreens.gamma.stack.header

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.appbar.MaterialToolbar
import com.swmansion.rnscreens.ext.detachFromCurrentParent
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderConfigurationProviding
import com.swmansion.rnscreens.gamma.stack.header.config.StackHeaderRenderer

internal class StackHeaderComposeAppBarLayout(
    context: Context,
) : StackHeaderAppBarLayout(context) {
    override val renderer = StackHeaderRenderer.COMPOSE

    // The View renderer's coordinator owns toolbar menus. Compose configurations
    // with menus resolve back to that renderer, so this toolbar is never attached.
    override val toolbar = MaterialToolbar(context)

    private var title by mutableStateOf("")
    private var leadingView by mutableStateOf<View?>(null)
    private var showUpButton by mutableStateOf(false)
    private var onNavigationIconClick by mutableStateOf<() -> Unit>({})

    private val composeView =
        ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val dark = isSystemInDarkTheme()
                val colorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    } else {
                        if (dark) darkColorScheme() else lightColorScheme()
                    }

                MaterialExpressiveTheme(colorScheme = colorScheme) {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                modifier = Modifier.semantics { heading() },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            val customLeadingView = leadingView
                            when {
                                customLeadingView != null ->
                                    AndroidView(
                                        factory = {
                                            customLeadingView.detachFromCurrentParent()
                                            customLeadingView
                                        },
                                    )
                                showUpButton ->
                                    IconButton(onClick = onNavigationIconClick) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                                        )
                                    }
                            }
                        },
                    )
                }
            }
        }

    init {
        fitsSystemWindows = false
        addView(composeView, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    fun applyConfiguration(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
    ) {
        title = config.title
        updateNavigation(config, canNavigateBack, onNavigationIconClick)
    }

    fun applyTitle(title: String) {
        this.title = title
    }

    fun applyBackButton(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
    ) {
        updateNavigation(config, canNavigateBack, onNavigationIconClick)
    }

    private fun updateNavigation(
        config: StackHeaderConfigurationProviding,
        canNavigateBack: Boolean,
        onNavigationIconClick: () -> Unit,
    ) {
        leadingView = config.leadingSubview?.view
        showUpButton = leadingView == null && canNavigateBack && !config.backButtonHidden
        this.onNavigationIconClick = onNavigationIconClick
    }
}
