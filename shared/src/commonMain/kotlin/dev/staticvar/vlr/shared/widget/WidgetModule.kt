/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetEngine
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetRemote
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

internal val WidgetJson = named("widgetJson")

internal fun widgetModule() = module {
  single<Json>(WidgetJson) {
    Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
  }
  single { FavoriteWidgetEngine(FavoriteWidgetRemote(get(), get(WidgetJson)), Clock.System, get(WidgetJson)) }
  single { FavoriteMatchLogos(get(), get(WidgetJson)) }
}
