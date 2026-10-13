/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.app_logo

/**
 * The Match point app logo in its fixed brand colors.
 *
 * @param contentDescription Spoken label. Leave it null when nearby text already names the app.
 */
@Composable
public fun SharedAppLogo(modifier: Modifier = Modifier, contentDescription: String? = null) {
  Image(
    painter = painterResource(Res.drawable.app_logo),
    contentDescription = contentDescription,
    modifier = modifier,
  )
}
