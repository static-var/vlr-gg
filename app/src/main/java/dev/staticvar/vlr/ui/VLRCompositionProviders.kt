package dev.staticvar.vlr.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.utils.ColorExtractor

val Local16DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local16DP_8DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local8DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local8DP_4DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local4DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local4DP_2DPPadding = staticCompositionLocalOf { PaddingValues() }
val Local2DPPadding = staticCompositionLocalOf { PaddingValues() }
val LocalColorExtractor = staticCompositionLocalOf<ColorExtractor> { error("No color extractor provided") }
val LocalNavigationSuiteType = compositionLocalOf { NavigationSuiteType.None }

@Composable
fun PaddingLocalCompositions(content: @Composable () -> Unit) {
  CompositionLocalProvider(
    Local16DPPadding provides PaddingValues(16.dp),
    Local16DP_8DPPadding provides PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    Local8DPPadding provides PaddingValues(8.dp),
    Local8DP_4DPPadding provides PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    Local4DPPadding provides PaddingValues(4.dp),
    Local4DP_2DPPadding provides PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    Local2DPPadding provides PaddingValues(horizontal = 2.dp)
  ) { content() }
}
