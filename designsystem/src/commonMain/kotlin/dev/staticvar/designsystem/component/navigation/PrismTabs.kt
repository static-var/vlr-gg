package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.prism.Prism

/**
 * Standard horizontal tabs with a brutalist underline treatment.
 *
 * Unlike [PrismSegmentedFilterTabs], this component is intended for content section
 * switching rather than compact equal-width filters.
 */
@Composable
public fun PrismTabs(
  tabs: List<PrismTab>,
  selectedTabId: String,
  onTabSelected: (PrismTab) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val scrollState = rememberScrollState()

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = Prism.dimens.controlHeight)
          .horizontalScroll(scrollState)
          .selectableGroup(),
      verticalAlignment = Alignment.Bottom,
    ) {
      tabs.forEach { tab ->
        val selected: Boolean = tab.id == selectedTabId
        val tabEnabled: Boolean = enabled && tab.enabled
        val animation = Prism.anim.standard
        val contentColor by
          animateColorAsState(
            targetValue =
              when {
                !tabEnabled -> Prism.color.captionColor
                selected -> Prism.color.titleColor
                else -> Prism.color.labelColor
              },
            animationSpec = animation.colorSpec(),
            label = "tabs_content_${tab.id}",
          )
        val indicatorColor by
          animateColorAsState(
            targetValue =
              when {
                !tabEnabled -> Prism.color.stroke
                selected -> Prism.color.accent
                else -> Prism.color.strokeVariant
              },
            animationSpec = animation.colorSpec(),
            label = "tabs_indicator_${tab.id}",
          )
        val indicatorHeight by
          animateDpAsState(
            targetValue = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
            animationSpec = animation.dpSpec(),
            label = "tabs_indicator_height_${tab.id}",
          )

        Column(
          modifier =
            Modifier.widthIn(min = Prism.dimens.controlHeight * 2)
              .selectable(
                selected = selected,
                enabled = tabEnabled,
                role = Role.Tab,
                onClick = { onTabSelected(tab) },
              )
              .padding(
                start = Prism.dimens.spacingM,
                end = Prism.dimens.spacingM,
                top = Prism.dimens.spacingXs,
              ),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = tab.label,
            style = Prism.typography.button,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(if (tabEnabled) 1f else 0.72f),
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(indicatorColor)
                .heightIn(min = indicatorHeight),
          )
        }
      }
    }
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Prism.color.strokeVariant)
          .heightIn(min = Prism.dimens.strokeDefault),
    )
  }
}
