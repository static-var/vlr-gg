/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class CacheCleanupStats(
  val deletedRecords: Long,
  val lastRunEpochMillis: Long?,
)
