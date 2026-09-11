/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun SpoilerHiddenNotice(modifier: Modifier = Modifier) {
  Text(
    text = "Results and stats hidden by No spoilers mode",
    modifier = modifier,
    style = Prism.typography.bodySmall,
    color = Prism.color.labelColor,
  )
}
