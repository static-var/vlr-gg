/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package dev.staticvar.vlr.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.staticvar.vlr.shared.di.initializeAppKoin
import dev.staticvar.vlr.shared.network.iosNetworkModule
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import platform.posix.fflush
import platform.posix.fputs
import platform.posix.stderr
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.processUnhandledException
import kotlin.native.setUnhandledExceptionHook

private const val GeneratedPlaceholderToken: String = "replace-with-your-token"
private const val UnknownThrowableDescription: String = "<unable to render throwable>"

private var hasInstalledUnhandledExceptionHook: Boolean = false

/**
 * Creates the main UIViewController for iOS that hosts the Compose UI.
 */
fun MainViewController(authToken: String? = null): UIViewController {
  ensureUnhandledExceptionLoggingInstalled()

  return try {
    initializeAppKoin(
      appDeclaration = { modules(iosNetworkModule) },
      authToken = resolveAuthToken(authToken),
    )
    ComposeUIViewController { App() }
  } catch (throwable: Throwable) {
    logThrowable("Synchronous Kotlin startup failure", throwable)
    processUnhandledException(throwable)
    error("processUnhandledException should terminate execution")
  }
}

private fun ensureUnhandledExceptionLoggingInstalled() {
  if (hasInstalledUnhandledExceptionHook) {
    return
  }

  setUnhandledExceptionHook { throwable ->
    logThrowable("Unhandled Kotlin exception", throwable)
  }
  hasInstalledUnhandledExceptionHook = true
}

private fun logThrowable(prefix: String, throwable: Throwable) {
  logDiagnosticLine("$prefix: ${safeThrowableDescription(throwable)}")
  logDiagnosticLine(safeThrowableStackTrace(throwable))
}

private fun safeThrowableDescription(throwable: Throwable): String = try {
  throwable.toString()
} catch (_: Throwable) {
  UnknownThrowableDescription
}

private fun safeThrowableStackTrace(throwable: Throwable): String = try {
  throwable.stackTraceToString()
} catch (_: Throwable) {
  UnknownThrowableDescription
}

private fun logDiagnosticLine(message: String) {
  fputs(message, stderr)
  fputs("\n", stderr)
  fflush(stderr)
}

private fun resolveAuthToken(providedAuthToken: String?): String? = normalizeAuthToken(providedAuthToken)
  ?: normalizeAuthToken(NSProcessInfo.processInfo.environment["VLR_AUTH_TOKEN"] as? String)
  ?: normalizeAuthToken(NSBundle.mainBundle.objectForInfoDictionaryKey("VLR_AUTH_TOKEN") as? String)

private fun normalizeAuthToken(rawToken: String?): String? = rawToken
  ?.trim()
  ?.removeSurrounding("\"")
  ?.removeSurrounding("'")
  ?.takeIf { token -> token.isNotBlank() && token != GeneratedPlaceholderToken }
