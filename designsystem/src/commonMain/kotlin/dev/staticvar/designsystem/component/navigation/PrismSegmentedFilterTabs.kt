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
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Segmented filter tabs styled for flat brutalist layouts.
 *
 * [style] controls the group surface and each tab's selected, unselected, and disabled colors and
 * border treatment.
 */
@Composable
public fun PrismSegmentedFilterTabs(
  tabs: List<PrismSegmentedFilterTab>,
  selectedTabId: String,
  onTabSelected: (PrismSegmentedFilterTab) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismSegmentedFilterTabStyle = PrismSegmentedFilterTabStyle.Flat,
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = style.groupContainerColor,
    shape = Prism.shapes.small,
    border = style.groupBorder,
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
          style = style,
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
  style: PrismSegmentedFilterTabStyle,
  onClick: () -> Unit,
) {
  val visualState =
    rememberSegmentedFilterTabVisualState(
      tabId = tab.id,
      selected = selected,
      enabled = enabled,
      style = style,
    )
  PrismSurface(
    modifier =
    Modifier.weight(1f)
      .heightIn(min = Prism.dimens.controlHeight)
      .selectable(selected = selected, onClick = onClick, enabled = enabled, role = Role.Tab),
    color = visualState.containerColor,
    shape = Prism.shapes.small,
    border = visualState.border,
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
  style: PrismSegmentedFilterTabStyle,
): PrismSegmentedFilterTabVisualState {
  val animation = Prism.anim.standard
  val containerColor by
    animateColorAsState(
      targetValue = style.containerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_container_$tabId",
    )
  val contentColor by
    animateColorAsState(
      targetValue = style.contentColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_content_$tabId",
    )
  val borderColor by
    animateColorAsState(
      targetValue = style.borderColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "segmented_border_$tabId",
    )
  val borderWidth by
    animateDpAsState(
      targetValue = style.borderWidth(selected = selected),
      animationSpec = animation.dpSpec(),
      label = "segmented_border_width_$tabId",
    )

  return PrismSegmentedFilterTabVisualState(
    containerColor = containerColor,
    contentColor = contentColor,
    border = BorderStroke(width = borderWidth, color = borderColor),
  )
}

private data class PrismSegmentedFilterTabVisualState(
  val containerColor: Color,
  val contentColor: Color,
  val border: BorderStroke,
)
