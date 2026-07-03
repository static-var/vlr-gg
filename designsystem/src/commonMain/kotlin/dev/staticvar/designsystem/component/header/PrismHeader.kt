/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.header

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun PrismHeader(modifier: Modifier = Modifier, text: String) {
  Text(
    "// $text",
    modifier.padding(Prism.dimens.spacingXs),
    style = Prism.typography.labelAlt,
    color = Prism.color.labelColor,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
  )
}
