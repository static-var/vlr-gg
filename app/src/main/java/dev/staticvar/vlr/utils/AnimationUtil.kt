package dev.staticvar.vlr.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.staticvar.vlr.data.NavState

val tween: FiniteAnimationSpec<IntOffset> = tween(400)
private val tweenF: FiniteAnimationSpec<Float> = tween(400)

val slideInFromLeft = slideInHorizontally(animationSpec = tween, initialOffsetX = { -it })

val slideInFromRight = slideInHorizontally(animationSpec = tween, initialOffsetX = { it })

val slideInFromBottom = slideInVertically(animationSpec = tween, initialOffsetY = { it })

val slideInFromTop = slideInVertically(animationSpec = tween, initialOffsetY = { -it })

val fadeIn = fadeIn(animationSpec = tweenF)
val fadeOut = fadeOut(animationSpec = tweenF)

fun AnimatedContentScope<NavState>.navAnimation(navState: NavState) =
  fadeIn(animationSpec = tween(200, 200)) with
    fadeOut(animationSpec = tween(200)) using
    SizeTransform { initialSize, targetSize ->
      if (
        navState != NavState.MATCH_DETAILS &&
          navState != NavState.TOURNAMENT_DETAILS &&
          navState != NavState.TEAM_DETAILS &&
          navState != NavState.NEWS
      ) {
        keyframes {
          // Expand horizontally first.
          IntSize(targetSize.width, initialSize.height) at 150
          durationMillis = 300
        }
      } else {
        keyframes {
          // Shrink vertically first.
          IntSize(initialSize.width, targetSize.height) at 150
          durationMillis = 300
        }
      }
    }
