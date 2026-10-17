/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_lynx_description

@Composable
public fun LynxMascot(modifier: Modifier = Modifier, animated: Boolean = true) {
  MascotFigure(LynxArt, LynxMotion, stringResource(Res.string.shared_lynx_description), modifier, animated)
}

/** A slow, lazy bobtail flick with one late blink. */
private val LynxMotion = MascotMotion(tailDegrees = 9f, tailSwingsPerLoop = 1, blinkAt = 0.79f)
