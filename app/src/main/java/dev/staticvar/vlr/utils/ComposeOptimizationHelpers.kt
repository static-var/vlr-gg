package dev.staticvar.vlr.utils

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
class StableHolder<T>(val item: T) {
  operator fun component1(): T = item
}

@Immutable
class ImmutableHolder<T>(val item: T) {
  operator fun component1(): T = item
}
