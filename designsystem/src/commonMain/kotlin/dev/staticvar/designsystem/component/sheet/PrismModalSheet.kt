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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

private object PrismModalSheetConstants {
  const val ScrimAlpha: Float = 0.64f
  const val DurationMillis: Int = 280
}

/**
 * Modal [PrismSheet] with a fading scrim and a full-surface slide transition.
 *
 * Back and scrim taps request dismissal. Content scrolls within the safe viewport. [onCollapsed]
 * runs after the exit finishes and the dialog leaves composition, including the initial hidden state.
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
  scrimColor: Color = Prism.color.scrim.copy(alpha = PrismModalSheetConstants.ScrimAlpha),
  paneTitle: String? = null,
  onCollapsed: () -> Unit = {},
  content: @Composable ColumnScope.() -> Unit,
) {
  val visibility = remember { MutableTransitionState(false) }
  LaunchedEffect(visible) { visibility.targetState = visible }
  val collapsed = !visible && !visibility.currentState && visibility.isIdle
  val currentOnCollapsed = rememberUpdatedState(onCollapsed)
  LaunchedEffect(collapsed) {
    if (collapsed) {
      withFrameNanos { }
      currentOnCollapsed.value()
    }
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
  content: @Composable ColumnScope.() -> Unit,
) {
  val density = LocalDensity.current
  val bottomSpacing = WindowInsets.safeDrawing.getBottom(density) + with(density) { Prism.dimens.spacingM.roundToPx() }
  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier.fillMaxSize()
        .animateEnterExit(
          enter = fadeIn(tween(PrismModalSheetConstants.DurationMillis)),
          exit = fadeOut(tween(PrismModalSheetConstants.DurationMillis)),
        )
        .background(scrimColor)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest,
        ),
    )
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(Prism.dimens.spacingM)) {
      PrismSheet(
        modifier =
        modifier.align(Alignment.BottomCenter)
          .animateEnterExit(
            enter = slideInVertically(tween(PrismModalSheetConstants.DurationMillis)) { it + bottomSpacing },
            exit = slideOutVertically(tween(PrismModalSheetConstants.DurationMillis)) { it + bottomSpacing },
          )
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .semantics { if (title != null) paneTitle = title },
        header = { PrismModalSheetHeader(dragHandle, header) },
        footer = footer,
        shape = shape,
        color = color,
        contentColor = contentColor,
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
