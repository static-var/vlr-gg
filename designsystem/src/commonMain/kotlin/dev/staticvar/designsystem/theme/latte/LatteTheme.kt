package dev.staticvar.designsystem.theme.latte

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme

/**
 * Latte theme composable.
 *
 * Provides the complete Catppuccin Latte theme including:
 * - Material3 ColorScheme (light theme)
 * - Typography with harmonized color usage
 * - Shape definitions
 * - Extended Prism color palette accessible via MaterialTheme.prismColors
 *
 * Example usage:
 * ```
 * LatteTheme {
 *   Text("Hello", color = MaterialTheme.prismColors.titleColor)
 * }
 * ```
 */
@Composable
public fun LatteTheme(content: @Composable () -> Unit) {
  ProvidePrismTheme(definition = LatteThemeDefinition, content = content)
}
