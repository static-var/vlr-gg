/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.state

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism

/**
 * Lightweight message block for empty, loading, and error states inside feature content.
 *
 * The component owns Prism typography, color, and spacing for inline state copy while callers keep
 * control of the text and placement.
 */
@Composable
public fun PrismStateMessage(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
    style = Prism.typography.bodyLarge,
    color = Prism.color.labelColor,
  )
}
