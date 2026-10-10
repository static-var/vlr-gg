/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

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
import androidx.compose.foundation.rememberScrollState
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
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.selection.PrismCheckbox
import dev.staticvar.designsystem.component.sheet.PrismFabSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.EventDetailsPreferences
import dev.staticvar.vlr.core.settings.EventDetailsSection
import dev.staticvar.vlr.domain.model.EventDetails
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.choose_and_order_event_sections
import vlr.feature_events.generated.resources.done
import vlr.feature_events.generated.resources.event_options
import vlr.feature_events.generated.resources.matches_option_description
import vlr.feature_events.generated.resources.move_section_down
import vlr.feature_events.generated.resources.move_section_up
import vlr.feature_events.generated.resources.participants
import vlr.feature_events.generated.resources.participants_option_description
import vlr.feature_events.generated.resources.prizes
import vlr.feature_events.generated.resources.prizes_option_description
import vlr.feature_events.generated.resources.reorder_section
import vlr.feature_events.generated.resources.section_unavailable
import vlr.feature_events.generated.resources.standings
import vlr.feature_events.generated.resources.standings_option_description
import vlr.feature_events.generated.resources.tab_matches

@Composable
internal fun EventDetailOptionsSheet(
  event: EventDetails,
  preferences: EventDetailsPreferences,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onPreferencesChange: (EventDetailsPreferences) -> Unit,
) {
  val scrollState = rememberScrollState()
  PrismFabSheet(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    icon = EventOptionsIcon,
    contentDescription = stringResource(Res.string.event_options),
    sheetTitle = stringResource(Res.string.event_options),
    scrollState = scrollState,
    header = {
      Text(
        stringResource(Res.string.event_options),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
      )
      Text(
        stringResource(Res.string.choose_and_order_event_sections),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    },
    footer = {
      PrismButton(onClick = { onExpandedChange(false) }, style = PrismButtonStyle.Secondary) {
        Text(stringResource(Res.string.done))
      }
    },
  ) {
    EventSectionList(
      preferences = preferences,
      sectionsWithData = event.sectionsWithData,
      scrollState = scrollState,
      onPreferencesChange = onPreferencesChange,
    )
  }
}

/**
 * Every section in the saved order. Rows reorder by dragging their handle, which takes the gesture
 * from the sheet's scroll once it passes touch slop and scrolls the sheet when held near its edges.
 */
@Composable
private fun EventSectionList(
  preferences: EventDetailsPreferences,
  sectionsWithData: Set<EventDetailsSection>,
  scrollState: ScrollState,
  onPreferencesChange: (EventDetailsPreferences) -> Unit,
) {
  val savedOrder = preferences.orderedSections
  val spacing = Prism.dimens.spacingS
  val density = LocalDensity.current
  val settleSpec = Prism.anim.standard.floatSpec()
  val scope = rememberCoroutineScope()
  val state = remember(scope, density) {
    EventSectionReorderState(savedOrder, scope, with(density) { spacing.toPx() }, settleSpec)
  }
  LaunchedEffect(state, savedOrder) { state.sync(savedOrder) }
  val currentPreferences by rememberUpdatedState(preferences)
  val currentOnPreferencesChange by rememberUpdatedState(onPreferencesChange)
  val haptics = LocalHapticFeedback.current
  var columnCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  val holding = state.dragged != null
  LaunchedEffect(state, holding) {
    if (!holding) return@LaunchedEffect
    val edge = with(density) { AutoScrollEdge.toPx() }
    val speed = with(density) { AutoScrollSpeedPerSecond.toPx() }
    var previousFrame = withFrameNanos { it }
    while (true) {
      val frame = withFrameNanos { it }
      val row = state.draggedSpan() ?: break
      val coordinates = columnCoordinates?.takeIf { it.isAttached }
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

  Column(
    modifier = Modifier.fillMaxWidth().onPlaced { columnCoordinates = it },
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    state.order.forEach { section ->
      key(section) {
        var handleRevision by remember { mutableIntStateOf(0) }
        val title = stringResource(section.title)
        val savedIndex = savedOrder.indexOf(section)
        val hasData = section in sectionsWithData
        EventSectionOption(
          title = title,
          description = stringResource(if (hasData) section.description else Res.string.section_unavailable),
          checked = preferences.isVisible(section),
          hasData = hasData,
          handleRevision = handleRevision,
          lifted = state.isLifted(section),
          onCheckedChange = { visible ->
            if (state.dragged == null) onPreferencesChange(preferences.withVisibility(section, visible))
          },
          moveUpLabel = stringResource(Res.string.move_section_up, title).takeIf { savedIndex > 0 },
          moveDownLabel = stringResource(Res.string.move_section_down, title)
            .takeIf { savedIndex < savedOrder.lastIndex },
          onMove = { step ->
            if (state.dragged == null) onPreferencesChange(preferences.moved(section, savedIndex + step))
          },
          modifier = Modifier
            .zIndex(if (state.isLifted(section)) 1f else 0f)
            .onSizeChanged { state.onMeasured(section, it.height) }
            .graphicsLayer { translationY = state.offsetOf(section) },
          handleModifier = Modifier.pointerInput(section, state) {
            detectVerticalDragGestures(
              onDragStart = {
                if (state.start(section)) {
                  haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
              },
              onDragEnd = {
                if (state.dragged == section) {
                  state.end()?.let { order ->
                    handleRevision++
                    currentOnPreferencesChange(currentPreferences.copy(sectionOrder = order))
                  }
                  haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                }
              },
              onDragCancel = { if (state.dragged == section) state.cancel() },
              onVerticalDrag = { _, delta ->
                if (state.dragged == section && state.dragBy(delta)) {
                  haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
              },
            )
          },
        )
      }
    }
  }
}

private val AutoScrollEdge = 48.dp
private val AutoScrollSpeedPerSecond = 720.dp
private const val NanosPerSecond: Float = 1_000_000_000f
private const val LiftScale: Float = 0.02f

private val EventDetailsSection.title: StringResource
  get() = when (this) {
    EventDetailsSection.Participants -> Res.string.participants
    EventDetailsSection.Prizes -> Res.string.prizes
    EventDetailsSection.Standings -> Res.string.standings
    EventDetailsSection.Matches -> Res.string.tab_matches
  }

private val EventDetailsSection.description: StringResource
  get() = when (this) {
    EventDetailsSection.Participants -> Res.string.participants_option_description
    EventDetailsSection.Prizes -> Res.string.prizes_option_description
    EventDetailsSection.Standings -> Res.string.standings_option_description
    EventDetailsSection.Matches -> Res.string.matches_option_description
  }

/**
 * One section row. Sections without data keep their saved visibility but cannot change it. The
 * handle is its own accessibility node, so its move actions stay usable when the toggle is disabled.
 */
@Composable
private fun EventSectionOption(
  title: String,
  description: String,
  checked: Boolean,
  hasData: Boolean,
  handleRevision: Int,
  lifted: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  moveUpLabel: String?,
  moveDownLabel: String?,
  onMove: (step: Int) -> Unit,
  modifier: Modifier = Modifier,
  handleModifier: Modifier = Modifier,
) {
  val lift by animateFloatAsState(if (lifted) 1f else 0f, Prism.anim.press.floatSpec())
  val shape = Prism.shapes.medium
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
        value = checked,
        enabled = hasData,
        role = Role.Checkbox,
        onValueChange = onCheckedChange,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      PrismCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = hasData,
        modifier = Modifier.clearAndSetSemantics {},
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          title,
          style = Prism.typography.bodyLarge,
          color = if (hasData) Prism.color.titleColor else Prism.color.contentTertiary,
        )
        Text(
          description,
          style = Prism.typography.bodySmall,
          color = if (hasData) Prism.color.bodyColor else Prism.color.contentTertiary,
        )
      }
    }
    val reorderLabel = stringResource(Res.string.reorder_section, title)
    key(handleRevision) {
      Box(
        modifier = Modifier
          .size(Prism.dimens.touchTargetMin)
          .then(handleModifier)
          .semantics {
            contentDescription = reorderLabel
            customActions = listOfNotNull(
              moveUpLabel?.let { CustomAccessibilityAction(it) {
                onMove(-1)
                true
              } },
              moveDownLabel?.let { CustomAccessibilityAction(it) {
                onMove(1)
                true
              } },
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
}

private val DragHandleIcon: ImageVector = ImageVector.Builder(
  name = "DragSection",
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

private val EventOptionsIcon: ImageVector = ImageVector.Builder(
  name = "EventOptions",
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
    moveTo(4f, 7f)
    lineTo(8f, 7f)
    moveTo(12f, 7f)
    lineTo(20f, 7f)
    moveTo(4f, 17f)
    lineTo(12f, 17f)
    moveTo(16f, 17f)
    lineTo(20f, 17f)
    moveTo(10f, 4f)
    lineTo(10f, 10f)
    moveTo(14f, 14f)
    lineTo(14f, 20f)
  }
}.build()
