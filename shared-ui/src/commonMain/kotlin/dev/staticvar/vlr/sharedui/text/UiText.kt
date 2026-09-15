/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

public sealed interface UiText {
  public data class Literal(val value: String) : UiText

  public data class Resource(val resource: StringResource, val arguments: List<Any> = emptyList()) : UiText
}

@Composable
public fun UiText.resolve(): String = when (this) {
  is UiText.Literal -> value
  is UiText.Resource -> stringResource(resource, *arguments.toTypedArray())
}
