/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

private object PrismModalSheetConstants {
  const val ScrimAlpha: Float = 0.64f
  const val MaxHeightFraction: Float = 0.9f
}

/**
 * Modal [PrismSheet] with a fading scrim and a full-surface slide transition.
 *
 * Back and scrim taps request dismissal. Content scrolls within the safe viewport. [onCollapsed]
 * runs after the exit finishes and the dialog leaves composition, including the initial hidden state.
 * [onEnterFinished] runs when the Compose enter animation finishes while [visible] is true; it does
 * not wait for the platform to present the frame.
 * Pass [scrollState] when content needs to scroll the body itself, such as while dragging a row;
 * otherwise each opening starts at the top.
 */
@Composable
public fun PrismModalSheet(
  visible: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  header: (@Composable ColumnScope.() -> Unit)? = null,
  footer: (@Composable RowScope.() -> Unit)? = null,
  dragHandle: (@Composable () -> Unit)? = { PrismSheetDragHandle() },
  shape: Shape = Prism.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
  color: Color = Prism.color.backgroundElevated,
  contentColor: Color = contentColorFor(color),
  scrimColor: Color = Prism.color.scrim.copy(alpha = PrismModalSheetConstants.ScrimAlpha),
  paneTitle: String? = null,
  onCollapsed: () -> Unit = {},
  onEnterFinished: () -> Unit = {},
  scrollState: ScrollState? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val visibility = remember { MutableTransitionState(false) }
  SideEffect(visible) { visibility.targetState = visible }
  val collapsed = !visible && !visibility.currentState && visibility.isIdle
  val currentOnCollapsed = rememberUpdatedState(onCollapsed)
  LaunchedEffect(collapsed) {
    if (collapsed) {
      withFrameNanos { }
      currentOnCollapsed.value()
    }
  }
  val entered = visible && visibility.currentState && visibility.isIdle
  val currentOnEnterFinished = rememberUpdatedState(onEnterFinished)
  LaunchedEffect(entered) {
    if (entered) currentOnEnterFinished.value()
  }
  if (visible || visibility.currentState || !visibility.isIdle) {
    PrismModalSheetDialog(onDismissRequest = onDismissRequest) {
      AnimatedVisibility(
        visibleState = visibility,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
      ) {
        PrismModalSheetContent(
          onDismissRequest = onDismissRequest,
          modifier = modifier,
          header = header,
          footer = footer,
          dragHandle = dragHandle,
          shape = shape,
          color = color,
          contentColor = contentColor,
          scrimColor = scrimColor,
          title = paneTitle,
          scrollState = scrollState,
          content = content,
        )
      }
    }
  }
}

@Composable
private fun AnimatedVisibilityScope.PrismModalSheetContent(
  onDismissRequest: () -> Unit,
  modifier: Modifier,
  header: (@Composable ColumnScope.() -> Unit)?,
  footer: (@Composable RowScope.() -> Unit)?,
  dragHandle: (@Composable () -> Unit)?,
  shape: Shape,
  color: Color,
  contentColor: Color,
  scrimColor: Color,
  title: String?,
  scrollState: ScrollState?,
  content: @Composable ColumnScope.() -> Unit,
) {
  val density = LocalDensity.current
  val sheetMaxHeight = remember { mutableIntStateOf(Constraints.Infinity) }
  val bottomInset = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }
  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier.fillMaxSize()
        .animateEnterExit(
          enter = fadeIn(Prism.anim.scrimEnter.floatSpec()),
          exit = fadeOut(Prism.anim.scrimExit.floatSpec()),
        )
        .background(scrimColor)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest,
        ),
    )
    Box(
      Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .imePadding()
        .layout { measurable, constraints ->
          sheetMaxHeight.intValue = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight.toDp() * PrismModalSheetConstants.MaxHeightFraction).roundToPx()
          } else {
            constraints.maxHeight
          }
          val placeable = measurable.measure(constraints)
          layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        },
    ) {
      PrismSheet(
        modifier =
        modifier.align(Alignment.BottomCenter)
          .layout { measurable, constraints ->
            val maxHeight = sheetMaxHeight.intValue.coerceIn(constraints.minHeight, constraints.maxHeight)
            val placeable = measurable.measure(constraints.copy(maxHeight = maxHeight))
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
          }
          .animateEnterExit(
            enter = slideInVertically(Prism.anim.sheetEnter.intOffsetSpec()) { it },
            exit = slideOutVertically(Prism.anim.sheetExit.intOffsetSpec()) { it },
          )
          .fillMaxWidth()
          .semantics { if (title != null) paneTitle = title },
        header = { PrismModalSheetHeader(dragHandle, header) },
        footer = footer,
        shape = shape,
        style = PrismSheetStyle.Modal,
        color = color,
        contentColor = contentColor,
        scrollState = scrollState ?: rememberScrollState(),
        contentPadding = PaddingValues(
          start = Prism.dimens.spacingM,
          top = Prism.dimens.spacingM,
          end = Prism.dimens.spacingM,
          bottom = Prism.dimens.spacingM + bottomInset,
        ),
        content = content,
      )
    }
  }
}

@Composable
private fun ColumnScope.PrismModalSheetHeader(
  dragHandle: (@Composable () -> Unit)?,
  header: (@Composable ColumnScope.() -> Unit)?,
) {
  if (dragHandle != null) {
    Box(
      modifier = Modifier.fillMaxWidth().padding(bottom = Prism.dimens.spacingS),
      contentAlignment = Alignment.Center,
    ) {
      dragHandle()
    }
  }
  header?.invoke(this)
}

@Composable
internal expect fun PrismModalSheetDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit)
