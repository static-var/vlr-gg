/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import platform.UIKit.UIDevice

/**
 * iOS platform implementation.
 */
actual class Platform {
  actual val name: String = "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}
