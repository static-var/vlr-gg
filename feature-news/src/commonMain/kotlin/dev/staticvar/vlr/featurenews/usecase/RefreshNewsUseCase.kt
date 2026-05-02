/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.usecase

import dev.staticvar.vlr.domain.repository.NewsRepository

public class RefreshNewsUseCase(private val newsRepository: NewsRepository) {
  public suspend operator fun invoke(): Result<Unit> = newsRepository.refreshNews()
}
