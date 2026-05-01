package dev.staticvar.vlr.shared.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import dev.staticvar.designsystem.prism.Prism

internal class GroupedListDetailScene<T : Any>(
  override val key: Any,
  override val previousEntries: List<NavEntry<T>>,
  private val listEntry: NavEntry<T>,
  private val detailEntry: NavEntry<T>,
) : Scene<T> {
  override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)
  override val content: @Composable () -> Unit = {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Column(modifier = Modifier.weight(0.4f).fillMaxSize()) {
        listEntry.Content()
      }
      Column(modifier = Modifier.weight(0.6f).fillMaxSize()) {
        detailEntry.Content()
      }
    }
  }
}

internal class GroupedListDetailSceneStrategy<T : Any>(
  private val enabled: Boolean,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    if (!enabled) {
      return null
    }

    val detailEntry: NavEntry<T> =
      entries.lastOrNull()?.takeIf { it.metadata[DetailGroupKey] is String } ?: return null
    val detailGroup: String = detailEntry.metadata[DetailGroupKey] as? String ?: return null
    val listEntry: NavEntry<T> =
      entries.findLast { entry ->
        entry.metadata[ListGroupKey] == detailGroup
      } ?: return null

    return GroupedListDetailScene(
      key = listEntry.contentKey,
      previousEntries = entries.dropLast(1),
      listEntry = listEntry,
      detailEntry = detailEntry,
    )
  }
}

@Composable
internal fun <T : Any> rememberGroupedListDetailSceneStrategy(
  enabled: Boolean,
): SceneStrategy<T> =
  remember(enabled) {
    GroupedListDetailSceneStrategy(enabled = enabled)
  }

internal fun listPane(group: String): Map<String, Any> = mapOf(ListGroupKey to group)

internal fun detailPane(group: String): Map<String, Any> = mapOf(DetailGroupKey to group)

private const val ListGroupKey: String = "AppListDetail-ListGroup"
private const val DetailGroupKey: String = "AppListDetail-DetailGroup"
