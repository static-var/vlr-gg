package dev.staticvar.designsystem.theme.frappe

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme

/**
 * Frappé theme composable.
 *
 * Provides the complete Catppuccin Frappé theme including:
 * - Material3 ColorScheme (dark theme)
 * - Typography with harmonized color usage
 * - Shape definitions
 * - Extended Prism color palette accessible via MaterialTheme.prismColors
 *
 * Example usage:
 * ```
 * FrappeTheme {
 *   Text("Hello", color = MaterialTheme.prismColors.titleColor)
 * }
 * ```
 */
@Composable
public fun FrappeTheme(content: @Composable () -> Unit) {
  ProvidePrismTheme(definition = FrappeThemeDefinition, content = content)
}
