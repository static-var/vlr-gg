/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.reorder

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder state for the rows of a [PrismReorderableOptionList].
 *
 * Rows sit in a plain column, so slot positions come from measured heights in the column's own
 * space and stay put while the container scrolls or animates. [order] changes live during a drag;
 * the saved order changes only when [end] returns a new one. Rows that change slots get an offset
 * back to where they were drawn, then slide to rest, so nothing jumps. Every saved order holds the
 * same unique keys.
 */
// One cohesive drag state machine; splitting it would only spread its shared fields around.
@Suppress("TooManyFunctions")
@Stable
internal class PrismReorderState<K : Any>(
  savedOrder: List<K>,
  private val scope: CoroutineScope,
  private val spacing: Float,
  private val settleSpec: AnimationSpec<Float>,
) {
  private var savedOrder = savedOrder

  // The order a drag started from, so a drop back into place is not mistaken for a move.
  private var dragStartOrder: List<K> = savedOrder

  var order: List<K> by mutableStateOf(savedOrder)
    private set

  var dragged: K? by mutableStateOf(null)
    private set

  private var settling: K? by mutableStateOf(null)

  // Follows the finger without clamping, so the row catches up only once the finger is back over the list.
  private var dragOffset by mutableFloatStateOf(0f)
  private val heights = mutableMapOf<K, Int>()
  private val offsets = mutableMapOf<K, MutableFloatState>()
  private val settleJobs = mutableMapOf<K, Job>()

  /** Vertical draw offset of [key] from its slot. */
  fun offsetOf(key: K): Float = if (key == dragged) clampedDragOffset(key) else offsetState(key).floatValue

  /** Whether [key] is held or still settling after a drop, so it draws above its neighbours. */
  fun isLifted(key: K): Boolean = key == dragged || key == settling

  /** Records a row's height. A held row keeps its drawn top when a row above it resizes mid-drag. */
  fun onMeasured(key: K, height: Int) {
    val held = dragged
    if (held == null || heights[key] == height) {
      heights[key] = height
      return
    }
    val before = slotTops(order).getValue(held)
    heights[key] = height
    dragOffset += before - slotTops(order).getValue(held)
  }

  /**
   * Adopts a newly saved order. Mid-drag it is only remembered: the rows follow the finger, and the
   * drop or cancel settles against the latest saved order.
   */
  fun sync(saved: List<K>) {
    savedOrder = saved
    if (dragged == null && order != saved) applyOrder(saved)
  }

  /** Picks up [key]. Returns false when another row is already held. */
  fun start(key: K): Boolean {
    if (dragged != null) return false
    settleJobs.remove(key)?.cancel()
    dragOffset = offsetState(key).floatValue
    offsetState(key).floatValue = 0f
    if (settling == key) settling = null
    dragStartOrder = order
    dragged = key
    return true
  }

  /** Moves the held row by [delta] pixels. Returns true when that changed [order]. */
  fun dragBy(delta: Float): Boolean {
    val key = dragged ?: return false
    dragOffset += delta
    val top = slotTops(order).getValue(key) + clampedDragOffset(key)
    val target = order.reorderedForDrag(key, top, heights, spacing)
    if (target == order) return false
    applyOrder(target)
    return true
  }

  /** The held row's top and bottom in column space, or null when nothing is held. */
  fun draggedSpan(): ClosedFloatingPointRange<Float>? {
    val key = dragged ?: return null
    val top = slotTops(order).getValue(key) + clampedDragOffset(key)
    return top..top + heightOf(key)
  }

  /** Drops the held row into its current slot. Returns the order to save, or null when nothing moved. */
  fun end(): List<K>? {
    dragged ?: return null
    settleHeldRow()
    if (order == dragStartOrder) {
      if (order != savedOrder) applyOrder(savedOrder)
      return null
    }
    return order.takeIf { it != savedOrder }?.also { savedOrder = it }
  }

  /** Abandons the drag and slides every row back to the saved order. */
  fun cancel() {
    dragged ?: return
    applyOrder(savedOrder)
    settleHeldRow()
  }

  private fun settleHeldRow() {
    val key = dragged ?: return
    offsetState(key).floatValue = clampedDragOffset(key)
    dragOffset = 0f
    dragged = null
    settling = key
    slideToRest(key)
  }

  private fun applyOrder(target: List<K>) {
    val before = slotTops(order)
    val after = slotTops(target)
    order = target
    target.forEach { key ->
      val shift = before.getValue(key) - after.getValue(key)
      if (shift == 0f) return@forEach
      if (key == dragged) {
        dragOffset += shift
      } else {
        offsetState(key).floatValue += shift
        slideToRest(key)
      }
    }
  }

  private fun slideToRest(key: K) {
    settleJobs.remove(key)?.cancel()
    val offset = offsetState(key)
    settleJobs[key] = scope.launch {
      animate(offset.floatValue, 0f, animationSpec = settleSpec) { value, _ -> offset.floatValue = value }
      if (settling == key) settling = null
    }
  }

  private fun clampedDragOffset(key: K): Float {
    val top = slotTops(order).getValue(key)
    val listHeight = order.sumOf(::heightOf) + spacing * (order.size - 1)
    val maxTop = (listHeight - heightOf(key)).coerceAtLeast(0f)
    return (top + dragOffset).coerceIn(0f, maxTop) - top
  }

  private fun offsetState(key: K): MutableFloatState = offsets.getOrPut(key) { mutableFloatStateOf(0f) }

  private fun heightOf(key: K): Int = heights[key] ?: 0

  private fun slotTops(order: List<K>): Map<K, Float> = order.slotTops(heights, spacing)
}

internal fun <K> List<K>.slotTops(heights: Map<K, Int>, spacing: Float): Map<K, Float> {
  var top = 0f
  return associateWith { key ->
    top.also { top += (heights[key] ?: 0) + spacing }
  }
}

/**
 * Order once the held [dragged] row, drawn at [top], passes its neighbours. It moves down when its
 * bottom passes the next row's middle and up when its top passes the previous row's middle. Edges
 * rather than the center keep the first and last slots reachable while the row is clamped to the
 * list, whatever the row heights, and a row cannot swap straight back after moving.
 */
internal fun <K> List<K>.reorderedForDrag(dragged: K, top: Float, heights: Map<K, Int>, spacing: Float): List<K> {
  val order = toMutableList()
  var index = order.indexOf(dragged)
  if (index < 0) return this
  val bottom = top + (heights[dragged] ?: 0)
  while (true) {
    val tops = order.slotTops(heights, spacing)
    fun middle(key: K) = tops.getValue(key) + (heights[key] ?: 0) / 2f
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
