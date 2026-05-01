package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import androidx.compose.ui.tooling.preview.PreviewParameter

@PrismPreview
@Composable
internal fun PrismCardPreview(
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
      Text("Outlined", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismCard(variant = PrismCardVariant.Outlined) {
        Text("Outlined card content", style = Prism.typography.bodyLarge)
        Text("Secondary text", style = Prism.typography.caption, color = Prism.color.labelColor)
      }

      Text("Filled", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismCard(variant = PrismCardVariant.Filled) {
        Text("Filled card content", style = Prism.typography.bodyLarge)
        Text("Secondary text", style = Prism.typography.caption, color = Prism.color.labelColor)
      }

      Text("Clickable", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismCard(variant = PrismCardVariant.Outlined, onClick = {}) {
        Text("Click me!", style = Prism.typography.bodyLarge)
        Text("This card is interactive", style = Prism.typography.caption, color = Prism.color.labelColor)
      }
    }
  }
}
