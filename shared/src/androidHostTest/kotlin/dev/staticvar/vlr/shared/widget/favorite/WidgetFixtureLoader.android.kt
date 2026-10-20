/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import java.io.File

internal actual fun readWidgetFixture(name: String): String {
  val directory = System.getProperty("VLR_WIDGET_FIXTURES")
  if (directory != null) return File(directory, name).readText()
  val loader = requireNotNull(Thread.currentThread().contextClassLoader)
  return requireNotNull(loader.getResource(name)) { "Missing widget fixture $name" }
    .openStream().bufferedReader().use { it.readText() }
}
