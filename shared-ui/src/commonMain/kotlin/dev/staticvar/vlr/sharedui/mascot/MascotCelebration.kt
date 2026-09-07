/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.prism.Prism
import kotlinx.coroutines.delay

/** Place over content in a Box; [animated] controls the character's idle motion. */
@Composable
public fun MascotCelebration(
  visible: Boolean,
  character: MascotCharacter,
  message: String,
  onFinished: () -> Unit,
  modifier: Modifier = Modifier,
  animated: Boolean = true,
): Unit {
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
  val colors = Prism.color
  val fur = lerp(colors.accent, colors.surface, 0.28f)
  val outline = lerp(colors.accent, colors.contentPrimary, 0.55f)
  Box(modifier.fillMaxWidth().height(160.dp).zIndex(1f).clipToBounds()) {
    if (visible) {
      CharacterMascot(
        Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-20).dp).size(160.dp)
          .graphicsLayer {
            rotationZ = -45f
            translationX = (1f - entrance.value) * 160.dp.toPx()
          },
        animated = animated,
        character = character,
      )
      Box(
        Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-8).dp)
          .width(190.dp).height(54.dp)
          .graphicsLayer {
            transformOrigin = TransformOrigin(1f, 0f)
            scaleX = banner.value
            alpha = banner.value
            rotationZ = -3f * (1f - banner.value)
          }
          .background(colors.accentSubtle, Prism.shapes.small)
          .border(2.dp, colors.accent, Prism.shapes.small),
        contentAlignment = Alignment.Center,
      ) {
        Text(message, style = Prism.typography.sectionTitle, color = colors.contentPrimary)
      }
      if (character == MascotCharacter.Rosie) {
        RosiePaws(
          Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-49).dp).size(52.dp, 27.dp)
            .graphicsLayer { alpha = banner.value },
        )
      } else {
        Canvas(
          Modifier.align(Alignment.BottomEnd).offset(x = (-32).dp, y = (-53).dp).size(48.dp, 18.dp)
            .graphicsLayer { alpha = banner.value },
        ) {
          val pawWidth = size.width * 0.38f
          for (x in listOf(0f, size.width - pawWidth)) {
            drawOval(fur, Offset(x, 0f), Size(pawWidth, size.height))
            drawOval(outline, Offset(x, 0f), Size(pawWidth, size.height), style = Stroke(1.5.dp.toPx()))
            drawLine(
              outline,
              Offset(x + pawWidth * 0.4f, size.height * 0.55f),
              Offset(x + pawWidth * 0.4f, size.height * 0.85f),
              1.dp.toPx(),
            )
          }
        }
      }
    }
  }
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
