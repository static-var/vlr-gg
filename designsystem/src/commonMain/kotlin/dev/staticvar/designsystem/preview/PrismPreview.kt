package dev.staticvar.designsystem.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-theme preview annotation for Prism design system.
 *
 * Automatically generates previews for all theme variants.
 * Use with [PrismPreviewProvider] parameter.
 *
 * Example usage:
 * ```
 * @PrismPreview
 * @Composable
 * fun MyComponentPreview(
 *   @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant
 * ) {
 *   PrismTheme(variant = variant) {
 *     MyComponent()
 *   }
 * }
 * ```
 *
 * Note: Preview names will show as "variant 0", "variant 1", etc.
 * This is a limitation of Compose Preview - it cannot dynamically
 * name previews based on parameter values.
 */
@Preview(name = "Prism Themes", group = "Prism", showBackground = true)
public annotation class PrismPreview
