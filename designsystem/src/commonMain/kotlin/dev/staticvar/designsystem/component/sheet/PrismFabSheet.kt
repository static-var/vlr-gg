/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Floating action that opens a [PrismModalSheet] and returns after its dismissal animation.
 *
 * Place this above screen content in a full-size Box. The host owns [expanded]; dismissal, including
 * the platform back action, requests false through [onExpandedChange]. [sheetTitle] identifies the
 * modal pane to accessibility services. [onCollapsed] runs after the modal has left composition and
 * a frame has passed, including the initial collapsed state. Motion follows the system animation
 * duration scale.
 */
@Composable
public fun PrismFabSheet(
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  icon: ImageVector,
  contentDescription: String,
  sheetTitle: String,
  modifier: Modifier = Modifier,
  style: PrismFabSheetStyle = PrismFabSheetStyle.Accent,
  onCollapsed: () -> Unit = {},
  header: (@Composable ColumnScope.() -> Unit)? = null,
  footer: (@Composable RowScope.() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  var collapsed by remember { mutableStateOf(true) }
  LaunchedEffect(expanded) { if (expanded) collapsed = false }
  Box(modifier = modifier.fillMaxSize().safeDrawingPadding().padding(Prism.dimens.spacingM)) {
    if (!expanded && collapsed) {
      PrismFabSheetAction(
        icon = icon,
        contentDescription = contentDescription,
        onClick = { onExpandedChange(true) },
        style = style,
        modifier = Modifier.align(Alignment.BottomEnd),
      )
    }
  }
  PrismModalSheet(
    visible = expanded,
    onDismissRequest = { onExpandedChange(false) },
    paneTitle = sheetTitle,
    onCollapsed = {
      collapsed = true
      onCollapsed()
    },
    header = header,
    footer = footer,
    content = content,
  )
}

@Composable
private fun PrismFabSheetAction(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  style: PrismFabSheetStyle,
  modifier: Modifier = Modifier,
) {
  PrismSurface(
    modifier =
    modifier
      .size(style.actionSize)
      .clip(Prism.shapes.large)
      .clickable(
        role = Role.Button,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        onClick = onClick,
      ),
    color = style.containerColor,
    contentColor = style.contentColor,
    border = style.border,
    shape = Prism.shapes.large,
  ) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription, Modifier.size(Prism.dimens.iconL))
    }
  }
}
