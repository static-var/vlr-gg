package dev.staticvar.vlr.utils

import java.time.*
import java.time.format.DateTimeFormatter

private const val FULL_DATE_TIME_PATTERN_WITH_ZONE = "E, dd MMM yyyy HH:mm z"
private const val CUSTOM_DATE_TIME_PATTERN = "HH:mm:ss yyyy-MM-dd"
private const val FULL_DATE_TIME_PATTERN = "E, dd MMM yyyy HH:mm"
private const val FULL_DATE_PATTERN = "E, dd MMM yyyy"
private const val FULL_TIME_PATTERN = "HH:mm a"
private val deviceZoneId = ZoneId.systemDefault()

val String.timeDiff: String
  get() {
    val givenTime =
      LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
        .atOffset(ZoneOffset.UTC)
        .atZoneSameInstant(deviceZoneId)
    val diff = Duration.between(LocalDateTime.now(), givenTime)
    return buildString {
      if (diff.toDays() != 0L) {
        append(diff.abs().toDays())
        append("d ")
      }
      if (diff.toHours() != 0L) {
        append(diff.abs().toHours() % 24)
        append("h ")
      }
      if (diff.toMinutes() != 0L) {
        append(diff.abs().toMinutes() % 60)
        append("m ")
      }
      if (diff.isNegative) append(" ago") else insert(0, "in ")
    }
  }

val String.hasElapsed: Boolean
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .isBefore(ZonedDateTime.now())

val String.readableDateAndTimeWithZone: String
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .format(DateTimeFormatter.ofPattern(ABBREVIATED_DATE_TIME_WITH_ZONE))

val String.readableDateAndTime: String
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .withZoneSameInstant(deviceZoneId)
      .format(DateTimeFormatter.ofPattern(FULL_DATE_TIME_PATTERN))

val String.readableDate: String
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .format(DateTimeFormatter.ofPattern(FULL_DATE_PATTERN))

val String.readableTime: String
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .format(DateTimeFormatter.ofPattern(FULL_TIME_PATTERN))

val String.timeToEpoch: Long
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .toEpochSecond()

const val ABBREVIATED_DATE_TIME_WITH_ZONE = "EEE, dd MMM hh:mm a z"

val String.patternDateTimeToReadable: String
  get() = LocalDateTime.parse(this, DateTimeFormatter.ofPattern(CUSTOM_DATE_TIME_PATTERN))
    .atOffset(ZoneOffset.UTC)
    .atZoneSameInstant(deviceZoneId)
    .withZoneSameInstant(deviceZoneId)
    .format(DateTimeFormatter.ofPattern(FULL_DATE_TIME_PATTERN))
