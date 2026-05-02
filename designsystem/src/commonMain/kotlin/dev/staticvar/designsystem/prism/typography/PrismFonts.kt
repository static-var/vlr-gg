package dev.staticvar.designsystem.prism.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily

@Immutable
internal data class PrismFontFamilies(
  val display: FontFamily,
  val numeric: FontFamily,
  val title: FontFamily,
  val body: FontFamily,
  val label: FontFamily,
  val caption: FontFamily,
  val button: FontFamily,
)

@Composable
internal fun rememberPrismFontFamilies(): PrismFontFamilies {
  return remember {
    PrismFontFamilies(
      display = FontFamily.Default,
      numeric = FontFamily.Monospace,
      title = FontFamily.Default,
      body = FontFamily.Default,
      label = FontFamily.Default,
      caption = FontFamily.Default,
      button = FontFamily.Default,
    )
  }
}
