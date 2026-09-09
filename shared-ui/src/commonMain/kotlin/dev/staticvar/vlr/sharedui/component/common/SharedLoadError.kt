/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
  val title = if (!isOnline) "You’re offline" else if (centered) "Could not load data" else "Could not refresh data"
  val message = if (!isOnline) {
    if (centered) "Reconnect to the internet. This screen will update automatically." else "Showing saved content. Reconnect to get updates."
  } else {
    if (centered) "Something went wrong. Try loading this screen again." else "Your saved content is still available. Try again."
  }
  var showDetails by remember(errorMessage, errorDetails) { mutableStateOf(false) }
  val actions: @Composable () -> Unit = {
    if (isOnline) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.CenterHorizontally),
      ) {
        PrismButton(onClick = onRefresh, style = PrismButtonStyle.Tertiary) { Text("Retry") }
        PrismButton(onClick = { showDetails = true }, style = PrismButtonStyle.Tertiary) { Text("Show details") }
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
private fun ErrorDetailsSheet(
  visible: Boolean,
  errorMessage: String,
  errorDetails: String?,
  onDismiss: () -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  val details = errorDetails ?: errorMessage
  var copied by remember(details, visible) { mutableStateOf(false) }
  PrismModalSheet(
    visible = visible,
    onDismissRequest = onDismiss,
    paneTitle = "Error details",
    header = {
      Text("Error details", style = Prism.typography.sectionTitle)
    },
    footer = {
      PrismButton(onClick = onDismiss, style = PrismButtonStyle.Tertiary) { Text("Close") }
      PrismButton(
        onClick = {
          clipboard.setText(AnnotatedString(details))
          copied = true
        },
        style = PrismButtonStyle.Secondary,
      ) { Text(if (copied) "Copied" else "Copy stack trace") }
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
