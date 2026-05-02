package dev.staticvar.designsystem.component.navigation

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
internal fun PrismSegmentedFilterTabsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedTabId by remember { mutableStateOf("all") }
    val tabs =
      remember {
        listOf(
          PrismSegmentedFilterTab(id = "all", label = "All"),
          PrismSegmentedFilterTab(id = "live", label = "Live"),
          PrismSegmentedFilterTab(id = "upcoming", label = "Upcoming"),
          PrismSegmentedFilterTab(id = "results", label = "Results", enabled = false),
        )
      }

    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Segmented Filters", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismSegmentedFilterTabs(
        tabs = tabs,
        selectedTabId = selectedTabId,
        onTabSelected = { selectedTabId = it.id },
      )

      Text("Disabled", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismSegmentedFilterTabs(
        tabs = tabs,
        selectedTabId = "live",
        onTabSelected = {},
        enabled = false,
      )
    }
  }
}
