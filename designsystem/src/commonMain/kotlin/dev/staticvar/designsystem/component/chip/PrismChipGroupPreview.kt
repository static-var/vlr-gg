package dev.staticvar.designsystem.component.chip

import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.CalendarAlt
import compose.icons.lineawesomeicons.CalendarAltSolid
import compose.icons.lineawesomeicons.Compass
import compose.icons.lineawesomeicons.CompassSolid
import compose.icons.lineawesomeicons.Newspaper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismChipGroupPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    var selectedChipId by remember { mutableStateOf("completed") }
    var selectedChipIds by remember { mutableStateOf(setOf("completed", "live")) }
    val chips =
      remember {
        listOf(
          PrismChip(id = "all", label = "All"),
          PrismChip(
            id = "live",
            label = "Live",
            icon = LineAwesomeIcons.CalendarAlt,
            selectedIcon = LineAwesomeIcons.CalendarAltSolid,
          ),
          PrismChip(
            id = "completed",
            label = "Completed",
            icon = LineAwesomeIcons.Compass,
            selectedIcon = LineAwesomeIcons.CompassSolid,
          ),
          PrismChip(
            id = "upcoming",
            label = "Upcoming",
            icon = LineAwesomeIcons.Newspaper,
            enabled = false,
          ),
        )
      }

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Prism.color.background)
          .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Single Select", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipId = selectedChipId,
        onChipSelected = { selectedChipId = it.id },
      )

      Text("Multi Select", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipIds = selectedChipIds,
        onSelectionChange = { selectedChipIds = it },
      )

      Text("Disabled", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipId = "completed",
        onChipSelected = {},
        enabled = false,
      )
    }
  }
}
