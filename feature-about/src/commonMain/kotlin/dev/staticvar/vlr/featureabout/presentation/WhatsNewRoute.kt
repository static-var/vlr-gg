/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.done

@Composable
public fun WhatsNewRoute(
  onBack: () -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
      PrismScreenTitleBar(
        title = stringResource(BundledRelease.title),
        subtitle = appVersionText(),
        onBackPress = onBack,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Prism.dimens.spacingM)
          .padding(bottom = Prism.dimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        Text(
          text = stringResource(BundledRelease.introduction),
          style = Prism.typography.bodyLarge,
          color = Prism.color.bodyColor,
        )
        BundledRelease.highlights.forEachIndexed { index, highlight ->
          WhatsNewHighlightCard(
            highlight = highlight,
            emphasized = index == 0,
          )
        }
        PrismButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(Res.string.done))
        }
      }
    }
  }
}

@Composable
private fun WhatsNewHighlightCard(
  highlight: BundledRelease.Highlight,
  emphasized: Boolean,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = if (emphasized) PrismCardStyle.Filled else PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      Text(
        text = stringResource(highlight.title),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text = stringResource(highlight.description),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
  }
}
