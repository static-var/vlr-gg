package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.card.BreezeCardStyle.Elevated
import dev.staticvar.designsystem.component.card.BreezeCardStyle.Filled
import dev.staticvar.designsystem.component.card.BreezeCardStyle.Outlined
import dev.staticvar.designsystem.theme.BreezeTheme
import dev.staticvar.designsystem.util.PreviewComponent

@Composable
fun BreezeCard(modifier: Modifier = Modifier, viewState: BreezeCardViewState) {
  when (viewState.style) {
    Filled -> Card(modifier) { ContentColumn(viewState.content) }
    Elevated -> ElevatedCard(modifier) { ContentColumn(viewState.content) }
    Outlined -> OutlinedCard(modifier) { ContentColumn(viewState.content) }
  }
}

@Composable
private fun ColumnScope.ContentColumn(content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier.padding(horizontal = BreezeTheme.dimens.large, vertical = BreezeTheme.dimens.medium)
  ) {
    content()
  }
}

@PreviewComponent
@Composable
fun BreezeCardPreview(modifier: Modifier = Modifier) {
  val breezeCardViewState =
    BreezeCardViewState(content = { Text("Hello, BreezeCard!") }, style = Filled)
  BreezeTheme(dynamicColor = false) {
    Column(
      modifier = Modifier
        .background(BreezeTheme.colorScheme.background)
        .padding(BreezeTheme.dimens.medium),
      verticalArrangement = Arrangement.spacedBy(BreezeTheme.dimens.large)
    ) {
      BreezeCard(modifier = modifier, viewState = breezeCardViewState)
      BreezeCard(modifier = modifier, viewState = breezeCardViewState.copy(style = Elevated))
      BreezeCard(modifier = modifier, viewState = breezeCardViewState.copy(style = Outlined))
    }
  }
}
