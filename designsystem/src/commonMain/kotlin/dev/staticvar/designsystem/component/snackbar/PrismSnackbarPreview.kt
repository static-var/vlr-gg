package dev.staticvar.designsystem.component.snackbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.CalendarAltSolid
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismSnackbarPreview(
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
      PrismSnackbar(message = "Live now: Sentinels vs PRX")

      PrismSnackbar(
        message = "Roster update saved",
        leadingIcon = {
          Icon(
            imageVector = LineAwesomeIcons.CalendarAltSolid,
            contentDescription = null,
          )
        },
        action = {
          Text(text = "UNDO", style = Prism.typography.button, color = Prism.color.accent)
        },
      )
    }
  }
}
