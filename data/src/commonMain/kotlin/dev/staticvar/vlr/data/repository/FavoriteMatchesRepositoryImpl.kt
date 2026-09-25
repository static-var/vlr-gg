/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.data.mapper.toDomain
import dev.staticvar.vlr.data.mapper.toOverviewEntity
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.repository.FavoriteMatchFeed
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRetryLaterException
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesDataSource
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesUnavailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

internal class FavoriteMatchesRepositoryImpl(
  private val source: FavoriteMatchesDataSource,
  private val identity: UserIdentityRepository,
  private val favorites: FavoritesRepository,
) : FavoriteMatchesRepository {
  private val mutableHomeMatches = MutableStateFlow<FavoriteMatchFeed?>(null)
  override val homeMatches: StateFlow<FavoriteMatchFeed?> = mutableHomeMatches

  override suspend fun fetch(includeResults: Boolean): Result<List<MatchPreview>> {
    val selection = if (includeResults) favorites.observeDirectFavorites().first() else null
    return source.list(identity.id.value.toString(), includeResults).fold(
      onSuccess = { response ->
        Result.success(response.map { it.toOverviewEntity().toDomain() }.also { matches ->
          if (selection != null) mutableHomeMatches.value = FavoriteMatchFeed(selection, matches)
        })
      },
      onFailure = { error ->
        Result.failure(
          if (error is FavoriteMatchesUnavailableException) FavoriteMatchesRetryLaterException(error.statusCode) else error,
        )
      },
    )
  }
}
