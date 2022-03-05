package dev.staticvar.vlr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
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

    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(getString(R.string.notification_channel_id),
        getString(R.string.notification_channel_title),
        NotificationManager.IMPORTANCE_HIGH)
      getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

  }
}
