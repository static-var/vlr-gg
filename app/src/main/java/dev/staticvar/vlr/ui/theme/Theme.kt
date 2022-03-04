package dev.staticvar.vlr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import dev.staticvar.vlr.ui.COLOR_ALPHA

private val DarkColorScheme =
    darkColorScheme(primary = Purple200, secondary = Purple500, tertiary = Teal200)

private val LightColorScheme =
    lightColorScheme(primary = Purple700, secondary = Purple500, tertiary = Teal200

        /* Other default colors to override
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
        */
        )

@Composable
fun VLRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

val VLRTheme
  @Composable get() = MaterialTheme

val MaterialTheme.primaryBackground
  @Composable get() = VLRTheme.colorScheme.primary.copy(COLOR_ALPHA)

val MaterialTheme.secondaryBackground
  @Composable get() = VLRTheme.colorScheme.secondary.copy(COLOR_ALPHA)
