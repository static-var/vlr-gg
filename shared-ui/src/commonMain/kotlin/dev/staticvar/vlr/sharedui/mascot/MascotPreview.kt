/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

/** Both characters at celebration, About, and card sizes, plus the banner paws. */
@PrismPreview
@Composable
internal fun MascotPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      Modifier.background(Prism.color.background).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        LynxMascot(Modifier.size(160.dp), animated = false)
        RosieMascot(Modifier.size(160.dp), animated = false)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        LynxMascot(Modifier.size(88.dp), animated = false)
        RosieMascot(Modifier.size(88.dp), animated = false)
        MascotPaws(LynxArt)
        MascotPaws(RosieArt)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        CardCrop(MascotCharacter.Lynx, 60.dp)
        CardCrop(MascotCharacter.Rosie, 60.dp)
        CardCrop(MascotCharacter.Lynx, 20.dp)
        CardCrop(MascotCharacter.Rosie, 20.dp)
      }
    }
  }
}

/** The card viewport at [width], over a strip of card surface like the real card edge. */
@Composable
private fun CardCrop(character: MascotCharacter, width: Dp) {
  Column {
    Box(Modifier.size(width, width * 16f / 20f).clipToBounds()) {
      val artwork = Modifier.mascotPeekArtwork(width)
      when (character) {
        MascotCharacter.Lynx -> LynxMascot(artwork, animated = false)
        MascotCharacter.Rosie -> RosieMascot(artwork, animated = false)
      }
    }
    Box(Modifier.size(width * 3, 24.dp).background(Prism.color.surface))
  }
}
