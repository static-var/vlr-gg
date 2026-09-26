/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.sharedui.mascot.PauseCardMascots
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.close
import vlr.feature_about.generated.resources.note_about
import vlr.feature_about.generated.resources.note_contact
import vlr.feature_about.generated.resources.note_intro
import vlr.feature_about.generated.resources.note_review
import vlr.feature_about.generated.resources.note_signoff
import vlr.feature_about.generated.resources.note_store_unavailable
import vlr.feature_about.generated.resources.note_testflight
import vlr.feature_about.generated.resources.note_title
import vlr.feature_about.generated.resources.note_write
import vlr.feature_about.generated.resources.shreyansh_lodha

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
      val closeLabel = stringResource(Res.string.close)
      val closeColor = Prism.color.titleColor
      IconButton(
        onClick = onBack,
        modifier = Modifier.align(Alignment.End).semantics { contentDescription = closeLabel },
      ) {
        Canvas(Modifier.size(22.dp)) {
          drawLine(closeColor, Offset.Zero, Offset(size.width, size.height), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
          drawLine(closeColor, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
      }
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Box(
          modifier = Modifier.size(56.dp).background(Prism.color.accentSubtle, CircleShape).clearAndSetSemantics {},
          contentAlignment = Alignment.Center,
        ) {
          Text("SL", style = Prism.typography.headline, color = Prism.color.accent)
        }
        Text(
          text = stringResource(Res.string.note_title),
          style = Prism.typography.headline.copy(fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.7).sp),
          color = Prism.color.titleColor,
          modifier = Modifier.semantics { heading() },
        )
        listOf(
          Res.string.note_intro,
          Res.string.note_about,
          if (onRate == null) Res.string.note_testflight else Res.string.note_review,
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
          Text(
            stringResource(Res.string.shreyansh_lodha),
            style = Prism.typography.headline.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 28.sp),
            color = Prism.color.titleColor,
          )
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
        Button(
          onClick = onRate ?: onContact,
          modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
          shape = RoundedCornerShape(28.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Prism.color.titleColor, contentColor = Prism.color.background),
        ) {
          Text(if (onRate == null) stringResource(Res.string.note_write) else reviewLabel, style = Prism.typography.button)
        }
        if (onRate != null) {
          TextButton(
            onClick = onContact,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Prism.color.bodyColor),
          ) {
            Text(stringResource(Res.string.note_write), style = Prism.typography.button)
          }
        }
      }
    }
  }
}
