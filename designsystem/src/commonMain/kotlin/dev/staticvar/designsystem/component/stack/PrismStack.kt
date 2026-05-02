package dev.staticvar.designsystem.component.stack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Prism-flavored swipeable card stack.
 *
 * Phase 1: Drag the top card downward with proportional alpha/scale changes.
 * Phase 2: Animate every surviving card into its new layer.
 *
 * @param shape shape applied to each card; clipping matches the incoming shadow.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
public fun <T> PrismStack(
  modifier: Modifier = Modifier,
  list: List<T>,
  onSwipe: (item: T) -> Unit = {},
  shape: Shape = RoundedCornerShape(PrismStackValues.STACK_CARD_CORNER_RADIUS_DP.dp),
  content: @Composable BoxScope.(item: T) -> Unit,
) {
  if (list.isEmpty()) return

  val visibleCards by remember(list) { derivedStateOf { list.take(4) } }
  val topItem = visibleCards.first()

  val coroutineScope = rememberCoroutineScope()
  val swipeCallback = rememberUpdatedState(onSwipe)

  val layers = remember { StackLayers() }

  val phase2Active = remember { mutableStateOf(false) }
  val isAnimating = remember { mutableStateOf(false) }

  val isAnimatingState = rememberUpdatedState(isAnimating.value)
  val phase2State = rememberUpdatedState(phase2Active.value)

  val resetTopCard: () -> Unit = {
    isAnimating.value = true
    coroutineScope.launch {
      layers.top.animateToBaselineSpring()
      isAnimating.value = false
    }
  }

  LaunchedEffect(topItem) {
    layers.top.snapToBaseline()
    layers.middle.snapTo(StackPosition.Middle)
    layers.bottom.snapTo(StackPosition.Bottom)
    layers.hidden.snapTo(StackPosition.Hidden)

    phase2Active.value = false
    isAnimating.value = false
  }

  val density = LocalDensity.current

  Box(modifier = modifier.padding(vertical = PrismStackValues.STACK_VERTICAL_PADDING_DP.dp)) {
    if (visibleCards.size > 3 && phase2State.value) {
      val incomingItem = visibleCards[3]
      key(incomingItem) {
        Box(
          modifier = Modifier
            .zIndex(StackPosition.Hidden.zIndex)
            .offset(y = layers.hidden.offset.value)
            .graphicsLayer {
              scaleX = layers.hidden.scale.value
              scaleY = layers.hidden.scale.value
              shadowElevation = with(density) { layers.hidden.elevation.value.toPx() }
              this.shape = shape
              clip = true
            },
        ) {
          content(incomingItem)
        }
      }
    }

    visibleCards.take(3).reversed().forEachIndexed { index, item ->
      val stackPosition = when (3 - 1 - index) {
        2 -> StackPosition.Bottom
        1 -> StackPosition.Middle
        else -> StackPosition.Top
      }

      key(item) {
        val isTopCard = stackPosition == StackPosition.Top
        var cardModifier =
          Modifier
            .zIndex(stackPosition.zIndex)
            .run {
              val state =
                when (stackPosition) {
                  StackPosition.Top ->
                    CardVisualState(
                      scale = layers.top.scale.value,
                      offset =
                      StackPosition.Top.baseOffset +
                        with(density) { layers.top.dragOffset.value.y.toDp() },
                      elevation = layers.top.elevation.value,
                      alpha = layers.top.alpha.value,
                    )

                  StackPosition.Middle ->
                    CardVisualState(
                      scale = layers.middle.scale.value,
                      offset = layers.middle.offset.value,
                      elevation = layers.middle.elevation.value,
                      alpha = 1f,
                    )

                  StackPosition.Bottom ->
                    CardVisualState(
                      scale = layers.bottom.scale.value,
                      offset = layers.bottom.offset.value,
                      elevation = layers.bottom.elevation.value,
                      alpha = 1f,
                    )

                  StackPosition.Hidden ->
                    CardVisualState(
                      scale = layers.hidden.scale.value,
                      offset = layers.hidden.offset.value,
                      elevation = layers.hidden.elevation.value,
                      alpha = 1f,
                    )
                }

              offset(y = state.offset).graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                alpha = state.alpha
                shadowElevation = with(density) { state.elevation.toPx() }
                this.shape = shape
                clip = true
              }
            }

        if (isTopCard) {
          cardModifier =
            cardModifier.pointerInput(topItem) {
              val velocityTracker = VelocityTracker()
              detectDragGestures(
                onDragStart = {
                  if (isAnimatingState.value) return@detectDragGestures
                },
                onDrag = { change, dragAmount ->
                  if (isAnimatingState.value) return@detectDragGestures
                  change.consume()
                  velocityTracker.addPosition(change.uptimeMillis, change.position)

                  val newY =
                    (layers.top.dragOffset.value.y + dragAmount.y)
                      .coerceIn(0f, PrismStackValues.MAX_DRAG_DISTANCE_DP)
                  coroutineScope.launch {
                    layers.top.dragOffset.animateTo(Offset(0f, newY))
                    val progress = (newY / PrismStackValues.DRAG_THRESHOLD_DP).coerceIn(0f, 1f)
                    layers.top.alpha.snapTo(lerp(1f, PrismStackValues.MIN_ALPHA, progress))
                    layers.top.scale.snapTo(lerp(1f, PrismStackValues.MIN_SCALE, progress))
                  }
                },
                onDragCancel = {
                  if (isAnimatingState.value) return@detectDragGestures
                  resetTopCard()
                },
                onDragEnd = {
                  if (isAnimatingState.value) return@detectDragGestures

                  val velocity = velocityTracker.calculateVelocity()
                  val dragDistance = layers.top.dragOffset.value.y
                  val shouldRemove =
                    dragDistance > PrismStackValues.DRAG_THRESHOLD_DP ||
                      (velocity.y > PrismStackValues.VELOCITY_THRESHOLD && dragDistance > 0f)

                  if (shouldRemove && list.size > 1) {
                    val currentTop = topItem
                    isAnimating.value = true
                    phase2Active.value = true

                    coroutineScope.launch {
                      launch {
                        layers.top.dragOffset.animateTo(
                          Offset(0f, dragDistance * 2),
                          animationSpec = tween(PrismStackValues.PHASE_2_DURATION_MS),
                        )
                      }
                      launch {
                        layers.top.alpha.animateTo(0f, animationSpec = tween(PrismStackValues.PHASE_2_DURATION_MS))
                      }
                      launch {
                        layers.top.elevation.animateTo(
                          StackPosition.Hidden.baseElevation,
                          animationSpec = tween(PrismStackValues.PHASE_2_DURATION_MS),
                        )
                      }
                      if (visibleCards.size > 1) {
                        launch { layers.middle.animateTo(StackPosition.Top, PrismStackValues.PHASE_2_DURATION_MS) }
                      }
                      if (visibleCards.size > 2) {
                        launch { layers.bottom.animateTo(StackPosition.Middle, PrismStackValues.PHASE_2_DURATION_MS) }
                      }
                      if (visibleCards.size > 3) {
                        launch { layers.hidden.animateTo(StackPosition.Bottom, PrismStackValues.PHASE_2_DURATION_MS) }
                      }
                    }

                    coroutineScope.launch {
                      delay(PrismStackValues.PHASE_2_DURATION_MS.toLong())
                      swipeCallback.value(currentTop)

                      if (phase2Active.value) {
                        phase2Active.value = false
                        layers.top.snapToBaseline()
                        layers.middle.snapTo(StackPosition.Middle)
                        layers.bottom.snapTo(StackPosition.Bottom)
                        layers.hidden.snapTo(StackPosition.Hidden)

                        isAnimating.value = false
                      }
                    }
                  } else {
                    resetTopCard()
                  }
                },
              )
            }
        }

        Box(modifier = cardModifier) {
          content(item)
        }
      }
    }
  }
}

/**
 * Defines the stacked layer positions used by [PrismStack].
 *
 * Each entry exposes the baseline offset, scale, elevation, and z-index that other
 * animation code references when cards transition between layers.
 */
private sealed interface StackPosition {
  val baseOffset: Dp
  val baseScale: Float
  val baseElevation: Dp
  val zIndex: Float

  data object Top : StackPosition {
    override val baseOffset: Dp = (2 * PrismStackValues.CARD_OFFSET_DP).dp
    override val baseScale: Float = 1f
    override val baseElevation: Dp = PrismStackValues.TOP_CARD_ELEVATION_DP.dp
    override val zIndex: Float = 3f
  }

  data object Middle : StackPosition {
    override val baseOffset: Dp = PrismStackValues.CARD_OFFSET_DP.dp
    override val baseScale: Float = 1f - PrismStackValues.SCALE_MULTIPLIER
    override val baseElevation: Dp = PrismStackValues.MIDDLE_CARD_ELEVATION_DP.dp
    override val zIndex: Float = 2f
  }

  data object Bottom : StackPosition {
    override val baseOffset: Dp = 0.dp
    override val baseScale: Float = 1f - (PrismStackValues.SCALE_MULTIPLIER * 2)
    override val baseElevation: Dp = PrismStackValues.BOTTOM_CARD_ELEVATION_DP.dp
    override val zIndex: Float = 1f
  }

  data object Hidden : StackPosition {
    override val baseOffset: Dp = (-PrismStackValues.CARD_OFFSET_DP).dp
    override val baseScale: Float = 1f - (PrismStackValues.SCALE_MULTIPLIER * 3)
    override val baseElevation: Dp = PrismStackValues.HIDDEN_CARD_ELEVATION_DP.dp
    override val zIndex: Float = 0f
  }
}

/** Snapshot of the drawing parameters used while composing a stack layer. */
private data class CardVisualState(val scale: Float, val offset: Dp, val elevation: Dp, val alpha: Float)

/**
 * Aggregates the composable animation state for each visible and incoming layer.
 */
private class StackLayers {
  val top = TopLayerState()
  val middle = CardLayerState(StackPosition.Middle)
  val bottom = CardLayerState(StackPosition.Bottom)
  val hidden = CardLayerState(StackPosition.Hidden)
}

/**
 * Animation channels dedicated to the interactive top card.
 */
private class TopLayerState {
  val dragOffset = Animatable(Offset.Zero, Offset.VectorConverter)
  val scale = Animatable(StackPosition.Top.baseScale)
  val alpha = Animatable(1f)
  val elevation = Animatable(StackPosition.Top.baseElevation, Dp.VectorConverter)

  suspend fun animateToBaselineSpring() {
    coroutineScope {
      launch { dragOffset.animateTo(Offset.Zero, spring<Offset>()) }
      launch { scale.animateTo(StackPosition.Top.baseScale, spring<Float>()) }
      launch { alpha.animateTo(1f, spring<Float>()) }
      launch { elevation.animateTo(StackPosition.Top.baseElevation, spring<Dp>()) }
    }
  }

  suspend fun snapToBaseline() {
    dragOffset.snapTo(Offset.Zero)
    scale.snapTo(StackPosition.Top.baseScale)
    alpha.snapTo(1f)
    elevation.snapTo(StackPosition.Top.baseElevation)
  }
}

/**
 * Shared animation container for non-interactive stack layers.
 */
private class CardLayerState(private val defaultPosition: StackPosition) {
  val scale = Animatable(defaultPosition.baseScale)
  val offset = Animatable(defaultPosition.baseOffset, Dp.VectorConverter)
  val elevation = Animatable(defaultPosition.baseElevation, Dp.VectorConverter)

  suspend fun snapTo(position: StackPosition = defaultPosition) {
    scale.snapTo(position.baseScale)
    offset.snapTo(position.baseOffset)
    elevation.snapTo(position.baseElevation)
  }

  suspend fun animateTo(position: StackPosition, durationMillis: Int) {
    val floatSpec = tween<Float>(durationMillis = durationMillis)
    val dpSpec = tween<Dp>(durationMillis = durationMillis)
    coroutineScope {
      launch { scale.animateTo(position.baseScale, floatSpec) }
      launch { offset.animateTo(position.baseOffset, dpSpec) }
      launch { elevation.animateTo(position.baseElevation, dpSpec) }
    }
  }
}

/**
 * Internal constants that shape PrismStack choreography.
 */
private object PrismStackValues {
  const val SCALE_MULTIPLIER = 0.08f
  const val DRAG_THRESHOLD_DP = 100f
  const val MAX_DRAG_DISTANCE_DP = 250f
  const val VELOCITY_THRESHOLD = 1000f
  const val MIN_SCALE = 0.7f
  const val MIN_ALPHA = 0.3f
  const val PHASE_2_DURATION_MS = 400
  const val CARD_OFFSET_DP = 24
  const val STACK_VERTICAL_PADDING_DP = 100
  const val STACK_CARD_CORNER_RADIUS_DP = 24
  const val TOP_CARD_ELEVATION_DP = 16f
  const val MIDDLE_CARD_ELEVATION_DP = 8f
  const val BOTTOM_CARD_ELEVATION_DP = 2f
  const val HIDDEN_CARD_ELEVATION_DP = 0f
}
