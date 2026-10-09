/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.toSize
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.roundToInt

private class SharedTextColors {
  val entries = mutableStateMapOf<String, List<TextColorEndpoint>>()
}

private class TextColorEndpoint(color: Color, fontSize: Float?) {
  var color: Color by mutableStateOf(color)
  var fontSize: Float? by mutableStateOf(fontSize)
}

private val LocalSharedTextColors = staticCompositionLocalOf<SharedTextColors?> { null }

@Composable
public fun ProvideSharedTextTransitions(content: @Composable () -> Unit) {
  val colors = remember { SharedTextColors() }
  CompositionLocalProvider(LocalSharedTextColors provides colors, content = content)
}

internal data class SharedTextTransition(val modifier: Modifier, val color: Color)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberSharedTextTransition(
  key: String,
  sharedScope: SharedTransitionScope?,
  visibilityScope: AnimatedVisibilityScope?,
  color: Color,
  fontSize: TextUnit? = null,
): SharedTextTransition {
  if (sharedScope == null || visibilityScope == null) return SharedTextTransition(Modifier, color)
  val colors = LocalSharedTextColors.current
  val ownFontSize = fontSize?.value?.takeIf { it > 0f }
  val endpoint = remember(key) { TextColorEndpoint(color, ownFontSize) }
  DisposableEffect(colors, key, endpoint) {
    colors?.entries?.set(key, colors.entries[key].orEmpty() + endpoint)
    onDispose {
      colors?.let { registry ->
        val remaining = registry.entries[key].orEmpty() - endpoint
        if (remaining.isEmpty()) registry.entries.remove(key) else registry.entries[key] = remaining
      }
    }
  }
  SideEffect {
    endpoint.color = color
    endpoint.fontSize = ownFontSize
  }
  val otherEndpoint = colors?.entries?.get(key)?.filter { it !== endpoint }?.singleOrNull()
  val otherColor = otherEndpoint?.color ?: color
  val otherFontSize = otherEndpoint?.fontSize ?: ownFontSize
  val animation = Prism.anim.standard
  return with(sharedScope) {
    val state = rememberSharedContentState(key)
    val animatedColor by visibilityScope.transition.animateColor(
      transitionSpec = {
        if (otherColor == color) snap() else animation.colorSpec()
      },
      label = "shared_text_color",
    ) { visibility ->
      if (visibility == EnterExitState.Visible) color else otherColor
    }
    val animatedFontSize by visibilityScope.transition.animateFloat(
      transitionSpec = {
        if (ownFontSize == null || otherFontSize == ownFontSize) snap() else animation.floatSpec()
      },
      label = "shared_text_size",
    ) { visibility ->
      if (visibility == EnterExitState.Visible) ownFontSize ?: 1f else otherFontSize ?: 1f
    }
    val measurements = remember(key) { TextMeasurements() }
    val modifier = Modifier.sharedElement(
      sharedContentState = state,
      animatedVisibilityScope = visibilityScope,
      boundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = animation.durationMillis, easing = animation.easing)
      },
      zIndexInOverlay = 1f,
    ).clipToBounds().layout { measurable, constraints ->
      if (isLookingAhead) measurements.constraints = constraints
      val scaling = state.isMatchFound && isTransitionActive && measurements.constraints != null
      val child = measurable.measure(if (scaling && !isLookingAhead) measurements.constraints!! else constraints)
      if (isLookingAhead) measurements.size = IntSize(child.width, child.height)
      if (!scaling || isLookingAhead) {
        layout(child.width, child.height) { child.place(0, 0) }
      } else {
        val targetSize = measurements.size
        val size = constraints.constrain(targetSize)
        val scale = if (ownFontSize != null) {
          animatedFontSize / ownFontSize
        } else if (targetSize.width == 0 || targetSize.height == 0) {
          1f
        } else {
          ContentScale.Fit.computeScaleFactor(targetSize.toSize(), size.toSize()).scaleX
        }
        val alignment = if (ownFontSize != null) Alignment.TopStart else Alignment.Center
        val position = alignment.align(
          IntSize((child.width * scale).roundToInt(), (child.height * scale).roundToInt()),
          size,
          layoutDirection,
        )
        layout(size.width, size.height) {
          child.placeWithLayer(position.x, position.y) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
          }
        }
      }
    }
    SharedTextTransition(modifier, animatedColor)
  }
}

private class TextMeasurements {
  var constraints: Constraints? = null
  var size: IntSize = IntSize.Zero
}
