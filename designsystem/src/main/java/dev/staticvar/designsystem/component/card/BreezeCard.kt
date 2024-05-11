package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.theme.BreezeTheme
import dev.staticvar.designsystem.util.PreviewAll

@Composable
fun BreezeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(modifier = modifier, content = content)
}

@PreviewAll
@Composable
fun BreezeCardPreview(modifier: Modifier = Modifier) {
  BreezeTheme(dynamicColor = true) { BreezeCard(modifier = modifier) { Text("Hello, BreezeCard!") } }
}
