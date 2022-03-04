package dev.staticvar.vlr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.staticvar.vlr.utils.Logger

@HiltAndroidApp
class VLRapp() : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.init(true)
  }
}
