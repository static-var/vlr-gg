/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import android.content.Context
import android.os.LocaleList
import org.koin.mp.KoinPlatform

actual class PlatformAcceptLanguageProvider actual constructor() : AcceptLanguageProvider {
  actual override fun preferredLanguageTags(): List<String> {
    val locales = applicationLocales() ?: LocaleList.getAdjustedDefault()
    return List(locales.size()) { index -> locales[index].toLanguageTag() }
  }

  private fun applicationLocales(): LocaleList? = runCatching {
    KoinPlatform.getKoin().get<Context>().resources.configuration.locales
  }.getOrNull()
}
