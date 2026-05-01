package dev.staticvar.designsystem.component.chip

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Flat brutalist chip group with single-select behavior.
 */
@Composable
public fun PrismChipGroup(
  chips: List<PrismChip>,
  selectedChipId: String?,
  onChipSelected: (PrismChip) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  PrismChipGroupInternal(
    chips = chips,
    modifier = modifier,
    enabled = enabled,
    role = Role.RadioButton,
    isChipSelected = { chip -> chip.id == selectedChipId },
    onChipClick = onChipSelected,
  )
}

/**
 * Flat brutalist chip group with multi-select behavior.
 */
@Composable
public fun PrismChipGroup(
  chips: List<PrismChip>,
  selectedChipIds: Set<String>,
  onSelectionChange: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  PrismChipGroupInternal(
    chips = chips,
    modifier = modifier,
    enabled = enabled,
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

@Composable
private fun PrismChipGroupInternal(
  chips: List<PrismChip>,
  modifier: Modifier,
  enabled: Boolean,
  role: Role,
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
      val selected = isChipSelected(chip)
      val chipEnabled = enabled && chip.enabled
      val animation = Prism.anim.standard
      val containerColor =
        when {
          !chipEnabled -> Prism.color.surfaceDim
          selected -> Prism.color.accentSubtle
          else -> Prism.color.background
        }
      val contentColor =
        when {
          !chipEnabled -> Prism.color.captionColor
          selected -> Prism.color.accent
          else -> Prism.color.bodyColor
        }
      val borderColor =
        when {
          !chipEnabled -> Prism.color.stroke
          selected -> Prism.color.accent
          else -> Prism.color.stroke
        }
      val animatedContainerColor by
        animateColorAsState(
          targetValue = containerColor,
          animationSpec = animation.colorSpec(),
          label = "chip_container_${chip.id}",
        )
      val animatedContentColor by
        animateColorAsState(
          targetValue = contentColor,
          animationSpec = animation.colorSpec(),
          label = "chip_content_${chip.id}",
        )
      val animatedBorderColor by
        animateColorAsState(
          targetValue = borderColor,
          animationSpec = animation.colorSpec(),
          label = "chip_border_${chip.id}",
        )
      val chipIcon = if (selected) chip.selectedIcon ?: chip.icon else chip.icon

      PrismSurface(
        modifier =
          Modifier
            .heightIn(min = Prism.dimens.controlHeight)
            .selectable(
              selected = selected,
              onClick = { onChipClick(chip) },
              enabled = chipEnabled,
              role = role,
            ),
        color = animatedContainerColor,
        shape = Prism.shapes.small,
        border =
          BorderStroke(
            width = Prism.dimens.strokeDefault,
            color = animatedBorderColor,
          ),
      ) {
        Row(
          modifier =
            Modifier
              .heightIn(min = Prism.dimens.controlHeight)
              .padding(
                horizontal = Prism.dimens.spacingM,
              ),
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (chipIcon != null) {
            Icon(
              imageVector = chipIcon,
              contentDescription = null,
              modifier = Modifier.size(Prism.dimens.iconS),
              tint = animatedContentColor,
            )
          }
          Text(
            text = chip.label,
            style = Prism.typography.button,
            color = animatedContentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
