/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.host

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.shared.navigation.HostChrome
import platform.UIKit.UIScreen

public class IosHostChromeStore {
  internal var chrome: HostChrome? by mutableStateOf(null)
    private set

  public fun update(
    edge: Int,
    barTopReserved: Double,
    hingeX: Double,
    hingeY: Double,
    hingeWidth: Double,
    hingeHeight: Double,
  ) {
    val barSide = when (edge) {
      1 -> WindowInsetsSides.Start
      2 -> WindowInsetsSides.End
      else -> null
    }
    val scale = UIScreen.mainScreen.scale.toFloat()
    val hinge = if (hingeWidth > 0 && hingeHeight > 0) {
      Rect(
        left = hingeX.toFloat() * scale,
        top = hingeY.toFloat() * scale,
        right = (hingeX + hingeWidth).toFloat() * scale,
        bottom = (hingeY + hingeHeight).toFloat() * scale,
      )
    } else {
      null
    }
    chrome = if (barSide == null && hinge == null) {
      null
    } else {
      HostChrome(barSide = barSide, barTopReserved = barTopReserved.toFloat().dp, hinge = hinge)
    }
  }
}
