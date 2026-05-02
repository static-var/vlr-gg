/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.di

import dev.staticvar.vlr.core.di.dispatcherModule
import dev.staticvar.vlr.data.di.dataModule
import dev.staticvar.vlr.featureevents.di.eventsFeatureModule
import dev.staticvar.vlr.featurematches.di.matchesFeatureModule
import dev.staticvar.vlr.featurenews.di.newsFeatureModule
import dev.staticvar.vlr.featureplayer.di.playerFeatureModule
import dev.staticvar.vlr.featurerankings.di.rankingsFeatureModule
import dev.staticvar.vlr.featureteam.di.teamFeatureModule
import dev.staticvar.vlr.localsource.di.localSourceModule
import dev.staticvar.vlr.localsource.di.platformLocalSourceModule
import dev.staticvar.vlr.remotesource.di.remoteSourceModule
import dev.staticvar.vlr.remotesource.network.NetworkConfiguration
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

private object AppNetworkDefaults {
  const val ApiHost: String = "vlr-scraper.akhilnarang.dev"
  const val ApplicationHeader: String = "app-name"
  const val ApplicationId: String = "dev.staticvar.vlr"
  const val AuthorizationHeader: String = "Authorization"
}

internal fun buildDefaultNetworkHeaders(authToken: String?): Map<String, String> {
  val normalizedToken = authToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
  val headers = mutableMapOf(
    AppNetworkDefaults.ApplicationHeader to AppNetworkDefaults.ApplicationId,
  )
  if (!normalizedToken.isNullOrBlank()) {
    headers[AppNetworkDefaults.AuthorizationHeader] = normalizedToken
  }
  return headers
}

internal fun buildNetworkConfiguration(authToken: String?): NetworkConfiguration = NetworkConfiguration(
  host = AppNetworkDefaults.ApiHost,
  defaultHeaders = buildDefaultNetworkHeaders(authToken = authToken),
)

public fun initializeAppKoin(
  appDeclaration: KoinAppDeclaration = {},
  authToken: String? = null,
  networkConfiguration: NetworkConfiguration = buildNetworkConfiguration(authToken),
) {
  if (KoinPlatform.getKoinOrNull() != null) {
    return
  }

  startKoin {
    appDeclaration()
    modules(
      dispatcherModule(),
      remoteSourceModule(configuration = networkConfiguration),
      platformLocalSourceModule,
      localSourceModule,
      dataModule(),
      matchesFeatureModule(),
      eventsFeatureModule(),
      newsFeatureModule(),
      rankingsFeatureModule(),
      teamFeatureModule(),
      playerFeatureModule(),
    )
  }
}
