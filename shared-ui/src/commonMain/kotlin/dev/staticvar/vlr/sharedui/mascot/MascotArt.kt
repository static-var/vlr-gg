/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.color.PrismColorPalette

/** Layered artwork in a 160 by 160 unit space; [MascotFigure] animates the layers. */
internal interface MascotArt {
  val tailPivot: Offset
  val eyesPivot: Offset
  val pawsBounds: Rect

  fun DrawScope.drawShadow(palette: MascotPalette)

  fun DrawScope.drawTail(palette: MascotPalette)

  fun DrawScope.drawBody(palette: MascotPalette)

  /** The front paws alone, also drawn by [drawBody]; the celebration banner reuses them. */
  fun DrawScope.drawPaws(palette: MascotPalette)

  fun DrawScope.drawHead(palette: MascotPalette)

  fun DrawScope.drawEyes(palette: MascotPalette)

  fun DrawScope.drawFace(palette: MascotPalette)
}

internal val MascotCharacter.art: MascotArt
  get() = when (this) {
    MascotCharacter.Lynx -> LynxArt
    MascotCharacter.Rosie -> RosieArt
  }

/** The head region every mascot fills; cards show exactly this window of the artwork. */
internal val MascotPeekWindow: Rect = Rect(28f, 12f, 132f, 95f)

/** Positions 160 unit artwork so that [MascotPeekWindow] fills a viewport [width] wide. */
internal fun Modifier.mascotPeekArtwork(width: Dp): Modifier {
  val unit = width / MascotPeekWindow.width
  return offset(x = unit * -MascotPeekWindow.left, y = unit * -MascotPeekWindow.top)
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(unit * 160f)
}

internal data class MascotPalette(
  val fur: Color,
  val furLight: Color,
  val furDark: Color,
  val outline: Color,
  val ink: Color,
  val white: Color,
  val pink: Color,
  val shadow: Color,
)

internal fun mascotPalette(colors: PrismColorPalette): MascotPalette {
  val fur = lerp(colors.accent, colors.surface, 0.28f)
  val darkTheme = colors.surface.luminance() < 0.5f
  return MascotPalette(
    fur = fur,
    furLight = lerp(fur, Color.White, if (darkTheme) 0.45f else 0.62f),
    furDark = lerp(fur, Color.Black, 0.3f),
    outline = lerp(colors.accent, colors.contentPrimary, 0.55f),
    ink = Color(0xFF2A2233),
    white = Color.White,
    pink = Color(0xFFF2A7B8),
    shadow = colors.contentPrimary.copy(alpha = 0.08f),
  )
}
