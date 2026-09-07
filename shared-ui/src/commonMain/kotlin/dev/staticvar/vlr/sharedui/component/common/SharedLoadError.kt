/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun SharedLoadError(
  errorMessage: String,
  errorDetails: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  centered: Boolean = false,
) {
  var showDetails by remember(errorMessage, errorDetails) { mutableStateOf(false) }
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    PrismCard(
      modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
      style = PrismCardStyle.Outlined,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Text(
          text = if (centered) "Could not load data" else "Could not refresh data",
          color = Prism.color.danger,
          style = Prism.typography.cardTitle,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PrismButton(onClick = onRefresh, style = PrismButtonStyle.Tertiary) { Text("Retry") }
          PrismButton(onClick = { showDetails = true }, style = PrismButtonStyle.Tertiary) { Text("Show details") }
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
