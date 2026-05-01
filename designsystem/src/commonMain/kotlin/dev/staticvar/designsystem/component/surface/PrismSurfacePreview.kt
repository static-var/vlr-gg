package dev.staticvar.designsystem.component.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
internal fun PrismSurfacePreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        "Automatic Content Colors",
        style = Prism.typography.headline,
        color = Prism.color.titleColor,
      )

      // Accent surface - text automatically becomes light/white
      PrismSurface(
        color = Prism.color.accent,
        shape = RoundedCornerShape(Prism.dimens.cornerM),
        modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text("Accent Surface", style = Prism.typography.cardTitle)
          Text("Content automatically uses light color", style = Prism.typography.bodySmall)
        }
      }

      // Success container
      PrismSurface(
        color = Prism.color.successContainer,
        shape = RoundedCornerShape(Prism.dimens.cornerM),
        modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text("Success Container", style = Prism.typography.cardTitle)
          Text("Content uses success color", style = Prism.typography.bodySmall)
        }
      }

      // Warning container
      PrismSurface(
        color = Prism.color.warningContainer,
        shape = RoundedCornerShape(Prism.dimens.cornerM),
        modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text("Warning Container", style = Prism.typography.cardTitle)
          Text("Content uses warning color", style = Prism.typography.bodySmall)
        }
      }

      // Danger container
      PrismSurface(
        color = Prism.color.dangerContainer,
        shape = RoundedCornerShape(Prism.dimens.cornerM),
        modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text("Danger Container", style = Prism.typography.cardTitle)
          Text("Content uses danger color", style = Prism.typography.bodySmall)
        }
      }

      // Surface variant
      PrismSurface(
        color = Prism.color.surfaceVariant,
        shape = RoundedCornerShape(Prism.dimens.cornerM),
        modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text("Surface Variant", style = Prism.typography.cardTitle)
          Text("Content uses primary text color", style = Prism.typography.bodySmall)
        }
      }
    }
  }
}
