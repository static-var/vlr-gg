package dev.staticvar.vlr.utils

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val String.timeDiff: String
  get() {
    val givenTime = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
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
      if (diff.isNegative) append(" ago")
    }
  }
