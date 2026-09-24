/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Compact navigation rail for medium and larger windows.
 */
@Composable
public fun PrismBottomNavBarLarge(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
  modifier: Modifier = Modifier,
  style: PrismNavigationRailStyle = PrismNavigationRailStyle.Default,
) {
  PrismSurface(
    modifier = modifier.fillMaxHeight().width(80.dp),
    color = Prism.color.background,
    shape = Prism.shapes.medium,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Column(
      modifier =
      Modifier.fillMaxHeight()
        .fillMaxWidth()
        .padding(Prism.dimens.spacingXs)
        .verticalScroll(rememberScrollState())
        .selectableGroup(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      items.forEach { item ->
        val selected = item.id == selectedItemId

        PrismSurface(
          modifier =
          Modifier.fillMaxWidth()
            .selectable(
              selected = selected,
              onClick = { onItemSelected(item) },
              role = Role.Tab,
            ),
          color = style.containerColor(selected),
          shape = Prism.shapes.small,
          frame = style.frame.copy(border = style.border(selected)),
        ) {
          Column(
            modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 64.dp)
              .padding(
                horizontal = Prism.dimens.spacingXs,
                vertical = Prism.dimens.spacingS,
              ),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = if (selected) item.selectedIcon else item.icon,
              contentDescription = item.label,
              tint = style.iconColor(selected),
            )

            Text(
              text = item.label,
              style = style.labelTextStyle,
              textAlign = TextAlign.Center,
              color = style.contentColor(selected),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
