/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedNetworkIconTest {
  @Test
  fun fallbackInitialsUseTeamNameWords() {
    assertEquals("TH", sharedNetworkIconFallbackText("Team Heretics"))
    assertEquals("KB", sharedNetworkIconFallbackText("KRÜ BLAZE"))
    assertEquals("T", sharedNetworkIconFallbackText("TBD"))
    assertEquals("?", sharedNetworkIconFallbackText(null))
  }
}
