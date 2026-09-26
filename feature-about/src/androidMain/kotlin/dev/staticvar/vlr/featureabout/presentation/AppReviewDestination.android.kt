/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.note_rate_google_play

internal actual fun appReviewUrl(): String? =
  "https://play.google.com/store/apps/details?id=dev.staticvar.vlr"

@Composable
internal actual fun appReviewLabel(): String = stringResource(Res.string.note_rate_google_play)
