package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.theme.VLRTheme

val cardAlpha: Float
  @Composable
  get() {
    return if (isSystemInDarkTheme()) 0.3f else 0.6f
  }

val emphasisCardAlpha: Float
  @Composable
  get() {
    return if (isSystemInDarkTheme()) 0.6f else 0.6f
  }

@Composable
fun CardView(
  modifier: Modifier = Modifier,
  colors: CardColors =
    CardDefaults.cardColors(
      contentColor = VLRTheme.colorScheme.onPrimaryContainer,
      containerColor = VLRTheme.colorScheme.primaryContainer.copy(cardAlpha)
    ),
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .animateContentSize()
      .then(modifier),
    shape = RoundedCornerShape(8.dp),
    colors = colors
  ) { content(this) }
}

@Composable
fun EmphasisCardView(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier =
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .animateContentSize()
      .then(modifier),
    shape = RoundedCornerShape(8.dp),
    colors =
    CardDefaults.cardColors(
      contentColor = VLRTheme.colorScheme.onPrimaryContainer,
      containerColor = VLRTheme.colorScheme.primaryContainer.copy(emphasisCardAlpha)
    )
  ) { content(this) }
}
