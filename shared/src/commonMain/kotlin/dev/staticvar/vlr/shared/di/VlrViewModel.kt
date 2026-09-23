/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition

public val LocalViewModelObserver = staticCompositionLocalOf<((ViewModel) -> Unit)?> { null }

@Composable
internal inline fun <reified T : ViewModel> vlrViewModel(
  noinline parameters: ParametersDefinition? = null,
): T {
  val viewModel = koinViewModel<T>(parameters = parameters)
  val observer = LocalViewModelObserver.current
  if (observer != null) {
    SideEffect { observer(viewModel) }
  }
  return viewModel
}
