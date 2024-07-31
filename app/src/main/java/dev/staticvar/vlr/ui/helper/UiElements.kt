package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardView(
  modifier: Modifier = Modifier,
  colors: CardColors = CardDefaults.elevatedCardColors(),
  content: @Composable ColumnScope.() -> Unit,
) {
  ElevatedCard(
    modifier =
      Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        .animateContentSize(
          animationSpec =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )
        .then(modifier),
    shape = RoundedCornerShape(8.dp),
    colors = colors,
    elevation =
      CardDefaults.elevatedCardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
  ) {
    content(this)
  }
}

@Composable
fun EmphasisCardView(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  ElevatedCard(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .animateContentSize(
          animationSpec =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )
        .then(modifier),
    shape = RoundedCornerShape(8.dp),
    elevation =
      CardDefaults.elevatedCardElevation(defaultElevation = 12.dp, pressedElevation = 16.dp),
  ) {
    content(this)
  }
}
