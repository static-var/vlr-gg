/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.icon.back.StairStepBack

/**
 * Compact screen title bar with optional navigation, subtitle, and trailing content.
 *
 * [style] controls the token-backed height, spacing, and title treatment while this composable
 * owns inset handling and slot placement. [onBackPress] renders the standard app-bar back
 * affordance; [actions] supports call sites that provide trailing controls.
 */
@Composable
public fun PrismScreenTitleBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onBackPress: (() -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit)? = null,
  style: PrismScreenTitleBarStyle = PrismScreenTitleBarStyle.Default,
) {
  val systemBarHeight = with(LocalDensity.current) {
    WindowInsets.statusBars.getTop(this).toDp()
  }
  val hasNavigation = onBackPress != null

  Row(
    modifier = modifier
      .height(style.containerHeight + systemBarHeight)
      .padding(horizontal = style.horizontalPadding, vertical = style.verticalPadding)
      .windowInsetsPadding(WindowInsets.statusBars),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PrismScreenTitleNavigationSlot(
      onBackPress = onBackPress,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
      subtitle?.let { text ->
        PrismHeader(
          text = text,
          modifier = Modifier.padding(
            horizontal = style.subtitleHorizontalPadding(hasNavigation = hasNavigation),
          ),
        )
      }
      Text(
        text = title,
        modifier = Modifier
          .padding(horizontal = style.titleHorizontalPadding(hasNavigation = hasNavigation))
          .fillMaxHeight(),
        style = style.titleTextStyle,
      )
    }
    if (actions != null) {
      PrismScreenTitleActions(content = actions)
    }
  }
}

@Composable
private fun PrismScreenTitleNavigationSlot(
  onBackPress: (() -> Unit)?,
) {
  if (onBackPress != null) {
    PrismIcon(
      imageVector = StairStepBack,
      contentDescription = "Back",
      modifier = Modifier.clickable(
        role = Role.Button,
        onClick = onBackPress,
      ),
      tint = PrismIconTint.Inverted,
      style = PrismIconStyle.Borderless
    )
  }
}

@Composable
private fun PrismScreenTitleActions(content: @Composable RowScope.() -> Unit) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(PrismScreenTitleBarStyle.Default.actionSpacing),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}
