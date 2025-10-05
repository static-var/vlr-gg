package dev.staticvar.vlr.shared

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Creates the main UIViewController for iOS that hosts the Compose UI.
 */
fun MainViewController() = ComposeUIViewController { App() }
