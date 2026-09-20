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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.about_val_esports
import vlr.feature_about.generated.resources.appearance
import vlr.feature_about.generated.resources.auto_cleanup
import vlr.feature_about.generated.resources.deleted_cached_items
import vlr.feature_about.generated.resources.experimental
import vlr.feature_about.generated.resources.how_val_esports_handles_your_data
import vlr.feature_about.generated.resources.make_it_yours
import vlr.feature_about.generated.resources.privacy_policy
import vlr.feature_about.generated.resources.remove_cached_items_that_haven_t_been_refreshed_in_30_days
import vlr.feature_about.generated.resources.notifications
import vlr.feature_about.generated.resources.notification_preferences_description
import vlr.feature_about.generated.resources.settings
import vlr.feature_about.generated.resources.terms_of_service
import vlr.feature_about.generated.resources.the_latest_features_and_improvements
import vlr.feature_about.generated.resources.the_project_the_people_and_the_data
import vlr.feature_about.generated.resources.using_the_app_and_its_content
import vlr.feature_about.generated.resources.val_esports
import vlr.feature_about.generated.resources.what_s_new

/** Preferences shared by Android and iOS. Changes apply immediately. */
@Composable
public fun SettingsRoute(
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
  onAppearance: () -> Unit,
  onAbout: () -> Unit,
  onWhatsNew: () -> Unit,
  onNotifications: (() -> Unit)? = null,
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
      if (onNotifications != null) {
        SettingsLinkCard(
          title = stringResource(Res.string.notifications),
          description = stringResource(Res.string.notification_preferences_description),
          onClick = onNotifications,
        )
      }
      CleanupSettingsCard(
        autoCleanupEnabled = autoCleanupEnabled,
        deletedCacheRecords = deletedCacheRecords,
        onAutoCleanupChanged = onAutoCleanupChanged,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.what_s_new),
        description = stringResource(Res.string.the_latest_features_and_improvements),
        onClick = onWhatsNew,
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
private fun CleanupSettingsCard(
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
) {
  val autoCleanupLabel = stringResource(Res.string.auto_cleanup)
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismSectionTitle(title = stringResource(Res.string.experimental))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        Text(
          text = stringResource(Res.string.auto_cleanup),
          modifier = Modifier.weight(1f),
          style = Prism.typography.bodyLarge,
          color = Prism.color.contentPrimary,
        )
        PrismSwitch(
          checked = autoCleanupEnabled,
          onCheckedChange = onAutoCleanupChanged,
          modifier = Modifier.semantics { contentDescription = autoCleanupLabel },
        )
      }
      Text(
        text = stringResource(Res.string.remove_cached_items_that_haven_t_been_refreshed_in_30_days),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
      Text(
        text = pluralStringResource(
          Res.plurals.deleted_cached_items,
          deletedCacheRecords.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
          deletedCacheRecords.toString(),
        ),
        style = Prism.typography.caption,
        color = Prism.color.captionColor,
      )
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
