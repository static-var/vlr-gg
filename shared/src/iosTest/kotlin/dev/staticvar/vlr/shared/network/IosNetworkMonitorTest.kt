/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.network

import kotlin.test.Test
import kotlin.test.assertEquals
import dev.staticvar.vlr.core.network.NetworkStatus

class IosNetworkMonitorTest {
  @Test
  fun doesNotReportOfflineBeforeReceivingAPathUpdate() {
    val monitor = IosNetworkMonitor()
    try {
      assertEquals(NetworkStatus.Unknown, monitor.status.value)
    } finally {
      monitor.close()
    }
  }
}
