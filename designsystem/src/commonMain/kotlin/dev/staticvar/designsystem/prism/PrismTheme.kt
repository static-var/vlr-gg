package dev.staticvar.designsystem.prism

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.theme.dark.DarkTheme
import dev.staticvar.designsystem.theme.light.LightTheme

/**
 * Main Prism theme router.
 *
 * Routes to the appropriate theme implementation based on the variant.
 *
 * Example usage:
 * ```
 * PrismTheme(variant = PrismVariant.Light) {
 *   Text("Hello", color = MaterialTheme.prismColors.titleColor)
 * }
 * ```
 *
 * @param variant The theme variant to use. Defaults to [PrismVariant.Light].
 * @param content The composable content to theme.
 */
@Composable
public fun PrismTheme(
  variant: PrismVariant = PrismVariant.Light,
  content: @Composable () -> Unit,
) {
  when (variant) {
    PrismVariant.Light -> LightTheme(content)
    PrismVariant.Dark -> DarkTheme(content)
  }
}
