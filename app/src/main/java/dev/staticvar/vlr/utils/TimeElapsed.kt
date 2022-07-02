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
  private var timeMap: MutableMap<String, Long> = mutableMapOf()

  fun start(key: String, expireIn: Duration) {
    // Create a new entry in map or override existing with expiration time
    timeMap[key] = (Calendar.getInstance().timeInMillis + expireIn.inWholeMilliseconds)
    println(
      "$key started at ${Calendar.getInstance().timeInMillis} will expire in ${expireIn.absoluteValue}"
    )
  }

  fun hasElapsed(key: String): Boolean {
    return timeMap[key]?.let { expireDuration ->
      // If key exists then perform check
      println(
        "Elapsed check for $key, current time ${Calendar.getInstance().timeInMillis}, set to expire at $expireDuration"
      )
      expireDuration < Calendar.getInstance().timeInMillis
    }
      ?: true.also { // Key doesn't exist, return true
        println("$key not in records")
      }
  }

  fun reset(key: String) {
    // Set expired time
    i { "Resetting $key" }
    timeMap[key] = Calendar.getInstance().timeInMillis - 1
  }

  @VisibleForTesting internal fun timeForKey(key: String) = timeMap[key]
  @VisibleForTesting internal fun resetCache() = timeMap.clear()
}
