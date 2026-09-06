/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * A single-choice Material segmented button group with Prism colors and theme-shaped outer corners.
 *
 * [selectedOptionId] is controlled by the caller; [onOptionSelected] reports a new selection.
 * [style] owns the colors, border, shape, and typography. [enabled] disables the entire group.
 */
@Composable
public fun PrismSegmentedButtons(
  options: List<PrismSegmentedButtonOption>,
  selectedOptionId: String,
  onOptionSelected: (PrismSegmentedButtonOption) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismSegmentedButtonStyle = PrismSegmentedButtonStyle.Outlined,
) {
  SingleChoiceSegmentedButtonRow(
    modifier = modifier.fillMaxWidth(),
    space = style.borderWidth,
  ) {
    options.forEachIndexed { index, option ->
      SegmentedButton(
        selected = option.id == selectedOptionId,
        onClick = { onOptionSelected(option) },
        shape = SegmentedButtonDefaults.itemShape(index, options.size, style.shape),
        modifier = Modifier.weight(1f).heightIn(min = style.minHeight),
        enabled = enabled && option.enabled,
        colors = style.colors,
        border = BorderStroke(style.borderWidth, style.borderColor(enabled && option.enabled)),
      ) {
        Text(
          text = option.label,
          style = style.labelStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
