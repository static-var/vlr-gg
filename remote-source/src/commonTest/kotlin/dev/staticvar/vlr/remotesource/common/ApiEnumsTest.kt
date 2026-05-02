/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.common

import kotlin.test.Test

class ApiEnumsTest {

  @Test
  fun matchStatus_unknown_decodes_to_null() = EnumFallbackAsserts.assertNullableMappings(
    MatchStatusNullableSerializer,
    mapOf(
      "live" to MatchStatus.LIVE,
      "completed" to MatchStatus.COMPLETED,
      "upcoming" to MatchStatus.UPCOMING,
      "ongoing" to MatchStatus.ONGOING,
      "tbd" to MatchStatus.TBD,
      "brand_new" to null,
    ),
  )

  @Test
  fun eventStatus_unknown_decodes_to_null() = EnumFallbackAsserts.assertNullableMappings(
    EventStatusNullableSerializer,
    mapOf(
      "upcoming" to EventStatus.UPCOMING,
      "completed" to EventStatus.COMPLETED,
      "ongoing" to EventStatus.ONGOING,
      "brand_new" to null,
    ),
  )

  @Test
  fun searchCategory_unknown_decodes_to_null() = EnumFallbackAsserts.assertNullableMappings(
    SearchCategoryNullableSerializer,
    mapOf(
      "teams" to SearchCategory.TEAM,
      "players" to SearchCategory.PLAYER,
      "events" to SearchCategory.EVENT,
      "series" to SearchCategory.SERIES,
      "all" to SearchCategory.ALL,
      "esports_org" to null,
    ),
  )
}
