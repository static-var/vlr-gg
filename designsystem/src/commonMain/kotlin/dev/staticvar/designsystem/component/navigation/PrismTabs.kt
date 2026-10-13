/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.component.frame.rememberPrismPressProgress
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Horizontally scrolling chip tabs with optional counts.
 *
 * Unlike [PrismSegmentedFilterTabs], this component is intended for content section
 * switching rather than compact equal-width filters. [style] controls the chip shape, type, and
 * state colors.
 */
@Composable
public fun PrismTabs(
  tabs: List<PrismTab>,
  selectedTabId: String,
  onTabSelected: (PrismTab) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismTabStyle = PrismTabStyle.Default,
) {
  val scrollState = rememberScrollState()
  val fadeWidth = Prism.dimens.spacingL

  Row(
    modifier =
    modifier
      .fillMaxWidth()
      .scrollEdgeFade(scrollState, fadeWidth)
      .horizontalScroll(scrollState)
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
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
  val shape = style.shape
  val frame = style.frame
  val framed = frame.shadowOffset != DpOffset.Zero
  val interactions = remember { MutableInteractionSource() }
  val pressProgress = if (framed) rememberPrismPressProgress(interactions, enabled = enabled) else null
  // Keep the selected chip on screen, for example after the pager below it changes page.
  val bringIntoView = remember { BringIntoViewRequester() }
  LaunchedEffect(selected) { if (selected) bringIntoView.bringIntoView() }
  // The chip is shorter than 48 dp; Compose extends touches near it to the minimum touch target.
  Box(
    modifier =
    Modifier.widthIn(min = Prism.dimens.touchTargetMin)
      .bringIntoViewRequester(bringIntoView)
      // The selected chip rests pressed into its shadow; others press only while touched.
      .prismFrame(frame = frame, shape = shape) {
        maxOf(visualState.selectedProgress, pressProgress?.value ?: 0f)
      }
      .then(if (framed) Modifier else Modifier.clip(shape))
      .selectable(
        selected = selected,
        enabled = enabled,
        role = Role.Tab,
        interactionSource = interactions,
        indication = if (framed) null else ripple(),
        onClick = onClick,
      )
      .alpha(if (enabled) 1f else 0.72f),
  ) {
    PrismSurface(
      color = visualState.containerColor,
      shape = shape,
      border = visualState.border,
    ) {
      Row(
        modifier = Modifier.heightIn(min = ChipHeight).padding(
          horizontal =
          Prism.dimens.spacingM - Prism.dimens.spacingXs,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Text(
          text = if (style.uppercase) tab.label.uppercase() else tab.label,
          style = style.labelStyle,
          color = visualState.contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        tab.count?.let { count ->
          Text(
            text = if (count > MaxShownCount) "$MaxShownCount+" else count.toString(),
            modifier = Modifier.background(visualState.countContainerColor, style.countShape)
              .padding(style.countPadding),
            style = style.countStyle,
            color = visualState.countContentColor,
            maxLines = 1,
          )
        }
      }
    }
  }
}

private val ChipHeight = 40.dp

/** Fades whichever edge still has chips scrolled out of view. */
private fun Modifier.scrollEdgeFade(scrollState: ScrollState, width: Dp): Modifier =
  graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    val fade = width.toPx().coerceAtMost(size.width / 2)
    if (scrollState.canScrollBackward) {
      drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
        size = Size(fade, size.height),
        blendMode = BlendMode.DstIn,
      )
    }
    if (scrollState.canScrollForward) {
      drawRect(
        brush = Brush.horizontalGradient(
          listOf(Color.Black, Color.Transparent),
          startX = size.width - fade,
          endX = size.width,
        ),
        topLeft = Offset(size.width - fade, 0f),
        size = Size(fade, size.height),
        blendMode = BlendMode.DstIn,
      )
    }
  }
private const val MaxShownCount = 999

@Composable
private fun rememberPrismTabVisualState(
  tabId: String,
  selected: Boolean,
  enabled: Boolean,
  style: PrismTabStyle,
): PrismTabVisualState {
  val animation = Prism.anim.selection

  @Composable
  fun animated(target: Color, name: String) = animateColorAsState(
    targetValue = target,
    animationSpec = animation.colorSpec(),
    label = "tabs_${name}_$tabId",
  ).value
  val border = style.border(selected = selected, enabled = enabled)
  val selectedProgress =
    animateFloatAsState(
      targetValue = if (selected) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "tabs_selected_$tabId",
    )

  return PrismTabVisualState(
    containerColor = animated(style.containerColor(selected = selected, enabled = enabled), "container"),
    contentColor = animated(style.contentColor(selected = selected, enabled = enabled), "content"),
    border = BorderStroke(width = border.width, color = animated(border.brushColor(), "border")),
    countContainerColor = animated(style.countContainerColor(selected = selected), "count_container"),
    countContentColor = animated(style.countContentColor(selected = selected, enabled = enabled), "count_content"),
    selectedProgress = selectedProgress,
  )
}

private fun BorderStroke.brushColor(): Color = when (val brush = brush) {
  is androidx.compose.ui.graphics.SolidColor -> brush.value
  else -> Color.Unspecified
}

private class PrismTabVisualState(
  val containerColor: Color,
  val contentColor: Color,
  val border: BorderStroke,
  val countContainerColor: Color,
  val countContentColor: Color,
  selectedProgress: State<Float>,
) {
  val selectedProgress: Float by selectedProgress
}
