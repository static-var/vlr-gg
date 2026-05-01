package dev.staticvar.designsystem.theme.dark

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme

@Composable
public fun DarkTheme(content: @Composable () -> Unit) {
  ProvidePrismTheme(definition = DarkThemeDefinition, content = content)
}
