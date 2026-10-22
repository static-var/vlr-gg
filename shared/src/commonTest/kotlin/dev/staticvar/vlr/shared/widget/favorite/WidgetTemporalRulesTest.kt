/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class WidgetTemporalRulesTest {
  private val now = Instant.parse("2026-10-16T12:00:00Z")
  private val zone = TimeZone.UTC
  private val locale = EnglishWidgetLocale()

  @Test
  fun matchStartUsesUtcAndAcceptsIsoTimeAndNonPaddedDays() {
    val cases = listOf(
      Triple("2026-10-18", "06:00", "2026-10-18T06:00:00Z"),
      Triple("2026-10-18", "06:00:00", "2026-10-18T06:00:00Z"),
      Triple("", "2026-10-18T06:00:00.125Z", "2026-10-18T06:00:00.125Z"),
      Triple("invalid", "2026-10-18T08:00:00+02:00", "2026-10-18T06:00:00Z"),
      Triple("2026-1-5", "06:00", "2026-01-05T06:00:00Z"),
      Triple("2026-10-18", " 06:00 ", "2026-10-18T06:00:00Z"),
    )
    for ((date, time, expected) in cases) {
      assertEquals(Instant.parse(expected), WidgetTemporalRules.matchStart(date, time))
    }
    for (time in listOf("", "TBD", "25:00", "06:70", "6:00", "06:00:60", "-1:00", "2026-02-30T06:00:00Z")) {
      assertNull(WidgetTemporalRules.matchStart("2026-10-18", time), time)
    }
    assertNull(WidgetTemporalRules.matchStart("2026-02-30", "06:00"))
  }

  @Test
  fun isoInstantsRetainFractionalSecondsAndRejectDateOnlyValues() {
    assertEquals(Instant.parse("2026-10-22T20:00:00Z"), WidgetTemporalRules.parseInstant("2026-10-22T20:00:00Z"))
    assertEquals(
      Instant.parse("2026-10-22T20:00:00.125Z"),
      WidgetTemporalRules.parseInstant("2026-10-22T20:00:00.125Z"),
    )
    assertNull(WidgetTemporalRules.parseInstant("2026-10-15"))
    assertNull(WidgetTemporalRules.parseInstant(null))
  }

  @Test
  fun calendarDaysValidateAndKeepFloatingDates() {
    assertEquals(WidgetDay.parse("2026-01-05"), WidgetDay.parse("2026-1-5"))
    for (invalid in listOf("26-10-18", "0000-01-01", "2026-02-29", "2026-10-32", "2026-00-01", "2026-10-18 ")) {
      assertNull(WidgetDay.parse(invalid), invalid)
    }
    assertEquals("Oct 18", EnglishWidgetLocale().date(LocalDate(2026, 10, 18), DateSkeleton.MonthDay))
  }

  @Test
  fun eventRangesResolveYearsAndOpenEndsAndFallBackToMatchDays() {
    val cases = listOf(
      Triple("Sep 24 – Oct 18, 2026", "2026-09-24", "2026-10-18"),
      Triple("Dec 11–14, 2026", "2026-12-11", "2026-12-14"),
      Triple("Sep 12 – TBD", "2026-09-12", null),
      Triple("Nov 1 – TBD", "2026-11-01", null),
      Triple("Feb 20 – Mar 3, 2027", "2027-02-20", "2027-03-03"),
      Triple("Dec 28 – Jan 4, 2027", "2026-12-28", "2027-01-04"),
      Triple("Dec 28, 2026 — Jan 4", "2026-12-28", "2027-01-04"),
      Triple("Oct 1 - Oct 4, 2026", "2026-10-01", "2026-10-04"),
      Triple("Feb 29 - Mar 1, 2028", "2028-02-29", "2028-03-01"),
    )
    for ((text, first, last) in cases) {
      assertEquals(
        EventDates(WidgetDay.parse(first), last?.let(WidgetDay::parse)),
        WidgetTemporalRules.eventDates(text, emptyList(), now),
        text,
      )
    }
    for (text in listOf(
      "Feb 29 - Mar 1, 2027",
      "Oct 30 - Oct 1, 2026",
      "Feb 31 - Mar 2, 2026",
      "Oct 1 - Oct 2, 26",
      "bad",
    )) {
      assertEquals(EventDates(null, null), WidgetTemporalRules.eventDates(text, emptyList(), now), text)
    }
    val days = listOf(LocalDate(2027, 3, 3), LocalDate(2027, 2, 20))
    assertEquals(
      EventDates(WidgetDay.parse("2027-02-20"), WidgetDay.parse("2027-03-03")),
      WidgetTemporalRules.eventDates("bad", days, now),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2027-02-20"), WidgetDay.parse("2027-03-03")),
      WidgetTemporalRules.eventDates("Feb 20 – TBD", days, now),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2027-09-12"), null),
      WidgetTemporalRules.eventDates("Sep 12 – TBD", emptyList(), Instant.parse("2027-01-01T00:00:00Z")),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2027-09-12"), null),
      WidgetTemporalRules.eventDates("Sep 12 – TBD", days, now),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2026-12-28"), WidgetDay.parse("2027-01-04")),
      WidgetTemporalRules.eventDates("Dec 28 – Jan 4", listOf(LocalDate(2027, 1, 2)), now),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2026-12-28"), WidgetDay.parse("2027-01-04")),
      WidgetTemporalRules.eventDates("Dec 28 – Jan 4", listOf(LocalDate(2027, 1, 2), LocalDate(2026, 12, 30)), now),
    )
    assertEquals(
      EventDates(WidgetDay.parse("2026-12-28"), WidgetDay.parse("2027-01-04")),
      WidgetTemporalRules.eventDates("Dec 28 – Jan 4", emptyList(), Instant.parse("2027-01-02T00:00:00Z")),
    )
  }

  @Test
  fun localMidnightsFollowDstInsteadOfFixedDayDurations() {
    val la = TimeZone.of("America/Los_Angeles")
    val cases = listOf(
      Triple("2026-11-01T19:30:00Z", "2026-11-01T07:00:00Z", "2026-11-02T08:00:00Z"),
      Triple("2026-03-08T19:30:00Z", "2026-03-08T08:00:00Z", "2026-03-09T07:00:00Z"),
    )
    for ((instant, _, next) in cases) {
      assertEquals(Instant.parse(next), WidgetTemporalRules.nextMidnight(Instant.parse(instant), la))
    }
    assertEquals(
      Instant.parse("2026-10-18T18:30:00Z"),
      WidgetDay.parse("2026-10-19")?.start(TimeZone.of("Asia/Kolkata")),
    )
  }

  @Test
  fun kickoffUsesClockTimesAndNamedDaysUntilTheStart() {
    val cases = listOf(
      8040L to Kickoff.Scheduled("Today", "2:14\u202FPM"),
      1L to Kickoff.Scheduled("Today", "12:00\u202FPM"),
      86400L to Kickoff.Scheduled("Tomorrow", "12:00\u202FPM"),
      0L to Kickoff.Delayed("12:00\u202FPM"),
    )
    for ((offset, expected) in cases) {
      assertEquals(expected, WidgetTemporalRules.kickoff(now + offset.seconds, now, zone, locale))
    }
    assertEquals(Kickoff.Unknown, WidgetTemporalRules.kickoff(null, now, zone, locale))
    assertEquals(
      Kickoff.Scheduled("Thu, Oct 22", "8:00\u202FPM"),
      WidgetTemporalRules.kickoff(Instant.parse("2026-10-22T20:00:00Z"), now, zone, locale),
    )
    assertEquals(
      Kickoff.Scheduled("Tomorrow", "12:00"),
      WidgetTemporalRules.kickoff(now + 86400.seconds, now, zone, EnglishWidgetLocale(use24Hour = true)),
    )
    assertEquals("Any minute", Kickoff.Delayed("").hero(locale))
    assertEquals("TBD", Kickoff.Unknown.hero(locale))
    assertEquals("2:14\u202FPM", Kickoff.Scheduled("Today", "2:14\u202FPM").hero(locale))
    val evening = Instant.parse("2026-10-16T22:00:00Z")
    val start = Instant.parse("2026-10-17T01:00:00Z")
    for ((name, expected) in listOf(
      "UTC" to Kickoff.Scheduled("Tomorrow", "1:00\u202FAM"),
      "America/Los_Angeles" to Kickoff.Scheduled("Today", "6:00\u202FPM"),
      "Asia/Tokyo" to Kickoff.Scheduled("Today", "10:00\u202FAM"),
      "Asia/Kolkata" to Kickoff.Scheduled("Today", "6:30\u202FAM"),
    )) {
      assertEquals(expected, WidgetTemporalRules.kickoff(start, evening, TimeZone.of(name), EnglishWidgetLocale()))
    }
  }

  @Test
  fun rowClocksUseRelativeDayThenWeekdayThenMonthAndOmitWholeHourMinutes() {
    val cases = listOf(
      Triple("2026-10-15T20:00:00Z", "Yesterday", "8\u202FPM"),
      Triple("2026-10-16T20:14:00Z", "Today", "8:14\u202FPM"),
      Triple("2026-10-17T20:00:00Z", "Tomorrow", "8\u202FPM"),
      Triple("2026-10-22T20:00:00Z", "Thu", "8\u202FPM"),
      Triple("2026-10-23T20:00:00Z", "Oct 23", "8\u202FPM"),
      Triple("2026-10-09T20:00:00Z", "Oct 9", "8\u202FPM"),
    )
    for ((start, day, time) in cases) {
      assertEquals(day to time, WidgetTemporalRules.rowClock(Instant.parse(start), now, zone, locale))
    }
    assertEquals(
      "Today" to "20",
      WidgetTemporalRules.rowClock(
        Instant.parse("2026-10-16T20:00:00Z"),
        now,
        zone,
        EnglishWidgetLocale(use24Hour = true),
      ),
    )
  }

  @Test
  fun kickoffBoundariesContainOnlyFutureLabelAndStateChanges() {
    val cases = listOf(
      "2026-10-16T13:00:00Z" to listOf("2026-10-16T12:00:00Z", "2026-10-16T13:00:00Z"),
      "2026-10-17T12:00:00Z" to listOf("2026-10-16T12:00:00Z", "2026-10-17T00:00:00Z", "2026-10-17T12:00:00Z"),
      "2026-10-19T12:00:00Z" to
        listOf("2026-10-16T12:00:00Z", "2026-10-18T00:00:00Z", "2026-10-19T00:00:00Z", "2026-10-19T12:00:00Z"),
      "2026-10-17T00:00:00Z" to listOf("2026-10-16T12:00:00Z", "2026-10-17T00:00:00Z"),
      null to listOf("2026-10-16T12:00:00Z"),
      "2026-10-16T12:00:00Z" to listOf("2026-10-16T12:00:00Z"),
    )
    for ((start, expected) in cases) {
      assertEquals(
        expected.map(Instant::parse),
        WidgetTemporalRules.kickoffDates(start?.let(Instant::parse), now, zone),
      )
    }
    assertEquals(
      listOf(
        "2026-10-31T12:00:00Z",
        "2026-11-01T07:00:00Z",
        "2026-11-02T08:00:00Z",
        "2026-11-02T20:00:00Z",
      ).map(Instant::parse),
      WidgetTemporalRules.kickoffDates(
        Instant.parse("2026-11-02T20:00:00Z"),
        Instant.parse("2026-10-31T12:00:00Z"),
        TimeZone.of("America/Los_Angeles"),
      ),
    )
  }

  @Test
  fun scheduledRefreshCapsAtKickoffAndUsesTheCallerOverdueInterval() {
    val cases = listOf(
      Triple(-1L, 300L, 300L),
      Triple(0L, 300L, 300L),
      Triple(-1L, 900L, 900L),
      Triple(60L, 300L, 60L),
      Triple(86400L, 300L, 1800L),
      Triple(86401L, 300L, 7200L),
      Triple(null, 300L, 7200L),
    )
    for ((offset, overdue, expected) in cases) {
      assertEquals(
        now + expected.seconds,
        WidgetTemporalRules.scheduled(
          offset?.let {
            now + it.seconds
          },
          now,
          overdue.seconds,
        ),
      )
    }
  }

  @Test
  fun localeSupportsTypedTextPositionalArgumentsAndPlatformIntegers() {
    assertEquals("FNATIC beat Sentinels", locale.text(WidgetTextKey.TeamBeatTeam, "FNATIC", "Sentinels"))
    assertEquals("#2 Europe", locale.text(WidgetTextKey.RegionalRank, "2", "Europe"))
    assertEquals("Tap to choose", locale.text(WidgetTextKey.TapToChoose))
    assertEquals("2", locale.integer(2))
    assertEquals("0", locale.integer(0))
    assertEquals("-2", locale.integer(-2))
  }
}
