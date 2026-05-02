/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource

import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlin.io.use

internal actual fun readFixture(fileName: String): String {
  val loader = requireNotNull(Thread.currentThread().contextClassLoader) {
    "Context class loader unavailable while loading fixture $fileName"
  }
  val resource = requireNotNull(loader.getResource(fileName)) {
    "Fixture $fileName was not found in test resources."
  }
  return resource.openStream().use { stream ->
    stream.asSource().buffered().use { it.readString() }
  }
}
