/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import android.os.Build

/**
 * Android platform implementation.
 */
actual class Platform {
  actual val name: String = "Android ${Build.VERSION.SDK_INT}"
}
