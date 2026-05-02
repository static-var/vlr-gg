/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

private object PrismModalSheetConstants {
  const val ScrimAlpha: Float = 0.64f
}

/**
 * Modal bottom sheet with scrim + brutalist [PrismSheet] body.
 */
@Composable
public fun PrismModalSheet(
  visible: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  header: (@Composable ColumnScope.() -> Unit)? = null,
  footer: (@Composable RowScope.() -> Unit)? = null,
  dragHandle: (@Composable () -> Unit)? = { PrismSheetDragHandle() },
  shape: Shape = Prism.shapes.large,
  color: Color = Prism.color.backgroundElevated,
  contentColor: Color = contentColorFor(color),
  scrimColor: Color = Prism.color.titleColor.copy(alpha = PrismModalSheetConstants.ScrimAlpha),
  content: @Composable ColumnScope.() -> Unit,
) {
  if (!visible) {
    return
  }

  val scrimInteractionSource = remember { MutableInteractionSource() }

  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier =
      Modifier.fillMaxSize()
        .background(scrimColor)
        .clickable(
          interactionSource = scrimInteractionSource,
          indication = null,
          onClick = onDismissRequest,
        ),
    )

    PrismSheet(
      modifier =
      modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingM),
      header = {
        if (dragHandle != null) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = Prism.dimens.spacingS),
            contentAlignment = Alignment.Center,
          ) {
            dragHandle()
          }
        }
        header?.invoke(this)
      },
      footer = footer,
      shape = shape,
      color = color,
      contentColor = contentColor,
      content = content,
    )
  }
}
