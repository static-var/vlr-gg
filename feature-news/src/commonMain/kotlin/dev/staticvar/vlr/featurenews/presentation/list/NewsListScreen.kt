package dev.staticvar.vlr.featurenews.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonVariant
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagVariant
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsItem

@Composable
internal fun NewsListScreen(
  uiState: NewsListUiState,
  selectedArticleId: String?,
  onArticleSelected: (String) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "NEWS",
      subtitle = "OFFLINE-FIRST FEED",
      preLabel = "VLR",
      actions = {
        PrismButton(
          onClick = onRefresh,
          variant = PrismButtonVariant.Secondary,
        ) {
          Text("REFRESH")
        }
      },
    )

    when {
      uiState.isLoading && uiState.items.isEmpty() -> {
        PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "SYNCING NEWS",
          supportingText = "Loading local cache and remote updates",
        )
      }

      uiState.items.isEmpty() -> {
        EmptyNewsList(
          errorMessage = uiState.errorMessage,
          onRefresh = onRefresh,
          modifier = Modifier.fillMaxSize(),
        )
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(
            items = uiState.items,
            key = { item -> item.id },
          ) { item ->
            NewsListItem(
              item = item,
              selected = selectedArticleId == item.id,
              onClick = { onArticleSelected(item.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyNewsList(
  errorMessage: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      variant = PrismCardVariant.Outlined,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismSectionTitle(
          title = "No stories cached",
          preLabel = "news",
          showDivider = false,
        )
        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage,
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
        }
        PrismButton(
          onClick = onRefresh,
          variant = PrismButtonVariant.Primary,
        ) {
          Text("TRY AGAIN")
        }
      }
    }
  }
}

@Composable
private fun NewsListItem(
  item: NewsItem,
  selected: Boolean,
  onClick: () -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    variant = if (selected) PrismCardVariant.Filled else PrismCardVariant.Outlined,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        PrismTag(
          text = item.author.ifBlank { "unknown" },
          variant = PrismTagVariant.Accent,
        )
        PrismTag(
          text = item.date.ifBlank { "recent" },
          variant = PrismTagVariant.Info,
        )
      }

      Text(
        text = item.title,
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
      )

      if (item.description.isNotBlank()) {
        Text(
          text = item.description,
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
      }
    }
  }
}
