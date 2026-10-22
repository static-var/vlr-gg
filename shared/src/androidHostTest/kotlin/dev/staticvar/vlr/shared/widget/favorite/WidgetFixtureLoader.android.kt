/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

internal actual fun readWidgetFixture(name: String): String {
  val loader = requireNotNull(Thread.currentThread().contextClassLoader)
  return requireNotNull(loader.getResource(name)) { "Missing widget fixture $name" }
    .openStream().bufferedReader().use { it.readText() }
}
