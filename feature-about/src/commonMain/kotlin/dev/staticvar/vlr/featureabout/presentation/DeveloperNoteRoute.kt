/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.sharedui.mascot.PauseCardMascots
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.akhil_narang
import vlr.feature_about.generated.resources.close
import vlr.feature_about.generated.resources.note_about
import vlr.feature_about.generated.resources.note_contact
import vlr.feature_about.generated.resources.note_intro
import vlr.feature_about.generated.resources.note_review
import vlr.feature_about.generated.resources.note_signoff
import vlr.feature_about.generated.resources.note_store_unavailable
import vlr.feature_about.generated.resources.note_story
import vlr.feature_about.generated.resources.note_thanks
import vlr.feature_about.generated.resources.note_title
import vlr.feature_about.generated.resources.note_write
import vlr.feature_about.generated.resources.shreyansh_lodha
import vlr.feature_about.generated.resources.signature_akhil
import vlr.feature_about.generated.resources.signature_shreyansh

@Composable
public fun DeveloperNoteRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val uriHandler = LocalUriHandler.current
  val reviewUrl = appReviewUrl()
  var showFeedback by rememberSaveable { mutableStateOf(false) }
  var storeUnavailable by rememberSaveable { mutableStateOf(false) }
  PauseCardMascots(true)
  DeveloperNoteScreen(
    onBack = onBack,
    onContact = { showFeedback = true },
    onRate = reviewUrl?.let { url ->
      { storeUnavailable = runCatching { uriHandler.openUri(url) }.isFailure }
    },
    reviewLabel = appReviewLabel(),
    storeUnavailable = storeUnavailable,
    modifier = modifier,
  )
  if (showFeedback) {
    AboutFeedbackDialog(onDismiss = { showFeedback = false }, onSubmit = AppTelemetry::submitFeedback)
  }
}

@Composable
internal fun DeveloperNoteScreen(
  onBack: () -> Unit,
  onContact: () -> Unit,
  onRate: (() -> Unit)?,
  reviewLabel: String,
  modifier: Modifier = Modifier,
  storeUnavailable: Boolean = false,
) {
  Box(modifier.fillMaxSize().background(Prism.color.background), contentAlignment = Alignment.TopCenter) {
    Column(
      Modifier.widthIn(max = 560.dp).fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp),
    ) {
      PrismButton(
        onClick = onBack,
        modifier = Modifier.align(Alignment.End),
        style = PrismButtonStyle.Tertiary,
      ) {
        Text(stringResource(Res.string.close))
      }
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Text(
          text = stringResource(Res.string.note_title),
          style = Prism.typography.headline.copy(fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.7).sp),
          color = Prism.color.titleColor,
          modifier = Modifier.semantics { heading() },
        )
        listOf(
          Res.string.note_intro,
          Res.string.note_about,
          Res.string.note_story,
          if (onRate == null) Res.string.note_thanks else Res.string.note_review,
          Res.string.note_contact,
        ).forEach { paragraph ->
          Text(
            text = stringResource(paragraph),
            style = Prism.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
            color = Prism.color.bodyColor,
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(stringResource(Res.string.note_signoff), style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
          Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            DeveloperSignature(
              signature = Res.drawable.signature_shreyansh,
              name = stringResource(Res.string.shreyansh_lodha),
              modifier = Modifier.weight(1f),
            )
            DeveloperSignature(
              signature = Res.drawable.signature_akhil,
              name = stringResource(Res.string.akhil_narang),
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (storeUnavailable) {
          Text(
            stringResource(Res.string.note_store_unavailable),
            style = Prism.typography.bodySmall,
            color = Prism.color.bodyColor,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          )
          Spacer(Modifier.height(8.dp))
        }
        PrismButton(
          onClick = onRate ?: onContact,
          modifier = Modifier.fillMaxWidth(),
          style = PrismButtonStyle.Primary,
        ) {
          Text(if (onRate == null) stringResource(Res.string.note_write) else reviewLabel, style = Prism.typography.button)
        }
        if (onRate != null) {
          PrismButton(
            onClick = onContact,
            modifier = Modifier.fillMaxWidth(),
            style = PrismButtonStyle.Tertiary,
          ) {
            Text(stringResource(Res.string.note_write), style = Prism.typography.button)
          }
        }
      }
    }
  }
}

@Composable
private fun DeveloperSignature(signature: DrawableResource, name: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Image(
      painter = painterResource(signature),
      contentDescription = null,
      colorFilter = ColorFilter.tint(Prism.color.titleColor),
      modifier = Modifier.fillMaxWidth().height(64.dp),
    )
    Text(name, style = Prism.typography.caption, color = Prism.color.bodyColor)
  }
}
