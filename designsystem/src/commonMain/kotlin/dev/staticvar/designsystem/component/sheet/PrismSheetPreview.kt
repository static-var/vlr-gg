package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonVariant
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismSheetPreview(
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
      PrismSheet(
        header = {
          Text("Match filters", style = Prism.typography.cardTitle)
          Text(
            "Persistent sheet variant",
            style = Prism.typography.caption,
            color = Prism.color.labelColor,
          )
        },
        footer = {
          PrismButton(
            onClick = {},
            modifier = Modifier.padding(end = Prism.dimens.spacingS),
            variant = PrismButtonVariant.Secondary,
          ) {
            Text("Reset")
          }
          PrismButton(onClick = {}, variant = PrismButtonVariant.Primary) { Text("Apply") }
        },
      ) {
        Text("Region: EMEA", style = Prism.typography.bodyLarge)
        Text("Format: Bo3", style = Prism.typography.bodyLarge)
        Text("Status: Live + upcoming", style = Prism.typography.bodyLarge)
      }
    }
  }
}

@PrismPreview
@Composable
internal fun PrismModalSheetPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Prism.color.background),
    ) {
      PrismModalSheet(
        visible = true,
        onDismissRequest = {},
        header = {
          Text("Reminder", style = Prism.typography.cardTitle)
          Text(
            "Modal sheet variant",
            style = Prism.typography.caption,
            color = Prism.color.labelColor,
          )
        },
        footer = {
          PrismButton(onClick = {}, variant = PrismButtonVariant.Primary) { Text("Save") }
        },
      ) {
        Text("Enable match reminder for Fnatic vs TH", style = Prism.typography.bodyLarge)
        Text(
          "You will get a push 15 minutes before start.",
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
        )
      }
    }
  }
}
