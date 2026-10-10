/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.reorder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.component.selection.PrismCheckbox
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.prism_move_item_down
import vlr.designsystem.generated.resources.prism_move_item_up
import vlr.designsystem.generated.resources.prism_reorder_item

/**
 * Checkable options in their saved order, each with a drag handle for reordering.
 *
 * [options] arrive in the saved order and keep the same set of unique keys for the life of the
 * list; only their order and content change. Dragging a handle takes the gesture from the enclosing
 * scroll once it passes touch slop; the rows rearrange live and [onMove] reports the held row's new
 * index only when a drop changes the order. Cancelled drags save nothing. Each handle is its own
 * accessibility node with move up and move down actions, so rows whose toggle is disabled can still
 * be reordered with a screen reader. Pass the [scrollState] of the scroll container holding the list
 * to scroll it while a row is held near its edges.
 */
@Composable
public fun <K : Any> PrismReorderableOptionList(
  options: List<PrismReorderableOption<K>>,
  onCheckedChange: (key: K, checked: Boolean) -> Unit,
  onMove: (key: K, index: Int) -> Unit,
  modifier: Modifier = Modifier,
  scrollState: ScrollState? = null,
) {
  val savedOrder = options.map { it.key }
  val spacing = Prism.dimens.spacingS
  val density = LocalDensity.current
  val settleSpec = Prism.anim.standard.floatSpec()
  val scope = rememberCoroutineScope()
  val state = remember(scope, density) {
    PrismReorderState(savedOrder, scope, with(density) { spacing.toPx() }, settleSpec)
  }
  LaunchedEffect(state, savedOrder) { state.sync(savedOrder) }
  // Drag gestures and accessibility actions can outlive a recomposition, so they read these.
  val currentSavedOrder by rememberUpdatedState(savedOrder)
  val currentOnMove by rememberUpdatedState(onMove)
  val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
  val haptics = LocalHapticFeedback.current
  var columnCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  if (scrollState != null) ReorderAutoScroll(state, scrollState, haptics) { columnCoordinates }

  // Resolves the target from the order saved now, not when the action was created.
  fun moveBy(key: K, step: Int) {
    if (state.dragged != null) return
    val index = currentSavedOrder.indexOf(key)
    val target = index + step
    if (index >= 0 && target != index && target in currentSavedOrder.indices) currentOnMove(key, target)
  }

  val optionsByKey = options.associateBy { it.key }
  Column(
    modifier = modifier.fillMaxWidth().onPlaced { columnCoordinates = it },
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    state.order.mapNotNull(optionsByKey::get).forEach { option ->
      key(option.key) {
        // Changes only after a drop moves this row, so its handle's native accessibility node is
        // rebuilt with fresh action labels. Screen reader moves keep the node and its focus.
        var handleRevision by remember { mutableIntStateOf(0) }
        val savedIndex = savedOrder.indexOf(option.key)
        val lifted = state.isLifted(option.key)
        PrismReorderableOptionRow(
          option = option,
          lifted = lifted,
          onCheckedChange = { checked ->
            if (state.dragged == null) currentOnCheckedChange(option.key, checked)
          },
          modifier = Modifier
            .zIndex(if (lifted) 1f else 0f)
            .onSizeChanged { state.onMeasured(option.key, it.height) }
            .graphicsLayer { translationY = state.offsetOf(option.key) },
        ) {
          PrismReorderHandle(
            title = option.title,
            revision = handleRevision,
            lifted = lifted,
            canMoveUp = savedIndex > 0,
            canMoveDown = savedIndex < savedOrder.lastIndex,
            onMove = { step -> moveBy(option.key, step) },
            modifier = Modifier.reorderDrag(state, option.key, haptics) { order ->
              handleRevision++
              currentOnMove(option.key, order.indexOf(option.key))
            },
          )
        }
      }
    }
  }
}

/** Scrolls the list's container each frame while a held row pushes into its edge bands. */
@Composable
private fun <K : Any> ReorderAutoScroll(
  state: PrismReorderState<K>,
  scrollState: ScrollState,
  haptics: HapticFeedback,
  columnCoordinates: () -> LayoutCoordinates?,
) {
  val density = LocalDensity.current
  val holding = state.dragged != null
  LaunchedEffect(state, holding, scrollState) {
    if (!holding) return@LaunchedEffect
    val edge = with(density) { AutoScrollEdge.toPx() }
    val speed = with(density) { AutoScrollSpeedPerSecond.toPx() }
    var previousFrame = withFrameNanos { it }
    while (true) {
      val frame = withFrameNanos { it }
      val row = state.draggedSpan() ?: break
      val coordinates = columnCoordinates()?.takeIf { it.isAttached }
      if (coordinates != null) {
        // The scroll container clips the column, so its clipped bounds are the visible part.
        val origin = coordinates.positionInRoot().y
        val bounds = coordinates.boundsInRoot()
        val maxStep = speed * (frame - previousFrame) / NanosPerSecond
        val step = autoScrollStep(row, (bounds.top - origin)..(bounds.bottom - origin), edge, maxStep)
        if (step != 0f) {
          val scrolled = scrollState.scrollBy(step)
          if (state.dragBy(scrolled)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
      }
      previousFrame = frame
    }
  }
}

/** Drags [key] with a vertical gesture on this handle. [onDrop] receives an order that changed. */
private fun <K : Any> Modifier.reorderDrag(
  state: PrismReorderState<K>,
  key: K,
  haptics: HapticFeedback,
  onDrop: (order: List<K>) -> Unit,
): Modifier = pointerInput(key, state) {
  detectVerticalDragGestures(
    onDragStart = {
      if (state.start(key)) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    },
    onDragEnd = {
      if (state.dragged == key) {
        state.end()?.let(onDrop)
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
      }
    },
    onDragCancel = { if (state.dragged == key) state.cancel() },
    onVerticalDrag = { _, delta ->
      if (state.dragged == key && state.dragBy(delta)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    },
  )
}

private val AutoScrollEdge = 48.dp
private val AutoScrollSpeedPerSecond = 720.dp
private const val NanosPerSecond: Float = 1_000_000_000f
private const val LiftScale: Float = 0.02f

/** One option row: a checkbox toggle over the title and description, then the [handle]. */
@Composable
private fun PrismReorderableOptionRow(
  option: PrismReorderableOption<*>,
  lifted: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  handle: @Composable () -> Unit,
) {
  val lift by animateFloatAsState(if (lifted) 1f else 0f, Prism.anim.press.floatSpec())
  val shape = Prism.shapes.medium
  val enabled = option.toggleEnabled
  Row(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = 1f + LiftScale * lift
        scaleY = 1f + LiftScale * lift
      }
      .background(Prism.color.surface.copy(alpha = lift), shape)
      .border(Prism.dimens.strokeDefault, Prism.color.accent.copy(alpha = lift), shape)
      .padding(start = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    Row(
      modifier = Modifier.weight(1f).toggleable(
        value = option.checked,
        enabled = enabled,
        role = Role.Checkbox,
        onValueChange = onCheckedChange,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      PrismCheckbox(
        checked = option.checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier.clearAndSetSemantics {},
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          option.title,
          style = Prism.typography.bodyLarge,
          color = if (enabled) Prism.color.titleColor else Prism.color.contentTertiary,
        )
        Text(
          option.description,
          style = Prism.typography.bodySmall,
          color = if (enabled) Prism.color.bodyColor else Prism.color.contentTertiary,
        )
      }
    }
    handle()
  }
}

/**
 * The drag handle, its own accessibility node so its move actions stay usable when the row's toggle
 * is disabled. A new [revision] rebuilds the node; otherwise it keeps its identity and focus.
 */
@Suppress("LongParameterList")
@Composable
private fun PrismReorderHandle(
  title: String,
  revision: Int,
  lifted: Boolean,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onMove: (step: Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val reorderLabel = stringResource(Res.string.prism_reorder_item, title)
  val moveUpLabel = stringResource(Res.string.prism_move_item_up, title)
  val moveDownLabel = stringResource(Res.string.prism_move_item_down, title)
  // A native accessibility node may keep an action created by an earlier composition.
  val currentOnMove by rememberUpdatedState(onMove)
  key(revision) {
    Box(
      modifier = Modifier
        .size(Prism.dimens.touchTargetMin)
        .then(modifier)
        .semantics {
          contentDescription = reorderLabel
          customActions = listOfNotNull(
            CustomAccessibilityAction(moveUpLabel) {
              currentOnMove(-1)
              true
            }.takeIf { canMoveUp },
            CustomAccessibilityAction(moveDownLabel) {
              currentOnMove(1)
              true
            }.takeIf { canMoveDown },
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        DragHandleIcon,
        contentDescription = null,
        modifier = Modifier.size(Prism.dimens.iconM),
        tint = if (lifted) Prism.color.accent else Prism.color.contentSecondary,
      )
    }
  }
}

private val DragHandleIcon: ImageVector = ImageVector.Builder(
  name = "PrismDragHandle",
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  ) {
    moveTo(5f, 8f)
    lineTo(19f, 8f)
    moveTo(5f, 12f)
    lineTo(19f, 12f)
    moveTo(5f, 16f)
    lineTo(19f, 16f)
  }
}.build()
