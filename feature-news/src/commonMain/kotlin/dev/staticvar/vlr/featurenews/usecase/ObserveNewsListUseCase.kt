package dev.staticvar.vlr.featurenews.usecase

import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

public class ObserveNewsListUseCase(
  private val newsRepository: NewsRepository,
) {
  public operator fun invoke(): Flow<List<NewsItem>> = newsRepository.getNewsList()
}
