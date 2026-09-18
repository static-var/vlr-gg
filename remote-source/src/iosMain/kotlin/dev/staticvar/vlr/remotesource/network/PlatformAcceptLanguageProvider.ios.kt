/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual class PlatformAcceptLanguageProvider actual constructor() : AcceptLanguageProvider {
  actual override fun preferredLanguageTags(): List<String> = NSLocale.preferredLanguages.filterIsInstance<String>()
}
