/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Standard horizontal ticket tabs.
 *
 * Unlike [PrismSegmentedFilterTabs], this component is intended for content section
 * switching rather than compact equal-width filters. [style] controls the ticket surface,
 * indicator, border, and state colors.
 */
@Composable
public fun PrismTabs(
  tabs: List<PrismTab>,
  selectedTabId: String,
  onTabSelected: (PrismTab) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismTabStyle = PrismTabStyle.Ticket,
) {
  val scrollState = rememberScrollState()

  Row(
    modifier =
    modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalAlignment = Alignment.Bottom,
  ) {
    tabs.forEach { tab ->
      PrismTabItem(
        tab = tab,
        selected = tab.id == selectedTabId,
        enabled = enabled && tab.enabled,
        style = style,
        onClick = { onTabSelected(tab) },
      )
    }
  }
}

@Composable
private fun PrismTabItem(
  tab: PrismTab,
  selected: Boolean,
  enabled: Boolean,
  style: PrismTabStyle,
  onClick: () -> Unit,
) {
  val visualState =
    rememberPrismTabVisualState(
      tabId = tab.id,
      selected = selected,
      enabled = enabled,
      style = style,
    )
  Column(
    modifier =
    Modifier.widthIn(min = visualState.minWidth)
      .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
      .alpha(if (enabled) 1f else 0.72f)
      .drawWithContent {
        drawContent()
        val indicatorHeight = visualState.indicatorHeight.toPx()
        if (indicatorHeight > 0f && visualState.indicatorColor != Color.Transparent) {
          val borderWidth = visualState.indicatorBorderWidth.toPx()
          val indicatorInset = visualState.indicatorHorizontalInset.toPx()
          val indicatorTop = visualState.indicatorOffsetY.toPx()
          val indicatorWidth = (size.width - indicatorInset * 2).coerceAtLeast(0f)
          val indicatorLeft = indicatorInset
          val indicatorRight = indicatorLeft + indicatorWidth
          val borderCenter = borderWidth / 2f

          drawLine(
            color = visualState.indicatorBorderColor,
            start = Offset(x = borderCenter, y = borderCenter),
            end = Offset(x = indicatorLeft, y = borderCenter),
            strokeWidth = borderWidth,
          )
          drawLine(
            color = visualState.indicatorBorderColor,
            start = Offset(x = indicatorRight, y = borderCenter),
            end = Offset(x = size.width - borderCenter, y = borderCenter),
            strokeWidth = borderWidth,
          )
          drawLine(
            color = visualState.indicatorBorderColor,
            start = Offset(x = borderCenter, y = borderCenter),
            end = Offset(x = borderCenter, y = size.height - borderCenter),
            strokeWidth = borderWidth,
          )
          drawLine(
            color = visualState.indicatorBorderColor,
            start = Offset(x = size.width - borderCenter, y = borderCenter),
            end = Offset(x = size.width - borderCenter, y = size.height - borderCenter),
            strokeWidth = borderWidth,
          )
          drawLine(
            color = visualState.indicatorBorderColor,
            start = Offset(x = borderCenter, y = size.height - borderCenter),
            end = Offset(x = size.width - borderCenter, y = size.height - borderCenter),
            strokeWidth = borderWidth,
          )

          drawRect(
            color = visualState.indicatorColor,
            topLeft = Offset(x = indicatorLeft, y = indicatorTop),
            size = Size(width = indicatorWidth, height = indicatorHeight),
          )
        }
      },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PrismSurface(
      modifier = Modifier.fillMaxWidth().heightIn(min = Prism.dimens.touchTargetMin),
      color = visualState.containerColor,
      border = if (visualState.hasIndicator) null else visualState.border,
    ) {
      Box(
        modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = Prism.dimens.touchTargetMin)
          .padding(horizontal = Prism.dimens.spacingM),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = tab.label,
          style = Prism.typography.button,
          color = visualState.contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun rememberPrismTabVisualState(
  tabId: String,
  selected: Boolean,
  enabled: Boolean,
  style: PrismTabStyle,
): PrismTabVisualState {
  val animation = Prism.anim.standard
  val containerColor by
    animateColorAsState(
      targetValue = style.containerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tabs_container_$tabId",
    )
  val contentColor by
    animateColorAsState(
      targetValue = style.contentColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tabs_content_$tabId",
    )
  val indicatorColor by
    animateColorAsState(
      targetValue = style.indicatorColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tabs_indicator_$tabId",
    )
  val indicatorHeight by
    animateDpAsState(
      targetValue = style.indicatorHeight(selected = selected),
      animationSpec = animation.dpSpec(),
      label = "tabs_indicator_height_$tabId",
    )
  val indicatorHorizontalInset by
    animateDpAsState(
      targetValue = style.indicatorHorizontalInset(selected = selected),
      animationSpec = animation.dpSpec(),
      label = "tabs_indicator_inset_$tabId",
    )
  val indicatorOffsetY by
    animateDpAsState(
      targetValue = style.indicatorOffsetY(selected = selected),
      animationSpec = animation.dpSpec(),
      label = "tabs_indicator_offset_y_$tabId",
    )
  val borderColor by
    animateColorAsState(
      targetValue = style.border(selected = selected, enabled = enabled).brushColor(),
      animationSpec = animation.colorSpec(),
      label = "tabs_border_$tabId",
    )
  val borderWidth by
    animateDpAsState(
      targetValue = style.border(selected = selected, enabled = enabled).width,
      animationSpec = animation.dpSpec(),
      label = "tabs_border_width_$tabId",
    )

  return PrismTabVisualState(
    containerColor = containerColor,
    contentColor = contentColor,
    indicatorColor = indicatorColor,
    indicatorHeight = indicatorHeight,
    indicatorHorizontalInset = indicatorHorizontalInset,
    indicatorOffsetY = indicatorOffsetY,
    indicatorBorderColor = borderColor,
    indicatorBorderWidth = borderWidth,
    hasIndicator = indicatorHeight > 0.dp && indicatorColor != Color.Transparent,
    border = BorderStroke(width = borderWidth, color = borderColor),
    minWidth = style.minWidth,
  )
}

private fun BorderStroke.brushColor(): Color = when (val brush = brush) {
  is androidx.compose.ui.graphics.SolidColor -> brush.value
  else -> Color.Unspecified
}

private data class PrismTabVisualState(
  val containerColor: Color,
  val contentColor: Color,
  val indicatorColor: Color,
  val indicatorHeight: Dp,
  val indicatorHorizontalInset: Dp,
  val indicatorOffsetY: Dp,
  val indicatorBorderColor: Color,
  val indicatorBorderWidth: Dp,
  val hasIndicator: Boolean,
  val border: BorderStroke,
  val minWidth: Dp,
)
