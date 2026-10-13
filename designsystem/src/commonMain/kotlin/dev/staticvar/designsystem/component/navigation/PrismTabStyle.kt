/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/**
 * Visual treatment for [PrismTabs].
 *
 * A style owns the chip shape, frame, label type, and state colors. Built-in styles read from
 * [Prism] tokens so they adapt to the active theme family.
 */
@Immutable
public sealed interface PrismTabStyle {
  /** Theme frame. A selected chip sits pressed into its shadow when the frame has one. */
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens

  @get:Composable
  @get:ReadOnlyComposable
  public val shape: Shape

  @get:Composable
  @get:ReadOnlyComposable
  public val labelStyle: TextStyle

  @get:Composable
  @get:ReadOnlyComposable
  public val uppercase: Boolean

  /** Face color. Keep it opaque so a frame shadow never shows through. */
  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun border(selected: Boolean, enabled: Boolean): BorderStroke

  @get:Composable
  @get:ReadOnlyComposable
  public val countStyle: TextStyle

  @get:Composable
  @get:ReadOnlyComposable
  public val countShape: Shape

  /** Background behind the count. Transparent draws a bare number. */
  @Composable
  @ReadOnlyComposable
  public fun countContainerColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun countContentColor(selected: Boolean, enabled: Boolean): Color

  /** Padding inside the count background. */
  @get:Composable
  @get:ReadOnlyComposable
  public val countPadding: PaddingValues

  /**
   * Chips with an optional count. Each theme family draws them in its own idiom: square
   * monochrome chips with an accent count, raised console tags with an inverted count block, or
   * accent pills with a count badge.
   */
  public data object Chip : PrismTabStyle {
    private val tokens: PrismTabTokens
      @Composable
      @ReadOnlyComposable
      get() = LocalPrismTabTokens.current

    override val frame: PrismFrameTokens
      @Composable
      @ReadOnlyComposable
      get() = Prism.frames.compact

    override val shape: Shape
      @Composable
      @ReadOnlyComposable
      get() = if (tokens.pill) RoundedCornerShape(percent = 50) else Prism.shapes.small

    override val labelStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = if (tokens.displayLabel) {
        Prism.typography.labelAlt.copy(fontSize = 13.sp, letterSpacing = 1.sp)
      } else {
        Prism.typography.button
      }

    override val uppercase: Boolean
      @Composable
      @ReadOnlyComposable
      get() = tokens.displayLabel

    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      !selected -> Prism.color.background
      tokens.selectedFill == PrismTabFill.Accent -> Prism.color.accent
      else -> Prism.color.inverseSurface
    }

    @Composable
    @ReadOnlyComposable
    override fun contentColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.captionColor
      !selected -> Prism.color.labelColor
      tokens.selectedFill == PrismTabFill.Accent -> Prism.color.onAccent
      else -> Prism.color.onInverseSurface
    }

    @Composable
    @ReadOnlyComposable
    override fun border(selected: Boolean, enabled: Boolean): BorderStroke {
      val width = frame.border?.width ?: Prism.dimens.strokeDefault
      val color = when {
        !enabled -> Prism.color.strokeVariant
        !selected -> Prism.color.stroke
        else -> containerColor(selected = true, enabled = true)
      }
      return BorderStroke(width = width, color = color)
    }

    override val countStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = when (tokens.count) {
        PrismTabCount.Plain -> Prism.typography.labelAlt.copy(fontSize = 12.sp)
        PrismTabCount.Badge -> Prism.typography.caption
        PrismTabCount.Block -> Prism.typography.button.let { it.copy(fontSize = it.fontSize * 0.82f) }
      }

    override val countShape: Shape
      @Composable
      @ReadOnlyComposable
      get() = if (tokens.count == PrismTabCount.Badge) RoundedCornerShape(percent = 50) else Prism.shapes.extraSmall

    @Composable
    @ReadOnlyComposable
    override fun countContainerColor(selected: Boolean): Color = when (tokens.count) {
      PrismTabCount.Plain -> Color.Transparent
      PrismTabCount.Badge -> if (selected) contentColor(selected = true, enabled = true) else Prism.color.surfaceVariant
      PrismTabCount.Block -> if (selected) Prism.color.onInverseSurface else Prism.color.titleColor
    }

    @Composable
    @ReadOnlyComposable
    override fun countContentColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.captionColor

      tokens.count == PrismTabCount.Plain -> if (selected) Prism.color.inverseAccent else Prism.color.labelColor

      tokens.count == PrismTabCount.Badge -> if (selected) {
        containerColor(
          selected = true,
          enabled = true,
        )
      } else {
        Prism.color.labelColor
      }

      else -> if (selected) Prism.color.inverseSurface else Prism.color.background
    }

    override val countPadding: PaddingValues
      @Composable
      @ReadOnlyComposable
      get() = when (tokens.count) {
        PrismTabCount.Plain -> PaddingValues()
        PrismTabCount.Badge -> PaddingValues(horizontal = 7.dp, vertical = 2.dp)
        PrismTabCount.Block -> PaddingValues(horizontal = Prism.dimens.spacingXs, vertical = 1.dp)
      }
  }

  public companion object {
    public val Default: PrismTabStyle = Chip
  }
}
