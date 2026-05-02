@file:Suppress("MatchingDeclarationName")

package dev.staticvar.designsystem.prism.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.FontResource
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.allFontResources
import org.jetbrains.compose.resources.Font as ResourceFont

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
  val displayFont = fontFamilyResource("prism_display") ?: FontFamily.Default
  val bodyFont = fontFamilyResource("prism_body") ?: FontFamily.Default

  return remember(displayFont, bodyFont) {
    PrismFontFamilies(
      display = displayFont,
      numeric = displayFont,
      title = displayFont,
      body = bodyFont,
      label = bodyFont,
      caption = bodyFont,
      button = bodyFont,
    )
  }
}

@Composable
private fun fontFamilyResource(resourceName: String): FontFamily? =
  fontResource(resourceName)?.let { fontResource -> FontFamily(ResourceFont(fontResource)) }

private fun fontResource(resourceName: String): FontResource? = Res.allFontResources[resourceName]
