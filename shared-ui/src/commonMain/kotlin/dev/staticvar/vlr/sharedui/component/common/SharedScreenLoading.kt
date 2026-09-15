/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_loading
import vlr.shared_ui.generated.resources.shared_offline_loading
import vlr.shared_ui.generated.resources.shared_offline_title

@Composable
public fun SharedScreenLoading(label: String, modifier: Modifier = Modifier) {
  if (!LocalIsOnline.current) {
    SharedEmptyState(
      artwork = EmptyStateArtwork.NoInternet,
      title = stringResource(Res.string.shared_offline_title),
      message = stringResource(Res.string.shared_offline_loading),
      modifier = modifier,
    )
    return
  }
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      SharedLoadingIndicator(modifier = Modifier.width(180.dp))
      Text(text = label, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
    }
  }
}

@Composable
internal fun SharedLoadingIndicator(modifier: Modifier = Modifier) {
  val loadingDescription = stringResource(Res.string.shared_loading)
  LinearProgressIndicator(
    modifier = modifier.fillMaxWidth().semantics { contentDescription = loadingDescription },
    color = Prism.color.accent,
    trackColor = Prism.color.accentSubtle,
  )
}
