/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.cancel
import vlr.feature_about.generated.resources.close
import vlr.feature_about.generated.resources.done
import vlr.feature_about.generated.resources.feedback_character_count
import vlr.feature_about.generated.resources.feedback_queued
import vlr.feature_about.generated.resources.feedback_queued_description
import vlr.feature_about.generated.resources.feedback_unavailable
import vlr.feature_about.generated.resources.send
import vlr.feature_about.generated.resources.send_feedback
import vlr.feature_about.generated.resources.tell_us_what_happened_or_what_you_d_change
import vlr.feature_about.generated.resources.your_message

private const val FeedbackMessageLimit: Int = 2_000

@Composable
internal fun AboutFeedbackDialog(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
  var message by rememberSaveable { mutableStateOf("") }
  var queued by rememberSaveable { mutableStateOf(false) }
  var unavailable by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Prism.color.backgroundElevated,
    titleContentColor = Prism.color.titleColor,
    textContentColor = Prism.color.bodyColor,
    shape = Prism.shapes.large,
    title = {
      Text(text = if (queued) stringResource(Res.string.feedback_queued) else stringResource(Res.string.send_feedback), style = Prism.typography.sectionTitle)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        if (queued) {
          Text(
            text = stringResource(Res.string.feedback_queued_description),
            style = Prism.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          )
        } else {
          Text(text = stringResource(Res.string.tell_us_what_happened_or_what_you_d_change), style = Prism.typography.bodyLarge)
          OutlinedTextField(
            value = message,
            onValueChange = { message = it.take(FeedbackMessageLimit) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.your_message)) },
            supportingText = { Text(stringResource(Res.string.feedback_character_count, message.length, FeedbackMessageLimit)) },
            textStyle = Prism.typography.bodyLarge,
            minLines = 3,
            maxLines = 6,
            readOnly = unavailable,
            shape = Prism.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Prism.color.contentPrimary,
              unfocusedTextColor = Prism.color.contentPrimary,
              cursorColor = Prism.color.accent,
              focusedBorderColor = Prism.color.accent,
              unfocusedBorderColor = Prism.color.stroke,
              focusedLabelColor = Prism.color.accent,
              unfocusedLabelColor = Prism.color.bodyColor,
              focusedSupportingTextColor = Prism.color.bodyColor,
              unfocusedSupportingTextColor = Prism.color.bodyColor,
            ),
          )
          if (unavailable) {
            Text(
              text = stringResource(Res.string.feedback_unavailable),
              style = Prism.typography.bodySmall,
              modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
          }
        }
      }
    },
    confirmButton = {
      if (queued) {
        PrismButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) }
      } else {
        PrismButton(
          onClick = {
            queued = onSubmit(message.trim())
            unavailable = !queued
          },
          enabled = message.isNotBlank() && !unavailable,
        ) { Text(stringResource(Res.string.send)) }
      }
    },
    dismissButton = {
      if (!queued) {
        PrismButton(onClick = onDismiss, style = PrismButtonStyle.Tertiary) {
          Text(if (unavailable) stringResource(Res.string.close) else stringResource(Res.string.cancel))
        }
      }
    },
  )
}
