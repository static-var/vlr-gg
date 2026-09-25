/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.settings.LiveMatchNotificationSettingsController
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.allow_notifications
import vlr.feature_about.generated.resources.auto_cleanup
import vlr.feature_about.generated.resources.deleted_cached_items
import vlr.feature_about.generated.resources.experimental
import vlr.feature_about.generated.resources.experimental_description
import vlr.feature_about.generated.resources.live_activities
import vlr.feature_about.generated.resources.live_activity_favorites
import vlr.feature_about.generated.resources.live_activity_favorites_description
import vlr.feature_about.generated.resources.live_activities_description
import vlr.feature_about.generated.resources.live_activities_disabled
import vlr.feature_about.generated.resources.live_match_updates
import vlr.feature_about.generated.resources.match_alerts
import vlr.feature_about.generated.resources.match_alerts_description
import vlr.feature_about.generated.resources.live_match_updates_description
import vlr.feature_about.generated.resources.notification_promotion_unavailable
import vlr.feature_about.generated.resources.notification_access_error
import vlr.feature_about.generated.resources.notifications_permission_description
import vlr.feature_about.generated.resources.open_system_settings
import vlr.feature_about.generated.resources.remove_cached_items_that_haven_t_been_refreshed_in_30_days

@Composable
public fun ExperimentalRoute(
  controller: LiveMatchNotificationSettingsController?,
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(
      title = stringResource(Res.string.experimental),
      subtitle = stringResource(Res.string.experimental_description),
      onBackPress = onBack,
    )
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      if (controller != null) LiveUpdatesSettings(controller)
      PrismSectionTitle(title = stringResource(Res.string.auto_cleanup))
      CleanupSettingsCard(autoCleanupEnabled, deletedCacheRecords, onAutoCleanupChanged)
    }
  }
}

@Composable
private fun LiveUpdatesSettings(controller: LiveMatchNotificationSettingsController) {
  val preferences by controller.preferences.collectAsStateWithLifecycle()
  val access by controller.access.collectAsStateWithLifecycle()
  LifecycleResumeEffect(controller) {
    controller.refresh()
    onPauseOrDispose { }
  }
  val hasLiveActivities = !access.requiresNotificationPermission
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    Text(
      stringResource(when {
        hasLiveActivities -> Res.string.live_activities
        access.supportsLiveUpdates -> Res.string.live_match_updates
        else -> Res.string.match_alerts
      }),
      style = Prism.typography.sectionTitle,
      color = Prism.color.contentPrimary,
    )
    Text(
      stringResource(when {
        hasLiveActivities -> Res.string.live_activities_description
        access.supportsLiveUpdates -> Res.string.live_match_updates_description
        else -> Res.string.match_alerts_description
      }),
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
    NotificationPreferenceCard(
      title = stringResource(Res.string.live_activity_favorites),
      description = stringResource(Res.string.live_activity_favorites_description),
      checked = preferences.enabled,
      enabled = access.supportsNotifications && !access.requesting,
      onChange = controller::setEnabled,
    )
    if (preferences.enabled && access.supportsNotifications) {
      if (access.activitiesEnabled == false) {
        PermissionMessage(stringResource(Res.string.live_activities_disabled))
      }
      if (access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Denied) {
        PermissionMessage(stringResource(Res.string.notifications_permission_description))
      }
      if (access.activitiesEnabled == false ||
        access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Denied
      ) {
        PrismButton(onClick = controller::openSettings) { Text(stringResource(Res.string.open_system_settings)) }
      }
      if (access.supportsLiveUpdates && access.notifications == NotificationAuthorization.Authorized && access.promotionAllowed == false) {
        PermissionMessage(stringResource(Res.string.notification_promotion_unavailable))
        PrismButton(onClick = controller::openPromotionSettings) { Text(stringResource(Res.string.open_system_settings)) }
      }
      if (access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Error) {
        PermissionMessage(stringResource(Res.string.notification_access_error))
      }
      if (access.requiresNotificationPermission &&
        (access.notifications == NotificationAuthorization.Error || access.notifications == NotificationAuthorization.NotDetermined)
      ) {
        PrismButton(onClick = controller::requestNotifications, enabled = !access.requesting) {
          Text(stringResource(Res.string.allow_notifications))
        }
      }
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
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
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
private fun PermissionMessage(message: String) {
  Text(message, style = Prism.typography.bodySmall, color = Prism.color.contentPrimary)
}

@Composable
private fun NotificationPreferenceCard(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean,
  onChange: (Boolean) -> Unit,
) {
  PrismCard(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(title, style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      }
      PrismSwitch(checked, onChange, modifier = Modifier.semantics { contentDescription = title }, enabled = enabled)
    }
  }
}
