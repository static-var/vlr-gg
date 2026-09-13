/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.roundToInt

internal data class GroupedListDetailScene<T : Any>(
  override val key: Any,
  override val previousEntries: List<NavEntry<T>>,
  private val listEntry: NavEntry<T>,
  private val detailEntry: NavEntry<T>?,
) : Scene<T> {
  val paneState: GroupedPaneState = GroupedPaneState(key, detailEntry)
  override val entries: List<NavEntry<T>> = listOfNotNull(listEntry, detailEntry)
  override val content: @Composable () -> Unit = {
    GroupedListDetailContent(listEntry, detailEntry)
  }
}

@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun <T : Any> GroupedListDetailContent(listEntry: NavEntry<T>, detailEntry: NavEntry<T>?) {
  val paneTransition = checkNotNull(LocalGroupedPaneTransition.current)
  val detailTransition = paneTransition.createChildTransition(label = "detail pane") { pane ->
    if (pane?.groupKey == listEntry.contentKey) pane.detailEntry else detailEntry
  }
  val detailVisibility = detailTransition.animateFloat(
    transitionSpec = { tween(durationMillis = PaneTransitionDurationMillis, easing = FastOutSlowInEasing) },
    label = "detail pane width",
  ) { if (it == null) 0f else 1f }
  val paneSpacing = Prism.dimens.spacingS

  Layout(
    modifier = Modifier.fillMaxSize().clipToBounds(),
    content = {
      Box(Modifier.fillMaxSize()) { listEntry.Content() }
      detailTransition.AnimatedContent(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
        contentKey = { it?.contentKey },
        transitionSpec = {
          (fadeIn(tween(PaneTransitionDurationMillis)) togetherWith fadeOut(tween(PaneTransitionDurationMillis))).using(null)
        },
      ) { entry ->
        Box(Modifier.fillMaxSize()) { entry?.Content() }
      }
    },
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val spacing = paneSpacing.roundToPx().coerceAtMost(width)
    val detailWidth = ((width - spacing) * 0.6f).roundToInt()
    val progress = detailVisibility.value
    val visibleDetailWidth = (detailWidth * progress).roundToInt()
    val visibleSpacing = (spacing * progress).roundToInt()
    val listWidth = width - visibleDetailWidth - visibleSpacing
    val list = measurables[0].measure(Constraints.fixed(listWidth, height))
    val detail = measurables[1].measure(Constraints.fixed(detailWidth, height))
    layout(width, height) {
      list.placeRelative(0, 0)
      detail.placeRelative(listWidth + visibleSpacing, 0)
    }
  }
}

internal class GroupedListDetailSceneStrategy<T : Any>(private val enabled: Boolean) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    if (!enabled) {
      return null
    }

    val topEntry = entries.lastOrNull() ?: return null
    val detailGroup = topEntry.metadata[DetailGroupKey] as? String
    val detailEntry: NavEntry<T>?
    val listEntry: NavEntry<T>
    if (detailGroup != null) {
      listEntry = entries.dropLast(1).findLast { it.metadata[ListGroupKey] == detailGroup } ?: return null
      detailEntry = topEntry
    } else {
      if (topEntry.metadata[ListGroupKey] !is String) return null
      listEntry = topEntry
      detailEntry = null
    }

    return GroupedListDetailScene(
      key = listEntry.contentKey,
      previousEntries = entries.dropLast(1),
      listEntry = listEntry,
      detailEntry = detailEntry,
    )
  }
}

@Composable
internal fun <T : Any> rememberGroupedListDetailSceneStrategy(enabled: Boolean): SceneStrategy<T> = remember(enabled) {
  GroupedListDetailSceneStrategy(enabled = enabled)
}

internal fun listPane(group: String): Map<String, Any> = mapOf(ListGroupKey to group)

internal fun detailPane(group: String): Map<String, Any> = mapOf(DetailGroupKey to group)

private const val ListGroupKey: String = "AppListDetail-ListGroup"
private const val DetailGroupKey: String = "AppListDetail-DetailGroup"
private const val PaneTransitionDurationMillis: Int = 300
