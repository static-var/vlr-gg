@file:Suppress("MatchingDeclarationName")

package dev.staticvar.designsystem.prism

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.staticvar.designsystem.prism.animation.DefaultPrismAnimations
import dev.staticvar.designsystem.prism.animation.PrismAnimationTokens
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.color.prismColors
import dev.staticvar.designsystem.prism.dimens.PrismDimens
import dev.staticvar.designsystem.prism.dimens.prismDimens
import dev.staticvar.designsystem.prism.typography.PrismTypography
import dev.staticvar.designsystem.prism.typography.prismTypography

/**
 * Public facade exposing Prism tokens through a concise `Prism.*` namespace.
 *
 * Usage:
 * ```
 * Text(
 *   text = "Headline",
 *   style = Prism.typography.headline,
 *   color = Prism.color.titleColor,
 * )
 * Box(
 *   modifier = Modifier.padding(Prism.dimens.spacingL)
 * )
 * ```
 */
public object Prism {
  public val typography: PrismTypography
    @Composable @ReadOnlyComposable get() = MaterialTheme.prismTypography

  public val color: PrismColorPalette
    @Composable @ReadOnlyComposable get() = MaterialTheme.prismColors

  public val dimens: PrismDimens
    @Composable @ReadOnlyComposable get() = MaterialTheme.prismDimens

  public val shapes: Shapes
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes

  public val anim: PrismAnimationTokens
    get() = DefaultPrismAnimations
}
