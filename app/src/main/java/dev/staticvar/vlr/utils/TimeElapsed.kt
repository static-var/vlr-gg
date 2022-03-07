package dev.staticvar.vlr.utils

import kotlin.time.Duration

object TimeElapsed {
  private var timeMap: MutableMap<String, Long> = mutableMapOf()

  fun start(key: String, expireIn: Duration) {
    // Create a new entry in map or override existing with expiration time
    timeMap[key] = (System.currentTimeMillis() + expireIn.inWholeMilliseconds)
    i { "$key started at ${System.currentTimeMillis()} will expire in ${expireIn.absoluteValue}" }
  }

  fun hasElapsed(key: String): Boolean {
    return timeMap[key]?.let { expireDuration ->
      // If key exists then perform check
      i {
        "Elapsed check for $key, current time ${System.currentTimeMillis()}, set to expire at $expireDuration"
      }
      expireDuration < System.currentTimeMillis()
    }
      ?: true.also { // Key doesn't exist, return true
        i { "$key not in records" }
      }
  }

  fun reset(key: String) {
    // Set expired time
    i { "Resetting $key" }
    timeMap[key] = System.currentTimeMillis() - 1
  }
}
