package dev.staticvar.vlr.featurenews.di

import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleViewModel
import dev.staticvar.vlr.featurenews.presentation.list.NewsListViewModel
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsListUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public fun newsFeatureModule(): Module =
  module {
    factory { ObserveNewsListUseCase(newsRepository = get()) }
    factory { RefreshNewsUseCase(newsRepository = get()) }
    factory { ObserveNewsArticleUseCase(newsRepository = get()) }
    factory { RefreshNewsArticleUseCase(newsRepository = get()) }

    factory {
      NewsListViewModel(
        observeNewsListUseCase = get(),
        refreshNewsUseCase = get(),
        dispatchers = get(),
      )
    }
    factory {
      NewsArticleViewModel(
        observeNewsArticleUseCase = get(),
        refreshNewsArticleUseCase = get(),
        dispatchers = get(),
      )
    }
  }
