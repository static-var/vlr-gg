/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.about_val_esports
import vlr.feature_about.generated.resources.appearance
import vlr.feature_about.generated.resources.experimental
import vlr.feature_about.generated.resources.experimental_description
import vlr.feature_about.generated.resources.how_val_esports_handles_your_data
import vlr.feature_about.generated.resources.make_it_yours
import vlr.feature_about.generated.resources.note_settings_description
import vlr.feature_about.generated.resources.note_title
import vlr.feature_about.generated.resources.privacy_policy
import vlr.feature_about.generated.resources.settings
import vlr.feature_about.generated.resources.terms_of_service
import vlr.feature_about.generated.resources.the_latest_features_and_improvements
import vlr.feature_about.generated.resources.the_project_the_people_and_the_data
import vlr.feature_about.generated.resources.using_the_app_and_its_content
import vlr.feature_about.generated.resources.val_esports
import vlr.feature_about.generated.resources.what_s_new

@Composable
public fun SettingsRoute(
  onAppearance: () -> Unit,
  onExperimental: () -> Unit,
  onAbout: () -> Unit,
  onDeveloperNote: () -> Unit,
  onWhatsNew: () -> Unit,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val uriHandler = LocalUriHandler.current
  Column(modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(
      title = stringResource(Res.string.settings),
      subtitle = stringResource(Res.string.make_it_yours),
      onBackPress = onBack,
    )
    Column(
      modifier = Modifier.weight(1f).cardMascotViewport().verticalScroll(rememberScrollState())
        .padding(bottom = Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      SettingsLinkCard(
        title = stringResource(Res.string.appearance),
        description = stringResource(Res.string.make_it_yours),
        onClick = onAppearance,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.experimental),
        description = stringResource(Res.string.experimental_description),
        onClick = onExperimental,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.what_s_new),
        description = stringResource(Res.string.the_latest_features_and_improvements),
        onClick = onWhatsNew,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.note_title),
        description = stringResource(Res.string.note_settings_description),
        onClick = onDeveloperNote,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.about_val_esports),
        description = stringResource(Res.string.the_project_the_people_and_the_data),
        onClick = onAbout,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.privacy_policy),
        description = stringResource(Res.string.how_val_esports_handles_your_data),
        onClick = { uriHandler.openUri(AppWebsite.Privacy) },
      )
      SettingsLinkCard(
        title = stringResource(Res.string.terms_of_service),
        description = stringResource(Res.string.using_the_app_and_its_content),
        onClick = { uriHandler.openUri(AppWebsite.Terms) },
      )
      SettingsVersionFooter()
    }
  }
}

@Composable
private fun SettingsLinkCard(title: String, description: String, onClick: () -> Unit) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
    style = PrismCardStyle.Outlined,
    onClick = onClick,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(title, style = Prism.typography.sectionTitle, color = Prism.color.contentPrimary)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      }
      Text("→", style = Prism.typography.sectionTitle, color = Prism.color.accent)
    }
  }
}

@Composable
private fun SettingsVersionFooter() {
  Spacer(Modifier.height(80.dp))
  Text(
    text = "${stringResource(Res.string.val_esports)}\n${appVersionText()}",
    modifier = Modifier.fillMaxWidth(),
    style = Prism.typography.caption,
    color = Prism.color.captionColor,
    textAlign = TextAlign.Center,
  )
}

@Composable
internal expect fun appVersionText(): String
