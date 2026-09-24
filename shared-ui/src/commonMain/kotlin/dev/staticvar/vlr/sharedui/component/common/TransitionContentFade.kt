/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.staticvar.designsystem.prism.Prism

/** Identifies the single card whose metadata participates in a detail transition. */
@Immutable
public sealed interface TransitionItem {
  @Immutable
  public data class Match(public val id: String) : TransitionItem
  @Immutable
  public data class Event(public val id: String) : TransitionItem
}

/** Alpha and readiness for content that must stay out of a shared navigation transition. */
@Stable
@ConsistentCopyVisibility
public data class TransitionContentFade internal constructor(
  public val alpha: State<Float>,
  public val isSettled: Boolean,
  public val isVisible: Boolean,
  internal val acceptsInputState: State<Boolean>,
) {
  /** Whether the content is fully visible and its actions may receive input. */
  public val acceptsInput: Boolean
    get() = isVisible && acceptsInputState.value
}

private object FullyVisibleAlpha : State<Float> {
  override val value: Float = 1f
}

private object AcceptingInput : State<Boolean> {
  override val value: Boolean = true
}

private val UnscopedTransitionContentFade = TransitionContentFade(
  alpha = FullyVisibleAlpha,
  isSettled = true,
  isVisible = true,
  acceptsInputState = AcceptingInput,
)

private val LocalTransitionItem = compositionLocalOf<TransitionItem?> { null }

private val LocalTransitionContentFade = compositionLocalOf<TransitionContentFade?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProvideTransitionContentScope(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  item: TransitionItem?,
  content: @Composable () -> Unit,
) {
  val navTransition = animatedVisibilityScope.transition
  val settled = transitionContentSettled(
    hasScope = item != null,
    navCurrentVisible = navTransition.currentState == EnterExitState.Visible,
    navTargetVisible = navTransition.targetState == EnterExitState.Visible,
    navRunning = navTransition.isRunning,
    sharedRunning = sharedTransitionScope.isTransitionActive,
  )
  val fade = rememberTransitionContentFadeState(visible = settled, isSettled = settled)
  CompositionLocalProvider(
    LocalTransitionItem provides item,
    LocalTransitionContentFade provides item?.let { fade },
    content = content,
  )
}

@Composable
internal fun ProvideUnscopedTransitionContent(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalTransitionItem provides null, LocalTransitionContentFade provides null, content = content)
}

/** Returns the scope-level fade shared by preview and header extras. */
@Composable
public fun currentTransitionContentFade(item: TransitionItem? = null): TransitionContentFade =
  selectTransitionContentFade(LocalTransitionContentFade.current, LocalTransitionItem.current, item)

internal fun selectTransitionContentFade(
  fade: TransitionContentFade?,
  activeItem: TransitionItem?,
  requestedItem: TransitionItem?,
): TransitionContentFade =
  if (requestedItem == null || requestedItem == activeItem) fade ?: UnscopedTransitionContentFade
  else UnscopedTransitionContentFade

/** Fades ready detail content after its navigation and shared transitions have settled. */
@Composable
public fun rememberTransitionContentFade(ready: Boolean): TransitionContentFade {
  val scopedFade = LocalTransitionContentFade.current
  val settled = scopedFade?.isSettled ?: true
  return rememberTransitionContentFadeState(visible = ready && settled, isSettled = settled)
}

@Composable
private fun rememberTransitionContentFadeState(visible: Boolean, isSettled: Boolean): TransitionContentFade {
  val animation = Prism.anim.standard
  val alpha = remember { Animatable(if (visible) 1f else 0f) }
  LaunchedEffect(visible, alpha) {
    alpha.animateTo(
      targetValue = if (visible) 1f else 0f,
      animationSpec = tween(durationMillis = animation.durationMillis, easing = animation.easing),
    )
  }
  val acceptsInput = remember(alpha) { derivedStateOf { alpha.value == 1f } }
  return remember(alpha, acceptsInput, isSettled, visible) {
    TransitionContentFade(
      alpha = alpha.asState(),
      isSettled = isSettled,
      isVisible = visible,
      acceptsInputState = acceptsInput,
    )
  }
}

/** Applies [fade] while hiding semantics and consuming input until the content is fully visible. */
public fun Modifier.transitionContentFade(fade: TransitionContentFade): Modifier {
  return this
    .graphicsLayer { alpha = fade.alpha.value }
    .transitionContentInput(fade.acceptsInput)
}

internal fun Modifier.transitionContentInput(acceptsInput: Boolean): Modifier =
  this
    .then(if (acceptsInput) Modifier else Modifier.clearAndSetSemantics {})
    .then(
      if (acceptsInput) {
        Modifier
      } else {
        Modifier.pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
          }
        }
      },
    )

internal fun transitionContentSettled(
  hasScope: Boolean,
  navCurrentVisible: Boolean,
  navTargetVisible: Boolean,
  navRunning: Boolean,
  sharedRunning: Boolean,
): Boolean = !hasScope || (
  navCurrentVisible &&
    navTargetVisible &&
    !navRunning &&
    !sharedRunning
  )
