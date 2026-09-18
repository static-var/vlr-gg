/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val WidgetJson = named("widgetJson")

internal fun widgetModule() = module {
  single<Json>(WidgetJson) {
    Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
  }
}
