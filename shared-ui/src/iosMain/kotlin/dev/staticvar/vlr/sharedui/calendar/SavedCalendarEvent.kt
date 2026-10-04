/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.calendar

internal data class SavedCalendarEvent(
  val id: String,
  val externalId: String? = null,
  val calendarId: String? = null,
) {
  fun matchesFallback(externalId: String?, calendarId: String?): Boolean =
    this.externalId != null && this.calendarId != null && this.externalId == externalId && this.calendarId == calendarId

  fun toRecord(): Map<String, String> = buildMap {
    put("id", id)
    externalId?.let { put("externalId", it) }
    calendarId?.let { put("calendarId", it) }
  }

  companion object {
    fun fromRecord(record: Any?): SavedCalendarEvent? {
      if (record !is Map<*, *>) return null
      val id = (record["id"] as? String)?.takeIf(String::isNotBlank) ?: return null
      return SavedCalendarEvent(
        id = id,
        externalId = (record["externalId"] as? String)?.takeIf(String::isNotBlank),
        calendarId = (record["calendarId"] as? String)?.takeIf(String::isNotBlank),
      )
    }
  }
}
