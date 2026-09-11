/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBarStyle
import dev.staticvar.vlr.sharedui.spoilers.SpoilerModeButton

/** App title bar with the global score-visibility control after screen-specific actions. */
@Composable
public fun SharedScreenTitleBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onBackPress: (() -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit)? = null,
  style: PrismScreenTitleBarStyle = PrismScreenTitleBarStyle.Default,
) {
  PrismScreenTitleBar(
    title = title,
    modifier = modifier,
    subtitle = subtitle,
    onBackPress = onBackPress,
    style = style,
    actions = {
      actions?.invoke(this)
      SpoilerModeButton()
    },
  )
}

@Composable
public fun SharedScreenTitleBar(
  modifier: Modifier = Modifier,
  style: PrismScreenTitleBarStyle = PrismScreenTitleBarStyle.Default,
  content: @Composable RowScope.() -> Unit,
) {
  PrismScreenTitleBar(modifier = modifier, style = style) {
    content()
    Spacer(Modifier.width(style.actionSpacing))
    SpoilerModeButton()
  }
}
