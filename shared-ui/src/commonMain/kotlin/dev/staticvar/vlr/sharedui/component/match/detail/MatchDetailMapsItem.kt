/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice

/**
 * Reusable match-detail maps section.
 *
 * Keeps the section title and map dropdown in one left/right header row, while
 * [MatchDetailMapBreakdown] swaps the content for the selected dropdown value. Spoiler mode
 * replaces both the selector and breakdown with a hidden-results notice.
 */
@Composable
public fun MatchDetailMapsItem(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onPlayerSelected: ((String) -> Unit)? = null,
  onMenuExpandedChange: (Boolean) -> Unit = {},
) {
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val animation = Prism.anim.standard
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(
      title = "Maps",
      preLabel = "breakdown",
      trailing = {
        AnimatedVisibility(
          visible = !spoilersHidden,
          enter = fadeIn(animation.floatSpec()) + expandHorizontally(animationSpec = tween(animation.durationMillis, easing = animation.easing)),
          exit = fadeOut(animation.floatSpec()) + shrinkHorizontally(animationSpec = tween(animation.durationMillis, easing = animation.easing)),
        ) {
          MatchDetailMapsSelectorTrailing(
            maps = maps,
            selectedMapIndex = selectedMapIndex,
            onMapSelected = onMapSelected,
            enabled = enabled,
            onMenuExpandedChange = onMenuExpandedChange,
          )
        }
      },
    )
    AnimatedContent(
      targetState = spoilersHidden,
      modifier = Modifier.fillMaxWidth(),
      transitionSpec = {
        (fadeIn(animation.floatSpec()) togetherWith fadeOut(animation.floatSpec())).using(
          SizeTransform { _, _ -> tween(durationMillis = animation.durationMillis, easing = animation.easing) },
        )
      },
      label = "match_maps_visibility",
    ) { hidden ->
      if (hidden) {
        SpoilerHiddenNotice()
      } else {
        MatchDetailMapBreakdown(
          maps = maps,
          selectedMapIndex = selectedMapIndex,
          onPlayerSelected = onPlayerSelected,
        )
      }
    }
  }
}

@Composable
private fun RowScope.MatchDetailMapsSelectorTrailing(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  enabled: Boolean,
  onMenuExpandedChange: (Boolean) -> Unit,
) {
  MatchDetailMapSelector(
    maps = maps,
    selectedMapIndex = selectedMapIndex,
    onMapSelected = onMapSelected,
    enabled = enabled,
    onMenuExpandedChange = onMenuExpandedChange,
  )
}
