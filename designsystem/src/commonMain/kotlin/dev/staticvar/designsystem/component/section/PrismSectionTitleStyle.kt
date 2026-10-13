/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.section

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

private object PrismSectionTitleStyleConstants {
  const val TabTitleScale = 0.8f
  val TabLetterSpacing = 1.sp
  val TabPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 5.dp)

  // TabPadding minus the 4 dp gap between trailing content and the rule, so the action's top
  // lines up with the tab's top.
  val TabActionPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 3.dp)
  val SubtleActionPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
}

/**
 * Visual treatment for [PrismSectionTitle].
 *
 * A style owns the title tab, the prefix and title colors, the rule under the header, and the
 * defaults provided to trailing content. Built-in styles read from [Prism] tokens, so the same
 * style adapts to each theme family: corner shape comes from [Prism.shapes], title type from
 * [Prism.typography], and the tab shadow from [Prism.frames].
 */
@Immutable
public sealed interface PrismSectionTitleStyle {
  /** Text style shared by the prefix and title. */
  @get:Composable
  @get:ReadOnlyComposable
  public val titleTextStyle: TextStyle

  /** Color of the title text. */
  @get:Composable
  @get:ReadOnlyComposable
  public val titleColor: Color

  /** Color of the optional prefix shown before the title. */
  @get:Composable
  @get:ReadOnlyComposable
  public val prefixColor: Color

  /** Whether the prefix and title render in uppercase. */
  public val uppercase: Boolean

  /** Container color behind the prefix and title. Transparent styles draw no tab. */
  @get:Composable
  @get:ReadOnlyComposable
  public val tabColor: Color

  /** Shape of the tab behind the prefix and title. */
  @get:Composable
  @get:ReadOnlyComposable
  public val tabShape: Shape

  /** Theme frame applied to the tab. */
  @get:Composable
  @get:ReadOnlyComposable
  public val tabFrame: PrismFrameTokens

  /** Padding between the tab edge and the prefix and title. */
  @get:Composable
  @get:ReadOnlyComposable
  public val tabPadding: PaddingValues

  /** Padding inside a [PrismSectionTitleAction] placed in the trailing content. */
  @get:Composable
  @get:ReadOnlyComposable
  public val actionPadding: PaddingValues

  /** Thickness of the rule under the header. Zero draws no rule. */
  @get:Composable
  @get:ReadOnlyComposable
  public val ruleThickness: Dp

  /** Color of the rule under the header. */
  @get:Composable
  @get:ReadOnlyComposable
  public val ruleColor: Color

  /** Default text style provided to trailing content. */
  @get:Composable
  @get:ReadOnlyComposable
  public val trailingTextStyle: TextStyle

  /** Default content color provided to trailing content. */
  @get:Composable
  @get:ReadOnlyComposable
  public val trailingContentColor: Color

  /**
   * Inverted title tab sitting on a full-width rule.
   *
   * The tab and rule use [Prism.color.inverseSurface], and the prefix uses
   * [Prism.color.inverseAccent]. Square themes get a square tab, rounded themes get rounded top
   * corners, and themes with framed controls get the compact frame's shadow.
   */
  public data object Tab : PrismSectionTitleStyle {
    override val titleTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() {
        val base = Prism.typography.cardTitle
        return base.copy(
          fontSize = base.fontSize * PrismSectionTitleStyleConstants.TabTitleScale,
          letterSpacing = PrismSectionTitleStyleConstants.TabLetterSpacing,
        )
      }

    override val titleColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.onInverseSurface

    override val prefixColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.inverseAccent

    override val uppercase: Boolean = true

    override val tabColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.inverseSurface

    override val tabShape: Shape
      @Composable
      @ReadOnlyComposable
      get() = Prism.shapes.small.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

    override val tabFrame: PrismFrameTokens
      @Composable
      @ReadOnlyComposable
      get() = Prism.frames.compact.copy(border = null)

    override val tabPadding: PaddingValues
      @Composable
      @ReadOnlyComposable
      get() = PrismSectionTitleStyleConstants.TabPadding

    override val actionPadding: PaddingValues
      @Composable
      @ReadOnlyComposable
      get() = PrismSectionTitleStyleConstants.TabActionPadding

    override val ruleThickness: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.strokeThick

    override val ruleColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.inverseSurface

    override val trailingTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.label

    override val trailingContentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor
  }

  /** Untabbed subheading for groups nested under a [Tab] section. */
  public data object Subtle : PrismSectionTitleStyle {
    override val titleTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.cardTitle

    override val titleColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.titleColor

    override val prefixColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accent

    override val uppercase: Boolean = false

    override val tabColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Color.Transparent

    override val tabShape: Shape
      @Composable
      @ReadOnlyComposable
      get() = RectangleShape

    override val tabFrame: PrismFrameTokens
      @Composable
      @ReadOnlyComposable
      get() = PrismFrameTokens()

    override val tabPadding: PaddingValues
      @Composable
      @ReadOnlyComposable
      get() = PaddingValues()

    override val actionPadding: PaddingValues
      @Composable
      @ReadOnlyComposable
      get() = PrismSectionTitleStyleConstants.SubtleActionPadding

    override val ruleThickness: Dp
      @Composable
      @ReadOnlyComposable
      get() = 0.dp

    override val ruleColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Color.Transparent

    override val trailingTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.label

    override val trailingContentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor
  }
}
