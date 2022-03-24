package dev.staticvar.vlr.ui.helper

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.theme.VLRTheme

const val CARD_ALPHA = 0.4f

@Composable
fun CardView(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).then(modifier),
    shape = RoundedCornerShape(16.dp),
    contentColor = VLRTheme.colorScheme.onPrimaryContainer,
    containerColor = VLRTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) { content(this) }
}

@Composable
fun EmphasisCardView(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).then(modifier),
    shape = RoundedCornerShape(16.dp),
    contentColor = VLRTheme.colorScheme.onPrimaryContainer,
    containerColor = VLRTheme.colorScheme.primaryContainer.copy(CARD_ALPHA + CARD_ALPHA)
  ) { content(this) }
}
