package dev.staticvar.designsystem.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
internal fun PrismSectionTitlePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      PrismSectionTitle(
        title = "Upcoming Matches",
        preLabel = "Today",
      )

      PrismSectionTitle(
        title = "Featured Events",
        preLabel = "Weekend",
        trailing = {
          Text(
            text = "04",
            style = Prism.typography.numericSecondary,
            color = Prism.color.accent,
          )
        },
      )

      PrismSectionTitle(
        title = "News",
        showDivider = false,
      )

      Text(
        text = "Display sample · AGENT 1234",
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
      )
      Text(
        text = "Body sample · The quick brown fox jumps over the lazy dog.",
        style = Prism.typography.bodyLarge,
        color = Prism.color.bodyColor,
      )
    }
  }
}
