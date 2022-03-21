package dev.staticvar.vlr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

private val DarkColorScheme =
  darkColorScheme(primary = Purple200, secondary = Purple500, tertiary = Teal200)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple700,
    secondary = Purple500,
    tertiary = Teal200,
  )

@Composable
fun VLRTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

const val LIGHT_THEME_ALPHA = 0.2f
const val DARK_THEME_ALPHA = 0.05f

val VLRTheme
  @Composable get() = MaterialTheme

val ColorScheme.tintedBackground: Color
  @Composable
  get() =
    primaryContainer.copy(
      if (isSystemInDarkTheme()) DARK_THEME_ALPHA else LIGHT_THEME_ALPHA
    )
