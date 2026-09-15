/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_spoilers_notice

@Composable
public fun SpoilerHiddenNotice(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(Res.string.shared_spoilers_notice),
    modifier = modifier,
    style = Prism.typography.bodySmall,
    color = Prism.color.labelColor,
  )
}
