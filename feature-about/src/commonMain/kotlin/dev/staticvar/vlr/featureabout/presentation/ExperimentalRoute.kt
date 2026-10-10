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
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.selection.PrismSegmentedButtonOption
import dev.staticvar.designsystem.component.selection.PrismSegmentedButtons
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.settings.LiveActivityStyle
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
import vlr.feature_about.generated.resources.live_activity_style
import vlr.feature_about.generated.resources.live_activity_style_classic
import vlr.feature_about.generated.resources.live_activity_style_classic_description
import vlr.feature_about.generated.resources.live_activity_style_description
import vlr.feature_about.generated.resources.live_activity_style_glance
import vlr.feature_about.generated.resources.live_activity_style_glance_description
import vlr.feature_about.generated.resources.live_match_updates
import vlr.feature_about.generated.resources.match_alerts
import vlr.feature_about.generated.resources.match_alerts_description
import vlr.feature_about.generated.resources.match_reminders_description
import vlr.feature_about.generated.resources.match_reminders_permission_description
import vlr.feature_about.generated.resources.live_updates_with_reminders_description
import vlr.feature_about.generated.resources.notification_promotion_unavailable
import vlr.feature_about.generated.resources.notification_access_error
import vlr.feature_about.generated.resources.notifications_permission_description
import vlr.feature_about.generated.resources.open_system_settings
import vlr.feature_about.generated.resources.remove_cached_items_that_haven_t_been_refreshed_in_30_days
import vlr.feature_about.generated.resources.show_score_in_status_bar
import vlr.feature_about.generated.resources.show_score_in_status_bar_description

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
  val remindersOnly = access.supportsMatchAlerts && !access.supportsLiveUpdates
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    PrismSectionTitle(
      title = stringResource(when {
        hasLiveActivities -> Res.string.live_activities
        access.supportsLiveUpdates -> Res.string.live_match_updates
        else -> Res.string.match_alerts
      }),
    )
    Text(
      stringResource(when {
        hasLiveActivities -> Res.string.live_activities_description
        access.supportsLiveUpdates -> Res.string.live_updates_with_reminders_description
        remindersOnly -> Res.string.match_reminders_description
        else -> Res.string.match_alerts_description
      }),
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
    if (!remindersOnly) {
      SettingsPreferenceCard(
        title = stringResource(Res.string.live_activity_favorites),
        description = stringResource(Res.string.live_activity_favorites_description),
        checked = preferences.enabled,
        enabled = access.supportsNotifications && !access.requesting,
        onChange = controller::setEnabled,
      )
    }
    if (access.supportsLiveActivityStyles) {
      LiveActivityStyleCard(preferences.liveActivityStyle, controller::setLiveActivityStyle)
    }
    if (access.supportsLiveUpdates && access.requiresNotificationPermission) {
      SettingsPreferenceCard(
        title = stringResource(Res.string.show_score_in_status_bar),
        description = stringResource(Res.string.show_score_in_status_bar_description),
        checked = preferences.showScoreInStatusBar,
        enabled = !access.requesting,
        onChange = controller::setShowScoreInStatusBar,
      )
    }
    if ((preferences.enabled || access.supportsMatchAlerts) && access.supportsNotifications) {
      if (access.activitiesEnabled == false) {
        PermissionMessage(stringResource(Res.string.live_activities_disabled))
      }
      if (access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Denied) {
        PermissionMessage(stringResource(
          if (preferences.enabled && !remindersOnly) Res.string.notifications_permission_description
          else Res.string.match_reminders_permission_description,
        ))
      }
      if (access.activitiesEnabled == false ||
        access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Denied
      ) {
        PrismButton(onClick = controller::openSettings) { Text(stringResource(Res.string.open_system_settings)) }
      }
      if (preferences.showScoreInStatusBar && access.supportsLiveUpdates &&
        access.notifications == NotificationAuthorization.Authorized && access.promotionAllowed == false
      ) {
        PermissionMessage(stringResource(Res.string.notification_promotion_unavailable))
        PrismButton(onClick = controller::openPromotionSettings) { Text(stringResource(Res.string.open_system_settings)) }
      }
      if (access.requiresNotificationPermission && access.notifications == NotificationAuthorization.Error) {
        PermissionMessage(stringResource(Res.string.notification_access_error))
      }
      if (access.requiresNotificationPermission &&
        (access.notifications == NotificationAuthorization.Error || access.notifications == NotificationAuthorization.NotDetermined)
      ) {
        if (access.notifications == NotificationAuthorization.NotDetermined) {
          PermissionMessage(stringResource(Res.string.match_reminders_permission_description))
        }
        PrismButton(onClick = controller::requestNotifications, enabled = !access.requesting) {
          Text(stringResource(Res.string.allow_notifications))
        }
      }
    }
  }
}

@Composable
private fun LiveActivityStyleCard(selected: LiveActivityStyle, onSelected: (LiveActivityStyle) -> Unit) {
  val options = LiveActivityStyle.entries.map { style ->
    PrismSegmentedButtonOption(
      id = style.storageValue,
      label = stringResource(when (style) {
        LiveActivityStyle.Classic -> Res.string.live_activity_style_classic
        LiveActivityStyle.Glance -> Res.string.live_activity_style_glance
      }),
    )
  }
  PrismCard(modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      Text(stringResource(Res.string.live_activity_style), style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
      Text(stringResource(Res.string.live_activity_style_description), style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      PrismSegmentedButtons(
        options = options,
        selectedOptionId = selected.storageValue,
        onOptionSelected = { option -> onSelected(LiveActivityStyle.fromStorage(option.id)) },
      )
      Text(
        stringResource(when (selected) {
          LiveActivityStyle.Classic -> Res.string.live_activity_style_classic_description
          LiveActivityStyle.Glance -> Res.string.live_activity_style_glance_description
        }),
        style = Prism.typography.caption,
        color = Prism.color.captionColor,
      )
    }
  }
}

@Composable
private fun CleanupSettingsCard(
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
) {
  SettingsPreferenceCard(
    title = stringResource(Res.string.auto_cleanup),
    description = stringResource(Res.string.remove_cached_items_that_haven_t_been_refreshed_in_30_days),
    checked = autoCleanupEnabled,
    enabled = true,
    onChange = onAutoCleanupChanged,
    supportingText = pluralStringResource(
      Res.plurals.deleted_cached_items,
      deletedCacheRecords.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
      deletedCacheRecords.toString(),
    ),
  )
}

@Composable
private fun PermissionMessage(message: String) {
  Text(message, style = Prism.typography.bodySmall, color = Prism.color.contentPrimary)
}

@Composable
private fun SettingsPreferenceCard(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean,
  onChange: (Boolean) -> Unit,
  supportingText: String? = null,
) {
  PrismCard(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(title, style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
        if (supportingText != null) {
          Text(supportingText, style = Prism.typography.caption, color = Prism.color.captionColor)
        }
      }
      PrismSwitch(checked, onChange, modifier = Modifier.semantics { contentDescription = title }, enabled = enabled)
    }
  }
}
