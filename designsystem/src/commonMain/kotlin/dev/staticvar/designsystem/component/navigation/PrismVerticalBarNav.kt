/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/** Icon-only root navigation for a system-reserved vertical bar. */
@Composable
public fun PrismVerticalBarNav(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
  modifier: Modifier = Modifier,
  style: PrismNavigationRailStyle = PrismNavigationRailStyle.Default,
) {
  PrismSurface(
    modifier = modifier.width(48.dp),
    color = Prism.color.background,
    shape = CircleShape,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Column(
      modifier = Modifier.padding(vertical = Prism.dimens.spacingXs).selectableGroup(),
    ) {
      items.forEach { item ->
        val selected = item.id == selectedItemId

        PrismSurface(
          modifier = Modifier
            .size(48.dp)
            .selectable(
              selected = selected,
              onClick = { onItemSelected(item) },
              role = Role.Tab,
            ),
          color = style.containerColor(selected),
          shape = CircleShape,
          frame = style.frame.copy(border = style.border(selected)),
        ) {
          Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (selected) item.selectedIcon else item.icon,
              contentDescription = item.label,
              tint = style.iconColor(selected),
            )
          }
        }
      }
    }
  }
}
