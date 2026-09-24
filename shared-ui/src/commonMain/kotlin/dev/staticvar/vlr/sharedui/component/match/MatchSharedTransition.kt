/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match

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
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.rememberTransitionContentFade

@OptIn(ExperimentalSharedTransitionApi::class)
private data class MatchTransitionScope(
  val sharedTransitionScope: SharedTransitionScope,
  val animatedVisibilityScope: AnimatedVisibilityScope,
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalMatchTransitionScope = compositionLocalOf<MatchTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun ProvideMatchTransitionScope(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalMatchTransitionScope provides if (enabled) {
      MatchTransitionScope(sharedTransitionScope, animatedVisibilityScope)
    } else {
      null
    },
    content = content,
  )
}

/** Keeps detail-only content hidden until the shared entrance has settled. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun rememberMatchDetailContentFade(ready: Boolean = true): TransitionContentFade {
  val scope = LocalMatchTransitionScope.current
  return rememberTransitionContentFade(scope?.sharedTransitionScope, scope?.animatedVisibilityScope, ready)
}

internal enum class MatchSharedContent {
  Card, Event, TeamName, TeamScore,
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.matchSharedBounds(matchId: String, content: MatchSharedContent, teamId: String? = null): Modifier {
  if ((content == MatchSharedContent.TeamName || content == MatchSharedContent.TeamScore) && teamId.isNullOrBlank()) {
    return this
  }
  val transitionScope = LocalMatchTransitionScope.current ?: return this
  val animation = Prism.anim.standard
  return with(transitionScope.sharedTransitionScope) {
    sharedBounds(
      sharedContentState = rememberSharedContentState(key = "match:$matchId:$content:${teamId.orEmpty()}"),
      animatedVisibilityScope = transitionScope.animatedVisibilityScope,
      boundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = animation.durationMillis, easing = animation.easing)
      },
      resizeMode = if (content == MatchSharedContent.Card) {
        SharedTransitionScope.ResizeMode.RemeasureToBounds
      } else {
        SharedTransitionScope.ResizeMode.scaleToBounds()
      },
      zIndexInOverlay = if (content == MatchSharedContent.Card) 0f else 1f,
    )
  }
}
