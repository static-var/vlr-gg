/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.sharedui.component.news.detail.NewsDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.news.detail.NewsDetailMediaSummaryItem
import dev.staticvar.vlr.sharedui.component.news.detail.NewsDetailReferencesItem
import dev.staticvar.vlr.sharedui.component.news.detail.NewsDetailStoryItem
import org.koin.compose.currentKoinScope

@Composable
public fun NewsArticleRoute(
  articleId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = currentKoinScope()
  val viewModel: NewsArticleViewModel = remember(scope) { scope.get<NewsArticleViewModel>() }
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(articleId) {
    viewModel.openArticle(articleId)
  }


  NewsArticleScreen(
    uiState = uiState,
    onBack = onBack,
    onRefresh = viewModel::refresh,
    showBackAction = true,
    modifier = modifier,
  )
}

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
      .padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = article?.title ?: "Article",
      subtitle = article?.author?.ifBlank { null } ?: article?.date,
      onBackPress = onBack.takeIf { showBackAction },
      actions = {
        PrismButton(
          onClick = onRefresh,
          style = PrismButtonStyle.Tertiary,
        ) {
          Text("Refresh")
        }
      },
    )

    when {
      uiState.isLoading && article == null -> {
        PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "Loading article",
          supportingText = "Reading story from local cache and remote updates",
        )
      }

      article == null -> {
        PrismCard(
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
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item(key = "header") {
            NewsDetailHeaderItem(article = article)
          }
          item(key = "story") {
            NewsDetailStoryItem(article = article)
          }
          item(key = "references") {
            NewsDetailReferencesItem(links = article.media.links)
          }
          item(key = "media") {
            NewsDetailMediaSummaryItem(media = article.media)
          }
          item(key = "navigation-bar-spacer") {
            Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
          }
        }
      }
    }
  }
}
