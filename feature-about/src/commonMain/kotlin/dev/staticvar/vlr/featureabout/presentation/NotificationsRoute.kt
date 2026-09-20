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
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.settings.LiveMatchNotificationSettingsController
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.allow_notifications
import vlr.feature_about.generated.resources.live_activities
import vlr.feature_about.generated.resources.live_activity_favorites
import vlr.feature_about.generated.resources.live_activity_favorites_description
import vlr.feature_about.generated.resources.live_activities_description
import vlr.feature_about.generated.resources.live_activities_disabled
import vlr.feature_about.generated.resources.live_activity_permissions_description
import vlr.feature_about.generated.resources.live_match_updates
import vlr.feature_about.generated.resources.live_match_updates_description
import vlr.feature_about.generated.resources.notification_access_denied
import vlr.feature_about.generated.resources.notification_access_error
import vlr.feature_about.generated.resources.notification_preferences_description
import vlr.feature_about.generated.resources.notifications
import vlr.feature_about.generated.resources.notifications_permission_description
import vlr.feature_about.generated.resources.open_system_settings

@Composable
public fun NotificationsRoute(
  controller: LiveMatchNotificationSettingsController,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences by controller.preferences.collectAsStateWithLifecycle()
  val access by controller.access.collectAsStateWithLifecycle()
  LifecycleResumeEffect(controller) {
    controller.refresh()
    onPauseOrDispose { }
  }
  val hasLiveActivities = access.activitiesEnabled != null
  Column(modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(
      title = stringResource(Res.string.notifications),
      subtitle = stringResource(Res.string.notification_preferences_description),
      onBackPress = onBack,
    )
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        stringResource(if (hasLiveActivities) Res.string.live_activities else Res.string.live_match_updates),
        style = Prism.typography.sectionTitle,
        color = Prism.color.contentPrimary,
      )
      Text(
        stringResource(if (hasLiveActivities) Res.string.live_activities_description else Res.string.live_match_updates_description),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
      NotificationPreferenceCard(
        title = stringResource(Res.string.live_activity_favorites),
        description = stringResource(Res.string.live_activity_favorites_description),
        checked = preferences.enabled,
        enabled = !access.requesting,
        onChange = controller::setEnabled,
      )
      Text(stringResource(Res.string.live_activity_permissions_description), style = Prism.typography.caption, color = Prism.color.captionColor)
      if (preferences.enabled) {
        if (access.activitiesEnabled == false) {
          PermissionMessage(stringResource(Res.string.live_activities_disabled))
        }
        if (access.notifications == NotificationAuthorization.Denied) {
          PermissionMessage(stringResource(if (hasLiveActivities) Res.string.notification_access_denied else Res.string.notifications_permission_description))
        }
        if (access.activitiesEnabled == false || access.notifications == NotificationAuthorization.Denied) {
          PrismButton(onClick = controller::openSettings) { Text(stringResource(Res.string.open_system_settings)) }
        }
        if (access.notifications == NotificationAuthorization.Error) {
          PermissionMessage(stringResource(Res.string.notification_access_error))
        }
        if (access.notifications == NotificationAuthorization.Error || access.notifications == NotificationAuthorization.NotDetermined) {
          PrismButton(onClick = controller::requestNotifications, enabled = !access.requesting) {
            Text(stringResource(Res.string.allow_notifications))
          }
        }
      }
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
