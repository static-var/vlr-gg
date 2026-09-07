/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.loader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor
import kotlin.math.absoluteValue

private object PrismLoaderConstants {
  val smallWidth: Dp = 96.dp
  val mediumWidth: Dp = 128.dp
  val largeWidth: Dp = 160.dp
  val smallHeight: Dp = 36.dp
  val mediumHeight: Dp = 64.dp
  val largeHeight: Dp = 72.dp
  val smallSegmentHeight: Dp = 8.dp
  val mediumSegmentHeight: Dp = 16.dp
  val largeSegmentHeight: Dp = 20.dp
  const val defaultSegmentCount: Int = 5
  const val phaseSweepMillis: Int = 2200
  const val pulseMillis: Int = 900
  val phaseLabels: List<String> = listOf("SYNC", "FETCH", "PARSE", "MERGE", "RENDER")
}

public enum class PrismLoaderSize {
  Small,
  Medium,
  Large,
}

@Composable
public fun PrismLoader(
  modifier: Modifier = Modifier,
  size: PrismLoaderSize = PrismLoaderSize.Medium,
  label: String = "LOADING",
  color: Color = Prism.color.accent,
  trackColor: Color = Prism.color.surface,
  containerColor: Color = Prism.color.background,
  containerContentColor: Color = contentColorFor(containerColor),
  shape: Shape = Prism.shapes.small,
) {
  val metrics = loaderMetrics(size)
  val progress = rememberLoaderProgress()
  val animatedLabelText = loaderLabelText(label = label, phaseProgress = progress.phase)

  PrismSurface(
    modifier =
    modifier
      .width(metrics.containerWidth)
      .defaultMinSize(minHeight = metrics.containerHeight),
    color = containerColor,
    contentColor = containerContentColor,
    shape = shape,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Box(
      modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = metrics.containerHeight)
        .padding(Prism.dimens.spacingS),
    ) {
      if (metrics.showLabel) {
        LabeledLoaderContent(
          label = animatedLabelText,
          metrics = metrics,
          colors = LoaderColors(color = color, trackColor = trackColor),
          progress = progress,
        )
      } else {
        LoaderSegments(
          modifier = Modifier.fillMaxWidth().align(Alignment.Center),
          metrics = metrics,
          colors = LoaderColors(color = color, trackColor = trackColor),
          progress = progress,
        )
      }
    }
  }
}

private fun loaderMetrics(size: PrismLoaderSize): LoaderMetrics = when (size) {
  PrismLoaderSize.Small ->
    LoaderMetrics(
      containerWidth = PrismLoaderConstants.smallWidth,
      containerHeight = PrismLoaderConstants.smallHeight,
      segmentHeight = PrismLoaderConstants.smallSegmentHeight,
      showLabel = false,
    )

  PrismLoaderSize.Medium ->
    LoaderMetrics(
      containerWidth = PrismLoaderConstants.mediumWidth,
      containerHeight = PrismLoaderConstants.mediumHeight,
      segmentHeight = PrismLoaderConstants.mediumSegmentHeight,
      showLabel = true,
    )

  PrismLoaderSize.Large ->
    LoaderMetrics(
      containerWidth = PrismLoaderConstants.largeWidth,
      containerHeight = PrismLoaderConstants.largeHeight,
      segmentHeight = PrismLoaderConstants.largeSegmentHeight,
      showLabel = true,
    )
}

@Composable
private fun rememberLoaderProgress(): LoaderProgress {
  val infiniteTransition = rememberInfiniteTransition(label = "prism_loader")
  val phaseProgress =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = PrismLoaderConstants.defaultSegmentCount.toFloat(),
      animationSpec =
      infiniteRepeatable(
        animation = tween(durationMillis = PrismLoaderConstants.phaseSweepMillis, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "prism_loader_phase",
    )
  val pulseProgress =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
      infiniteRepeatable(
        animation = tween(durationMillis = PrismLoaderConstants.pulseMillis, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "prism_loader_pulse",
    )

  return LoaderProgress(phase = phaseProgress.value, pulse = pulseProgress.value)
}

private fun loaderLabelText(label: String, phaseProgress: Float): String {
  val phaseLabel = PrismLoaderConstants.phaseLabels[phaseProgress.toInt() % PrismLoaderConstants.phaseLabels.size]
  val resolvedLabel = label.trim()
  return if (resolvedLabel.isEmpty()) phaseLabel else "$resolvedLabel · $phaseLabel"
}

@Composable
private fun LabeledLoaderContent(
  label: String,
  metrics: LoaderMetrics,
  colors: LoaderColors,
  progress: LoaderProgress,
) {
  Column(
    modifier = Modifier.fillMaxWidth().heightIn(min = metrics.containerHeight),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LoaderPhaseLabel(text = label)
    LoaderSegments(
      modifier = Modifier.fillMaxWidth(),
      metrics = metrics,
      colors = colors,
      progress = progress,
    )
  }
}

@Composable
private fun LoaderPhaseLabel(text: String) {
  val animations = Prism.anim
  AnimatedContent(
    targetState = text,
    transitionSpec = {
      fadeIn(animationSpec = animations.slowFade.floatSpec()) togetherWith
        fadeOut(animationSpec = animations.standard.floatSpec())
    },
    modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingXs),
    label = "loader_phase_label",
  ) { animatedLabel ->
    Text(
      text = animatedLabel,
      style = Prism.typography.caption,
      color = Prism.color.titleColor,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun LoaderSegments(
  modifier: Modifier,
  metrics: LoaderMetrics,
  colors: LoaderColors,
  progress: LoaderProgress,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    repeat(PrismLoaderConstants.defaultSegmentCount) { index ->
      LoaderSegment(index = index, metrics = metrics, colors = colors, progress = progress)
    }
  }
}

@Composable
private fun RowScope.LoaderSegment(
  index: Int,
  metrics: LoaderMetrics,
  colors: LoaderColors,
  progress: LoaderProgress,
) {
  val distance = (progress.phase - index.toFloat()).absoluteValue
  val segmentStrength = if (distance >= 1f) 0f else 1f - distance
  val pulseStrength = (segmentStrength + (progress.pulse * 0.4f)).coerceIn(0f, 1f)
  val segmentColor = lerp(colors.trackColor, colors.color.copy(alpha = 0.20f), pulseStrength)
  val segmentBorderColor = lerp(Prism.color.stroke, colors.color, pulseStrength)

  PrismSurface(
    modifier = Modifier.weight(1f).height(metrics.segmentHeight),
    color = segmentColor,
    shape = Prism.shapes.small,
    border = BorderStroke(Prism.dimens.strokeDefault, segmentBorderColor),
  ) {
    Box(modifier = Modifier.fillMaxWidth())
  }
}

/**
 * Fullscreen/available-space loader container that centers [PrismLoader] in parent bounds.
 */
@Composable
public fun PrismFullscreenLoader(
  modifier: Modifier = Modifier,
  size: PrismLoaderSize = PrismLoaderSize.Large,
  label: String = "LOADING",
  supportingText: String? = null,
  color: Color = Prism.color.accent,
  trackColor: Color = Prism.color.surface,
  backgroundColor: Color = Prism.color.background,
  panelColor: Color = Prism.color.backgroundElevated,
  shape: Shape = Prism.shapes.small,
) {
  val resolvedSupportingText = supportingText?.trim().orEmpty()

  Box(
    modifier =
    modifier
      .fillMaxSize()
      .background(backgroundColor),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      PrismLoader(
        size = size,
        label = label,
        color = color,
        trackColor = trackColor,
        containerColor = panelColor,
        containerContentColor = contentColorFor(panelColor),
        shape = shape,
      )

      if (resolvedSupportingText.isNotEmpty()) {
        Text(
          text = resolvedSupportingText,
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

private data class LoaderMetrics(
  val containerWidth: Dp,
  val containerHeight: Dp,
  val segmentHeight: Dp,
  val showLabel: Boolean,
)

private data class LoaderColors(val color: Color, val trackColor: Color)

private data class LoaderProgress(val phase: Float, val pulse: Float)
