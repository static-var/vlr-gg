/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismBottomNavBarPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedItemId by remember { mutableStateOf("matches") }
    val items = rememberBottomNavPreviewItems()

    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        text = "BottomNavBar",
        style = Prism.typography.label,
        color = Prism.color.labelColor,
      )
      PrismBottomNavBar(
        items = items,
        selectedItemId = selectedItemId,
        onItemSelected = { selectedItemId = it.id },
      )

      PrismBottomNavRailPreview(
        items = items,
        selectedItemId = selectedItemId,
        onItemSelected = { selectedItemId = it.id },
      )
    }
  }
}

@Composable
private fun rememberBottomNavPreviewItems(): List<PrismBottomNavItem> {
  val icons = Prism.icons
  return remember(icons) {
    listOf(
      navPreviewItem("matches", "Matches", icons.matches.unselected, icons.matches.selected),
      navPreviewItem("events", "Events", icons.events.unselected, icons.events.selected),
      navPreviewItem("rankings", "Rankings", icons.rankings.unselected, icons.rankings.selected),
      navPreviewItem("news", "News", icons.news.unselected, icons.news.selected),
    )
  }
}

private fun navPreviewItem(
  id: String,
  label: String,
  icon: ImageVector,
  selectedIcon: ImageVector = icon,
): PrismBottomNavItem = PrismBottomNavItem(
  id = id,
  label = label,
  icon = icon,
  selectedIcon = selectedIcon,
)

@Composable
private fun PrismBottomNavRailPreview(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
) {
  Text(
    text = "BottomNavBarLarge (Rail)",
    style = Prism.typography.label,
    color = Prism.color.labelColor,
  )
  Row(
    modifier = Modifier.fillMaxWidth().height(320.dp),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismBottomNavBarLarge(
      items = items,
      selectedItemId = selectedItemId,
      onItemSelected = onItemSelected,
      modifier = Modifier.fillMaxHeight(),
    )
    Text(
      text = "Large-screen content",
      modifier = Modifier.weight(1f).padding(Prism.dimens.spacingM),
      style = Prism.typography.bodyLarge,
      color = Prism.color.bodyColor,
    )
  }
}
