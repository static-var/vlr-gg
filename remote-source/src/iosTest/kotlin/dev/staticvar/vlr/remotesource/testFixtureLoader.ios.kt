/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import platform.Foundation.NSProcessInfo

internal actual fun readFixture(fileName: String): String {
  val fixturesDirectory = requireNotNull(
    NSProcessInfo.processInfo.environment["VLR_TEST_FIXTURES"] as? String,
  ) {
    "VLR_TEST_FIXTURES must point to the fixture directory; run iOS tests through Gradle."
  }
  val path = Path(fixturesDirectory, fileName)
  val source = SystemFileSystem.source(path).buffered()
  return try {
    source.readString()
  } finally {
    source.close()
  }
}
