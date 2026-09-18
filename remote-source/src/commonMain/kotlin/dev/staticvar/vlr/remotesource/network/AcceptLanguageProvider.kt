/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

/** Supplies the device's current preferred BCP 47 language tags in preference order. */
interface AcceptLanguageProvider {
  fun preferredLanguageTags(): List<String>

  fun acceptLanguage(): String? = normalizedLanguageTags().takeIf { it.isNotEmpty() }?.joinToString(",")

  fun currentLanguageTag(): String? = normalizedLanguageTags().firstOrNull()

  private fun normalizedLanguageTags(): List<String> = preferredLanguageTags()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy { it.lowercase() }
}

expect class PlatformAcceptLanguageProvider() : AcceptLanguageProvider {
  override fun preferredLanguageTags(): List<String>
}
