/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

/**
 * Desktop platform implementation.
 */
actual class Platform {
  actual val name: String = buildString {
    append("Desktop")
    System.getProperty("os.name")?.let { osName ->
      append(' ')
      append(osName)
    }
  }
}
