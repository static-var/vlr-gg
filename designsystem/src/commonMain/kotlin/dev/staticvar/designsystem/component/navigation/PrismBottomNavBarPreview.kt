package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.CalendarAlt
import compose.icons.lineawesomeicons.CalendarAltSolid
import compose.icons.lineawesomeicons.Compass
import compose.icons.lineawesomeicons.CompassSolid
import compose.icons.lineawesomeicons.HomeSolid
import compose.icons.lineawesomeicons.Newspaper
import compose.icons.lineawesomeicons.NewspaperSolid
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismBottomNavBarPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    var selectedItemId by remember { mutableStateOf("matches") }
    val items =
      remember {
        listOf(
          PrismBottomNavItem(
            id = "matches",
            label = "Matches",
            icon = LineAwesomeIcons.HomeSolid,
          ),
          PrismBottomNavItem(
            id = "events",
            label = "Events",
            icon = LineAwesomeIcons.Compass,
            selectedIcon = LineAwesomeIcons.CompassSolid,
          ),
          PrismBottomNavItem(
            id = "schedule",
            label = "Schedule",
            icon = LineAwesomeIcons.CalendarAlt,
            selectedIcon = LineAwesomeIcons.CalendarAltSolid,
          ),
          PrismBottomNavItem(
            id = "news",
            label = "News",
            icon = LineAwesomeIcons.Newspaper,
            selectedIcon = LineAwesomeIcons.NewspaperSolid,
          ),
        )
      }

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
          onItemSelected = { selectedItemId = it.id },
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
  }
}
