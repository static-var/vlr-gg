package dev.staticvar.vlr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.ktx.performance
import dagger.hilt.android.HiltAndroidApp
import dev.staticvar.vlr.utils.Logger
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.i
import dev.staticvar.vlr.utils.queueWorker
import javax.inject.Inject

@HiltAndroidApp
class VLRapp() : Application(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  override fun onCreate() {
    super.onCreate()
    Logger.init(true)
    firebaseInit()
    createNotificationChannel()
    queueWorker()
  }

  private fun firebaseInit() {
    Firebase.performance.isPerformanceCollectionEnabled = !BuildConfig.DEBUG
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        i { "FCM Token ${task.result}" }
      } else {
        e { "FCM Token error" }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          getString(R.string.notification_channel_id),
          getString(R.string.notification_channel_title),
          NotificationManager.IMPORTANCE_HIGH
        )
      getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
  }

  override fun getWorkManagerConfiguration() =
    Configuration.Builder().setWorkerFactory(workerFactory).build()
}
