/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.prism.Prism
import kotlinx.coroutines.delay

/** Place over content in a viewport-clipped Box; [animated] controls the character's idle motion. */
@Composable
public fun MascotCelebration(
  visible: Boolean,
  character: MascotCharacter,
  message: String,
  onFinished: () -> Unit,
  modifier: Modifier = Modifier,
  animated: Boolean = true,
  secondaryMessage: String? = null,
) {
  PauseCardMascots(visible)
  val entrance = remember { Animatable(0f) }
  val banner = remember { Animatable(0f) }
  val currentOnFinished by rememberUpdatedState(onFinished)
  LaunchedEffect(visible) {
    entrance.snapTo(0f)
    banner.snapTo(0f)
    if (!visible) return@LaunchedEffect
    entrance.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    delay(250)
    banner.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    delay(2_400)
    banner.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
    delay(150)
    entrance.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
    currentOnFinished()
  }
  if (visible) {
    CelebrationScene(
      character = character,
      message = message,
      secondaryMessage = secondaryMessage,
      animated = animated,
      entrance = { entrance.value },
      bannerProgress = { banner.value },
      modifier = modifier,
    )
  }
}

/** [entrance] and [bannerProgress] are read in layer blocks only, so animation frames never recompose the scene. */
@Composable
internal fun CelebrationScene(
  character: MascotCharacter,
  message: String,
  secondaryMessage: String?,
  animated: Boolean,
  entrance: () -> Float,
  bannerProgress: () -> Float,
  modifier: Modifier = Modifier,
) {
  val colors = Prism.color
  Box(modifier.fillMaxWidth().height(160.dp).zIndex(1f)) {
    CharacterMascot(
      Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-20).dp).size(160.dp)
        .graphicsLayer {
          rotationZ = -45f
          translationX = (1f - entrance()) * 160.dp.toPx()
        },
      animated = animated,
      character = character,
    )
    Column(
      Modifier.align(Alignment.BottomEnd).padding(start = 16.dp, end = 30.dp, bottom = 8.dp),
      horizontalAlignment = Alignment.End,
    ) {
      BannerPaws(character, Modifier.zIndex(1f).graphicsLayer { alpha = bannerProgress() })
      Box(
        Modifier
          .graphicsLayer {
            val progress = bannerProgress()
            transformOrigin = TransformOrigin(1f, 0f)
            scaleX = progress
            alpha = progress
            rotationZ = -3f * (1f - progress)
          }
          .background(colors.accentSubtle, Prism.shapes.small)
          .border(2.dp, colors.accent, Prism.shapes.small),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            message,
            style = Prism.typography.sectionTitle,
            color = colors.contentPrimary,
            textAlign = TextAlign.Center,
            softWrap = true,
          )
          if (secondaryMessage != null) {
            Text(
              secondaryMessage,
              style = Prism.typography.sectionTitle,
              color = colors.contentPrimary,
              textAlign = TextAlign.Center,
              softWrap = true,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BannerPaws(character: MascotCharacter, modifier: Modifier) {
  val art = character.art
  MascotPaws(art, modifier.offset(y = (art.pawsBounds.height / 2f).dp))
}

public enum class MascotCharacter {
  Lynx,
  Rosie,
}

@Composable
private fun CharacterMascot(modifier: Modifier, animated: Boolean, character: MascotCharacter) {
  when (character) {
    MascotCharacter.Lynx -> LynxMascot(modifier, animated = animated)
    MascotCharacter.Rosie -> RosieMascot(modifier, animated = animated)
  }
}
