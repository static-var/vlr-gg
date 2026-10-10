/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.staticvar.vlr.core.settings.EventDetailsSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder state for the rows of the event options sheet.
 *
 * Rows sit in a plain column, so slot positions come from measured heights in the column's own
 * space and stay put while the sheet scrolls or animates. [order] changes live during a drag; the
 * saved order changes only when [end] returns a new one. Rows that change slots get an offset back
 * to where they were drawn, then slide to rest, so nothing jumps.
 */
@Stable
internal class EventSectionReorderState(
  savedOrder: List<EventDetailsSection>,
  private val scope: CoroutineScope,
  private val spacing: Float,
  private val settleSpec: AnimationSpec<Float>,
) {
  private var savedOrder = savedOrder

  var order: List<EventDetailsSection> by mutableStateOf(savedOrder)
    private set

  var dragged: EventDetailsSection? by mutableStateOf(null)
    private set

  private var settling: EventDetailsSection? by mutableStateOf(null)

  // Follows the finger without clamping, so the row catches up only once the finger is back over the list.
  private var dragOffset by mutableFloatStateOf(0f)
  private val heights = mutableMapOf<EventDetailsSection, Int>()
  private val offsets = EventDetailsSection.entries.associateWith { mutableFloatStateOf(0f) }
  private val settleJobs = mutableMapOf<EventDetailsSection, Job>()

  /** Vertical draw offset of [section] from its slot. */
  fun offsetOf(section: EventDetailsSection): Float =
    if (section == dragged) clampedDragOffset(section) else offsets.getValue(section).floatValue

  /** Whether [section] is held or still settling after a drop, so it draws above its neighbours. */
  fun isLifted(section: EventDetailsSection): Boolean = section == dragged || section == settling

  /** Records a row's height. A held row keeps its drawn top when a row above it resizes mid-drag. */
  fun onMeasured(section: EventDetailsSection, height: Int) {
    val held = dragged
    if (held == null || heights[section] == height) {
      heights[section] = height
      return
    }
    val before = slotTops(order).getValue(held)
    heights[section] = height
    dragOffset += before - slotTops(order).getValue(held)
  }

  /** Adopts a newly saved order. Ignored mid-drag; the drag's own outcome decides what is saved. */
  fun sync(saved: List<EventDetailsSection>) {
    if (dragged != null) return
    savedOrder = saved
    if (order != saved) applyOrder(saved)
  }

  /** Picks up [section]. Returns false when another row is already held. */
  fun start(section: EventDetailsSection): Boolean {
    if (dragged != null) return false
    settleJobs.remove(section)?.cancel()
    dragOffset = offsets.getValue(section).floatValue
    offsets.getValue(section).floatValue = 0f
    if (settling == section) settling = null
    dragged = section
    return true
  }

  /** Moves the held row by [delta] pixels. Returns true when that changed [order]. */
  fun dragBy(delta: Float): Boolean {
    val section = dragged ?: return false
    dragOffset += delta
    val top = slotTops(order).getValue(section) + clampedDragOffset(section)
    val target = order.reorderedForDrag(section, top, heights, spacing)
    if (target == order) return false
    applyOrder(target)
    return true
  }

  /** The held row's top and bottom in column space, or null when nothing is held. */
  fun draggedSpan(): ClosedFloatingPointRange<Float>? {
    val section = dragged ?: return null
    val top = slotTops(order).getValue(section) + clampedDragOffset(section)
    return top..top + heightOf(section)
  }

  /** Drops the held row into its current slot. Returns the order to save, or null when nothing moved. */
  fun end(): List<EventDetailsSection>? {
    dragged ?: return null
    settleHeldRow()
    return order.takeIf { it != savedOrder }?.also { savedOrder = it }
  }

  /** Abandons the drag and slides every row back to the saved order. */
  fun cancel() {
    dragged ?: return
    applyOrder(savedOrder)
    settleHeldRow()
  }

  private fun settleHeldRow() {
    val section = dragged ?: return
    offsets.getValue(section).floatValue = clampedDragOffset(section)
    dragOffset = 0f
    dragged = null
    settling = section
    slideToRest(section)
  }

  private fun applyOrder(target: List<EventDetailsSection>) {
    val before = slotTops(order)
    val after = slotTops(target)
    order = target
    target.forEach { section ->
      val shift = before.getValue(section) - after.getValue(section)
      if (shift == 0f) return@forEach
      if (section == dragged) {
        dragOffset += shift
      } else {
        offsets.getValue(section).floatValue += shift
        slideToRest(section)
      }
    }
  }

  private fun slideToRest(section: EventDetailsSection) {
    settleJobs.remove(section)?.cancel()
    val offset = offsets.getValue(section)
    settleJobs[section] = scope.launch {
      animate(offset.floatValue, 0f, animationSpec = settleSpec) { value, _ -> offset.floatValue = value }
      if (settling == section) settling = null
    }
  }

  private fun clampedDragOffset(section: EventDetailsSection): Float {
    val top = slotTops(order).getValue(section)
    val listHeight = order.sumOf(::heightOf) + spacing * (order.size - 1)
    val maxTop = (listHeight - heightOf(section)).coerceAtLeast(0f)
    return (top + dragOffset).coerceIn(0f, maxTop) - top
  }

  private fun heightOf(section: EventDetailsSection): Int = heights[section] ?: 0

  private fun slotTops(order: List<EventDetailsSection>): Map<EventDetailsSection, Float> =
    order.slotTops(heights, spacing)
}

internal fun List<EventDetailsSection>.slotTops(
  heights: Map<EventDetailsSection, Int>,
  spacing: Float,
): Map<EventDetailsSection, Float> {
  var top = 0f
  return associateWith { section ->
    top.also { top += (heights[section] ?: 0) + spacing }
  }
}

/**
 * Order once the held [dragged] row, drawn at [top], passes its neighbours. It moves down when its
 * bottom passes the next row's middle and up when its top passes the previous row's middle. Edges
 * rather than the center keep the first and last slots reachable while the row is clamped to the
 * list, whatever the row heights, and a row cannot swap straight back after moving.
 */
internal fun List<EventDetailsSection>.reorderedForDrag(
  dragged: EventDetailsSection,
  top: Float,
  heights: Map<EventDetailsSection, Int>,
  spacing: Float,
): List<EventDetailsSection> {
  val order = toMutableList()
  var index = order.indexOf(dragged)
  if (index < 0) return this
  val bottom = top + (heights[dragged] ?: 0)
  while (true) {
    val tops = order.slotTops(heights, spacing)
    fun middle(section: EventDetailsSection) = tops.getValue(section) + (heights[section] ?: 0) / 2f
    val next = order.getOrNull(index + 1)
    val previous = order.getOrNull(index - 1)
    val step = when {
      next != null && bottom > middle(next) -> 1
      previous != null && top < middle(previous) -> -1
      else -> return order
    }
    order[index] = order[index + step]
    order[index + step] = dragged
    index += step
  }
}

/**
 * Pixels to scroll this frame so the held row stays reachable. Positive scrolls down. Speed ramps up
 * as the row pushes past an [edge]-sized band inside the visible area, up to [maxStep]. A row tall
 * enough to reach both bands scrolls toward the edge it reaches further past, so moving it steers.
 */
internal fun autoScrollStep(
  row: ClosedFloatingPointRange<Float>,
  visible: ClosedFloatingPointRange<Float>,
  edge: Float,
  maxStep: Float,
): Float {
  if (edge <= 0f) return 0f
  val below = row.endInclusive - (visible.endInclusive - edge)
  val above = (visible.start + edge) - row.start
  return when {
    below > 0f && below > above -> maxStep * (below / edge).coerceAtMost(1f)
    above > 0f && above > below -> -maxStep * (above / edge).coerceAtMost(1f)
    else -> 0f
  }
}
