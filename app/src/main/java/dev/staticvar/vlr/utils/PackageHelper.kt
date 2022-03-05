package dev.staticvar.vlr.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dev.staticvar.vlr.AppInstallerService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PackageHelper {

  private fun install(path: String, context: Context) {
    val intentFlags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    val callbackIntent = Intent(context, AppInstallerService::class.java)
    val pendingIntent = PendingIntent.getService(context, 0, callbackIntent, intentFlags)
    val packageInstaller = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
    }
    val sessionId: Int
    var session: PackageInstaller.Session? = null
    try {
      sessionId = packageInstaller.createSession(params)
      session = packageInstaller.openSession(sessionId)
      FileInputStream(path).use { inputStream ->
        session.openWrite("install", 0, -1).use { stream ->
          val buffer = ByteArray(65536)
          var length: Int
          while (inputStream.read(buffer).also { length = it } > 0) {
            stream.write(buffer, 0, length)
          }
          session.fsync(stream)
        }
      }
      session.commit(pendingIntent.intentSender)
    } catch (e: Exception) {
      e.stackTraceToString()
    } finally {
      session?.close()
    }
  }

  fun convertByteToFileAndInstall(context: Context, byteArray: ByteArray) {
    val path = context.filesDir
    val tempFile = File.createTempFile("update", ".apk", path)
    FileOutputStream(tempFile).use { it.write(byteArray) }
    install(tempFile.absolutePath, context)
  }
}
