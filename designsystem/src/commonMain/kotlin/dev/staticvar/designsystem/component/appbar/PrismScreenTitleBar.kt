/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.prism_back

/**
 * Compact screen title bar with optional navigation, subtitle, and trailing content.
 *
 * [style] controls the token-backed height, spacing, and title treatment while this composable
 * owns inset handling and slot placement. [onBackPress] renders the standard app-bar back
 * affordance; [actions] supports call sites that provide trailing controls. [titleAccessory]
 * sits right after the title, for a short status such as a tag.
 */
@Composable
public fun PrismScreenTitleBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onBackPress: (() -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit)? = null,
  style: PrismScreenTitleBarStyle = PrismScreenTitleBarStyle.Default,
  titleAccessory: (@Composable () -> Unit)? = null,
) {
  val hasNavigation = onBackPress != null

  PrismScreenTitleBar(modifier = modifier, style = style) {
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
      Row(
        modifier = Modifier.padding(
          horizontal = style.titleHorizontalPadding(hasNavigation = hasNavigation),
        ),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          modifier = Modifier.weight(1f, fill = false),
          style = style.titleTextStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        titleAccessory?.invoke()
      }
    }
    if (actions != null) {
      PrismScreenTitleActions(content = actions)
    }
  }
}

/** App-bar container for contextual controls, with the standard height and status-bar inset. */
@Composable
public fun PrismScreenTitleBar(
  modifier: Modifier = Modifier,
  style: PrismScreenTitleBarStyle = PrismScreenTitleBarStyle.Default,
  content: @Composable RowScope.() -> Unit,
) {
  val systemBarHeight = with(LocalDensity.current) {
    WindowInsets.statusBars.getTop(this).toDp()
  }
  Row(
    modifier = modifier
      .heightIn(min = style.containerHeight + systemBarHeight)
      .padding(horizontal = 0.dp, vertical = style.verticalPadding)
      .windowInsetsPadding(WindowInsets.statusBars),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

@Composable
private fun PrismScreenTitleNavigationSlot(onBackPress: (() -> Unit)?) {
  if (onBackPress != null) {
    PrismIconButton(
      icon = Prism.icons.back,
      contentDescription = stringResource(Res.string.prism_back),
      onClick = onBackPress,
      size = PrismIconButtonSize.Toolbar,
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
