package dev.staticvar.vlr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.ui.LocalColorExtractor
import dev.staticvar.vlr.ui.PaddingLocalCompositions
import dev.staticvar.vlr.ui.VLR
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.ColorExtractor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val colorExtractor by lazy { ColorExtractor(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      VLRTheme() {
        CompositionLocalProvider(LocalColorExtractor provides colorExtractor) {
          PaddingLocalCompositions { VLR() }
        }
      }
    }
  }
}
