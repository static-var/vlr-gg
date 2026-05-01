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
internal fun PrismSwitchPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    var checked by remember { mutableStateOf(false) }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(Prism.color.background)
          .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Interactive", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PrismSwitch(
          checked = checked,
          onCheckedChange = { checked = it },
        )
        Text(
          text = if (checked) "On" else "Off",
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
      }

      Text("States", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismSwitch(checked = false, onCheckedChange = {})
        PrismSwitch(checked = true, onCheckedChange = {})
        PrismSwitch(checked = false, onCheckedChange = {}, enabled = false)
        PrismSwitch(checked = true, onCheckedChange = {}, enabled = false)
      }
    }
  }
}
