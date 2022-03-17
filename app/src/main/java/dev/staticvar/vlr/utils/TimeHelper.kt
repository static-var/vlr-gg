package dev.staticvar.vlr.utils

import java.time.*
import java.time.format.DateTimeFormatter

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

val String.readableTime: String
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .format(DateTimeFormatter.RFC_1123_DATE_TIME)
      .substringBefore("+")

val String.readableDate: String
  get() =
    LocalDate.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .format(DateTimeFormatter.ofPattern("E, dd MMM yyyy"))

val String.timeToEpoch: Long
  get() =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
      .atOffset(ZoneOffset.UTC)
      .atZoneSameInstant(deviceZoneId)
      .toEpochSecond()

private val deviceZoneId = ZoneId.systemDefault()
