package dev.staticvar.vlr.featurenews.usecase

import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

public class ObserveNewsArticleUseCase(
  private val newsRepository: NewsRepository,
) {
  public operator fun invoke(articleId: String): Flow<NewsArticle?> =
    newsRepository.getNewsArticle(articleId = articleId)
}
