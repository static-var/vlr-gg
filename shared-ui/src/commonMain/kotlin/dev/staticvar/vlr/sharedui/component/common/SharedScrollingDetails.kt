/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.prism.Prism

private const val DetailHeroKey = "shared-detail-hero"

@Composable
public fun SharedScrollingDetails(
  state: LazyListState,
  contentAlpha: State<Float>,
  showContent: Boolean,
  modifier: Modifier = Modifier,
  hero: @Composable () -> Unit,
  loading: @Composable (Modifier) -> Unit,
  content: LazyListScope.() -> Unit,
) {
  Box(modifier) {
    LazyColumn(
      state = state,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      modifier = Modifier.fillMaxSize().cardMascotViewport(),
    ) {
      item(key = DetailHeroKey, contentType = DetailHeroKey) { hero() }
      if (showContent) FadingDetailItems(this, contentAlpha).content()
    }
    if (!showContent || contentAlpha.value < 1f) {
      loading(
        Modifier.matchParentSize()
          .layout { measurable, constraints ->
            val visibleItems = state.layoutInfo.visibleItemsInfo
            val heroItem = visibleItems.firstOrNull { it.key == DetailHeroKey }
            val heroBottom = when {
              heroItem != null -> heroItem.offset + heroItem.size
              visibleItems.isNotEmpty() -> 0
              else -> constraints.maxHeight
            }.coerceIn(0, constraints.maxHeight)
            val height = constraints.maxHeight - heroBottom
            val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
            layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, heroBottom) }
          }
          .graphicsLayer {
            alpha = 1f - contentAlpha.value
            clip = true
          },
      )
    }
  }
}

private class FadingDetailItems(
  private val scope: LazyListScope,
  private val alpha: State<Float>,
) : LazyListScope by scope {
  override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
    scope.item(key, contentType) {
      Box(Modifier.fillMaxWidth().graphicsLayer { alpha = this@FadingDetailItems.alpha.value }) { content() }
    }
  }

  override fun items(
    count: Int,
    key: ((Int) -> Any)?,
    contentType: (Int) -> Any?,
    itemContent: @Composable LazyItemScope.(Int) -> Unit,
  ) {
    scope.items(count, key, contentType) { index ->
      Box(Modifier.fillMaxWidth().graphicsLayer { alpha = this@FadingDetailItems.alpha.value }) { itemContent(index) }
    }
  }
}
