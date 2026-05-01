package dev.staticvar.designsystem.theme.macchiato

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme

/**
 * Macchiato theme composable.
 *
 * Provides the complete Catppuccin Macchiato theme including:
 * - Material3 ColorScheme (dark theme)
 * - Typography with harmonized color usage
 * - Shape definitions
 * - Extended Prism color palette accessible via MaterialTheme.prismColors
 *
 * Example usage:
 * ```
 * MacchiatoTheme {
 *   Text("Hello", color = MaterialTheme.prismColors.titleColor)
 * }
 * ```
 */
@Composable
public fun MacchiatoTheme(content: @Composable () -> Unit) {
  ProvidePrismTheme(definition = MacchiatoThemeDefinition, content = content)
}
