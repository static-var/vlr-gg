/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.staticvar.vlr.shared.App
import dev.staticvar.vlr.shared.di.initializeAppKoin
import org.koin.android.ext.koin.androidContext

/**
 * Main activity for VLR Android app.
 * Uses shared Compose UI from the shared module.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val authToken =
      BuildConfig.TOKEN
        .trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .takeIf { token -> token.isNotBlank() }
    initializeAppKoin(
      appDeclaration = { androidContext(this@MainActivity.applicationContext) },
      authToken = authToken,
    )
    enableEdgeToEdge()

    setContent {
      App()
    }
  }
}
