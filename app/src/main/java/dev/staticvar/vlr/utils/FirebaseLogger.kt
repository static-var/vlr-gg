package dev.staticvar.vlr.utils

import androidx.compose.runtime.Immutable
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase crashlytics logger - This logger is used to log the current navigation state, where the
 * user is in the app.
 *
 * @constructor Create Firebase logger
 */
@Immutable
object FirebaseLogger {

  private val crashlytics = Firebase.crashlytics

  fun setDestinationKey(destination: String) {
    crashlytics.setCustomKey("nav_state", destination)
  }
}
