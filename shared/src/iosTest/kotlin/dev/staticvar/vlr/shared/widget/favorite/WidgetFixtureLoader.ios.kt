/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import platform.Foundation.NSProcessInfo

internal actual fun readWidgetFixture(name: String): String {
  val directory = requireNotNull(NSProcessInfo.processInfo.environment["VLR_WIDGET_FIXTURES"] as? String) {
    "VLR_WIDGET_FIXTURES must point to the widget fixture directory; run iOS tests through Gradle."
  }
  val source = SystemFileSystem.source(Path(directory, name)).buffered()
  return try {
    source.readString()
  } finally {
    source.close()
  }
}
