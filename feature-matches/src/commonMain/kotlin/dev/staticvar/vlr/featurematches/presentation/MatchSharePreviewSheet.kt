/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.share.ImageSharer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun MatchSharePreviewSheet(matches: List<MatchPreview>, imageSharer: ImageSharer, onDismiss: () -> Unit) {
  val imageLayer = rememberGraphicsLayer()
  var previewDrawn by remember(matches) { mutableStateOf(false) }
  var sharing by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var visible by remember { mutableStateOf(true) }

  PrismModalSheet(
    visible = visible,
    onDismissRequest = { visible = false },
    onCollapsed = onDismiss,
    paneTitle = "Match share preview",
    header = {
      Text("Preview", style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
    },
    footer = {
      val scope = rememberCoroutineScope()
      PrismButton(onClick = { visible = false }, style = PrismButtonStyle.Tertiary) { Text("Close") }
      PrismButton(
        enabled = previewDrawn && !sharing && matches.isNotEmpty(),
        onClick = {
          if (!sharing) {
            sharing = true
            errorMessage = null
            scope.launch {
              var imageReady = false
              try {
                val bitmap = imageLayer.toImageBitmap()
                imageReady = true
                imageSharer.share(bitmap, matchShareText(matches))
              } catch (cancelled: CancellationException) {
                throw cancelled
              } catch (_: Exception) {
                errorMessage = if (imageReady) {
                  "Could not open the share menu. Tap Share to try again."
                } else {
                  "Could not create the image. Tap Share to try again."
                }
              } finally {
                sharing = false
              }
            }
          }
        },
      ) { Text(if (sharing) "Sharing…" else "Share") }
    },
  ) {
    errorMessage?.let { message ->
      Text(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = Prism.typography.bodySmall,
        color = Prism.color.danger,
      )
    }
    Column(
      modifier = Modifier.fillMaxWidth()
        .drawWithContent {
          imageLayer.record { this@drawWithContent.drawContent() }
          drawLayer(imageLayer)
          previewDrawn = size.width > 0 && size.height > 0
        }
        .background(Prism.color.background.copy(alpha = 1f))
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      matches.forEachIndexed { index, match ->
        if (index > 0) PrismDivider(style = PrismDividerStyle.Hairline)
        MatchSharePreviewItem(match)
      }
    }
  }
}

@Composable
private fun MatchSharePreviewItem(match: MatchPreview) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(
      text = match.event,
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = matchShareTime(match),
      style = Prism.typography.label,
      color = if (match.status == MatchStatus.LIVE) Prism.color.accent else Prism.color.labelColor,
    )
    MatchShareTeamRow(match.team1)
    MatchShareTeamRow(match.team2)
    if (match.series.isNotBlank()) {
      Text(match.series, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
    }
  }
}

@Composable
private fun MatchShareTeamRow(team: TeamPreview) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = team.name,
      modifier = Modifier.weight(1f),
      style = Prism.typography.headline,
      color = Prism.color.titleColor,
    )
    Text(
      text = team.score?.toString() ?: "-",
      style = Prism.typography.headline,
      color = Prism.color.titleColor,
    )
  }
}
