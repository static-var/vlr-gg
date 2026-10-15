/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("CyclomaticComplexMethod")

package dev.staticvar.designsystem.component.chip

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.component.surface.prismAnimatedSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Theme-aware chip group with single-select behavior.
 */
@Composable
public fun PrismChipGroup(
  chips: List<PrismChip>,
  selectedChipId: String?,
  onChipSelected: (PrismChip) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismChipStyle = PrismChipStyle.Default,
) {
  PrismChipGroupInternal(
    chips = chips,
    modifier = modifier,
    enabled = enabled,
    style = style,
    role = Role.RadioButton,
    isChipSelected = { chip -> chip.id == selectedChipId },
    onChipClick = onChipSelected,
  )
}

/**
 * Theme-aware chip group with multi-select behavior.
 */
@Composable
public fun PrismChipGroup(
  chips: List<PrismChip>,
  selectedChipIds: Set<String>,
  onSelectionChange: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismChipStyle = PrismChipStyle.Default,
) {
  PrismChipGroupInternal(
    chips = chips,
    modifier = modifier,
    enabled = enabled,
    style = style,
    role = Role.Checkbox,
    isChipSelected = { chip -> chip.id in selectedChipIds },
    onChipClick = { chip ->
      val updatedSelection =
        if (chip.id in selectedChipIds) {
          selectedChipIds - chip.id
        } else {
          selectedChipIds + chip.id
        }
      onSelectionChange(updatedSelection)
    },
  )
}

@Suppress("LongParameterList")
@Composable
private fun PrismChipGroupInternal(
  chips: List<PrismChip>,
  modifier: Modifier,
  enabled: Boolean,
  role: Role,
  style: PrismChipStyle,
  isChipSelected: (PrismChip) -> Boolean,
  onChipClick: (PrismChip) -> Unit,
) {
  Row(
    modifier =
    modifier
      .horizontalScroll(rememberScrollState())
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    chips.forEach { chip ->
      PrismChipItem(
        chip = chip,
        selected = isChipSelected(chip),
        enabled = enabled && chip.enabled,
        role = role,
        style = style,
        onClick = { onChipClick(chip) },
      )
    }
  }
}

@Composable
private fun PrismChipItem(
  chip: PrismChip,
  selected: Boolean,
  enabled: Boolean,
  role: Role,
  style: PrismChipStyle,
  onClick: () -> Unit,
) {
  val visualState =
    rememberPrismChipVisualState(chipId = chip.id, selected = selected, enabled = enabled, style = style)
  val chipIcon = if (selected) chip.selectedIcon ?: chip.icon else chip.icon

  val shape = Prism.shapes.small
  val frame = style.frame
  val borderWidth = style.borderWidth
  Box(
    modifier =
    Modifier
      .heightIn(min = Prism.dimens.controlHeight)
      .selectable(selected = selected, onClick = onClick, enabled = enabled, role = role)
      .prismFrame(frame, shape)
      .prismAnimatedSurface(
        shape = shape,
        containerColor = { visualState.containerColor.value },
        borderColor = { visualState.borderColor.value },
        borderWidth = { borderWidth },
      )
      .clip(shape),
  ) {
    PrismChipContent(chip = chip, chipIcon = chipIcon, visualState = visualState)
  }
}

@Composable
private fun PrismChipContent(chip: PrismChip, chipIcon: ImageVector?, visualState: PrismChipVisualState) {
  Row(
    modifier = Modifier.heightIn(min = Prism.dimens.controlHeight).padding(horizontal = Prism.dimens.spacingM),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (chipIcon != null) {
      Icon(
        imageVector = chipIcon,
        contentDescription = null,
        modifier = Modifier.size(Prism.dimens.iconS),
        tint = visualState.contentColor.value,
      )
    }
    Text(
      text = chip.label,
      style = Prism.typography.button,
      color = visualState.contentColor.value,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (chip.supportingText != null) {
      Text(
        text = chip.supportingText,
        modifier = Modifier.padding(start = Prism.dimens.spacingXs),
        style = Prism.typography.button,
        color = visualState.supportingContentColor.value,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun rememberPrismChipVisualState(
  chipId: String,
  selected: Boolean,
  enabled: Boolean,
  style: PrismChipStyle,
): PrismChipVisualState {
  val animation = Prism.anim.selection
  val containerColor =
    animateColorAsState(
      targetValue = style.containerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "chip_container_$chipId",
    )
  val contentColor =
    animateColorAsState(
      targetValue = style.contentColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "chip_content_$chipId",
    )
  val borderColor =
    animateColorAsState(
      targetValue = style.borderColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "chip_border_$chipId",
    )
  val supportingContentColor =
    animateColorAsState(
      targetValue = style.supportingContentColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "chip_supporting_$chipId",
    )

  return PrismChipVisualState(containerColor, contentColor, borderColor, supportingContentColor)
}

private data class PrismChipVisualState(
  val containerColor: State<Color>,
  val contentColor: State<Color>,
  val borderColor: State<Color>,
  val supportingContentColor: State<Color>,
)
