/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.shared.widget.favorite.DateSkeleton
import dev.staticvar.vlr.shared.widget.favorite.RelativeDay
import dev.staticvar.vlr.shared.widget.favorite.TimeSkeleton
import dev.staticvar.vlr.shared.widget.favorite.WidgetTextKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale
import kotlin.time.Instant
import java.util.TimeZone as JavaTimeZone

class AndroidWidgetLocaleTest {
  private val languageTags = listOf("en-US", "de", "es", "fr", "hi", "ko", "pt-BR", "ru", "tr")
  private val untranslatedWords = mapOf(
    "de" to setOf(WidgetTextKey.TeamTitle, WidgetTextKey.EventTitle, WidgetTextKey.Live, WidgetTextKey.RegionalRank),
    "es" to setOf(WidgetTextKey.RegionalRank, WidgetTextKey.Matchup),
    "fr" to setOf(WidgetTextKey.Champions),
    "hi" to setOf(WidgetTextKey.RegionalRank),
    "ko" to setOf(WidgetTextKey.Matchup),
    "pt-BR" to setOf(WidgetTextKey.RegionalRank, WidgetTextKey.Matchup),
  )

  @Test
  fun everyKeyResolvesInEverySupportedLocale() {
    languageTags.forEach { tag ->
      val locale = locale(tag)
      WidgetTextKey.entries.forEach { key ->
        assertFalse("$tag $key", template(locale, key).isBlank())
      }
    }
  }

  @Test
  fun translationsDifferFromEnglishExceptSharedWordsAndFormats() {
    val english = locale()
    languageTags.drop(1).forEach { tag ->
      val translated = locale(tag)
      WidgetTextKey.entries.filterNot { it in untranslatedWords[tag].orEmpty() }.forEach { key ->
        assertNotEquals("$tag $key", template(english, key), template(translated, key))
      }
    }
  }

  @Test
  fun translationsKeepEveryPositionalPlaceholder() {
    val english = locale()
    val placeholder = Regex("%[1-9][0-9]*\\\$s")
    languageTags.forEach { tag ->
      val translated = locale(tag)
      WidgetTextKey.entries.forEach { key ->
        assertEquals(
          "$tag $key",
          placeholder.findAll(template(english, key)).map { it.value }.toSet(),
          placeholder.findAll(template(translated, key)).map { it.value }.toSet(),
        )
      }
    }
  }

  @Test
  fun textFormatsArgumentsInTheTranslatedOrder() {
    assertEquals("Sentinels ने FNATIC को हराया", locale("hi").text(WidgetTextKey.TeamBeatTeam, "Sentinels", "FNATIC"))
    assertEquals("N° 12 mondial", locale("fr").text(WidgetTextKey.WorldwideRank, "12"))
  }

  @Test
  fun setupTapInstructionUsesEachSupportedLanguage() {
    val translations = listOf(
      "Tap to choose", "Zum Auswählen tippen", "Toca para elegir", "Touchez pour choisir", "चुनने के लिए टैप करें",
      "탭하여 선택", "Toque para escolher", "Нажмите, чтобы выбрать", "Seçmek için dokunun",
    )
    languageTags.zip(translations).forEach { (tag, expected) ->
      assertEquals(tag, expected, locale(tag).text(WidgetTextKey.TapToChoose))
    }
  }

  @Test
  fun twelveHourClockUsesTheZoneOfEachCall() {
    val locale = locale()
    val instant = Instant.parse("2026-10-22T20:00:00Z")
    assertEquals("1:30 AM", locale.instant(instant, TimeSkeleton.HourMinute, TimeZone.of("Asia/Kolkata")))
    assertEquals("1:00 PM", locale.instant(instant, TimeSkeleton.HourMinute, TimeZone.of("America/Los_Angeles")))
  }

  @Test
  fun fixedOffsetZoneKeepsItsOffset() {
    assertEquals(
      "1:30 AM",
      locale().instant(Instant.parse("2026-10-22T20:00:00Z"), TimeSkeleton.HourMinute, TimeZone.of("+05:30")),
    )
  }

  @Test
  fun twentyFourHourSettingOverridesTheLocaleClock() {
    val locale = locale(use24Hour = true)
    val instant = Instant.parse("2026-10-22T20:00:00Z")
    assertEquals("20:00", locale.instant(instant, TimeSkeleton.HourMinute, TimeZone.UTC))
    assertEquals("20", locale.instant(instant, TimeSkeleton.Hour, TimeZone.UTC))
  }

  @Test
  fun twelveHourSettingOverridesATwentyFourHourLocale() {
    val locale = locale("de-DE")
    val instant = Instant.parse("2026-10-22T20:00:00Z")
    assertEquals("8:00\u202FPM", locale.instant(instant, TimeSkeleton.HourMinute, TimeZone.UTC))
    assertEquals("8 Uhr PM", locale.instant(instant, TimeSkeleton.Hour, TimeZone.UTC))
  }

  @Test
  fun wholeHourClockOmitsTheMinutes() {
    assertEquals("8 PM", locale().instant(Instant.parse("2026-10-22T20:00:00Z"), TimeSkeleton.Hour, TimeZone.UTC))
  }

  @Test
  fun weekdayUsesTheLocalizedSkeleton() {
    val date = LocalDate(2026, 10, 22)
    assertEquals("Thu", locale().date(date, DateSkeleton.Weekday))
    assertEquals("Do", locale("de").date(date, DateSkeleton.Weekday))
  }

  @Test
  fun monthDayUsesTheLocalizedSkeleton() {
    val date = LocalDate(2026, 10, 15)
    assertEquals("Oct 15", locale().date(date, DateSkeleton.MonthDay))
    assertEquals("15. Okt.", locale("de").date(date, DateSkeleton.MonthDay))
  }

  @Test
  fun weekdayMonthDayUsesTheLocalizedSkeleton() {
    val date = LocalDate(2026, 10, 22)
    assertEquals("Thu, Oct 22", locale().date(date, DateSkeleton.WeekdayMonthDay))
    assertEquals("Do., 22. Okt.", locale("de").date(date, DateSkeleton.WeekdayMonthDay))
  }

  @Test
  fun calendarDateDoesNotShiftWithTheDefaultTimeZone() {
    val previous = JavaTimeZone.getDefault()
    val locale = locale()
    try {
      listOf("Pacific/Kiritimati", "America/Los_Angeles").forEach { zone ->
        JavaTimeZone.setDefault(JavaTimeZone.getTimeZone(zone))
        assertEquals(zone, "Oct 15", locale.date(LocalDate(2026, 10, 15), DateSkeleton.MonthDay))
      }
    } finally {
      JavaTimeZone.setDefault(previous)
    }
  }

  @Test
  fun relativeDaysUseNamedLocalizedWords() {
    val words = mapOf(
      "en-US" to listOf("Yesterday", "Today", "Tomorrow"),
      "de" to listOf("Gestern", "Heute", "Morgen"),
      "es" to listOf("Ayer", "Hoy", "Mañana"),
      "fr" to listOf("Hier", "Aujourd’hui", "Demain"),
      "hi" to listOf("बीता कल", "आज", "आने वाला कल"),
      "ko" to listOf("어제", "오늘", "내일"),
      "pt-BR" to listOf("Ontem", "Hoje", "Amanhã"),
      "ru" to listOf("Вчера", "Сегодня", "Завтра"),
      "tr" to listOf("Dün", "Bugün", "Yarın"),
    )
    words.forEach { (tag, expected) ->
      val locale = locale(tag)
      assertEquals(tag, expected, RelativeDay.entries.map(locale::relativeDay))
    }
  }

  @Test
  fun integerOmitsGroupingSeparators() {
    assertEquals("1234", locale("de").integer(1234))
    assertEquals("2147483648", locale("de").integer(2147483648L))
  }

  @Test
  fun supportedLocalesUseLatinDigitsByDefault() {
    languageTags.forEach { tag -> assertEquals(tag, "1234", locale(tag).integer(1234)) }
  }

  @Test
  fun integerHonorsTheLocalesNonLatinNumberingSystem() {
    assertEquals("१२३४", locale("hi-IN-u-nu-deva").integer(1234))
  }

  private fun locale(tag: String = "en-US", use24Hour: Boolean = false): AndroidWidgetLocale =
    AndroidWidgetLocale(context(tag), use24Hour)

  private fun context(tag: String): Context {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = Configuration(target.resources.configuration).apply {
      setLocales(LocaleList(Locale.forLanguageTag(tag)))
    }
    return target.createConfigurationContext(configuration)
  }

  private fun template(locale: AndroidWidgetLocale, key: WidgetTextKey): String = locale.text(key, "%1\$s", "%2\$s")
}
