/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.usecase

public fun interface InitialFavoriteProfilesRefresh {
  public suspend fun awaitInitialRefresh(): Result<Unit>
}
