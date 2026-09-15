/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.featurematches.usecase.SetMatchFavoriteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.favorite_update_failed

internal data class MatchFavoriteMutationState(
  val pendingIds: Set<String> = emptySet(),
  val errorMessage: StringResource? = null,
)

internal class MatchFavoriteController(
  private val scope: CoroutineScope,
  private val setFavorite: SetMatchFavoriteUseCase,
) {
  val state = MutableStateFlow(MatchFavoriteMutationState())

  fun toggle(matchId: String, isDirectFavorite: Boolean, awaitSelection: suspend (Boolean) -> Unit) {
    if (matchId in state.value.pendingIds) return
    state.update { it.copy(pendingIds = it.pendingIds + matchId, errorMessage = null) }
    scope.launch {
      try {
        setFavorite(matchId, !isDirectFavorite).getOrThrow()
        awaitSelection(!isDirectFavorite)
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        state.update { it.copy(errorMessage = Res.string.favorite_update_failed) }
      } finally {
        state.update { it.copy(pendingIds = it.pendingIds - matchId) }
      }
    }
  }
}
