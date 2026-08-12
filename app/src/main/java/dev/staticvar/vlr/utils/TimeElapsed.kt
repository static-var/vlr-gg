package dev.staticvar.vlr.utils

import androidx.annotation.VisibleForTesting
import java.util.*
import kotlin.time.Duration

/**
 * Time elapsed A simple in-memory time system to prevent a key from being accessed before its
 * allocated elapsed time.
 *
 * @constructor Create empty Time elapsed
 */
object TimeElapsed {
  private data class TimeEntry(val expiresAtMillis: Long, val startedAtMillis: Long?)

  private var timeMap: MutableMap<String, TimeEntry> = mutableMapOf()

  fun start(key: String, expireIn: Duration) {
    val startedAtMillis = Calendar.getInstance().timeInMillis
    timeMap[key] =
      TimeEntry(
        expiresAtMillis = startedAtMillis + expireIn.inWholeMilliseconds,
        startedAtMillis = startedAtMillis,
      )
    println("$key started at $startedAtMillis will expire in ${expireIn.absoluteValue}")
  }

  fun hasElapsed(key: String): Boolean {
    return timeMap[key]?.let { entry ->
      val currentTimeMillis = Calendar.getInstance().timeInMillis
      println(
        "Elapsed check for $key, current time $currentTimeMillis, set to expire at ${entry.expiresAtMillis}"
      )
      entry.expiresAtMillis < currentTimeMillis
    }
      ?: true.also { // Key doesn't exist, return true
        println("$key not in records")
      }
  }

  fun reset(key: String) {
    i { "Resetting $key" }
    timeMap[key] =
      TimeEntry(
        expiresAtMillis = Calendar.getInstance().timeInMillis - 1,
        startedAtMillis = null,
      )
  }

  internal fun lastStartedAtMillis(key: String) = timeMap[key]?.startedAtMillis

  @VisibleForTesting internal fun timeForKey(key: String) = timeMap[key]?.expiresAtMillis
  @VisibleForTesting internal fun resetCache() = timeMap.clear()
}
