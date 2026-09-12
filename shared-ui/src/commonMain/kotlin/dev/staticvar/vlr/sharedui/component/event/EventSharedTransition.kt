/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism

@OptIn(ExperimentalSharedTransitionApi::class)
private data class EventTransitionScope(
  val sharedTransitionScope: SharedTransitionScope,
  val animatedVisibilityScope: AnimatedVisibilityScope,
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalEventTransitionScope = compositionLocalOf<EventTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun ProvideEventTransitionScope(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalEventTransitionScope provides EventTransitionScope(
      sharedTransitionScope = sharedTransitionScope,
      animatedVisibilityScope = animatedVisibilityScope,
    ),
    content = content,
  )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.eventLogoSharedElement(eventId: String): Modifier {
  val transitionScope = LocalEventTransitionScope.current ?: return this
  val animation = Prism.anim.standard
  return with(transitionScope.sharedTransitionScope) {
    sharedElement(
      sharedContentState = rememberSharedContentState(key = "event:$eventId:${EventSharedContent.Logo}"),
      animatedVisibilityScope = transitionScope.animatedVisibilityScope,
      boundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = animation.durationMillis, easing = animation.easing)
      },
    )
  }
}

internal enum class EventSharedContent {
  Card, Logo, Title, Dates, Prize, Status, Favorite,
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.eventSharedBounds(eventId: String, content: EventSharedContent): Modifier {
  val transitionScope = LocalEventTransitionScope.current ?: return this
  val animation = Prism.anim.standard
  return with(transitionScope.sharedTransitionScope) {
    sharedBounds(
      sharedContentState = rememberSharedContentState(key = "event:$eventId:$content"),
      animatedVisibilityScope = transitionScope.animatedVisibilityScope,
      boundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = animation.durationMillis, easing = animation.easing)
      },
      resizeMode = if (content == EventSharedContent.Card) {
        SharedTransitionScope.ResizeMode.RemeasureToBounds
      } else {
        SharedTransitionScope.ResizeMode.scaleToBounds()
      },
      zIndexInOverlay = if (content == EventSharedContent.Card) 0f else 1f,
    )
  }
}
