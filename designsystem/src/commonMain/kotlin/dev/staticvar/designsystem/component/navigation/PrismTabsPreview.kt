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
internal fun PrismTabsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedTabId by remember { mutableStateOf("overview") }
    val tabs =
      remember {
        listOf(
          PrismTab(id = "overview", label = "Overview"),
          PrismTab(id = "matches", label = "Matches"),
          PrismTab(id = "standings", label = "Standings"),
          PrismTab(id = "history", label = "History", enabled = false),
        )
      }

    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(text = "Tabs", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismTabs(
        tabs = tabs,
        selectedTabId = selectedTabId,
        onTabSelected = { selectedTabId = it.id },
      )
    }
  }
}
