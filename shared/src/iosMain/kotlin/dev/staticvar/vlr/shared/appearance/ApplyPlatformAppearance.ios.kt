/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.setNeedsUpdateOfHomeIndicatorAutoHidden

@Composable
internal actual fun ApplyPlatformAppearance(isDark: Boolean, followSystem: Boolean) {
  val controller = LocalUIViewController.current
  val style = when {
    followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    isDark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
    else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
  }

  SideEffect {
    controller.overrideUserInterfaceStyle = style
    controller.view.window?.let { window ->
      window.overrideUserInterfaceStyle = style
      window.rootViewController?.setNeedsStatusBarAppearanceUpdate()
    }
    controller.setNeedsStatusBarAppearanceUpdate()
    controller.setNeedsUpdateOfHomeIndicatorAutoHidden()
  }
}
