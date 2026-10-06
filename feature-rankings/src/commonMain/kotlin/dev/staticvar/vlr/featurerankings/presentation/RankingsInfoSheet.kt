/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_info_close
import vlr.feature_rankings.generated.resources.rankings_info_context_body
import vlr.feature_rankings.generated.resources.rankings_info_context_title
import vlr.feature_rankings.generated.resources.rankings_info_eligibility_body
import vlr.feature_rankings.generated.resources.rankings_info_eligibility_title
import vlr.feature_rankings.generated.resources.rankings_info_elo_body
import vlr.feature_rankings.generated.resources.rankings_info_elo_title
import vlr.feature_rankings.generated.resources.rankings_info_introduction
import vlr.feature_rankings.generated.resources.rankings_info_title

@Composable
internal fun RankingsInfoSheet(visible: Boolean, onDismiss: () -> Unit) {
  val title = stringResource(Res.string.rankings_info_title)
  PrismModalSheet(
    visible = visible,
    onDismissRequest = onDismiss,
    paneTitle = title,
    header = {
      Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
      )
    },
    footer = {
      PrismButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.rankings_info_close))
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Text(
        text = stringResource(Res.string.rankings_info_introduction),
        style = Prism.typography.bodyLarge,
        color = Prism.color.bodyColor,
      )
      RankingInfoSection(
        title = stringResource(Res.string.rankings_info_eligibility_title),
        body = stringResource(Res.string.rankings_info_eligibility_body),
      )
      RankingInfoSection(
        title = stringResource(Res.string.rankings_info_elo_title),
        body = stringResource(Res.string.rankings_info_elo_body),
      )
      RankingInfoSection(
        title = stringResource(Res.string.rankings_info_context_title),
        body = stringResource(Res.string.rankings_info_context_body),
      )
    }
  }
}

@Composable
private fun RankingInfoSection(title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(
      text = title,
      modifier = Modifier.semantics { heading() },
      style = Prism.typography.cardTitle,
      color = Prism.color.titleColor,
    )
    Text(text = body, style = Prism.typography.bodyLarge, color = Prism.color.bodyColor)
  }
}
