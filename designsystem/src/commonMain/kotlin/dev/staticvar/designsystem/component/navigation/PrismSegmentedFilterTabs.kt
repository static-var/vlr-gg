package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Segmented filter tabs styled for flat brutalist layouts.
 */
@Composable
public fun PrismSegmentedFilterTabs(
  tabs: List<PrismSegmentedFilterTab>,
  selectedTabId: String,
  onTabSelected: (PrismSegmentedFilterTab) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = Prism.color.background,
    shape = Prism.shapes.small,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.strokeVariant),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(IntrinsicSize.Min)
          .selectableGroup(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEach { tab ->
        PrismSegmentedFilterTabItem(
          tab = tab,
          selected = tab.id == selectedTabId,
          enabled = enabled && tab.enabled,
          onClick = { onTabSelected(tab) },
        )
      }
    }
  }
}

@Composable
private fun RowScope.PrismSegmentedFilterTabItem(
  tab: PrismSegmentedFilterTab,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val visualState =
    rememberSegmentedFilterTabVisualState(
      tabId = tab.id,
      selected = selected,
      enabled = enabled,
    )
  PrismSurface(
    modifier =
      Modifier.weight(1f)
        .heightIn(min = Prism.dimens.controlHeight)
        .selectable(selected = selected, onClick = onClick, enabled = enabled, role = Role.Tab),
    color = visualState.containerColor,
    shape = Prism.shapes.small,
    border = BorderStroke(width = visualState.borderWidth, color = visualState.borderColor),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = Prism.dimens.controlHeight)
          .padding(horizontal = Prism.dimens.spacingM),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = tab.label,
        style = Prism.typography.button,
        color = visualState.contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun rememberSegmentedFilterTabVisualState(
  tabId: String,
  selected: Boolean,
  enabled: Boolean,
): PrismSegmentedFilterTabVisualState {
  val animation = Prism.anim.standard
  val containerColor by
    animateColorAsState(
      targetValue = segmentedContainerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_container_$tabId",
    )
  val contentColor by
    animateColorAsState(
      targetValue = segmentedContentColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_content_$tabId",
    )
  val borderColor by
    animateColorAsState(
      targetValue = segmentedBorderColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_border_$tabId",
    )
  val borderWidth by
    animateDpAsState(
      targetValue = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = animation.dpSpec(),
      label = "segmented_border_width_$tabId",
    )

  return PrismSegmentedFilterTabVisualState(containerColor, contentColor, borderColor, borderWidth)
}

@Composable
private fun segmentedContainerColor(
  selected: Boolean,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.surfaceDim
    selected -> Prism.color.accentSubtle
    else -> Prism.color.surface
  }

@Composable
private fun segmentedContentColor(
  selected: Boolean,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.captionColor
    selected -> Prism.color.titleColor
    else -> Prism.color.labelColor
  }

@Composable
private fun segmentedBorderColor(
  selected: Boolean,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.stroke
    selected -> Prism.color.strokeVariant
    else -> Prism.color.stroke
  }

private data class PrismSegmentedFilterTabVisualState(
  val containerColor: Color,
  val contentColor: Color,
  val borderColor: Color,
  val borderWidth: Dp,
)
