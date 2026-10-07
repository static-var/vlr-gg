/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.feature_team.generated.resources.Res
import vlr.feature_team.generated.resources.team_rating_beta
import vlr.feature_team.generated.resources.team_rating_info_beta_body
import vlr.feature_team.generated.resources.team_rating_info_beta_title
import vlr.feature_team.generated.resources.team_rating_info_context_body
import vlr.feature_team.generated.resources.team_rating_info_context_title
import vlr.feature_team.generated.resources.team_rating_info_done
import vlr.feature_team.generated.resources.team_rating_info_eligibility_body
import vlr.feature_team.generated.resources.team_rating_info_eligibility_title
import vlr.feature_team.generated.resources.team_rating_info_elo_body
import vlr.feature_team.generated.resources.team_rating_info_elo_title
import vlr.feature_team.generated.resources.team_rating_info_records_body
import vlr.feature_team.generated.resources.team_rating_info_records_title
import vlr.feature_team.generated.resources.team_rating_info_scopes_body
import vlr.feature_team.generated.resources.team_rating_info_scopes_title
import vlr.feature_team.generated.resources.team_rating_info_title

@Composable
internal fun TeamRatingInfoSheet(visible: Boolean, onDismiss: () -> Unit) {
  val title = stringResource(Res.string.team_rating_info_title)
  PrismModalSheet(
    visible = visible,
    onDismissRequest = onDismiss,
    paneTitle = title,
    dragHandle = null,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          modifier = Modifier.weight(1f).semantics { heading() },
          style = Prism.typography.sectionTitle,
          color = Prism.color.titleColor,
        )
        PrismTag(text = stringResource(Res.string.team_rating_beta), style = PrismTagStyle.Accent)
      }
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_beta_title),
        body = stringResource(Res.string.team_rating_info_beta_body),
      )
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_scopes_title),
        body = stringResource(Res.string.team_rating_info_scopes_body),
      )
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_elo_title),
        body = stringResource(Res.string.team_rating_info_elo_body),
      )
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_eligibility_title),
        body = stringResource(Res.string.team_rating_info_eligibility_body),
      )
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_records_title),
        body = stringResource(Res.string.team_rating_info_records_body),
      )
      TeamRatingInfoSection(
        title = stringResource(Res.string.team_rating_info_context_title),
        body = stringResource(Res.string.team_rating_info_context_body),
      )
      PrismButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.team_rating_info_done))
      }
    }
  }
}

@Composable
private fun TeamRatingInfoSection(title: String, body: String) {
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
