package dev.staticvar.designsystem.component.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.theme.BreezeTheme
import dev.staticvar.designsystem.util.PreviewComponent

@Composable
fun BreezeTitleText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = 1,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = BreezeTheme.typography.headlineMedium,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    maxLines = maxLines
  )
}

@Composable
fun BreezeSubtitleText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = BreezeTheme.typography.titleMedium,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    maxLines = maxLines
  )
}

@Composable
fun BreezeLargeText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = BreezeTheme.typography.displayMedium,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    maxLines = maxLines
  )
}

@Composable
fun BreezeText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = BreezeTheme.typography.bodyMedium,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    maxLines = maxLines
  )
}

@Composable
fun BreezeLabel(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = BreezeTheme.typography.labelMedium,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    maxLines = maxLines
  )
}

@PreviewComponent
@Composable
private fun BreezeTexts(modifier: Modifier = Modifier) {

  BreezeTheme() {
    Surface {
      Column(
        modifier = Modifier
          .background(BreezeTheme.colorScheme.background)
          .padding(BreezeTheme.dimens.medium),
        verticalArrangement = Arrangement.spacedBy(BreezeTheme.dimens.large)
      ) {
        BreezeLargeText("Breeze Large")
        BreezeTitleText("Breeze Title")
        BreezeSubtitleText("Breeze Subtitle")
        BreezeText("Breeze Default Text")
        BreezeLabel("Breeze Label Text")
      }
    }
  }
}
