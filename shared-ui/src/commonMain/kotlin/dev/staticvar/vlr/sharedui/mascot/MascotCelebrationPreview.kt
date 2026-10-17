/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

private val previewMessages = listOf(
  "Natus Vincere hat das Event gewonnen!" to null,
  "Победитель турнира — Natus Vincere!" to null,
  "Natus Vincere ने इवेंट जीत लिया!" to null,
  "Gen.G Global Esports 팀이 이벤트에서 우승했습니다!" to null,
  "Paper Rex hat gewonnen und" to "Something hat abgeliefert!!!",
)

@PrismPreview
@Composable
internal fun MascotCelebrationPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(Modifier.width(393.dp).background(Prism.color.background)) {
      previewMessages.forEachIndexed { index, (message, secondaryMessage) ->
        CelebrationScene(
          character = if (index % 2 == 0) MascotCharacter.Lynx else MascotCharacter.Rosie,
          message = message,
          secondaryMessage = secondaryMessage,
          animated = false,
          entrance = { 1f },
          bannerProgress = { 1f },
        )
      }
    }
  }
}

/** The narrowest supported width at the largest font scale: every message must fit the banner unclipped. */
@Preview(name = "Narrow, large type", widthDp = 320, heightDp = 1400, fontScale = 2f, showBackground = true)
@Composable
internal fun MascotCelebrationNarrowPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(Modifier.width(320.dp).background(Prism.color.background)) {
      CelebrationScene(
        character = MascotCharacter.Lynx,
        message = "Natus Vincere hat das Event gewonnen!",
        secondaryMessage = null,
        animated = false,
        entrance = { 1f },
        bannerProgress = { 1f },
      )
      CelebrationScene(
        character = MascotCharacter.Rosie,
        message = "Paper Rex hat gewonnen und",
        secondaryMessage = "Something hat abgeliefert!!!",
        animated = false,
        entrance = { 1f },
        bannerProgress = { 1f },
      )
    }
  }
}
