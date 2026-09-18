/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellationException

@Composable
internal fun rememberLogoTreatment(
  request: LogoTreatmentRequest?,
  provider: LogoTreatmentProvider,
): State<LogoTreatment?> {
  val cached = remember(request, provider) { request?.let(provider::cached) }
  val result = remember(request, provider) { mutableStateOf(cached as? LogoTreatment.Outlined) }
  LaunchedEffect(request, provider) {
    if (request != null && cached == null) {
      val prepared = try {
        provider.prepare(request)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        LogoTreatment.Original
      }
      if (prepared is LogoTreatment.Outlined) result.value = prepared
    }
  }
  return result
}
