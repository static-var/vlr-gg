/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.di

import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleViewModel
import dev.staticvar.vlr.featurenews.presentation.list.NewsListViewModel
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsListUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val NavigationEntryScopeQualifier: String = "navigation-entry"

public fun newsFeatureModule(): Module = module {
  factory { ObserveNewsListUseCase(newsRepository = get()) }
  factory { RefreshNewsUseCase(newsRepository = get()) }
  factory { ObserveNewsArticleUseCase(newsRepository = get()) }
  factory { RefreshNewsArticleUseCase(newsRepository = get()) }

  scope(named(NavigationEntryScopeQualifier)) {
    scoped {
      NewsListViewModel(
        observeNewsListUseCase = get(),
        refreshNewsUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
    scoped {
      NewsArticleViewModel(
        observeNewsArticleUseCase = get(),
        refreshNewsArticleUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
  }
}
