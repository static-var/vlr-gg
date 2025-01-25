package dev.staticvar.vlr.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.staticvar.vlr.ui.common.illustration.Gaming
import dev.staticvar.vlr.ui.common.illustration.Loading
import kotlin.collections.List as ____KtList

public object Illustration

private var __Icons: ____KtList<ImageVector>? = null

public val Illustration.Icons: ____KtList<ImageVector>
  @Composable get() {
    if (__Icons != null) {
      return __Icons!!
    }
    __Icons= listOf(Gaming, Loading)
    return __Icons!!
  }
