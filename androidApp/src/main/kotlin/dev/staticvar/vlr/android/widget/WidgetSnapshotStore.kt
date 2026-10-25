/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

internal object WidgetSnapshotStore {
  private const val LOG_TAG = "UpcomingWidget"
  private const val FILE_NAME = "upcoming_matches_widget.json"
  private val writeLock = Any()
  private val revisionFlow = MutableStateFlow(0L)

  /** Bumps after every write, so open widget sessions know to read the snapshot again. */
  val revisions: StateFlow<Long> = revisionFlow.asStateFlow()

  suspend fun writeIfChanged(context: Context, json: String): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val bytes = json.toByteArray(Charsets.UTF_8)
        synchronized(writeLock) {
          val atomicFile = AtomicFile(context.getFileStreamPath(FILE_NAME))
          val existing = atomicFile.readBytesOrNull()
          if (existing?.contentEquals(bytes) == true) {
            return@synchronized false
          }

          atomicFile.write(bytes)
          revisionFlow.update { it + 1 }
          true
        }
      } catch (error: IOException) {
        Log.w(LOG_TAG, "Could not save the widget snapshot", error)
        false
      }
    }

  suspend fun replaceIfUnchanged(
    context: Context,
    expectedJson: String,
    replacementJson: String,
  ): SnapshotReplaceResult = withContext(Dispatchers.IO) {
    try {
      synchronized(writeLock) {
        val atomicFile = AtomicFile(context.getFileStreamPath(FILE_NAME))
        val current = atomicFile.readBytesOrNull()?.toString(Charsets.UTF_8)
        when {
          current != expectedJson -> SnapshotReplaceResult.STALE
          current == replacementJson -> SnapshotReplaceResult.UNCHANGED
          else -> {
            atomicFile.write(replacementJson.toByteArray(Charsets.UTF_8))
            revisionFlow.update { it + 1 }
            SnapshotReplaceResult.WRITTEN
          }
        }
      }
    } catch (error: IOException) {
      Log.w(LOG_TAG, "Could not replace the widget snapshot", error)
      SnapshotReplaceResult.FAILED
    }
  }

  fun read(context: Context): String? = synchronized(writeLock) {
    val atomicFile = AtomicFile(context.getFileStreamPath(FILE_NAME))
    try {
      atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
    } catch (error: IOException) {
      if (error !is FileNotFoundException) {
        Log.w(LOG_TAG, "Could not read the widget snapshot", error)
      }
      null
    }
  }

  private fun AtomicFile.readBytesOrNull(): ByteArray? = try {
    openRead().use { input -> input.readBytes() }
  } catch (_: FileNotFoundException) {
    null
  }

  private fun AtomicFile.write(bytes: ByteArray) {
    val output = startWrite()
    try {
      output.write(bytes)
      finishWrite(output)
    } catch (error: IOException) {
      failWrite(output)
      throw error
    }
  }
}

internal enum class SnapshotReplaceResult {
  WRITTEN,
  UNCHANGED,
  STALE,
  FAILED,
}
