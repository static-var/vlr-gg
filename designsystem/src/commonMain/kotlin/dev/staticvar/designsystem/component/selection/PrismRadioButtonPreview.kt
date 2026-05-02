package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismRadioButtonPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedIndex by remember { mutableStateOf(0) }
    val labels = listOf("A", "B", "C")

    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Interactive", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        labels.forEachIndexed { index, label ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PrismRadioButton(
              selected = selectedIndex == index,
              onClick = { selectedIndex = index },
            )
            Text(text = label, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
          }
        }
      }

      Text("Disabled", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismRadioButton(selected = false, onClick = {}, enabled = false)
        PrismRadioButton(selected = true, onClick = {}, enabled = false)
      }
    }
  }
}
