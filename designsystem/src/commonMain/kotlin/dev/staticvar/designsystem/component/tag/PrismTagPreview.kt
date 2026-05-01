package dev.staticvar.designsystem.component.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismTagPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(Prism.color.background)
          .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("PrismTag", style = Prism.typography.label, color = Prism.color.labelColor)

      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismTag(text = "neutral")
        PrismTag(text = "live", variant = PrismTagVariant.Accent)
        PrismTag(text = "final", variant = PrismTagVariant.Info)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismTag(text = "success", variant = PrismTagVariant.Success)
        PrismTag(text = "warning", variant = PrismTagVariant.Warning)
        PrismTag(text = "danger", variant = PrismTagVariant.Danger)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismTag(text = "disabled", enabled = false)
        PrismTag(text = "sync", variant = PrismTagVariant.Accent, enabled = false)
      }
    }
  }
}
