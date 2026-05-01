@file:OptIn(ExperimentalResourceApi::class)

package dev.staticvar.designsystem.prism.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.artific_regular
import vlr.designsystem.generated.resources.construct_mono_regular

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
  val constructMonoNormal = Font(Res.font.construct_mono_regular, weight = FontWeight.Normal)
  val constructMonoMedium = Font(Res.font.construct_mono_regular, weight = FontWeight.Medium)
  val constructMonoSemi = Font(Res.font.construct_mono_regular, weight = FontWeight.SemiBold)
  val constructMonoBold = Font(Res.font.construct_mono_regular, weight = FontWeight.Bold)

  val artificRegular = Font(Res.font.artific_regular, weight = FontWeight.Normal)
  val artificMedium = Font(Res.font.artific_regular, weight = FontWeight.Medium)
  val artificSemi = Font(Res.font.artific_regular, weight = FontWeight.SemiBold)
  val artificBold = Font(Res.font.artific_regular, weight = FontWeight.Bold)

  return remember {
    val constructMono =
      FontFamily(
        constructMonoNormal,
        constructMonoMedium,
        constructMonoSemi,
        constructMonoBold,
      )
    val artific =
      FontFamily(
        artificRegular,
        artificMedium,
        artificSemi,
        artificBold,
      )

    PrismFontFamilies(
      display = constructMono,
      numeric = constructMono,
      title = constructMono,
      body = artific,
      label = constructMono,
      caption = constructMono,
      button = constructMono,
    )
  }
}
