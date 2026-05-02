package dev.staticvar.vlr.featurenews.presentation.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle

@Composable
internal fun NewsArticleScreen(
  uiState: NewsArticleUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  showBackAction: Boolean,
  modifier: Modifier = Modifier,
) {
  val article: NewsArticle? = uiState.article

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = article?.title ?: "ARTICLE",
      subtitle = article?.author?.ifBlank { null } ?: article?.date,
      preLabel = "NEWS DETAIL",
      navigationSlot =
        if (showBackAction) {
          {
            PrismButton(
              onClick = onBack,
              style = PrismButtonStyle.Secondary,
            ) {
              Text("BACK")
            }
          }
        } else {
          null
        },
      actions = {
        PrismButton(
          onClick = onRefresh,
          style = PrismButtonStyle.Tertiary,
        ) {
          Text("REFRESH")
        }
      },
    )

    when {
      uiState.isLoading && article == null -> {
        PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "LOADING ARTICLE",
          supportingText = "Reading story from local cache / network",
        )
      }

      article == null -> {
        PrismCard(
          modifier = Modifier.fillMaxWidth(),
          style = PrismCardStyle.Outlined,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
            PrismSectionTitle(
              title = "Article unavailable",
              preLabel = "news",
              showDivider = false,
            )
            if (!uiState.errorMessage.isNullOrBlank()) {
              Text(
                text = uiState.errorMessage,
                style = Prism.typography.bodySmall,
                color = Prism.color.labelColor,
              )
            }
          }
        }
      }

      else -> {
        val paragraphs: List<String> = articleParagraphs(contentHtml = article.contentHtml)
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          item {
            RowOfMetaTags(
              article = article,
            )
          }

          if (paragraphs.isNotEmpty()) {
            item {
              PrismSectionTitle(
                title = "Story",
                preLabel = "content",
              )
            }
            items(paragraphs) { paragraph ->
              Text(
                text = paragraph,
                style = Prism.typography.bodySmall,
                color = Prism.color.bodyColor,
              )
            }
          }

          if (article.media.links.isNotEmpty()) {
            item {
              PrismSectionTitle(
                title = "References",
                preLabel = "media",
              )
            }
            items(article.media.links) { link ->
              PrismTag(
                text = link.text.ifBlank { link.url },
                style = PrismTagStyle.Info,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RowOfMetaTags(article: NewsArticle) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    if (article.author.isNotBlank()) {
      PrismTag(
        text = article.author,
        style = PrismTagStyle.Accent,
      )
    }
    if (article.date.isNotBlank()) {
      PrismTag(
        text = article.date,
        style = PrismTagStyle.Neutral,
      )
    }
    if (article.media.images.isNotEmpty()) {
      PrismTag(
        text = "${article.media.images.size} image",
        style = PrismTagStyle.Success,
      )
    }
    if (article.media.videos.isNotEmpty()) {
      PrismTag(
        text = "${article.media.videos.size} video",
        style = PrismTagStyle.Warning,
      )
    }
  }
}
