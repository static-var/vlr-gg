package dev.unusedvariable.vlr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.unusedvariable.vlr.utils.Logger

@HiltAndroidApp
class VLRapp() : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.init(true)
  }
}
