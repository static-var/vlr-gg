/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.illustration.EmptyStateIllustration
import org.jetbrains.compose.resources.stringResource
import vlr.feature_home.generated.resources.Res
import vlr.feature_home.generated.resources.browse_events
import vlr.feature_home.generated.resources.browse_matches
import vlr.feature_home.generated.resources.empty_favorites_description
import vlr.feature_home.generated.resources.make_home_yours

@Composable
internal fun HomeEmptyFavorites(
  onBrowseMatches: () -> Unit,
  onBrowseEvents: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val artworkHeight = minOf(maxHeight * 0.58f, maxWidth * (460f / 360f), 400.dp)
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .heightIn(min = maxHeight).padding(vertical = Prism.dimens.spacingM),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS, Alignment.CenterVertically),
    ) {
      EmptyStateIllustration(
        artwork = EmptyStateArtwork.NoFavorites,
        modifier = Modifier.size(width = artworkHeight * (360f / 460f), height = artworkHeight),
      )
      Text(
        text = stringResource(Res.string.make_home_yours),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(Res.string.empty_favorites_description),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
        textAlign = TextAlign.Center,
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        PrismButton(onClick = onBrowseMatches, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(Res.string.browse_matches))
        }
        PrismButton(onClick = onBrowseEvents, modifier = Modifier.fillMaxWidth(), style = PrismButtonStyle.Secondary) {
          Text(stringResource(Res.string.browse_events))
        }
      }
    }
  }
}
