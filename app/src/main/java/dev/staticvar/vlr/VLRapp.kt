package dev.staticvar.vlr

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import dev.staticvar.vlr.utils.Logger
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.i

@HiltAndroidApp
class VLRapp() : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.init(true)

    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if(task.isSuccessful) {
        i {"FCM Token ${task.result}"}
      } else {
        e {"FCM Token error"}
      }
    }
  }
}
