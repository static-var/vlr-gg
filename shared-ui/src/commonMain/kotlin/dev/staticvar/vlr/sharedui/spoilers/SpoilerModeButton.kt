/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun SpoilerModeButton(modifier: Modifier = Modifier) {
  val mode = LocalSpoilerMode.current
  val iconColor = Prism.color.titleColor
  PrismIconButton(
    icon = Prism.icons.preview,
    contentDescription = if (mode.enabled) {
      "Show results and stats throughout app"
    } else {
      "Hide results and stats throughout app"
    },
    onClick = mode.onToggle,
    selected = mode.enabled,
    size = PrismIconButtonSize.Toolbar,
    modifier = modifier.testTag("no_spoilers_toggle").semantics {
      stateDescription = if (mode.enabled) "No spoilers on" else "No spoilers off"
    },
    iconModifier = if (mode.enabled) Modifier.closedEyeStroke(iconColor) else Modifier,
  )
}
