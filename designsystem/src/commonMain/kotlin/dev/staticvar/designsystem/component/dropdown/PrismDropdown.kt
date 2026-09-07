/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.dropdown

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Compact Prism dropdown using the active theme's surfaces and shapes.
 */
@Composable
public fun PrismDropdown(
  options: List<PrismDropdownOption>,
  selectedOptionId: String,
  onOptionSelected: (PrismDropdownOption) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  enabled: Boolean = true,
  style: PrismDropdownStyle = PrismDropdownStyle.Brutalist,
) {
  var expanded by remember { mutableStateOf(false) }
  var anchorWidth by remember { mutableIntStateOf(0) }
  val menuWidth = with(LocalDensity.current) { anchorWidth.toDp() }.coerceAtLeast(style.menuWidth)
  val selectedOption = options.firstOrNull { option -> option.id == selectedOptionId } ?: options.firstOrNull()

  Box(modifier = modifier.width(style.triggerWidth).onSizeChanged { anchorWidth = it.width }) {
    PrismDropdownTrigger(
      label = label,
      selectedLabel = selectedOption?.label.orEmpty(),
      expanded = expanded,
      enabled = enabled && options.isNotEmpty(),
      style = style,
      onClick = { expanded = true },
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.width(menuWidth)
        .prismFrame(style.menuFrame, Prism.shapes.small)
        .background(Prism.color.surface, Prism.shapes.small)
        .border(style.menuFrame.border ?: style.menuBorder, Prism.shapes.small)
        .clip(Prism.shapes.small),
      shape = RectangleShape,
      containerColor = Color.Transparent,
      tonalElevation = Prism.dimens.elevationNone,
      shadowElevation = Prism.dimens.elevationNone,
    ) {
      options.forEach { option ->
        PrismDropdownMenuItem(
          option = option,
          selected = option.id == selectedOption?.id,
          style = style,
          onClick = {
            expanded = false
            onOptionSelected(option)
          },
        )
      }
    }
  }
}

@Composable
private fun PrismDropdownTrigger(
  label: String?,
  selectedLabel: String,
  expanded: Boolean,
  enabled: Boolean,
  style: PrismDropdownStyle,
  onClick: () -> Unit,
) {
  PrismSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = Prism.dimens.controlHeight)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    color = style.triggerContainerColor(enabled = enabled),
    border = style.triggerBorder(expanded = expanded, enabled = enabled),
    frame = style.triggerFrame,
  ) {
    Row(
      modifier = Modifier.heightIn(min = Prism.dimens.controlHeight),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = Prism.dimens.spacingS),
      ) {
        if (label != null) {
          Text(
            text = label,
            style = Prism.typography.caption,
            color = Prism.color.labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = selectedLabel,
          modifier = Modifier.padding(top = if (label == null) 0.dp else Prism.dimens.spacingXs),
          style = Prism.typography.button,
          color = style.triggerContentColor(enabled = enabled),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Box(
        modifier = Modifier
          .width(style.chevronWidth)
          .heightIn(min = Prism.dimens.controlHeight)
          .border(BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.strokeVariant)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (expanded) "^" else "v",
          style = Prism.typography.button,
          color = if (enabled) Prism.color.accent else Prism.color.captionColor,
        )
      }
    }
  }
}

@Composable
private fun PrismDropdownMenuItem(
  option: PrismDropdownOption,
  selected: Boolean,
  style: PrismDropdownStyle,
  onClick: () -> Unit,
) {
  PrismSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = style.optionMinHeight)
      .clickable(enabled = option.enabled, role = Role.Button, onClick = onClick),
    color = style.optionContainerColor(selected = selected, enabled = option.enabled),
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = style.optionMinHeight)
        .border(BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.strokeVariant))
        .padding(horizontal = Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      if (selected) {
        Box(
          modifier = Modifier
            .width(3.dp)
            .height(24.dp)
            .background(style.selectedRailColor),
        )
      }
      Text(
        text = option.label,
        modifier = Modifier.weight(1f),
        style = Prism.typography.bodySmall,
        color = style.optionContentColor(selected = selected, enabled = option.enabled),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (selected) {
        Text(
          text = "*",
          style = Prism.typography.label,
          color = Prism.color.accent,
        )
      }
    }
  }
}
