/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.refresh

import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.domain.usecase.InitialFavoriteProfilesRefresh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal class InitialFavoriteProfilesRefreshImpl(
  private val favoritesRepository: FavoritesRepository,
  private val playerRepository: PlayerRepository,
  private val teamRepository: TeamRepository,
) : InitialFavoriteProfilesRefresh {
  private val stateMutex = Mutex()
  private var state: RefreshState = RefreshState.Pending

  override suspend fun awaitInitialRefresh(): Result<Unit> {
    while (true) {
      val call = stateMutex.withLock {
        when (val current = state) {
          RefreshState.Complete -> RefreshCall.AlreadyComplete
          RefreshState.Pending -> {
            val completion = CompletableDeferred<Result<Unit>>()
            state = RefreshState.Running(completion)
            RefreshCall.Run(completion)
          }
          is RefreshState.Running -> RefreshCall.Await(current.completion)
        }
      }

      when (call) {
        RefreshCall.AlreadyComplete -> return Result.success(Unit)
        is RefreshCall.Run -> return runRefresh(call.completion)
        is RefreshCall.Await -> try {
          return call.completion.await()
        } catch (cancellation: CancellationException) {
          currentCoroutineContext().ensureActive()
        }
      }
    }
  }

  private suspend fun runRefresh(completion: CompletableDeferred<Result<Unit>>): Result<Unit> = try {
    finish(completion, refreshFavoriteProfiles())
  } catch (cancellation: CancellationException) {
    withContext(NonCancellable) {
      stateMutex.withLock {
        reset(completion)
        completion.cancel(cancellation)
      }
    }
    throw cancellation
  } catch (error: Exception) {
    finish(completion, Result.failure(error))
  } catch (error: Throwable) {
    withContext(NonCancellable) {
      stateMutex.withLock {
        reset(completion)
        completion.completeExceptionally(error)
      }
    }
    throw error
  }

  private suspend fun finish(
    completion: CompletableDeferred<Result<Unit>>,
    result: Result<Unit>,
  ): Result<Unit> = stateMutex.withLock {
    if ((state as? RefreshState.Running)?.completion === completion) {
      state = if (result.isSuccess) RefreshState.Complete else RefreshState.Pending
    }
    completion.complete(result)
    result
  }

  private fun reset(completion: CompletableDeferred<Result<Unit>>) {
    if ((state as? RefreshState.Running)?.completion === completion) {
      state = RefreshState.Pending
    }
  }

  private suspend fun refreshFavoriteProfiles(): Result<Unit> {
    val favorites = favoritesRepository.observeDirectFavorites().first()
    val requests = buildList<suspend () -> Result<Unit>> {
      favorites.players.sortedBy { player -> player.id }.forEach { player ->
        add { playerRepository.refreshPlayerDetails(player.id) }
      }
      favorites.teams.sortedBy { team -> team.id }.forEach { team ->
        add { teamRepository.refreshTeamDetails(team.id) }
      }
    }
    val permits = Semaphore(PROFILE_REFRESH_CONCURRENCY)
    val results = coroutineScope {
      requests.map { refresh ->
        async {
          permits.withPermit { refreshResult(refresh) }
        }
      }.awaitAll()
    }
    return results.firstOrNull { result -> result.isFailure } ?: Result.success(Unit)
  }

  private sealed interface RefreshState {
    data object Pending : RefreshState
    data object Complete : RefreshState
    class Running(val completion: CompletableDeferred<Result<Unit>>) : RefreshState
  }

  private sealed interface RefreshCall {
    data object AlreadyComplete : RefreshCall
    class Run(val completion: CompletableDeferred<Result<Unit>>) : RefreshCall
    class Await(val completion: CompletableDeferred<Result<Unit>>) : RefreshCall
  }

  private companion object {
    const val PROFILE_REFRESH_CONCURRENCY: Int = 4
  }
}

private suspend fun refreshResult(refresh: suspend () -> Result<Unit>): Result<Unit> = try {
  refresh().also { result ->
    val error = result.exceptionOrNull()
    if (error is CancellationException) throw error
  }
} catch (cancellation: CancellationException) {
  throw cancellation
} catch (error: Exception) {
  Result.failure(error)
}
