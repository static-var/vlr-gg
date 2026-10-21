/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetSelection
import java.io.File
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source

internal data class FavoriteWidgetProvider(
  val receiver: Class<out AppWidgetProvider>,
  val widget: FavoriteEntityWidget,
)

internal object FavoriteEntityWidgets {
  val providers: List<FavoriteWidgetProvider> = emptyList()
}

internal data class FavoriteWidgetInstance(
  val appWidgetId: Int,
  val glanceId: GlanceId,
  val provider: FavoriteWidgetProvider,
  val selection: WidgetSelection,
)

internal data class FavoriteChoice(val id: String, val name: String)
internal data class FavoritePicker(val kind: WidgetKind, val choices: List<FavoriteChoice>)
internal enum class ConfigurationResult { Saved, FavoritesChanged, InvalidWidget }

internal class FavoriteWidgetBindings(
  private val context: Context,
  private val providers: List<FavoriteWidgetProvider> = FavoriteEntityWidgets.providers,
) {
  private val manager = AppWidgetManager.getInstance(context)
  private val glance = GlanceAppWidgetManager(context)

  fun provider(appWidgetId: Int): FavoriteWidgetProvider? {
    val component = manager.getAppWidgetInfo(appWidgetId)?.provider ?: return null
    return providers.firstOrNull { component == ComponentName(context, it.receiver) }
  }

  suspend fun installed(): List<FavoriteWidgetInstance> = providers.flatMap { provider ->
    manager.getAppWidgetIds(ComponentName(context, provider.receiver)).toList().mapNotNull { id ->
      read(id)
    }
  }

  suspend fun read(appWidgetId: Int): FavoriteWidgetInstance? {
    val provider = provider(appWidgetId) ?: return null
    val id = glance.getGlanceIdBy(appWidgetId)
    val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
    return FavoriteWidgetInstance(appWidgetId, id, provider, selection(provider.widget.kind, preferences))
  }

  fun picker(appWidgetId: Int, source: UpcomingMatchesSnapshot?): FavoritePicker? {
    val kind = provider(appWidgetId)?.widget?.kind ?: return null
    val collator = Collator.getInstance(context.resources.configuration.locales[0])
    val choices = source.choices(kind).sortedWith { left, right ->
      collator.compare(left.name, right.name).takeIf { it != 0 } ?: left.id.compareTo(right.id)
    }
    return FavoritePicker(kind, choices)
  }

  suspend fun choose(appWidgetId: Int, entityId: String, source: () -> UpcomingMatchesSnapshot?): ConfigurationResult {
    val kind = provider(appWidgetId)?.widget?.kind ?: return ConfigurationResult.InvalidWidget
    val id = glance.getGlanceIdBy(appWidgetId)
    var result = ConfigurationResult.InvalidWidget
    updateAppWidgetState(context, id) { state ->
      result = when {
        provider(appWidgetId)?.widget?.kind != kind -> ConfigurationResult.InvalidWidget

        source().choices(kind).none { it.id == entityId } -> ConfigurationResult.FavoritesChanged

        else -> {
          state[SelectedEntity] = entityId
          ConfigurationResult.Saved
        }
      }
    }
    return result
  }

  // Glance names each widget's state file after its appWidgetId. A restore assigns new IDs that can equal old IDs
  // of either provider, so callers read every restored selection before the first write.
  suspend fun restoredSelections(): Map<Int, String> = withContext(Dispatchers.IO) {
    File(context.applicationContext.filesDir, "datastore").listFiles().orEmpty().mapNotNull { file ->
      val id = StateFile.matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
      val selection = try {
        file.source().buffer().use { PreferencesSerializer.readFrom(it) }[SelectedEntity]
      } catch (_: CorruptionException) {
        null
      }
      selection?.let { id to it }
    }.toMap()
  }

  suspend fun restore(oldIds: IntArray, newIds: IntArray, selections: Map<Int, String>) {
    oldIds.zip(newIds).forEach { (old, new) ->
      if (provider(new) == null) return@forEach
      updateAppWidgetState(context, glance.getGlanceIdBy(new)) { state ->
        val selection = selections[old]
        if (selection == null) state.remove(SelectedEntity) else state[SelectedEntity] = selection
      }
    }
  }

  companion object {
    private val SelectedEntity = stringPreferencesKey("selected_entity_id")
    private val StateFile = Regex("""appWidget-(\d+)\.preferences_pb""")

    fun selection(kind: WidgetKind, preferences: Preferences): WidgetSelection =
      preferences[SelectedEntity]?.let { WidgetSelection.Chosen(kind, it) } ?: WidgetSelection.None(kind)
  }
}

private fun UpcomingMatchesSnapshot?.choices(kind: WidgetKind): List<FavoriteChoice> = when (kind) {
  WidgetKind.Team -> this?.favoriteTeams.orEmpty().map { FavoriteChoice(it.id, it.name) }
  WidgetKind.Event -> this?.favoriteEvents.orEmpty().map { FavoriteChoice(it.id, it.name) }
}
