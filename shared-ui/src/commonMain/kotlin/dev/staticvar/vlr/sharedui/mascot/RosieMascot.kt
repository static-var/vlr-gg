/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_rosie_description

@Composable
public fun RosieMascot(modifier: Modifier = Modifier, animated: Boolean = true) {
  MascotFigure(RosieArt, RosieMotion, stringResource(Res.string.shared_rosie_description), modifier, animated)
}

/** An eager wag, four swings per loop, with an earlier blink than Lynx. */
private val RosieMotion = MascotMotion(tailDegrees = 7f, tailSwingsPerLoop = 4, blinkAt = 0.73f)
