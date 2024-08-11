package dev.staticvar.vlr.ui.helper

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
operator fun PaddingValues.plus(paddingValues: PaddingValues): PaddingValues {
  val layoutDirection = LocalLayoutDirection.current
  return PaddingValues(
    top = calculateTopPadding() + paddingValues.calculateTopPadding(),
    bottom = calculateBottomPadding() + paddingValues.calculateBottomPadding(),
    start =
      calculateStartPadding(layoutDirection) + paddingValues.calculateStartPadding(layoutDirection),
    end = calculateEndPadding(layoutDirection) + paddingValues.calculateEndPadding(layoutDirection),
  )
}
