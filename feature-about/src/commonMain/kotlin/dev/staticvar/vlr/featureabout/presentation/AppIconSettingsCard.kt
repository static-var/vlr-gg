/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.app_icon
import vlr.feature_about.generated.resources.app_icon_amethyst
import vlr.feature_about.generated.resources.app_icon_applying
import vlr.feature_about.generated.resources.app_icon_choose_description
import vlr.feature_about.generated.resources.app_icon_default
import vlr.feature_about.generated.resources.app_icon_not_selected
import vlr.feature_about.generated.resources.app_icon_preview_amethyst
import vlr.feature_about.generated.resources.app_icon_preview_default
import vlr.feature_about.generated.resources.app_icon_preview_ticket
import vlr.feature_about.generated.resources.app_icon_selected
import vlr.feature_about.generated.resources.app_icon_ticket
import vlr.feature_about.generated.resources.app_icon_update_failed

@Composable
internal fun AppIconSettingsCard() {
  if (LocalInspectionMode.current) {
    AppIconSettingsCardContent(
      selectedIcon = AppIcon.Default,
      pendingIcon = null,
      selectionFailed = false,
      onSelect = {},
    )
    return
  }

  val controller = rememberAppIconController()
  if (!controller.supported) return

  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val coroutineScope = rememberCoroutineScope()
  var selectedIcon by remember(controller) { mutableStateOf(controller.current()) }
  var pendingIcon by remember(controller) { mutableStateOf<AppIcon?>(null) }
  var selectionFailed by remember(controller) { mutableStateOf(false) }

  LaunchedEffect(controller, lifecycleState) {
    if (lifecycleState == Lifecycle.State.RESUMED && pendingIcon == null) {
      selectedIcon = runCatching(controller::current).getOrDefault(selectedIcon)
    }
  }

  AppIconSettingsCardContent(
    selectedIcon = selectedIcon,
    pendingIcon = pendingIcon,
    selectionFailed = selectionFailed,
    onSelect = { icon ->
      if (pendingIcon != null || icon == selectedIcon) return@AppIconSettingsCardContent

      pendingIcon = icon
      selectionFailed = false
      coroutineScope.launch {
        try {
          controller.select(icon)
          selectedIcon = controller.current()
        } catch (error: CancellationException) {
          throw error
        } catch (_: Exception) {
          selectionFailed = true
          selectedIcon = runCatching(controller::current).getOrDefault(selectedIcon)
        } finally {
          pendingIcon = null
        }
      }
    },
  )
}

@Composable
private fun AppIconSettingsCardContent(
  selectedIcon: AppIcon,
  pendingIcon: AppIcon?,
  selectionFailed: Boolean,
  onSelect: (AppIcon) -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        PrismSectionTitle(title = stringResource(Res.string.app_icon))
        Text(
          text = stringResource(Res.string.app_icon_choose_description),
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        AppIcon.entries.forEach { icon ->
          AppIconOption(
            icon = icon,
            selected = icon == selectedIcon,
            pending = icon == pendingIcon,
            enabled = pendingIcon == null,
            onSelect = { onSelect(icon) },
            modifier = Modifier.weight(1f),
          )
        }
      }
      if (selectionFailed) {
        Text(
          text = stringResource(Res.string.app_icon_update_failed),
          style = Prism.typography.bodySmall,
          color = Prism.color.danger,
        )
      }
    }
  }
}

@Composable
private fun AppIconOption(
  icon: AppIcon,
  selected: Boolean,
  pending: Boolean,
  enabled: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val status = when {
    pending -> stringResource(Res.string.app_icon_applying)
    selected -> stringResource(Res.string.app_icon_selected)
    else -> stringResource(Res.string.app_icon_not_selected)
  }
  Column(
    modifier = modifier
      .semantics(mergeDescendants = true) { stateDescription = status }
      .selectable(
        selected = selected,
        enabled = enabled,
        role = Role.RadioButton,
        onClick = onSelect,
      )
      .padding(vertical = Prism.dimens.spacingXs),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Image(
      painter = painterResource(icon.preview),
      contentDescription = null,
      modifier = Modifier.size(72.dp)
        .clip(Prism.shapes.medium)
        .border(
          border = BorderStroke(
            width = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
            color = if (selected) Prism.color.accent else Prism.color.strokeVariant,
          ),
          shape = Prism.shapes.medium,
        ),
      contentScale = ContentScale.Crop,
    )
    Text(
      text = icon.label,
      style = Prism.typography.bodySmall,
      color = if (enabled) Prism.color.contentPrimary else Prism.color.contentTertiary,
      textAlign = TextAlign.Center,
    )
    Box(modifier = Modifier.height(Prism.dimens.iconS), contentAlignment = Alignment.Center) {
      when {
        pending -> CircularProgressIndicator(
          modifier = Modifier.size(Prism.dimens.iconS),
          color = Prism.color.accent,
          strokeWidth = Prism.dimens.strokeThick,
        )
        selected -> Text(
          text = stringResource(Res.string.app_icon_selected),
          style = Prism.typography.caption,
          color = Prism.color.accent,
        )
      }
    }
  }
}

private val AppIcon.label: String
  @Composable get() = when (this) {
    AppIcon.Default -> stringResource(Res.string.app_icon_default)
    AppIcon.Amethyst -> stringResource(Res.string.app_icon_amethyst)
    AppIcon.Ticket -> stringResource(Res.string.app_icon_ticket)
  }

private val AppIcon.preview: DrawableResource
  get() = when (this) {
    AppIcon.Default -> Res.drawable.app_icon_preview_default
    AppIcon.Amethyst -> Res.drawable.app_icon_preview_amethyst
    AppIcon.Ticket -> Res.drawable.app_icon_preview_ticket
  }
