package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
        val selected = tab.id == selectedTabId
        val tabEnabled = enabled && tab.enabled
        val containerColor =
          when {
            !tabEnabled -> Prism.color.surfaceDim
            selected -> Prism.color.accentSubtle
            else -> Prism.color.surface
          }
        val contentColor =
          when {
            !tabEnabled -> Prism.color.captionColor
            selected -> Prism.color.titleColor
            else -> Prism.color.labelColor
          }
        val borderColor =
          when {
            !tabEnabled -> Prism.color.stroke
            selected -> Prism.color.strokeVariant
            else -> Prism.color.stroke
          }
        val animation = Prism.anim.standard
        val animatedContainerColor by
          animateColorAsState(
            targetValue = containerColor,
            animationSpec = animation.colorSpec(),
            label = "segmented_container_${tab.id}",
          )
        val animatedContentColor by
          animateColorAsState(
            targetValue = contentColor,
            animationSpec = animation.colorSpec(),
            label = "segmented_content_${tab.id}",
          )
        val animatedBorderColor by
          animateColorAsState(
            targetValue = borderColor,
            animationSpec = animation.colorSpec(),
            label = "segmented_border_${tab.id}",
          )
        val animatedBorderWidth by
          animateDpAsState(
            targetValue = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
            animationSpec = animation.dpSpec(),
            label = "segmented_border_width_${tab.id}",
          )

        PrismSurface(
          modifier =
            Modifier.weight(1f)
              .heightIn(min = Prism.dimens.controlHeight)
              .selectable(
                selected = selected,
                onClick = { onTabSelected(tab) },
                enabled = tabEnabled,
                role = Role.Tab,
              ),
          color = animatedContainerColor,
          shape = Prism.shapes.small,
          border =
            BorderStroke(
              width = animatedBorderWidth,
              color = animatedBorderColor,
            ),
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
              color = animatedContentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
