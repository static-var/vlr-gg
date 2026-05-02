package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Compact brutalist bottom navigation bar with stacked icon+label items.
 */
@Composable
public fun PrismBottomNavBar(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = Prism.color.background,
    shape = Prism.shapes.medium,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Row(
      modifier =
      Modifier.fillMaxWidth()
        .padding(Prism.dimens.spacingXs)
        .selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { item ->
        val selected = item.id == selectedItemId
        val borderColor = if (selected) Prism.color.accent else Prism.color.stroke

        PrismSurface(
          modifier =
          Modifier.weight(1f)
            .selectable(
              selected = selected,
              onClick = { onItemSelected(item) },
              role = Role.Tab,
            ),
          color = if (selected) Prism.color.accentSubtle else Prism.color.background,
          shape = Prism.shapes.small,
          border = BorderStroke(Prism.dimens.strokeDefault, borderColor),
        ) {
          Column(
            modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = Prism.dimens.spacingXl + Prism.dimens.spacingS)
              .padding(
                horizontal = Prism.dimens.spacingS,
                vertical = Prism.dimens.spacingXs,
              ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
          ) {
            Icon(
              imageVector = if (selected) item.selectedIcon else item.icon,
              contentDescription = item.label,
              tint = if (selected) Prism.color.titleColor else Prism.color.labelColor,
            )

            Text(
              text = item.label,
              style = Prism.typography.caption,
              color = if (selected) Prism.color.titleColor else Prism.color.labelColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
