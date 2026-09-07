/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

/**
 * Bordered sheet with a fixed header and footer. Optional scrolling applies only to the body.
 */
@Composable
public fun PrismSheet(
  modifier: Modifier = Modifier,
  header: (@Composable ColumnScope.() -> Unit)? = null,
  footer: (@Composable RowScope.() -> Unit)? = null,
  shape: Shape = Prism.shapes.large,
  color: Color = Prism.color.backgroundElevated,
  contentColor: Color = contentColorFor(color),
  style: PrismSheetStyle = PrismSheetStyle.Standard,
  scrollState: ScrollState? = null,
  contentPadding: PaddingValues = PaddingValues(Prism.dimens.spacingM),
  content: @Composable ColumnScope.() -> Unit,
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = color,
    contentColor = contentColor,
    shape = shape,
    border = style.border,
    frame = style.frame,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(contentPadding),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      if (header != null) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
          content = header,
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth()
          .then(if (scrollState != null) Modifier.weight(1f, fill = false).verticalScroll(scrollState) else Modifier),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        content = content,
      )

      if (footer != null) {
        HorizontalDivider(
          modifier = Modifier.fillMaxWidth(),
          thickness = Prism.dimens.strokeDefault,
          color = Prism.color.stroke,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          content = footer,
        )
      }
    }
  }
}

/**
 * Default drag handle for [PrismModalSheet].
 */
@Composable
public fun PrismSheetDragHandle(modifier: Modifier = Modifier) {
  val handleWidth = Prism.dimens.spacingXl + Prism.dimens.spacingM

  PrismSurface(
    modifier = modifier.width(handleWidth).height(Prism.dimens.spacingS),
    color = Prism.color.surfaceVariant,
    shape = Prism.shapes.small,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.strokeVariant),
  ) {}
}
