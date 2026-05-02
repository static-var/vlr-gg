package dev.staticvar.designsystem.component.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
internal fun PrismHeaderPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant
) {
  PrismTheme(variant = variant) {
    Column(modifier = Modifier.fillMaxWidth().background(Prism.color.background)) {
      PrismHeader(text = "Matches")
      PrismHeader(text = "Latest News")
      PrismHeader(text = "VCT Pacific")
    }
  }
}
