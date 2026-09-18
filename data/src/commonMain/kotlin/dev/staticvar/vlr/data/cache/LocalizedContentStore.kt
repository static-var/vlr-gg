/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.cache

import com.russhwolf.settings.Settings
import dev.staticvar.vlr.domain.model.MatchVeto
import dev.staticvar.vlr.domain.model.VetoAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface RegionLabelStore {
  val version: StateFlow<Long>
  fun label(region: String): String
  suspend fun put(contentLanguage: String?, labels: Map<String, String>)
}

/** Schema-free storage for response-localized region labels. */
internal class LocalizedRegionLabelStore(
  private val storage: Settings,
  private val currentLanguageTag: () -> String?,
) : RegionLabelStore {
  private val json = Json { ignoreUnknownKeys = true }
  private val mutableVersion = MutableStateFlow(0L)
  private val writeMutex = Mutex()

  override val version: StateFlow<Long> = mutableVersion

  override fun label(region: String): String {
    if (region.isBlank()) return ""
    val language = currentLanguageTag()?.normalizeLanguageTag() ?: return ""
    return languageCandidates(language)
      .firstNotNullOfOrNull { candidate -> labels(candidate)[region]?.takeIf(String::isNotBlank) }
      .orEmpty()
  }

  override suspend fun put(contentLanguage: String?, labels: Map<String, String>) {
    val language = contentLanguage?.normalizeLanguageTag()?.takeIf(String::isNotBlank) ?: return
    val usableLabels = labels.filter { (region, label) -> region.isNotBlank() && label.isNotBlank() }
    if (usableLabels.isEmpty()) return

    writeMutex.withLock {
      val current = labels(language)
      val merged = current + usableLabels
      if (merged == current) return@withLock

      storage.putString(languageKey(language), json.encodeToString(merged))
      val languages = knownLanguages() + language
      storage.putString(LanguagesKey, languages.sorted().joinToString(LanguageSeparator))
      mutableVersion.value += 1
    }
  }

  private fun languageCandidates(language: String): List<String> {
    val base = language.substringBefore('-')
    return buildList {
      add(language)
      if (base != language) add(base)
      addAll(knownLanguages().filter { it.substringBefore('-') == base })
    }.distinct()
  }

  private fun knownLanguages(): Set<String> = storage
    .getStringOrNull(LanguagesKey)
    ?.split(LanguageSeparator)
    ?.filter(String::isNotBlank)
    ?.toSet()
    .orEmpty()

  private fun labels(language: String): Map<String, String> = storage
    .getStringOrNull(languageKey(language))
    ?.let { encoded -> runCatching { json.decodeFromString<Map<String, String>>(encoded) }.getOrNull() }
    .orEmpty()

  private fun languageKey(language: String): String = "$LabelsKeyPrefix$language"

  private companion object {
    const val LabelsKeyPrefix = "localized_content.region_labels."
    const val LanguagesKey = "localized_content.region_labels.languages"
    const val LanguageSeparator = "|"
  }
}

internal interface VetoStore {
  val version: StateFlow<Long>
  fun get(matchId: String, rawBans: List<String>): List<MatchVeto>
  suspend fun put(matchId: String, rawBans: List<String>, veto: List<MatchVeto>)
}

/** Stores server-parsed veto steps only while they still match the locally cached raw notes. */
internal class MatchVetoStore(private val storage: Settings) : VetoStore {
  private val json = Json { ignoreUnknownKeys = true }
  private val mutableVersion = MutableStateFlow(0L)
  private val writeMutex = Mutex()

  override val version: StateFlow<Long> = mutableVersion

  override fun get(matchId: String, rawBans: List<String>): List<MatchVeto> {
    val entry = entries()[matchId] ?: return emptyList()
    if (entry.rawBans != rawBans) return emptyList()
    return entry.veto.map { cached ->
      MatchVeto(
        team = cached.team,
        action = cached.action.toVetoAction(),
        map = cached.map,
      )
    }
  }

  override suspend fun put(matchId: String, rawBans: List<String>, veto: List<MatchVeto>) {
    if (matchId.isBlank()) return
    val cached = CachedMatchVeto(
      rawBans = rawBans,
      veto = veto.map { step -> CachedVeto(step.team, step.action.name, step.map) },
    )
    writeMutex.withLock {
      val current = entries()
      if (current[matchId] == cached) return@withLock
      val updated = (current - matchId + (matchId to cached)).entries.toList()
        .takeLast(MaxEntries)
        .associate { it.toPair() }
      storage.putString(CacheKey, json.encodeToString(updated))
      mutableVersion.value += 1
    }
  }

  private fun entries(): Map<String, CachedMatchVeto> = storage
    .getStringOrNull(CacheKey)
    ?.let { encoded -> runCatching { json.decodeFromString<Map<String, CachedMatchVeto>>(encoded) }.getOrNull() }
    .orEmpty()

  private fun String.toVetoAction(): VetoAction = VetoAction.entries.firstOrNull { it.name == this } ?: VetoAction.UNKNOWN

  private companion object {
    const val CacheKey = "localized_content.match_veto"
    const val MaxEntries = 100
  }
}

internal object EmptyRegionLabelStore : RegionLabelStore {
  override val version: StateFlow<Long> = MutableStateFlow(0L)
  override fun label(region: String): String = ""
  override suspend fun put(contentLanguage: String?, labels: Map<String, String>) = Unit
}

internal object EmptyVetoStore : VetoStore {
  override val version: StateFlow<Long> = MutableStateFlow(0L)
  override fun get(matchId: String, rawBans: List<String>): List<MatchVeto> = emptyList()
  override suspend fun put(matchId: String, rawBans: List<String>, veto: List<MatchVeto>) = Unit
}

private fun String.normalizeLanguageTag(): String = trim().replace('_', '-').lowercase()

@Serializable
private data class CachedMatchVeto(val rawBans: List<String>, val veto: List<CachedVeto>)

@Serializable
private data class CachedVeto(val team: String?, val action: String, val map: String)
