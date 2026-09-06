/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal actual fun PrismModalSheetDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties =
    DialogProperties(
      usePlatformDefaultWidth = false,
      usePlatformInsets = false,
      scrimColor = Color.Transparent,
    ),
    content = content,
  )
}
