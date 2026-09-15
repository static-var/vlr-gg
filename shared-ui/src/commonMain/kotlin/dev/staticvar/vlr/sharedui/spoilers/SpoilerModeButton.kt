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
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_hide_spoilers
import vlr.shared_ui.generated.resources.shared_show_spoilers
import vlr.shared_ui.generated.resources.shared_spoilers_off
import vlr.shared_ui.generated.resources.shared_spoilers_on

@Composable
public fun SpoilerModeButton(modifier: Modifier = Modifier) {
  val mode = LocalSpoilerMode.current
  val iconColor = Prism.color.titleColor
  val spoilerState = if (mode.enabled) {
    stringResource(
      Res.string.shared_spoilers_on,
    )
  } else {
    stringResource(Res.string.shared_spoilers_off)
  }
  PrismIconButton(
    icon = Prism.icons.preview,
    contentDescription = if (mode.enabled) {
      stringResource(Res.string.shared_show_spoilers)
    } else {
      stringResource(Res.string.shared_hide_spoilers)
    },
    onClick = mode.onToggle,
    selected = mode.enabled,
    size = PrismIconButtonSize.Toolbar,
    modifier = modifier.testTag("no_spoilers_toggle").semantics {
      stateDescription = spoilerState
    },
    iconModifier = if (mode.enabled) Modifier.closedEyeStroke(iconColor) else Modifier,
  )
}
