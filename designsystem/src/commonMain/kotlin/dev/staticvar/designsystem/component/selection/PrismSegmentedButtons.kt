/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.component.frame.rememberPrismPressProgress

/**
 * A single-choice Material segmented button group with Prism colors and theme-shaped outer corners.
 *
 * [selectedOptionId] is controlled by the caller; [onOptionSelected] reports a new selection.
 * [style] owns the colors, border, shape, and typography. [enabled] disables the entire group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun PrismSegmentedButtons(
  options: List<PrismSegmentedButtonOption>,
  selectedOptionId: String,
  onOptionSelected: (PrismSegmentedButtonOption) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismSegmentedButtonStyle = PrismSegmentedButtonStyle.Outlined,
) {
  val interactions = remember { MutableInteractionSource() }
  val frame = style.frame
  val pressProgress by rememberPrismPressProgress(interactions, enabled && frame.shadowOffset != DpOffset.Zero)
  CompositionLocalProvider(
    LocalRippleConfiguration provides if (frame.shadowOffset == DpOffset.Zero) LocalRippleConfiguration.current else null,
  ) {
    SingleChoiceSegmentedButtonRow(
      modifier = modifier.fillMaxWidth().prismFrame(frame, style.shape, pressProgress),
      space = style.borderWidth,
    ) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
          interactionSource = interactions,
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
}
