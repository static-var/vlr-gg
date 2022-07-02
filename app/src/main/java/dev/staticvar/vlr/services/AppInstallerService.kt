package dev.staticvar.vlr.services

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.widget.Toast
import dev.staticvar.vlr.MainActivity
import dev.staticvar.vlr.R
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.i

/** App installer service */
class AppInstallerService : Service() {

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, DEFAULT_VALUE)) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        i { "Requesting user confirmation for installation" }
        val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        confirmationIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
          startActivity(confirmationIntent)
        } catch (e: Exception) {
          e.printStackTrace()
          e { "Unable to start installation" }
        }
      }
      PackageInstaller.STATUS_SUCCESS -> {
        i { "Installation successful" }
        filesDir
          .listFiles()
          ?.filter { it.name.startsWith(APK_NAME) && it.name.endsWith(FILE_SUFFIX) }
          ?.forEach { it.delete() }
        i { "Delete APK(s)" }
        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let {
          e { it }
          Toast.makeText(this, getString(R.string.app_updated), Toast.LENGTH_SHORT).show()
          startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        }
      }
      DEFAULT_VALUE -> {
        filesDir
          .listFiles()
          ?.filter { it.name.startsWith(APK_NAME) && it.name.endsWith(FILE_SUFFIX) }
          ?.forEach { it.delete() }
        i { "Delete APK(s)" }
        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let {
          e { it }
          Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
          startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        }
      }
    }
    stopSelf()
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}

const val DEFAULT_VALUE = -999
const val FILE_SUFFIX = ".apk"
const val APK_NAME = ".update"
