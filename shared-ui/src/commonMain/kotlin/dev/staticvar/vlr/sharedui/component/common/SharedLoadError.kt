/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.illustration.EmptyStateIllustration
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_close
import vlr.shared_ui.generated.resources.shared_copied
import vlr.shared_ui.generated.resources.shared_copy_stack_trace
import vlr.shared_ui.generated.resources.shared_error_details
import vlr.shared_ui.generated.resources.shared_load_error_message
import vlr.shared_ui.generated.resources.shared_load_failed
import vlr.shared_ui.generated.resources.shared_offline_empty
import vlr.shared_ui.generated.resources.shared_offline_saved
import vlr.shared_ui.generated.resources.shared_offline_title
import vlr.shared_ui.generated.resources.shared_refresh_error_message
import vlr.shared_ui.generated.resources.shared_refresh_failed
import vlr.shared_ui.generated.resources.shared_retry
import vlr.shared_ui.generated.resources.shared_show_details

@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun SharedLoadError(
  errorMessage: String,
  errorDetails: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  centered: Boolean = false,
) {
  val isOnline = LocalIsOnline.current
  val artwork = if (isOnline) EmptyStateArtwork.UnknownError else EmptyStateArtwork.NoInternet
  val title = if (!isOnline) {
    stringResource(Res.string.shared_offline_title)
  } else if (centered) {
    stringResource(Res.string.shared_load_failed)
  } else {
    stringResource(Res.string.shared_refresh_failed)
  }
  val message = if (!isOnline) {
    if (centered) stringResource(Res.string.shared_offline_empty) else stringResource(Res.string.shared_offline_saved)
  } else {
    if (centered) {
      stringResource(
        Res.string.shared_load_error_message,
      )
    } else {
      stringResource(Res.string.shared_refresh_error_message)
    }
  }
  var showDetails by remember(errorMessage, errorDetails) { mutableStateOf(false) }
  val actions: @Composable () -> Unit = {
    if (isOnline) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.CenterHorizontally),
      ) {
        PrismButton(onClick = onRefresh, style = PrismButtonStyle.Tertiary) {
          Text(stringResource(Res.string.shared_retry))
        }
        PrismButton(onClick = {
          showDetails = true
        }, style = PrismButtonStyle.Tertiary) { Text(stringResource(Res.string.shared_show_details)) }
      }
    }
  }
  if (centered) {
    SharedIllustratedState(
      artwork = artwork,
      title = title,
      message = message,
      modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
      actions = actions,
    )
  } else {
    PrismCard(
      modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
      style = PrismCardStyle.Outlined,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        EmptyStateIllustration(artwork = artwork, modifier = Modifier.size(width = 64.dp, height = 80.dp))
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
        ) {
          Text(text = title, color = Prism.color.titleColor, style = Prism.typography.cardTitle)
          Text(text = message, color = Prism.color.bodyColor, style = Prism.typography.bodySmall)
          actions()
        }
      }
    }
  }
  ErrorDetailsSheet(
    visible = showDetails,
    errorMessage = errorMessage,
    errorDetails = errorDetails,
    onDismiss = { showDetails = false },
  )
}

@Suppress("DEPRECATION")
@Composable
private fun ErrorDetailsSheet(visible: Boolean, errorMessage: String, errorDetails: String?, onDismiss: () -> Unit) {
  val clipboard = LocalClipboardManager.current
  val details = errorDetails ?: errorMessage
  var copied by remember(details, visible) { mutableStateOf(false) }
  PrismModalSheet(
    visible = visible,
    onDismissRequest = onDismiss,
    paneTitle = stringResource(Res.string.shared_error_details),
    header = {
      Text(stringResource(Res.string.shared_error_details), style = Prism.typography.sectionTitle)
    },
    footer = {
      PrismButton(onClick = onDismiss, style = PrismButtonStyle.Tertiary) {
        Text(stringResource(Res.string.shared_close))
      }
      PrismButton(
        onClick = {
          clipboard.setText(AnnotatedString(details))
          copied = true
        },
        style = PrismButtonStyle.Secondary,
      ) {
        Text(
          if (copied) stringResource(Res.string.shared_copied) else stringResource(Res.string.shared_copy_stack_trace),
        )
      }
    },
  ) {
    Text(errorMessage, style = Prism.typography.bodyLarge, color = Prism.color.danger)
    SelectionContainer {
      Text(
        text = details,
        modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
        style = Prism.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      )
    }
  }
}
