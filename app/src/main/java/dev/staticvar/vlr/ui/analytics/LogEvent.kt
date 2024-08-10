package dev.staticvar.vlr.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase

@Composable
fun LogEvent(modifier: Modifier = Modifier, event: AnalyticsEvent, extra: Map<String, Any> = emptyMap()) {
  LaunchedEffect(Unit) {
    Firebase.crashlytics.setCustomKeys {
      key("screen", event.name)
      extra.forEach { (key, value) ->
        key(key, value.toString())
      }
    }
  }
}