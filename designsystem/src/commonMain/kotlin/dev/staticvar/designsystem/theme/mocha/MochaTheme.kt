package dev.staticvar.designsystem.theme.mocha

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme

/**
 * Mocha theme composable.
 *
 * Provides the complete Catppuccin Mocha theme including:
 * - Material3 ColorScheme (dark theme)
 * - Typography with harmonized color usage
 * - Shape definitions
 * - Extended Prism color palette accessible via MaterialTheme.prismColors
 *
 * Example usage:
 * ```
 * MochaTheme {
 *   Text("Hello", color = MaterialTheme.prismColors.titleColor)
 * }
 * ```
 */
@Composable
public fun MochaTheme(content: @Composable () -> Unit) {
  ProvidePrismTheme(definition = MochaThemeDefinition, content = content)
}
