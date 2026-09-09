/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

public val PrismCardMascotWidth: Dp = 20.dp
public val PrismCardMascotHeight: Dp = 16.dp

public val LocalPrismCardMascot: ProvidableCompositionLocal<(@Composable (Modifier) -> Unit)?> =
  compositionLocalOf { null }

private data class CardMascotEligibility(val topClearance: Dp) : Modifier.Element

/** Declares existing space above the card. This modifier does not add padding or change its size. */
public fun Modifier.cardMascotEligible(topClearance: Dp): Modifier {
  require(topClearance >= 0.dp) { "Top clearance cannot be negative" }
  return then(CardMascotEligibility(topClearance))
}

/** Keeps decorations outside the clipped card surface, without contributing to its measured size. */
@Composable
public fun PrismCardHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val topClearance = modifier.foldIn(0.dp) { value, element ->
    if (element is CardMascotEligibility) element.topClearance else value
  }
  val mascot = LocalPrismCardMascot.current
  Box(modifier = modifier, propagateMinConstraints = true) {
    content()
    if (mascot != null && topClearance >= PrismCardMascotHeight) {
      Box(Modifier.matchParentSize()) {
        mascot(
          Modifier.align(Alignment.TopEnd)
            .offset(x = -Prism.dimens.spacingM, y = -PrismCardMascotHeight)
            .size(PrismCardMascotWidth, PrismCardMascotHeight),
        )
      }
    }
  }
}
