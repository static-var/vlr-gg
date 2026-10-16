/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.about_win_probability
import vlr.feature_matches.generated.resources.done
import vlr.feature_matches.generated.resources.elo_system
import vlr.feature_matches.generated.resources.prediction_about
import vlr.feature_matches.generated.resources.prediction_about_chances
import vlr.feature_matches.generated.resources.prediction_about_history
import vlr.feature_matches.generated.resources.prediction_about_outcome
import vlr.feature_matches.generated.resources.prediction_analysis_only
import vlr.feature_matches.generated.resources.prediction_gambling_notice
import vlr.feature_matches.generated.resources.prediction_limited_map_data
import vlr.feature_matches.generated.resources.prediction_limited_map_history
import vlr.feature_matches.generated.resources.prediction_limited_team_history
import vlr.feature_matches.generated.resources.prediction_model
import vlr.feature_matches.generated.resources.prediction_no_map_history
import vlr.feature_matches.generated.resources.prediction_no_match_history
import vlr.feature_matches.generated.resources.prediction_patch_not_specified
import vlr.feature_matches.generated.resources.prediction_unknown_warning
import vlr.feature_matches.generated.resources.win_probability
import vlr.feature_matches.generated.resources.insights
import vlr.feature_matches.generated.resources.pre_game_summary
import vlr.feature_matches.generated.resources.post_game_summary

@Composable
internal fun MatchInsightsSection(match: MatchDetails) {
  val summary = match.selectedInsightSummary()
  val prediction = match.insights?.prediction
  if (summary == null && prediction == null) return

  val aboutSheet = remember(match.id) { PredictionAboutSheetState(AppTelemetry) }
  LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = aboutSheet::cancelOpening)
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    PrismSectionTitle(title = stringResource(Res.string.insights))
    if (summary != null) {
      PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
        Text(
          text = stringResource(
            when (summary.phase) {
              MatchSummaryPhase.PreGame -> Res.string.pre_game_summary
              MatchSummaryPhase.PostGame -> Res.string.post_game_summary
            },
          ),
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
        )
        if (summary.phase == MatchSummaryPhase.PostGame && LocalSpoilerMode.current.enabled) {
          SpoilerHiddenNotice(modifier = Modifier.padding(top = Prism.dimens.spacingS))
        } else {
          Text(
            text = summary.summary.text,
            modifier = Modifier.padding(top = Prism.dimens.spacingS),
            style = Prism.typography.bodyLarge,
            color = Prism.color.bodyColor,
          )
        }
      }
    }
    if (prediction != null) {
      PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
        Text(
          text = stringResource(Res.string.win_probability),
          modifier = Modifier.padding(bottom = Prism.dimens.spacingM),
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
        )
        PredictionProbabilities(match, prediction)
        PrismDivider(modifier = Modifier.padding(top = Prism.dimens.spacingM))
        PredictionFooter(prediction, onAbout = aboutSheet::open)
        PrismDivider()
        GamblingNotice(modifier = Modifier.padding(top = Prism.dimens.spacingS))
      }
    }
  }
  PredictionAboutSheet(
    visible = aboutSheet.visible,
    onDismiss = aboutSheet::dismiss,
    onEnterFinished = aboutSheet::onEnterFinished,
  )
}

@Composable
private fun PredictionProbabilities(match: MatchDetails, prediction: MatchPrediction) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    PredictionTeam(
      name = match.teams.firstOrNull { it.id == prediction.teamAId }?.name.orEmpty(),
      probability = prediction.teamAProbability,
      accent = true,
      modifier = Modifier.weight(1f),
    )
    PredictionTeam(
      name = match.teams.firstOrNull { it.id == prediction.teamBId }?.name.orEmpty(),
      probability = prediction.teamBProbability,
      accent = false,
      modifier = Modifier.weight(1f),
    )
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM).height(8.dp),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    if (prediction.teamAProbability > 0.0) Box(Modifier.weight(prediction.teamAProbability.toFloat()).height(8.dp).background(Prism.color.accent))
    if (prediction.teamBProbability > 0.0) Box(Modifier.weight(prediction.teamBProbability.toFloat()).height(8.dp).background(Prism.color.contentSecondary))
  }
}

@Composable
private fun PredictionTeam(name: String, probability: Double, accent: Boolean, modifier: Modifier) {
  val alignment = if (accent) Alignment.Start else Alignment.End
  val textAlign = if (accent) TextAlign.Start else TextAlign.End
  Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(name, style = Prism.typography.label, color = Prism.color.bodyColor, textAlign = textAlign)
    Text(
      probability.asPercentage(),
      style = Prism.typography.numericPrimary.copy(fontSize = 32.sp),
      color = if (accent) Prism.color.accent else Prism.color.titleColor,
      textAlign = textAlign,
    )
  }
}

private fun Double.asPercentage(): String {
  val tenths = (this * 1000).roundToInt()
  return if (tenths % 10 == 0) "${tenths / 10}%" else "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun PredictionFooter(prediction: MatchPrediction, onAbout: () -> Unit) {
  val warning = prediction.warnings.primaryPredictionWarning()
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    if (warning != null) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        Icon(WarningIcon, contentDescription = null, tint = Prism.color.warning, modifier = Modifier.size(14.dp))
        Text(stringResource(warning), style = Prism.typography.caption.copy(fontSize = 11.sp), color = Prism.color.bodyColor)
      }
    }
    PrismTag(stringResource(if (prediction.source == PredictionSource.MODEL) Res.string.prediction_model else Res.string.elo_system))
    if (warning == null) Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(role = Role.Button, onClick = onAbout),
      contentAlignment = Alignment.Center,
    ) {
      Text(stringResource(Res.string.prediction_about), style = Prism.typography.bodySmall, color = Prism.color.accent)
    }
  }
}

@Composable
private fun GamblingNotice(modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(stringResource(Res.string.prediction_analysis_only), style = Prism.typography.caption, color = Prism.color.titleColor)
    Text(
      stringResource(Res.string.prediction_gambling_notice),
      style = Prism.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
      color = Prism.color.bodyColor,
    )
  }
}

@Composable
private fun PredictionAboutSheet(visible: Boolean, onDismiss: () -> Unit, onEnterFinished: () -> Unit) {
  val title = stringResource(Res.string.about_win_probability)
  PrismModalSheet(
    visible = visible,
    onDismissRequest = onDismiss,
    paneTitle = title,
    onEnterFinished = onEnterFinished,
    header = { Text(title, style = Prism.typography.sectionTitle, color = Prism.color.titleColor) },
    footer = { PrismButton(onClick = onDismiss, style = PrismButtonStyle.Secondary) { Text(stringResource(Res.string.done)) } },
  ) {
    for (paragraph in listOf(Res.string.prediction_about_chances, Res.string.prediction_about_history, Res.string.prediction_about_outcome)) {
      Text(stringResource(paragraph), style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
    }
    GamblingNotice(modifier = Modifier.padding(top = Prism.dimens.spacingS))
  }
}

internal fun List<PredictionWarning>.primaryPredictionWarning(): StringResource? =
  asSequence().filter { it.code != PredictionWarningCode.ELO_FALLBACK }
    .maxByOrNull { it.code.priority() }?.code?.label()

private fun PredictionWarningCode.priority(): Int = when (this) {
  PredictionWarningCode.NO_ELIGIBLE_HISTORY -> 100
  PredictionWarningCode.UNKNOWN_TEAM -> 90
  PredictionWarningCode.NO_TEAM_MAP_HISTORY -> 80
  PredictionWarningCode.MAP_NOT_IN_MODEL, PredictionWarningCode.UNVALIDATED_MAP_FALLBACK -> 70
  PredictionWarningCode.UNKNOWN -> 60
  PredictionWarningCode.LOW_COVERAGE -> 50
  PredictionWarningCode.LIMITED_TEAM_MAP_HISTORY -> 40
  PredictionWarningCode.UNKNOWN_PATCH -> 10
  PredictionWarningCode.ELO_FALLBACK -> 0
}

private fun PredictionWarningCode.label(): StringResource = when (this) {
  PredictionWarningCode.UNKNOWN_TEAM, PredictionWarningCode.LOW_COVERAGE -> Res.string.prediction_limited_team_history
  PredictionWarningCode.UNKNOWN_PATCH -> Res.string.prediction_patch_not_specified
  PredictionWarningCode.NO_ELIGIBLE_HISTORY -> Res.string.prediction_no_match_history
  PredictionWarningCode.NO_TEAM_MAP_HISTORY -> Res.string.prediction_no_map_history
  PredictionWarningCode.LIMITED_TEAM_MAP_HISTORY -> Res.string.prediction_limited_map_history
  PredictionWarningCode.MAP_NOT_IN_MODEL, PredictionWarningCode.UNVALIDATED_MAP_FALLBACK -> Res.string.prediction_limited_map_data
  PredictionWarningCode.UNKNOWN, PredictionWarningCode.ELO_FALLBACK -> Res.string.prediction_unknown_warning
}

private val WarningIcon = ImageVector.Builder(
  name = "PredictionWarning", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
  path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
    moveTo(12f, 3f)
    lineTo(2f, 21f)
    lineTo(22f, 21f)
    close()
    moveTo(12f, 9f)
    lineTo(12f, 14f)
    moveTo(12f, 17f)
    lineTo(12f, 18f)
  }
}.build()
