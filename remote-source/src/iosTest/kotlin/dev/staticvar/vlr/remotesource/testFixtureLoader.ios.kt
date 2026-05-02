/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import platform.Foundation.NSBundle

internal actual fun readFixture(fileName: String): String {
  val separatorIndex = fileName.lastIndexOf('.')
  require(separatorIndex in 1 until fileName.lastIndex) {
    "Fixture $fileName must include an extension."
  }
  val resourceName = fileName.substring(0, separatorIndex)
  val resourceExtension = fileName.substring(separatorIndex + 1)
  val resourcePath = requireNotNull(
    NSBundle.mainBundle.pathForResource(resourceName, resourceExtension),
  ) {
    "Fixture $fileName was not bundled with the iOS test target."
  }
  val path = Path(resourcePath)
  val source = SystemFileSystem.source(path).buffered()
  return try {
    source.readString()
  } finally {
    source.close()
  }
}
