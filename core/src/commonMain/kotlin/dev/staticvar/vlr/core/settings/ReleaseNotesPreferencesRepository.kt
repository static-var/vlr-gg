/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class ReleaseNotesPreferencesRepository(private val storage: Settings) {
  public val acknowledgedReleaseId: StateFlow<String?>
    field = MutableStateFlow(storage.getStringOrNull(AcknowledgedReleaseKey))

  public fun acknowledge(releaseId: String) {
    storage.putString(AcknowledgedReleaseKey, releaseId)
    acknowledgedReleaseId.value = releaseId
  }

  private companion object {
    const val AcknowledgedReleaseKey: String = "releaseNotes.acknowledgedReleaseId"
  }
}
