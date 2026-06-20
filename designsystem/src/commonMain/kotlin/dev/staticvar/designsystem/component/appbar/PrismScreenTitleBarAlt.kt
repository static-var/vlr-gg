/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */

package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.icon.back.StairStepBack
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@Composable
public fun PrismScreenTitleBarAlt(
  title: String,
  subtitle: String? = null,
  onBackPress: (() -> Unit)? = null,
  extraEntries: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.height(64.dp).padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
    onBackPress?.let { click ->
      PrismIcon(
        imageVector = StairStepBack,
        contentDescription = "Back",
      )
    }
  }
}

@PrismPreview
@Composable
internal fun PrismScreenTitleBarAltPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
    ) {
      PrismScreenTitleBarAlt(
        title = "Matches",
        subtitle = "Live and upcoming games",
        onBackPress = {},
        extraEntries = {
          Text(
            text = "Filter",
            style = Prism.typography.button,
            color = Prism.color.accent,
          )
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
